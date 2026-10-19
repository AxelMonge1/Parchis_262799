package org.itson;

import org.itson.conection.GestorRed;
import org.itson.conection.auxiliar.GestorDesconexiones;
import org.itson.controller.ControladorJuego;
import org.itson.model.ModeloJuego;
import org.itson.view.VistaJuego;

import javax.swing.SwingUtilities;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ModeloJuego modelo = new ModeloJuego();
            VistaJuego vista = new VistaJuego();
            GestorRed gestorRed = new GestorRed();

            ControladorJuego controlador = new ControladorJuego(modelo, vista, gestorRed);

            gestorRed.setControlador(controlador);
            vista.setModelo(modelo);
            vista.setControlador(controlador);

            GestorDesconexiones gestorDesconexiones = new GestorDesconexiones(10000, gestorRed);

            configurarEventosUI(vista, controlador, gestorRed, gestorDesconexiones);

            vista.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
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

        vista.getBtnConfirmarCrear().addActionListener(e -> {
            String nombre = vista.getNombreUsuarioCrear();
            if (nombre.trim().isEmpty()) {
                vista.mostrarMensaje("Por favor ingresa un nombre de usuario.");
                return;
            }

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

        vista.getBtnDado().addActionListener(e -> controlador.procesarTiradaDado());

        vista.getBtnAccionLobby().addActionListener(e -> controlador.presionarBotonListo());
    }
}