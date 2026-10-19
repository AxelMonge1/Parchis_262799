package org.itson;

import org.itson.conection.GestorRed;
import org.itson.conection.auxiliar.GestorDesconexiones;
import org.itson.controller.ControladorJuego;
import org.itson.model.ModeloJuego;
import org.itson.view.VistaJuego;

import javax.swing.*;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {

            // 1. Instanciar los componentes centrales de la arquitectura
            ModeloJuego modelo = new ModeloJuego();
            VistaJuego vista = new VistaJuego();
            GestorRed gestorRed = new GestorRed();

            // 2. Instanciar el Controlador unificado
            ControladorJuego controlador = new ControladorJuego(modelo, vista, gestorRed);

            // 3. Vincular dependencias bidireccionales indispensables
            gestorRed.setControlador(controlador);
            vista.setModelo(modelo);
            vista.setControlador(controlador); // 👈 ASIGNACIÓN CRÍTICA FALTANTE

            GestorDesconexiones gestorDesconexiones = new GestorDesconexiones(10000, gestorRed);

            // 4. Configurar eventos de la UI
            configurarEventosUI(vista, controlador, gestorRed, gestorDesconexiones);

            vista.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                    if (gestorRed.isEsHost()) {
                        gestorRed.cerrarServidor();
                    }
                }
            });

            vista.setVisible(true);
        });
    }

    private static void configurarEventosUI(VistaJuego vista,
                                            ControladorJuego controlador,
                                            GestorRed gestorRed,
                                            GestorDesconexiones gestorDesconexiones) {

        // --- BOTÓN: Confirmar Crear Partida (Modo Servidor / Host) ---
        vista.getBtnConfirmarCrear().addActionListener(e -> {
            String nombre = vista.getNombreUsuarioCrear();
            if (nombre.trim().isEmpty()) {
                vista.mostrarMensaje("Por favor ingresa un nombre de usuario.");
                return;
            }

            // Pasar la configuración elegida en la UI al modelo
            vista.getModelo().setNumSalida(vista.getNumSalidaSeleccionado());

            boolean servidorIniciado = gestorRed.iniciarServidor(8080);

            if (servidorIniciado) {
                gestorDesconexiones.monitorearHeartbeat();
                gestorDesconexiones.agregarPrioridad(1);

                vista.mostrarPantalla(VistaJuego.PANTALLA_JUEGO);
                vista.mostrarMensaje("Partida creada como Host. Esperando a los demás jugadores...");
            } else {
                vista.mostrarMensaje("Error al abrir el puerto de red.");
            }
        });

        // --- BOTÓN: Confirmar Unirse a Partida (Modo Cliente) ---
        vista.getBtnConfirmarUnirse().addActionListener(e -> {
            String ip = vista.getIpServidor();
            String nombre = vista.getNombreUsuarioUnirse();

            if (nombre.trim().isEmpty() || ip.trim().isEmpty()) {
                vista.mostrarMensaje("Ingresa la IP y tu nombre para continuar.");
                return;
            }

            boolean conectado = gestorRed.conectarAHost(ip, 8080);

            if (conectado) {
                vista.mostrarPantalla(VistaJuego.PANTALLA_JUEGO);
                vista.mostrarMensaje("Conectado exitosamente con el Host (" + ip + "). Presiona 'Listo'.");
            } else {
                vista.mostrarMensaje("No se pudo establecer conexión con " + ip);
            }
        });

        // --- BOTÓN: Lanzar Dado ---
        vista.getBtnDado().addActionListener(e -> {
            controlador.procesarTiradaDado();
        });

        // --- BOTÓN: Listo / Comenzar en el Lobby ---
        vista.getBtnAccionLobby().addActionListener(e -> {
            controlador.presionarBotonListo();
        });
    }
}