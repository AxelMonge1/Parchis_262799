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

    public ControladorJuego(GestorRed gestorRed) {
        this.gestorRed = gestorRed;
    }

    // Constructor para inyectar las dependencias
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
     * Inicia el ciclo principal del juego.
     */
    public void iniciarJuego() {
        // Inicializa el estado y lo transmite a los demás jugadores
        notificarCambioEstado();
    }

    /**
     * Se ejecuta cuando el jugador presiona el botón de lanzar dado en la Vista.
     */
    public void procesarTiradaDado() {
        // 1. Ejecutar la lógica local de la tirada
        int resultado = modelo.getDado().lanzar();
        vista.mostrarResultadoDado(resultado);

        // 2. 📍 Transmitir el nuevo estado a los demás jugadores
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

        // 📍 Filtro de seguridad: No transmitir nada si la partida no ha comenzado o el estado es nulo
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
    }

    private void verificarInicioPartida() {
        int conectados = getTotalJugadoresConectados();
        if (conectados >= 2 && jugadoresListos.size() >= conectados) {
            dispararInicioPartidaGlobal();
        }
    }

    private void dispararInicioPartidaGlobal() {
        PaqueteRed paqueteStart = new PaqueteRed(
                TipoPaquete.LOBBY_CONFIRM,
                gestorRed.getIdJugadorLocal(),
                "INICIAR_JUEGO"
        );

        gestorRed.enviarPaqueteATodos(paqueteStart);
        comenzarPartidaOficial();
    }

    public void presionarBotonListo() {
        int miId = gestorRed.getIdJugadorLocal();

        // Evitar enviar el paquete si el cliente aún no ha recibido su ID del Host
        if (miId == 0) {
            System.err.println("Aún no se ha asignado un ID de jugador para esta conexión.");
            return;
        }

        // 1. Armar paquete de lobby
        PaqueteRed paquete = new PaqueteRed(
                TipoPaquete.LOBBY_CONFIRM,
                miId,
                "JUGADOR_LISTO:" + miId
        );

        // 2. Transmitir según el rol
        if (gestorRed.isEsHost()) {
            procesarJugadorListo(miId);
            gestorRed.enviarPaqueteATodos(paquete);
        } else {
            if (!gestorRed.getConexiones().isEmpty()) {
                gestorRed.getConexiones().get(0).enviarPaquete(paquete);
            }
        }
    }

    public void comenzarPartidaOficial() {
        int miId = gestorRed.getIdJugadorLocal();
        int turnoInicial = 1;

        // Antes se manipulaban btnDado y btnAccionLobby directamente aquí; ahora solo se ordena a la vista
        vista.prepararInterfazParaInicio(miId, turnoInicial);

        System.out.println("¡Partida Iniciada! ID Local: " + miId);
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
                // Hay pasos extra pendientes (ej: +20 por comer, +10 por llegada).
                // Se conserva el turno del jugador actual para que use su bonificación.
                System.out.println("Jugador " + idJugador + " conserva turno por pasos extra pendientes.");
            } else if (pasos == 6) {
                // Sacar un 6 otorga repetición de tiro
                System.out.println("Jugador " + idJugador + " repite turno por haber sacado 6.");
            } else {
                // Si no hay recompensas ni sacó 6, pasa el turno al siguiente jugador
                modelo.cambiarTurno();
            }
        } else {
            System.out.println("El movimiento de la ficha " + idFicha + " no es válido.");
            return;
        }

        // 5. Actualizar la interfaz gráfica (Vista)
        vista.renderizarTablero();

        int turnoActual = modelo.getTurnoActual();
        int miIdLocal = gestorRed.getIdJugadorLocal();

        // Habilitar el botón de lanzar dado solo si es el turno de esta computadora
        vista.mostrarTurnoActual(turnoActual);
        vista.setBotonDadoHabilitado(miIdLocal == turnoActual);

        // 6. Transmitir el estado actualizado a los demás jugadores por la red
        transmitirCambioEstadoLocal();
    }

    /**
     * Actualiza la vista local y empaqueta el estado para enviarlo por red a los clientes.
     */
    public void notificarCambioEstado() {
        // 1. Actualizar la vista local[cite: 4]
        if (vista != null) {
            vista.actualizarFichas(); //[cite: 4]
            // vista.mostrarTurnoActual(modelo.getTurnoActual());
        }

        // 2. Broadcast del estado por la red[cite: 5]
        if (gestorRed != null) {
            // Crea un paquete de tipo SYNC_ESTADO según el diagrama de red[cite: 4, 5]
            PaqueteRed paquete = new PaqueteRed();
            paquete.setTipo(TipoPaquete.SYNC_ESTADO); //[cite: 4, 5]

            // El Gestor de Red itera sus conexiones TCP y envía el paquete a todos[cite: 4, 5]
            gestorRed.enviarPaqueteATodos(paquete); //[cite: 4]
        }
    }

    public ModeloJuego getModelo() { return modelo; }
    public void setModelo(ModeloJuego modelo) { this.modelo = modelo; }
    public VistaJuego getVista() { return vista; }
    public void setVista(VistaJuego vista) { this.vista = vista; }
    public GestorRed getGestorRed() { return gestorRed; }
    public void setGestorRed(GestorRed gestorRed) { this.gestorRed = gestorRed; }
}