package org.itson.controller;

import org.itson.conection.GestorRed;
import org.itson.conection.auxiliar.PaqueteRed;
import org.itson.conection.auxiliar.TipoPaquete;
import org.itson.model.ModeloJuego;
import org.itson.view.VistaJuego;

public class ControladorJuego {
    private ModeloJuego modelo;
    private VistaJuego vista;
    private GestorRed gestorRed;

    // Constructor para inyectar las dependencias
    public ControladorJuego(ModeloJuego modelo, VistaJuego vista, GestorRed gestorRed) {
        this.modelo = modelo;
        this.vista = vista;
        this.gestorRed = gestorRed;
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