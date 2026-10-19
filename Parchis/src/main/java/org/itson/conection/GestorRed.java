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
    private boolean esHost;
    private int puertoEscucha;
    private List<ConexionTCP> conexiones;
    private int idJugadorLocal;
    private ControladorJuego controlador;
    private ServerSocket serverSocket;

    public GestorRed() {
        this.conexiones = new ArrayList<>();
        this.esHost = false;
    }

    public boolean iniciarServidor(int puerto) {
        this.esHost = true;
        this.puertoEscucha = puerto;
        this.idJugadorLocal = 1;
        // Hilo secundario para escuchar clientes
        new Thread(this::escucharConexiones).start();
        iniciarEmisorHeartbeat();
        return true;
    }

    public boolean conectarAHost(String ip, int puerto) {
        this.esHost = false;
        try {
            Socket socket = new Socket(ip, puerto);
            ConexionTCP nuevaConexion = new ConexionTCP(socket);
            synchronized (conexiones) {
                this.conexiones.add(nuevaConexion);
            }
            // Inicia la escucha de paquetes entrantes enviados por el servidor
            procesarPaquetesEntrantes(nuevaConexion);
            return true;
        } catch (IOException e) {
            System.err.println("Error al conectar con el Host: " + e.getMessage());
            return false;
        }
    }
    public void removerConexion(ConexionTCP conexion) {
        synchronized (conexiones) {
            conexiones.remove(conexion);
        }
    }

    public void escucharConexiones() {
        try {
            this.serverSocket = new ServerSocket(puertoEscucha);
            System.out.println("Servidor a la escucha en el puerto " + puertoEscucha);

            while (esHost) {
                Socket socketCliente = serverSocket.accept();
                ConexionTCP nuevaConexion = new ConexionTCP(socketCliente);

                int idAsignado;
                synchronized (conexiones) {
                    conexiones.add(nuevaConexion);
                    idAsignado = conexiones.size() + 1; // Host es 1, Clientes son 2, 3, 4
                    nuevaConexion.setIdJugadorAsociado(idAsignado);
                }

                procesarPaquetesEntrantes(nuevaConexion);

                // 📍 NUEVO: Le enviamos un paquete al Cliente indicándole su ID de jugador
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

    public void procesarPaquetesEntrantes(ConexionTCP conexion) {
        new Thread(() -> {
            try {
                while (conexion.isEstaActivo()) {
                    PaqueteRed paquete = conexion.recibirPaquete();

                    if (paquete == null) {
                        break; // Cliente desconectado o socket cerrado
                    }

                    // 1. Cualquier paquete válido recibido reinicia el temporizador de vida
                    conexion.setUltimoHeartbeat(System.currentTimeMillis());

                    // 2. Procesamiento aislado de acciones (un error aquí NO cierra la conexión)
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
                                    controlador.notificarCambioEstado();
                                    break;

                                case HEARTBEAT:
                                    break;

                                case LOBBY_CONFIRM:
                                    String msg = paquete.getMensaje();

                                    if (msg.startsWith("ASIGNAR_ID:")) {
                                        int miId = Integer.parseInt(msg.split(":")[1]);
                                        setIdJugadorLocal(miId); // El cliente guarda su ID oficial (ej. 2)
                                        System.out.println("ID asignado por el Host: Jugador " + miId);
                                    }
                                    else if (msg.startsWith("JUGADOR_LISTO:")) {
                                        int idListo = Integer.parseInt(msg.split(":")[1]);
                                        if (esHost) {
                                            controlador.procesarJugadorListo(idListo);
                                            enviarPaqueteATodos(paquete);
                                        } else {
                                            controlador.procesarJugadorListo(idListo);
                                        }
                                    }
                                    else if ("INICIAR_JUEGO".equalsIgnoreCase(msg)) {
                                        controlador.comenzarPartidaOficial();
                                    }
                                    break;
                            }
                        } catch (Exception ex) {
                            System.err.println("Error procesando acción de juego: " + ex.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Conexión perdida con socket: " + e.getMessage());
            } finally {
                // Se ejecuta SOLO al perder el socket realmente
                conexion.cerrarConexion();
                removerConexion(conexion); // <--- Rompe el bucle infinito en la terminal
            }
        }).start();
    }

    public void enviarPaqueteATodos(PaqueteRed paquete) {
        synchronized (conexiones) {
            for (ConexionTCP conexion : conexiones) {
                conexion.enviarPaquete(paquete);
            }
        }
    }

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

    public boolean isEsHost() { return esHost; }

    public void setEsHost(boolean esHost) { this.esHost = esHost; }

    public int getPuertoEscucha() { return puertoEscucha; }

    public void setPuertoEscucha(int puertoEscucha) { this.puertoEscucha = puertoEscucha; }

    public List<ConexionTCP> getConexiones() { return conexiones; }

    public int getIdJugadorLocal() { return idJugadorLocal; }

    public void setIdJugadorLocal(int idJugadorLocal) { this.idJugadorLocal = idJugadorLocal; }

    public ControladorJuego getControlador() { return controlador; }

    public void setControlador(ControladorJuego controlador) { this.controlador = controlador; }
}