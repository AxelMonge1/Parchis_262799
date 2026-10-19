package org.itson.controller;

import org.itson.conection.GestorRed;
import org.itson.conection.auxiliar.PaqueteRed;
import org.itson.conection.auxiliar.TipoPaquete;
import org.itson.model.ModeloJuego;
import org.itson.view.VistaJuego;

import java.util.HashSet;
import java.util.Set;

public class ControladorJuego {
    private ModeloJuego modelo;
    private VistaJuego vista;
    private GestorRed gestorRed;
    private Set<Integer> jugadoresListos = new HashSet<>();
    private static final int CAPACIDAD_MAXIMA_SALA = 4;

    /**
     * Constructor principal con inyección de dependencias completa.
     */
    public ControladorJuego(ModeloJuego modelo, VistaJuego vista, GestorRed gestorRed) {
        this.modelo = modelo;
        this.vista = vista;
        this.gestorRed = gestorRed;
    }

    public int getTotalJugadoresConectados() {
        if (gestorRed == null) return 1;
        // El Host cuenta como 1 + la cantidad de clientes conectados en sus sockets
        return gestorRed.getConexiones().size() + 1;
    }

    /**
     * Se ejecuta cuando el jugador presiona el botón de lanzar dado en la Vista.
     */
    public void procesarTiradaDado() {
        int idLocal = gestorRed.getIdJugadorLocal();
        int turnoActual = modelo.getTurnoActual();

        // Validar turno
        if (turnoActual != idLocal) {
            System.out.println("No es tu turno para lanzar el dado. (Turno en Modelo: " + turnoActual + " | Tu ID Local: " + idLocal + ")");
            return;
        }

        // Lógica de lanzamiento...
        int resultado = modelo.getDado().lanzar();
        System.out.println("Jugador " + idLocal + " lanzó el dado: " + resultado);

        vista.mostrarResultadoDado(resultado);
        vista.setBotonDadoHabilitado(false);

        transmitirCambioEstadoLocal();
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

    /**
     * Emite el estado local hacia los demás jugadores.
     */
    public void transmitirCambioEstadoLocal() {
        String estadoSerializado = modelo.obtenerEstadoSerializado();

        // Filtro de seguridad: No transmitir nada si la partida no ha comenzado o el estado es nulo
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

    /**
     * Recibe la sincronización remota y actualiza vista y modelo sin retransmitir.
     */
    public void aplicarSincronizacionRemota(String datosEstado) {
        modelo.actualizarDesdeRed(datosEstado);
        vista.renderizarTablero();

        // Actualizar disponibilidad del botón de dado tras cambio de estado
        int turnoActual = modelo.getTurnoActual();
        int miIdLocal = gestorRed.getIdJugadorLocal();
        vista.mostrarTurnoActual(turnoActual);
        vista.setBotonDadoHabilitado(miIdLocal == turnoActual);
    }

    private void verificarInicioPartida() {
        int conectados = getTotalJugadoresConectados();
        if (conectados >= 2 && jugadoresListos.size() >= conectados) {
            dispararInicioPartidaGlobal();
        }
    }

    private void dispararInicioPartidaGlobal() {
        // 1. Establecer explícitamente el Turno 1 en el modelo del Host
        if (modelo.getTurnoActual() <= 0) {
            modelo.setTurnoActual(1);
        }

        // 2. Notificar a los clientes que la pantalla de lobby finalizó
        PaqueteRed paqueteStart = new PaqueteRed(
                TipoPaquete.LOBBY_CONFIRM,
                gestorRed.getIdJugadorLocal(),
                "INICIAR_JUEGO"
        );
        gestorRed.enviarPaqueteATodos(paqueteStart);

        // 3. Transmitir el estado inicial del tablero a todos los clientes
        transmitirCambioEstadoLocal();

        // 4. Iniciar la interfaz en la computadora del Host
        comenzarPartidaOficial();
    }

    public void presionarBotonListo() {
        int miId = gestorRed.getIdJugadorLocal();

        if (miId == 0) {
            System.err.println("Aún no se ha asignado un ID de jugador para esta conexión.");
            return;
        }

        // 1. Armar paquete de confirmación de lobby
        PaqueteRed paquete = new PaqueteRed(
                TipoPaquete.LOBBY_CONFIRM,
                miId,
                "JUGADOR_LISTO:" + miId
        );

        // 2. Transmitir según el rol
        if (gestorRed.isEsHost()) {
            procesarJugadorListo(miId);
            gestorRed.enviarPaqueteATodos(paquete);
        } else if (!gestorRed.getConexiones().isEmpty()) {
            gestorRed.getConexiones().get(0).enviarPaquete(paquete);
        }
    }

    public void comenzarPartidaOficial() {
        int miId = gestorRed.getIdJugadorLocal();

        // Asegurar que el turno inicial sea 1 si el modelo no fue inicializado aún
        if (modelo.getTurnoActual() <= 0) {
            modelo.setTurnoActual(1);
        }

        int turnoInicial = modelo.getTurnoActual();

        // Preparar la vista con el turno real del modelo
        vista.prepararInterfazParaInicio(miId, turnoInicial);

        System.out.println("¡Partida Iniciada! Mi ID: " + miId + " | Turno Actual en Modelo: " + turnoInicial);
    }

    /**
     * Se ejecuta cuando el jugador selecciona una de sus fichas en la Vista.
     */
    public void procesarAccionJugador(int idJugador, int idFicha) {
        // 1. Validar que la acción corresponda al jugador en turno
        if (idJugador != modelo.getTurnoActual()) {
            System.out.println("Intento de movimiento fuera de turno por parte del Jugador " + idJugador);
            return;
        }

        // 2. Obtener el valor actual del dado
        int pasos = modelo.getDado().getValorActual();

        // 3. Ejecutar el movimiento en el modelo
        boolean movimientoExitoso = modelo.moverFicha(idFicha, pasos);

        if (movimientoExitoso) {
            // 4. Aplicar reglas de repetición de turno o cambio de turno
            if (modelo.getPasosExtraPendientes() > 0) {
                System.out.println("Jugador " + idJugador + " conserva turno por pasos extra pendientes.");
            } else if (pasos == 6) {
                System.out.println("Jugador " + idJugador + " repite turno por haber sacado 6.");
            } else {
                modelo.cambiarTurno();
            }
        } else {
            System.out.println("El movimiento de la ficha " + idFicha + " no es válido.");
            return;
        }

        // 5. Actualizar la interfaz gráfica
        vista.renderizarTablero();

        int turnoActual = modelo.getTurnoActual();
        int miIdLocal = gestorRed.getIdJugadorLocal();

        vista.mostrarTurnoActual(turnoActual);
        vista.setBotonDadoHabilitado(miIdLocal == turnoActual);

        // 6. Transmitir el estado actualizado a los demás jugadores
        transmitirCambioEstadoLocal();
    }

    public ModeloJuego getModelo() { return modelo; }
    public void setModelo(ModeloJuego modelo) { this.modelo = modelo; }
    public VistaJuego getVista() { return vista; }
    public void setVista(VistaJuego vista) { this.vista = vista; }
    public GestorRed getGestorRed() { return gestorRed; }
    public void setGestorRed(GestorRed gestorRed) { this.gestorRed = gestorRed; }
}