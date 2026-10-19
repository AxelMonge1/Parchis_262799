package org.itson;

import org.itson.conection.GestorRed;
import org.itson.conection.auxiliar.GestorDesconexiones;
import org.itson.controller.ControladorJuego;
import org.itson.view.VistaJuego;
import org.itson.model.ModeloJuego;
import javax.swing.*;

public class Main {
    static void main() {
        // Ejecutar la interfaz gráfica en el Event Dispatch Thread (EDT) de Swing
        SwingUtilities.invokeLater(() -> {

            // 1. Instanciar los componentes centrales de la arquitectura
            ModeloJuego modelo = new ModeloJuego();
            VistaJuego vista = new VistaJuego();
            GestorRed gestorRed = new GestorRed();

            // Gestor de desconexiones configurado con un timeout de 5000ms (5 segundos)
            GestorDesconexiones gestorDesconexiones = new GestorDesconexiones(5000, gestorRed);

            // 2. Instanciar el Controlador uniendo las dependencias
            ControladorJuego controlador = new ControladorJuego(modelo, vista, gestorRed);

            // 3. Vincular las referencias cruzadas necesarias
            gestorRed.setControlador(controlador);
            vista.setModelo(modelo);

            // 4. Configurar los Listeners de la interfaz gráfica (Navegación del Storyboard)
            configurarEventosUI(vista, controlador, gestorRed, gestorDesconexiones);
            vista.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                    if (gestorRed.isEsHost()) {
                        gestorRed.cerrarServidor();
                    }
                }
            });

            // 5. Iniciar la aplicación mostrando el menú principal
            vista.setVisible(true);
        });
    }

    //Conectar la interfaz con la conexión
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

            // Iniciar servidor TCP en el puerto 8080
            boolean servidorIniciado = gestorRed.iniciarServidor(8080);

            if (servidorIniciado) {
                // Iniciar el hilo de monitoreo de caídas de red
                gestorDesconexiones.monitorearHeartbeat();
                gestorDesconexiones.agregarPrioridad(1); // El host tiene prioridad 1

                // Cambiar la pantalla al tablero e iniciar partida
                vista.mostrarPantalla(VistaJuego.PANTALLA_JUEGO);
                controlador.iniciarJuego();
                vista.mostrarMensaje("Partida creada como Host. Esperando jugadores...");
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

            // Intentar conectar vía socket al Host
            boolean conectado = gestorRed.conectarAHost(ip, 8080);

            if (conectado) {
                vista.mostrarPantalla(VistaJuego.PANTALLA_JUEGO);
                vista.mostrarMensaje("Conectado exitosamente con el Host (" + ip + ")");
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
            vista.mostrarMensaje("¡Sincronizando estado con todos los jugadores!");
            controlador.notificarCambioEstado();
        });

    }
}
