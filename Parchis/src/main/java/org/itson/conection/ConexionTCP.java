package org.itson.conection;

import org.itson.conection.auxiliar.PaqueteRed;
import org.itson.conection.auxiliar.TipoPaquete;

import java.io.*;
import java.net.Socket;

public class ConexionTCP {
    private String ipRemota;
    private int puerto;
    private int idJugadorAsociado;
    private boolean estaActivo;
    private long ultimoHeartbeat;

    private Socket socket;
    private BufferedReader entrada;
    private PrintWriter salida;

    public ConexionTCP(Socket socket) throws IOException {
        this.socket = socket;
        this.ipRemota = socket.getInetAddress().getHostAddress();
        this.puerto = socket.getPort();
        this.estaActivo = true;
        this.entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.salida = new PrintWriter(socket.getOutputStream(), true);
        this.ultimoHeartbeat = System.currentTimeMillis();
    }

    public void enviarPaquete(PaqueteRed paquete) {
        if (estaActivo && salida != null) {
            salida.println(paquete.serializar());
        }
    }

    public PaqueteRed recibirPaquete() throws IOException {
        if (!estaActivo) {
            return null;
        }
        String dato = entrada.readLine();
        if (dato != null) {
            return deserializar(dato);
        } else {
            cerrarConexion();
            return null;
        }
    }

    public PaqueteRed deserializar(String dato) {
        try {
            if (dato == null || dato.trim().isEmpty()) return null;

            String[] partes = dato.split(";");
            if (partes.length < 3) return null;

            return new PaqueteRed(
                    TipoPaquete.valueOf(partes[0]),
                    Integer.parseInt(partes[1]),
                    partes[2]
            );
        } catch (Exception e) {
            System.err.println("Error deserializando paquete recibido (" + dato + "): " + e.getMessage());
            return null;
        }
    }

    public void cerrarConexion() {
        if (!this.estaActivo) {
            return;
        }
        this.estaActivo = false;
        try {
            if (entrada != null) entrada.close();
            if (salida != null) salida.close();
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Error al intentar cerrar la conexión TCP: " + e.getMessage());
        }
    }

    public String getIpRemota() {
        return ipRemota;
    }

    public void setIpRemota(String ipRemota) {
        this.ipRemota = ipRemota;
    }

    public int getPuerto() {
        return puerto;
    }

    public void setPuerto(int puerto) {
        this.puerto = puerto;
    }

    public int getIdJugadorAsociado() {
        return idJugadorAsociado;
    }

    public void setIdJugadorAsociado(int idJugadorAsociado) {
        this.idJugadorAsociado = idJugadorAsociado;
    }

    public boolean isEstaActivo() {
        return estaActivo;
    }

    public void setEstaActivo(boolean estaActivo) {
        this.estaActivo = estaActivo;
    }

    public long getUltimoHeartbeat() {
        return ultimoHeartbeat;
    }

    public void actualizarHeartbeat() {
        this.ultimoHeartbeat = System.currentTimeMillis();
    }

    public void setUltimoHeartbeat(long ultimoHeartbeat) {
        this.ultimoHeartbeat = ultimoHeartbeat;
    }
}