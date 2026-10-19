package org.itson.conection;

import org.itson.conection.auxiliar.PaqueteRed;
import org.itson.conection.auxiliar.TipoPaquete;
import org.itson.controller.ControladorJuego;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class GestorRed {

    private ServerSocket serverSocket;
    private final List<ConexionTCP> conexiones = new ArrayList<>();
    private ControladorJuego controlador;

    private boolean esHost;
    private int puertoEscucha;
    private int idJugadorLocal;
    private volatile boolean activo;

    public GestorRed() {
        this.esHost = false;
        this.idJugadorLocal = 0;
        this.activo = true;
    }

    public boolean iniciarServidor(int puerto) {
        this.esHost = true;
        this.puertoEscucha = puerto;
        this.idJugadorLocal = 1;
        this.activo = true;

        new Thread(this::escucharConexiones).start();
        iniciarEmisorHeartbeat();
        return true;
    }

    public boolean conectarAHost(String ip, int puerto) {
        this.esHost = false;
        this.activo = true;
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

    public void escucharConexiones() {
        try {
            this.serverSocket = new ServerSocket(puertoEscucha);
            System.out.println("Servidor escuchando en el puerto " + puertoEscucha);

            while (esHost && activo) {
                Socket socketCliente = serverSocket.accept();
                ConexionTCP nuevaConexion = new ConexionTCP(socketCliente);

                int idAsignado;
                synchronized (conexiones) {
                    conexiones.add(nuevaConexion);
                    idAsignado = conexiones.size() + 1;
                    nuevaConexion.setIdJugadorAsociado(idAsignado);
                }

                procesarPaquetesEntrantes(nuevaConexion);

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
            if (!esHost || !activo) {
                System.out.println("Servidor detenido intencionalmente.");
            } else {
                System.err.println("Error en el servidor TCP: " + e.getMessage());
            }
        }
    }

    public void procesarPaquetesEntrantes(ConexionTCP conexion) {
        new Thread(() -> {
            try {
                while (conexion.isEstaActivo() && activo) {
                    PaqueteRed paquete = conexion.recibirPaquete();

                    if (paquete == null) {
                        break;
                    }

                    conexion.setUltimoHeartbeat(System.currentTimeMillis());

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

                    String mensaje = paquete.getMensaje();

                    if (paquete.getTipo() == TipoPaquete.LOBBY_CONFIRM
                            && mensaje != null && mensaje.startsWith("ASIGNAR_ID:")) {
                        int miId = Integer.parseInt(mensaje.split(":")[1]);
                        this.idJugadorLocal = miId;
                        System.out.println("ID asignado por el Host: Jugador " + miId);
                    }

                    if (controlador != null && mensaje != null) {
                        try {
                            switch (paquete.getTipo()) {
                                case ACCION:
                                    if ("TIRAR_DADO".equalsIgnoreCase(mensaje)) {
                                        controlador.procesarTiradaDado();
                                    } else if (mensaje.startsWith("MOVER_FICHA:")) {
                                        int idFicha = Integer.parseInt(mensaje.split(":")[1]);
                                        controlador.procesarAccionJugador(paquete.getIdEmisor(), idFicha);
                                    }
                                    break;

                                case SYNC_ESTADO:
                                    if (!"null".equalsIgnoreCase(mensaje) && !mensaje.trim().isEmpty()) {
                                        controlador.aplicarSincronizacionRemota(mensaje);
                                    }
                                    break;

                                case LOBBY_CONFIRM:
                                    if (mensaje.startsWith("JUGADOR_LISTO:")) {
                                        int idListo = Integer.parseInt(mensaje.split(":")[1]);
                                        if (esHost) {
                                            controlador.procesarJugadorListo(idListo);
                                            enviarPaqueteATodos(paquete);
                                        } else {
                                            controlador.procesarJugadorListo(idListo);
                                        }
                                    } else if ("INICIAR_JUEGO".equalsIgnoreCase(mensaje)) {
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

    public void enviarPaqueteATodos(PaqueteRed paquete) {
        synchronized (conexiones) {
            for (ConexionTCP conexion : conexiones) {
                if (conexion.isEstaActivo()) {
                    conexion.enviarPaquete(paquete);
                }
            }
        }
    }

    public void removerConexion(ConexionTCP conexion) {
        synchronized (conexiones) {
            conexiones.remove(conexion);
        }
    }

    public void iniciarEmisorHeartbeat() {
        new Thread(() -> {
            while (activo) {
                try {
                    Thread.sleep(2000);
                    if (!activo) break;

                    PaqueteRed ping = new PaqueteRed(
                            TipoPaquete.HEARTBEAT,
                            idJugadorLocal,
                            "PING"
                    );
                    enviarPaqueteATodos(ping);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }).start();
    }

    public void cerrarServidor() {
        this.activo = false;
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