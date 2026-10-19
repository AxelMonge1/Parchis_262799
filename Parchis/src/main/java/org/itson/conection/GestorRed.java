package org.itson.conection;
import org.itson.conection.auxiliar.PaqueteRed;
import org.itson.conection.auxiliar.TipoPaquete;
import org.itson.controller.ControladorJuego;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Administra las conexiones de red TCP, distingue roles (Host/Cliente),
 * emite latidos (HEARTBEAT) periódicos y enruta los paquetes hacia el Controlador.
 */
public class GestorRed {

    private ServerSocket serverSocket;
    private final List<ConexionTCP> conexiones = new ArrayList<>();
    private ControladorJuego controlador;

    private boolean esHost;
    private int puertoEscucha;
    private int idJugadorLocal;

    public GestorRed() {
        this.esHost = false;
        this.idJugadorLocal = 0;
    }

    /**
     * Inicializa la máquina actual como Host (Servidor) e inicia la escucha.
     */
    public boolean iniciarServidor(int puerto) {
        this.esHost = true;
        this.puertoEscucha = puerto;
        this.idJugadorLocal = 1; // El Host siempre es el Jugador 1

        new Thread(this::escucharConexiones).start();
        iniciarEmisorHeartbeat();
        return true;
    }

    /**
     * Conecta la máquina actual como Cliente al Host remoto.
     */
    public boolean conectarAHost(String ip, int puerto) {
        this.esHost = false;
        try {
            Socket socket = new Socket(ip, puerto);
            ConexionTCP nuevaConexion = new ConexionTCP(socket);

            synchronized (conexiones) {
                this.conexiones.add(nuevaConexion);
            }

            procesarPaquetesEntrantes(nuevaConexion);
            iniciarEmisorHeartbeat();
            return true;
        } catch (IOException e) {
            System.err.println("Error al conectar con el Host: " + e.getMessage());
            return false;
        }
    }

    /**
     * Bucle del servidor que acepta nuevos clientes, les asigna un ID
     * y notifica al controlador para evaluar capacidad de la sala.
     */
    public void escucharConexiones() {
        try {
            this.serverSocket = new ServerSocket(puertoEscucha);
            System.out.println("Servidor escuchando en el puerto " + puertoEscucha);

            while (esHost) {
                Socket socketCliente = serverSocket.accept();
                ConexionTCP nuevaConexion = new ConexionTCP(socketCliente);

                int idAsignado;
                synchronized (conexiones) {
                    conexiones.add(nuevaConexion);
                    idAsignado = conexiones.size() + 1; // Host=1, Clientes=2, 3, 4
                    nuevaConexion.setIdJugadorAsociado(idAsignado);
                }

                procesarPaquetesEntrantes(nuevaConexion);

                // Notifica al cliente conectado cuál es su ID asignado
                PaqueteRed paqueteId = new PaqueteRed(
                        TipoPaquete.LOBBY_CONFIRM,
                        1,
                        "ASIGNAR_ID:" + idAsignado
                );
                nuevaConexion.enviarPaquete(paqueteId);

                if (controlador != null) {
                    controlador.notificarNuevoJugadorConectado();
                }
            }
        } catch (IOException e) {
            if (!esHost) {
                System.out.println("Servidor detenido intencionalmente.");
            } else {
                System.err.println("Error en el servidor TCP: " + e.getMessage());
            }
        }
    }

    /**
     * Escucha y enruta los paquetes entrantes de una conexión TCP en un hilo dedicado.
     */
    public void procesarPaquetesEntrantes(ConexionTCP conexion) {
        new Thread(() -> {
            try {
                while (conexion.isEstaActivo()) {
                    PaqueteRed paquete = conexion.recibirPaquete();

                    if (paquete == null) {
                        break; // Socket cerrado o desconectado
                    }

                    // 1. Renueva el timestamp de vida con cualquier paquete recibido
                    conexion.setUltimoHeartbeat(System.currentTimeMillis());

                    // 2. Respuesta automática a Heartbeats (PING -> PONG) a nivel de red
                    if (paquete.getTipo() == TipoPaquete.HEARTBEAT) {
                        if ("PING".equalsIgnoreCase(paquete.getMensaje())) {
                            conexion.enviarPaquete(new PaqueteRed(
                                    TipoPaquete.HEARTBEAT,
                                    idJugadorLocal,
                                    "PONG"
                            ));
                        }
                        continue;
                    }

                    // 3. Registro de ID asignado al cliente
                    if (paquete.getTipo() == TipoPaquete.LOBBY_CONFIRM
                            && paquete.getMensaje().startsWith("ASIGNAR_ID:")) {
                        int miId = Integer.parseInt(paquete.getMensaje().split(":")[1]);
                        this.idJugadorLocal = miId;
                        System.out.println("ID asignado por el Host: Jugador " + miId);
                    }

                    // 4. Delegación de reglas y acciones al ControladorJuego
                    if (controlador != null) {
                        try {
                            switch (paquete.getTipo()) {
                                case ACCION:
                                    if ("TIRAR_DADO".equalsIgnoreCase(paquete.getMensaje())) {
                                        controlador.procesarTiradaDado();
                                    } else if (paquete.getMensaje().startsWith("MOVER_FICHA:")) {
                                        int idFicha = Integer.parseInt(paquete.getMensaje().split(":")[1]);
                                        controlador.procesarAccionJugador(paquete.getIdEmisor(), idFicha);
                                    }
                                    break;

                                case SYNC_ESTADO:
                                    if (paquete.getMensaje() != null && !"null".equalsIgnoreCase(paquete.getMensaje())) {
                                        // 📍 Aquí se consume el método de actualización remota
                                        controlador.aplicarSincronizacionRemota(paquete.getMensaje());
                                    }
                                    break;

                                case LOBBY_CONFIRM:
                                    String msg = paquete.getMensaje();
                                    if (msg.startsWith("JUGADOR_LISTO:")) {
                                        int idListo = Integer.parseInt(msg.split(":")[1]);
                                        if (esHost) {
                                            controlador.procesarJugadorListo(idListo);
                                            enviarPaqueteATodos(paquete);
                                        } else {
                                            controlador.procesarJugadorListo(idListo);
                                        }
                                    } else if ("INICIAR_JUEGO".equalsIgnoreCase(msg)) {
                                        controlador.comenzarPartidaOficial();
                                    }
                                    break;

                                default:
                                    break;
                            }
                        } catch (Exception ex) {
                            System.err.println("Error procesando paquete de juego: " + ex.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Conexión perdida con socket: " + e.getMessage());
            } finally {
                conexion.cerrarConexion();
                removerConexion(conexion);
            }
        }).start();
    }

    /**
     * Emite un paquete a todas las conexiones TCP activas.
     */
    public void enviarPaqueteATodos(PaqueteRed paquete) {
        synchronized (conexiones) {
            for (ConexionTCP conexion : conexiones) {
                if (conexion.isEstaActivo()) {
                    conexion.enviarPaquete(paquete);
                }
            }
        }
    }

    /**
     * Remueve una conexión inactiva de la lista compartida.
     */
    public void removerConexion(ConexionTCP conexion) {
        synchronized (conexiones) {
            conexiones.remove(conexion);
        }
    }

    /**
     * Hilo en segundo plano que envía un paquete PING cada 2 segundos.
     */
    public void iniciarEmisorHeartbeat() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(2000);
                    PaqueteRed ping = new PaqueteRed(
                            TipoPaquete.HEARTBEAT,
                            idJugadorLocal,
                            "PING"
                    );
                    enviarPaqueteATodos(ping);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }

    /**
     * Apaga el servidor y cierra todas las conexiones.
     */
    public void cerrarServidor() {
        this.esHost = false;
        synchronized (conexiones) {
            for (ConexionTCP conexion : conexiones) {
                conexion.cerrarConexion();
            }
            conexiones.clear();
        }
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error al cerrar ServerSocket: " + e.getMessage());
        }
    }

    // --- Getters y Setters ---

    public boolean isEsHost() {
        return esHost;
    }

    public void setEsHost(boolean esHost) {
        this.esHost = esHost;
    }

    public int getIdJugadorLocal() {
        return idJugadorLocal;
    }

    public void setIdJugadorLocal(int idJugadorLocal) {
        this.idJugadorLocal = idJugadorLocal;
    }

    public List<ConexionTCP> getConexiones() {
        return conexiones;
    }

    public ControladorJuego getControlador() {
        return controlador;
    }

    public void setControlador(ControladorJuego controlador) {
        this.controlador = controlador;
    }
}