package org.itson.conection;

import org.itson.conection.auxiliar.PaqueteRed;
import org.itson.conection.auxiliar.TipoPaquete;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ConexionTCP {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private boolean estaActivo;
    private int idJugadorAsociado;
    private long ultimoHeartbeat;

    public ConexionTCP(Socket socket) {
        this.socket = socket;
        this.estaActivo = true;
        this.ultimoHeartbeat = System.currentTimeMillis();

        try {
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            System.err.println("Error al inicializar flujos I/O en socket: " + e.getMessage());
            cerrarConexion();
        }
    }

    public synchronized void enviarPaquete(PaqueteRed paquete) {
        if (estaActivo && out != null && paquete != null) {
            String dato = paquete.serializar();
            System.out.println("Enviando paquete: " + dato);
            out.println(dato);
            out.flush();
        }
    }

    public PaqueteRed recibirPaquete() {
        try {
            if (in != null && estaActivo) {
                String linea = in.readLine();

                if (linea != null && !linea.trim().isEmpty()) {
                    System.out.println("Paquete recibido: " + linea);
                    return deserializar(linea);
                }
            }
        } catch (IOException e) {
        }
        return null;
    }

    public PaqueteRed deserializar(String dato) {
        try {
            if (dato == null || dato.trim().isEmpty()) return null;

            String[] partes = dato.split(";", 3);
            if (partes.length < 3) return null;

            TipoPaquete tipo = TipoPaquete.valueOf(partes[0]);
            int idEmisor = Integer.parseInt(partes[1]);
            String mensaje = partes[2];

            return new PaqueteRed(tipo, idEmisor, mensaje);
        } catch (Exception e) {
            System.err.println("Error deserializando paquete recibido (" + dato + "): " + e.getMessage());
            return null;
        }
    }

    public void cerrarConexion() {
        this.estaActivo = false;
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("Error al cerrar conexión TCP: " + e.getMessage());
        }
    }

    public boolean isEstaActivo() {
        return estaActivo;
    }

    public void setEstaActivo(boolean estaActivo) {
        this.estaActivo = estaActivo;
    }

    public int getIdJugadorAsociado() {
        return idJugadorAsociado;
    }

    public void setIdJugadorAsociado(int idJugadorAsociado) {
        this.idJugadorAsociado = idJugadorAsociado;
    }

    public long getUltimoHeartbeat() {
        return ultimoHeartbeat;
    }

    public void setUltimoHeartbeat(long ultimoHeartbeat) {
        this.ultimoHeartbeat = ultimoHeartbeat;
    }

    public Socket getSocket() {
        return socket;
    }
}