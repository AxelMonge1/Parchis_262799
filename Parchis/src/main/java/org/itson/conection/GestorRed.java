package org.itson.conection;

import org.itson.conection.auxiliar.PaqueteRed;
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

    public void escucharConexiones() {
        try {
            this.serverSocket = new ServerSocket(puertoEscucha);
            System.out.println("Servidor a la escucha en el puerto " + puertoEscucha);
            while (esHost) {
                Socket socketCliente = serverSocket.accept();
                ConexionTCP nuevaConexion = new ConexionTCP(socketCliente);

                synchronized (conexiones) {
                    conexiones.add(nuevaConexion);
                }
                procesarPaquetesEntrantes(nuevaConexion);
            }
        } catch (IOException e) {
            if (!esHost) {
                System.out.println("Servidor detenido intencionalmente.");
            } else {
                System.err.println("Error inesperado en ServerSocket: " + e.getMessage());
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
                        break;
                    }
                    conexion.actualizarHeartbeat();
                    if (controlador != null) {
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
                                System.out.println("Lobby confirmado: " + paquete.getMensaje());
                                break;
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error procesando mensajes de red: " + e.getMessage());
            } finally {
                conexion.cerrarConexion();
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