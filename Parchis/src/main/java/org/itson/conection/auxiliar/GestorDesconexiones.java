package org.itson.conection.auxiliar;

import org.itson.conection.ConexionTCP;
import org.itson.conection.GestorRed;

import java.util.ArrayList;
import java.util.List;

public class GestorDesconexiones {
    private List<Integer> ordenPrioridadHost;
    private int timeoutMs;

    private GestorRed gestorRed;

    public GestorDesconexiones(int timeoutMs, GestorRed gestorRed) {
        this.timeoutMs = timeoutMs;
        this.ordenPrioridadHost = new ArrayList<>();
        this.gestorRed = gestorRed;
    }

    public void monitorearHeartbeat() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(timeoutMs);
                    long tiempoActual = System.currentTimeMillis();

                    List<ConexionTCP> conexionesActivas = new ArrayList<>(gestorRed.getConexiones());

                    for (ConexionTCP conexion : conexionesActivas) {
                        // Verificar únicamente sockets que sigan marcados como activos
                        if (conexion.isEstaActivo() && (tiempoActual - conexion.getUltimoHeartbeat() > timeoutMs)) {
                            System.out.println("Timeout detectado para jugador: " + conexion.getIdJugadorAsociado());
                            conexion.cerrarConexion();
                            gestorRed.removerConexion(conexion); // Limpia la lista
                            manejarDesconexion(conexion.getIdJugadorAsociado());
                        }
                    }

                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }

    public void manejarDesconexion(int idJugador) { //[cite: 3]
        System.out.println("El jugador " + idJugador + " se ha desconectado.");
        ordenPrioridadHost.remove(Integer.valueOf(idJugador));
        if (eraElHostActual(idJugador) && !ordenPrioridadHost.isEmpty()) {
            int nuevoHostId = ordenPrioridadHost.get(0);
            reasignarHost(nuevoHostId);
        }
    }

    public void reasignarHost(int nuevoHostId) {
        System.out.println("Reasignando el Host al jugador ID: " + nuevoHostId);
        PaqueteRed paqueteMigracion = new PaqueteRed(
                TipoPaquete.LOBBY_CONFIRM,
                -1,
                "ERES_NUEVO_HOST"
        );

        if (gestorRed != null) {
            for (ConexionTCP conexion : gestorRed.getConexiones()) {
                if (conexion.getIdJugadorAsociado() == nuevoHostId) {
                    conexion.enviarPaquete(paqueteMigracion);
                    break;
                }
            }
        }
    }

    private boolean eraElHostActual(int idJugador) {
        return gestorRed != null && gestorRed.getIdJugadorLocal() == idJugador;
    }

    public void agregarPrioridad(int idJugador) {
        if (!ordenPrioridadHost.contains(idJugador)) {
            ordenPrioridadHost.add(idJugador); //[cite: 3]
        }
    }

    public List<Integer> getOrdenPrioridadHost() { return ordenPrioridadHost; }

    public void setOrdenPrioridadHost(List<Integer> ordenPrioridadHost) { this.ordenPrioridadHost = ordenPrioridadHost; }

    public int getTimeoutMs() { return timeoutMs; }

    public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
}