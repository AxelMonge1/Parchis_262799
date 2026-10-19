package org.itson.controller;

import org.itson.conection.GestorRed;
import org.itson.conection.auxiliar.PaqueteRed;
import org.itson.conection.auxiliar.TipoPaquete;
import org.itson.model.ModeloJuego;
import org.itson.view.VistaJuego;

import javax.swing.SwingUtilities;
import java.util.HashSet;
import java.util.Set;

public class ControladorJuego {
    private ModeloJuego modelo;
    private VistaJuego vista;
    private GestorRed gestorRed;
    private Set<Integer> jugadoresListos = new HashSet<>();
    private static final int CAPACIDAD_MAXIMA_SALA = 4;

    public ControladorJuego(ModeloJuego modelo, VistaJuego vista, GestorRed gestorRed) {
        this.modelo = modelo;
        this.vista = vista;
        this.gestorRed = gestorRed;
    }

    public int getTotalJugadoresConectados() {
        if (gestorRed == null) return 1;
        return gestorRed.getConexiones().size() + 1;
    }

    public void procesarTiradaDado() {
        int idLocal = gestorRed.getIdJugadorLocal();

        if (modelo.getTurnoActual() != idLocal) {
            vista.mostrarMensaje("No es tu turno para lanzar el dado.");
            return;
        }

        if (modelo.isDadoLanzado()) {
            vista.mostrarMensaje("Ya lanzaste el dado. Debes seleccionar una ficha.");
            return;
        }

        int resultado = modelo.getDado().lanzar();
        modelo.setDadoLanzado(true);

        System.out.println("Jugador " + idLocal + " lanzó el dado: " + resultado);
        vista.mostrarResultadoDado(resultado);

        if (!modelo.tieneMovimientosValidos(idLocal)) {
            vista.mostrarMensaje("Sin movimientos válidos con un " + resultado + ". Pasa el turno.");

            modelo.cambiarTurno();
            int nuevoTurno = modelo.getTurnoActual();

            vista.mostrarTurnoActual(nuevoTurno);
            vista.setBotonDadoHabilitado(idLocal == nuevoTurno);

            transmitirCambioEstadoLocal();
            return;
        }

        vista.setBotonDadoHabilitado(false);
        transmitirCambioEstadoLocal();
    }

    public void procesarAccionJugador(int idJugador, int idFicha) {
        int miIdLocal = gestorRed.getIdJugadorLocal();

        if (idJugador != modelo.getTurnoActual() || idJugador != miIdLocal) {
            System.out.println("Intento de movimiento fuera de turno.");
            return;
        }

        if (!modelo.isDadoLanzado()) {
            vista.mostrarMensaje("Debes lanzar el dado antes de mover una ficha.");
            return;
        }

        int pasos = modelo.getDado().getValorActual();

        boolean movimientoExitoso = modelo.moverFicha(idFicha, pasos);

        if (movimientoExitoso) {
            if (pasos == 6) {
                modelo.setDadoLanzado(false);
                vista.mostrarMensaje("¡Sacaste 6! Vuelves a lanzar el dado.");
            } else {
                modelo.cambiarTurno();
            }
        } else {
            vista.mostrarMensaje("Movimiento no válido para la ficha " + idFicha + ". Necesitas un " + modelo.getNumSalida() + " para salir de casa.");
            return;
        }

        vista.renderizarTablero();
        int turnoActual = modelo.getTurnoActual();

        vista.mostrarTurnoActual(turnoActual);
        vista.setBotonDadoHabilitado(miIdLocal == turnoActual);

        transmitirCambioEstadoLocal();
    }

    public void transmitirCambioEstadoLocal() {
        String estadoSerializado = modelo.obtenerEstadoSerializado();

        if (estadoSerializado == null || estadoSerializado.trim().isEmpty() || "null".equalsIgnoreCase(estadoSerializado)) {
            return;
        }

        PaqueteRed paquete = new PaqueteRed(
                TipoPaquete.SYNC_ESTADO,
                gestorRed.getIdJugadorLocal(),
                estadoSerializado
        );

        if (gestorRed.isEsHost()) {
            gestorRed.enviarPaqueteATodos(paquete);
        } else if (!gestorRed.getConexiones().isEmpty()) {
            gestorRed.getConexiones().get(0).enviarPaquete(paquete);
        }
    }

    public void aplicarSincronizacionRemota(String datosEstado) {
        modelo.actualizarDesdeRed(datosEstado);

        SwingUtilities.invokeLater(() -> {
            vista.renderizarTablero();

            int turnoActual = modelo.getTurnoActual();
            int miIdLocal = gestorRed.getIdJugadorLocal();

            vista.mostrarTurnoActual(turnoActual);
            vista.setBotonDadoHabilitado(miIdLocal == turnoActual);
        });
    }

    public synchronized void presionarBotonListo() {
        int miId = gestorRed.getIdJugadorLocal();

        if (miId == 0) {
            System.err.println("Aún no se ha asignado un ID de jugador para esta conexión.");
            return;
        }

        PaqueteRed paquete = new PaqueteRed(
                TipoPaquete.LOBBY_CONFIRM,
                miId,
                "JUGADOR_LISTO:" + miId
        );

        if (gestorRed.isEsHost()) {
            procesarJugadorListo(miId);
            gestorRed.enviarPaqueteATodos(paquete);
        } else if (!gestorRed.getConexiones().isEmpty()) {
            gestorRed.getConexiones().get(0).enviarPaquete(paquete);
        }
    }

    public synchronized void procesarJugadorListo(int idJugador) {
        jugadoresListos.add(idJugador);
        int conectados = getTotalJugadoresConectados();

        System.out.println("Jugador " + idJugador + " listo (" + jugadoresListos.size() + "/" + conectados + ")");

        if (gestorRed.isEsHost()) {
            verificarInicioPartida();
        }
    }

    public synchronized void notificarNuevoJugadorConectado() {
        if (!gestorRed.isEsHost()) return;

        int conectados = getTotalJugadoresConectados();
        System.out.println("Nuevo jugador unido. Total: " + conectados);

        if (conectados >= CAPACIDAD_MAXIMA_SALA) {
            dispararInicioPartidaGlobal();
        }
    }

    private void verificarInicioPartida() {
        int conectados = getTotalJugadoresConectados();
        if (conectados >= 2 && jugadoresListos.size() >= conectados) {
            dispararInicioPartidaGlobal();
        }
    }

    private void dispararInicioPartidaGlobal() {
        int conectados = getTotalJugadoresConectados();
        modelo.setTotalJugadores(conectados);

        if (modelo.getTurnoActual() <= 0) {
            modelo.setTurnoActual(1);
        }

        PaqueteRed paqueteStart = new PaqueteRed(
                TipoPaquete.LOBBY_CONFIRM,
                gestorRed.getIdJugadorLocal(),
                "INICIAR_JUEGO"
        );

        gestorRed.enviarPaqueteATodos(paqueteStart);
        transmitirCambioEstadoLocal();
        comenzarPartidaOficial();
    }

    public void comenzarPartidaOficial() {
        int miId = gestorRed.getIdJugadorLocal();
        int conectados = getTotalJugadoresConectados();
        modelo.setTotalJugadores(conectados);

        if (modelo.getTurnoActual() <= 0) {
            modelo.setTurnoActual(1);
        }

        int turnoInicial = modelo.getTurnoActual();
        vista.prepararInterfazParaInicio(miId, turnoInicial);

        System.out.println("¡Partida Iniciada! Mi ID: " + miId + " | Jugadores en Sala: " + conectados + " | Turno Inicial: " + turnoInicial);
    }

    public ModeloJuego getModelo() { return modelo; }
    public void setModelo(ModeloJuego modelo) { this.modelo = modelo; }
    public VistaJuego getVista() { return vista; }
    public void setVista(VistaJuego vista) { this.vista = vista; }
    public GestorRed getGestorRed() { return gestorRed; }
    public void setGestorRed(GestorRed gestorRed) { this.gestorRed = gestorRed; }
}