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

    public void presionarBotonListo() {
        int miId = gestorRed.getIdJugadorLocal();

        vista.getBtnAccionLobby().setEnabled(false);
        vista.getBtnAccionLobby().setText("Esperando a los demás...");

        PaqueteRed paqueteListo = new PaqueteRed(
                TipoPaquete.LOBBY_CONFIRM,
                miId,
                "JUGADOR_LISTO:" + miId
        );

        if (gestorRed.isEsHost()) {
            procesarJugadorListo(miId);
            gestorRed.enviarPaqueteATodos(paqueteListo);
        } else {
            if (!gestorRed.getConexiones().isEmpty()) {
                gestorRed.getConexiones().get(0).enviarPaquete(paqueteListo);
            }
        }
    }

    /**
     * Evalúa si se cumplen las condiciones para iniciar la partida.
     */
    public synchronized void procesarJugadorListo(int idJugador) {
        jugadoresListos.add(idJugador);

        int conectados = getTotalJugadoresConectados();
        vista.mostrarMensaje("Jugador " + idJugador + " listo (" + jugadoresListos.size() + "/" + conectados + ")");

        // Evaluación de inicio en el Host
        if (gestorRed.isEsHost()) {
            verificarInicioPartida();
        }
    }

    /**
     * Notificación cuando un nuevo cliente se conecta a la sala.
     */
    public synchronized void notificarNuevoJugadorConectado() {
        if (!gestorRed.isEsHost()) return;

        int conectados = getTotalJugadoresConectados();
        vista.mostrarMensaje("¡Nuevo jugador unido! Conectados: " + conectados + "/" + CAPACIDAD_MAXIMA_SALA);

        // Si la sala se llena (4 jugadores), inicia automáticamente
        if (conectados >= CAPACIDAD_MAXIMA_SALA) {
            vista.mostrarMensaje("¡Sala llena (4/4)! Iniciando automáticamente...");
            dispararInicioPartidaGlobal();
        }
    }

    /**
     * Revisa si todos los conectados le dieron a listo (Mínimo 2 jugadores).
     */
    private void verificarInicioPartida() {
        int conectados = getTotalJugadoresConectados();

        // Se requiere mínimo 2 personas y que TODOS los conectados estén listos
        boolean todosListos = (conectados >= 2) && (jugadoresListos.size() == conectados);

        if (todosListos) {
            dispararInicioPartidaGlobal();
        }
    }

    /**
     * Transmite el paquete de inicio a toda la red y arranca localmente.
     */
    private void dispararInicioPartidaGlobal() {
        PaqueteRed paqueteStart = new PaqueteRed(
                TipoPaquete.LOBBY_CONFIRM,
                gestorRed.getIdJugadorLocal(),
                "INICIAR_JUEGO"
        );

        gestorRed.enviarPaqueteATodos(paqueteStart);
        comenzarPartidaOficial();
    }

    /**
     * Activa la interfaz de juego real.
     */
    public void comenzarPartidaOficial() {
        vista.getBtnAccionLobby().setText("En Partida");
        vista.getBtnAccionLobby().setEnabled(false);

        int turnoActual = 1; // Comienza el Jugador 1
        vista.mostrarTurnoActual(turnoActual);

        // Se habilita el dado únicamente si es el turno del jugador local
        boolean esMiTurno = (gestorRed.getIdJugadorLocal() == turnoActual);
        vista.getBtnDado().setEnabled(esMiTurno);

        vista.mostrarMensaje("¡Comienza la partida!");
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
        // 1. El controlador pide al modelo lanzar el dado
        int valorDado = modelo.lanzarDado(); //

        // Según el diagrama de secuencia, aquí el modelo evalúa los 3 seises seguidos
        // y determina qué fichas son movibles. Si la lista de fichas movibles
        // está vacía, se pasa al siguiente turno automáticamente.

        // Actualizamos la interfaz y notificamos a la red
        notificarCambioEstado();
    }

    /**
     * Se ejecuta cuando el jugador selecciona una de sus fichas en la Vista.
     */
    public void procesarAccionJugador(int idJugador, int idFicha) {
        // Obtenemos el valor actual del dado para el movimiento
        // (Asumiendo acceso a través del modelo según la agregación Modelo->Dado)[cite: 4]
        int pasos = modelo.getDado().getValorActual();

        // 1. Ejecutamos el movimiento en el modelo
        boolean movimientoExitoso = modelo.moverFicha(idFicha, pasos); //

        if (movimientoExitoso) {
            // 2. Lógica de finalización de turno (Reglas de repetición)
            // Según el bloque "alt" del diagrama de secuencia:
            if (modelo.getPasosExtraPendientes() > 0 || pasos == 6) {
                // El jugador repite turno o debe gastar sus pasos extra[cite: 5]
                // modelo.prepararSiguienteAccion();
            } else {
                // Si no sacó 6 y no tiene recompensas, el turno cambia al siguiente jugador[cite: 5]
                modelo.cambiarTurno(); //
            }
        }

        // 3. Notificar los cambios a todos los clientes
        notificarCambioEstado(); //[cite: 4]
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