package org.itson.view;

import org.itson.model.ModeloJuego;
import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class VistaJuego extends JFrame {
    private ModeloJuego modelo;
    private Map<Integer, Color> coloresLocales;
    private Map<Integer, String> avataresLocales;
    private CardLayout cardLayout;
    private JPanel panelContenedor;

    public static final String PANTALLA_MENU = "MENU";
    public static final String PANTALLA_CREAR = "CREAR";
    public static final String PANTALLA_UNIRSE = "UNIRSE";
    public static final String PANTALLA_JUEGO = "JUEGO";

    // Subpaneles de Pantallas
    private JPanel panelMenu;
    private JPanel panelCrearPartida;
    private JPanel panelUnirsePartida;
    private JPanel panelTableroJuego;

    // Elementos de la UI
    private JButton btnCrearPartidaMenu;
    private JButton btnUnirsePartidaMenu;

    private JComboBox<Integer> cbCantFichas;
    private JComboBox<Integer> cbCasillasSeguras;
    private JComboBox<Integer> cbNumSalida;
    private JTextField txtNombreCrear;
    private JButton btnConfirmarCrear;

    private JTextField txtIpUnirse;
    private JTextField txtNombreUnirse;
    private JButton btnConfirmarUnirse;

    private JPanel panelTableroRender;
    private JLabel[] lblJugadores; // Muestra a los 4 jugadores en las esquinas
    private JButton btnDado;
    private JButton btnAccionLobby;

    public VistaJuego() {
        this.coloresLocales = new HashMap<>();
        this.avataresLocales = new HashMap<>();

        // Colores distintivos para cada slot de jugador (1: Rojo, 2: Azul, 3: Verde, 4: Amarillo)
        coloresLocales.put(1, new Color(220, 50, 50));
        coloresLocales.put(2, new Color(50, 120, 220));
        coloresLocales.put(3, new Color(50, 180, 50));
        coloresLocales.put(4, new Color(240, 200, 40));

        configurarVentana();
        inicializarPantallas();
    }

    private void configurarVentana() {
        setTitle("Parchís - Juego de Mesa");
        setSize(950, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        cardLayout = new CardLayout();
        panelContenedor = new JPanel(cardLayout);
        add(panelContenedor);
    }

    private void inicializarPantallas() {
        crearPanelMenu();
        crearPanelCrearPartida();
        crearPanelUnirsePartida();
        crearPanelTableroJuego();

        panelContenedor.add(panelMenu, PANTALLA_MENU);
        panelContenedor.add(panelCrearPartida, PANTALLA_CREAR);
        panelContenedor.add(panelUnirsePartida, PANTALLA_UNIRSE);
        panelContenedor.add(panelTableroJuego, PANTALLA_JUEGO);

        mostrarPantalla(PANTALLA_MENU);
    }

    // --- 1. MENÚ PRINCIPAL ---
    private void crearPanelMenu() {
        panelMenu = new JPanel(new GridBagLayout());
        panelMenu.setBackground(new Color(110, 40, 200)); // Color morado del storyboard

        JPanel container = new JPanel(new GridLayout(4, 1, 15, 15));
        container.setOpaque(false);

        JLabel lblTitulo = new JLabel("PARCHÍS", SwingConstants.CENTER);
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 42));
        lblTitulo.setForeground(Color.WHITE);

        btnCrearPartidaMenu = new JButton("Crear partida");
        btnUnirsePartidaMenu = new JButton("Unirse a partida");
        JButton btnPersonalización = new JButton("Personalización");
        btnPersonalización.setEnabled(false); // Ignorado por ahora según indicación

        estilarBoton(btnCrearPartidaMenu);
        estilarBoton(btnUnirsePartidaMenu);
        estilarBoton(btnPersonalización);

        btnCrearPartidaMenu.addActionListener(e -> mostrarPantalla(PANTALLA_CREAR));
        btnUnirsePartidaMenu.addActionListener(e -> mostrarPantalla(PANTALLA_UNIRSE));

        container.add(lblTitulo);
        container.add(btnCrearPartidaMenu);
        container.add(btnUnirsePartidaMenu);
        container.add(btnPersonalización);

        panelMenu.add(container);
    }

    // --- 2. CREAR PARTIDA (Actualizado con nuevos rangos y defaults) ---
    private void crearPanelCrearPartida() {
        panelCrearPartida = new JPanel(new GridBagLayout());
        panelCrearPartida.setBackground(new Color(110, 40, 200));

        JPanel form = new JPanel(new GridLayout(6, 2, 10, 15));
        form.setOpaque(false);

        JButton btnVolver = new JButton("Volver");
        estilarBoton(btnVolver);
        btnVolver.addActionListener(e -> mostrarPantalla(PANTALLA_MENU));

        // 1. Fichas por jugador: de 3 a 6 (Por defecto: 4)
        cbCantFichas = new JComboBox<>(new Integer[]{3, 4, 5, 6});
        cbCantFichas.setSelectedItem(4);

        // 2. Casillas seguras por ala: de 1 a 5 (Por defecto: 2, según la imagen)
        cbCasillasSeguras = new JComboBox<>(new Integer[]{1, 2, 3, 4, 5});
        cbCasillasSeguras.setSelectedItem(2);

        // 3. Número para sacar ficha: de 1 a 6 (Por defecto: 5)
        cbNumSalida = new JComboBox<>(new Integer[]{1, 2, 3, 4, 5, 6});
        cbNumSalida.setSelectedItem(5);

        txtNombreCrear = new JTextField(12);

        btnConfirmarCrear = new JButton("Crear partida");
        estilarBoton(btnConfirmarCrear);

        form.add(btnVolver);
        form.add(new JLabel(""));
        form.add(crearLabelTexto("Cant. de fichas:"));
        form.add(cbCantFichas);
        form.add(crearLabelTexto("Casillas seguras por ala:"));
        form.add(cbCasillasSeguras);
        form.add(crearLabelTexto("Número para sacar ficha:"));
        form.add(cbNumSalida);
        form.add(crearLabelTexto("Nombre de usuario:"));
        form.add(txtNombreCrear);
        form.add(new JLabel(""));
        form.add(btnConfirmarCrear);

        panelCrearPartida.add(form);
    }

    // --- 3. UNIRSE A PARTIDA ---
    private void crearPanelUnirsePartida() {
        panelUnirsePartida = new JPanel(new GridBagLayout());
        panelUnirsePartida.setBackground(new Color(110, 40, 200));

        JPanel form = new JPanel(new GridLayout(4, 2, 10, 15));
        form.setOpaque(false);

        JButton btnVolver = new JButton("Volver");
        estilarBoton(btnVolver);
        btnVolver.addActionListener(e -> mostrarPantalla(PANTALLA_MENU));

        txtIpUnirse = new JTextField("127.0.0.1", 12);
        txtNombreUnirse = new JTextField(12);

        btnConfirmarUnirse = new JButton("Unirse");
        estilarBoton(btnConfirmarUnirse);

        form.add(btnVolver);
        form.add(new JLabel(""));
        form.add(crearLabelTexto("Dirección IP:"));
        form.add(txtIpUnirse);
        form.add(crearLabelTexto("Nombre de usuario:"));
        form.add(txtNombreUnirse);
        form.add(new JLabel(""));
        form.add(btnConfirmarUnirse);

        panelUnirsePartida.add(form);
    }

    // --- 4. TABLERO DE JUEGO (LOBBY Y PARTIDA) ---
    private void crearPanelTableroJuego() {
        panelTableroJuego = new JPanel(new BorderLayout());
        panelTableroJuego.setBackground(new Color(110, 40, 200));

        // Área central gráfica del tablero
        panelTableroRender = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                dibujarTableroGrafico(g);
            }
        };
        panelTableroRender.setPreferredSize(new Dimension(480, 480));
        panelTableroRender.setBackground(Color.WHITE);

        // Tarjetas de jugadores en las esquinas según el storyboard
        lblJugadores = new JLabel[4];
        lblJugadores[0] = crearCardJugador("Jugador 1", coloresLocales.get(1));
        lblJugadores[1] = crearCardJugador("Jugador 2", coloresLocales.get(2));
        lblJugadores[2] = crearCardJugador("Jugador 3", coloresLocales.get(3));
        lblJugadores[3] = crearCardJugador("Jugador 4", coloresLocales.get(4));

        JPanel panelNorte = new JPanel(new BorderLayout());
        panelNorte.setOpaque(false);
        panelNorte.add(lblJugadores[0], BorderLayout.WEST);
        panelNorte.add(lblJugadores[1], BorderLayout.EAST);

        JPanel panelSur = new JPanel(new BorderLayout());
        panelSur.setOpaque(false);
        panelSur.add(lblJugadores[2], BorderLayout.WEST);
        panelSur.add(lblJugadores[3], BorderLayout.EAST);

        // Controles de juego en la parte inferior
        btnDado = new JButton("🎲 Lanzar Dado");
        estilarBoton(btnDado);

        btnAccionLobby = new JButton("Listo / Comenzar");
        estilarBoton(btnAccionLobby);

        JPanel panelInferior = new JPanel();
        panelInferior.setOpaque(false);
        panelInferior.add(btnDado);
        panelInferior.add(btnAccionLobby);

        panelTableroJuego.add(panelNorte, BorderLayout.NORTH);
        panelTableroJuego.add(panelTableroRender, BorderLayout.CENTER);
        panelTableroJuego.add(panelSur, BorderLayout.SOUTH);
        panelTableroJuego.add(panelInferior, BorderLayout.PAGE_END);
    }

    // --- Métodos del Diagrama de Clases ---

    public void renderizarTablero() { //
        if (panelTableroRender != null) {
            panelTableroRender.repaint();
        }
    }

    public void actualizarFichas() { //
        // Redibuja las posiciones de las fichas sobre el canvas del tablero
        renderizarTablero();
    }

    public void mostrarResultadoDado(int valor) { //[cite: 4]
        btnDado.setText("🎲 Dado: " + valor);
        JOptionPane.showMessageDialog(this, "El dado cayó en: " + valor, "Resultado Dado", JOptionPane.INFORMATION_MESSAGE);
    }

    public void mostrarTurnoActual(int idJugador) { //[cite: 4]
        for (int i = 0; i < 4; i++) {
            if (i + 1 == idJugador) {
                lblJugadores[i].setBorder(BorderFactory.createLineBorder(Color.YELLOW, 3));
            } else {
                lblJugadores[i].setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            }
        }
    }

    public void mostrarEstadoEsperandoJugadores() {
        SwingUtilities.invokeLater(() -> {
            btnAccionLobby.setEnabled(false);
            btnAccionLobby.setText("Esperando a los demás...");
        });
    }

    public void prepararInterfazParaInicio(int miId, int turnoActual) {
        SwingUtilities.invokeLater(() -> {
            // 1. FORZAR EL CAMBIO DE PANTALLA EN EL CardLayout
            // Reemplaza PANTALLA_JUEGO con la constante/nombre que uses para la pantalla del tablero
            mostrarPantalla(PANTALLA_JUEGO);

            // 2. Actualizar estado del botón de lobby
            btnAccionLobby.setText("En Partida");
            btnAccionLobby.setEnabled(false);

            // 3. Evaluar e informar el turno
            boolean esMiTurno = (miId == turnoActual);
            btnDado.setEnabled(esMiTurno);

            // 4. Feedback visual explícito en un Label o en la consola de la UI
            String mensajeEstado = esMiTurno
                    ? "¡Es TU turno! Haz clic en Lanzar Dado."
                    : "Turno del Jugador " + turnoActual + ". Esperando su jugada...";

            mostrarMensaje(mensajeEstado); // O lblEstado.setText(mensajeEstado);

            // 5. Dibujar tablero
            renderizarTablero();

            // Forzar a Swing a repintar la ventana
            revalidate();
            repaint();
        });
    }

    public void mostrarMensaje(String mensaje) { //[cite: 4]
        JOptionPane.showMessageDialog(this, mensaje, "Notificación", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Muestra la modal "Tabla de Posiciones" del storyboard al terminar el juego.
     */
    public void mostrarTablaPosiciones(String resumen) {
        JOptionPane.showMessageDialog(this, resumen, "Tabla de Posiciones - Fin del Juego", JOptionPane.PLAIN_MESSAGE);
    }

    // --- Métodos Auxiliares ---

    public void mostrarPantalla(String nombrePantalla) {
        cardLayout.show(panelContenedor, nombrePantalla);
    }

    private void dibujarTableroGrafico(Graphics g) {
        int w = panelTableroRender.getWidth();
        int h = panelTableroRender.getHeight();

        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(0, 0, w, h);

        // Casas esquineras
        g.setColor(coloresLocales.get(1)); // Rojo (Arriba Izq)
        g.fillRect(15, 15, w / 3, h / 3);

        g.setColor(coloresLocales.get(2)); // Azul (Arriba Der)
        g.fillRect(w - w / 3 - 15, 15, w / 3, h / 3);

        g.setColor(coloresLocales.get(3)); // Verde (Abajo Izq)
        g.fillRect(15, h - h / 3 - 15, w / 3, h / 3);

        g.setColor(coloresLocales.get(4)); // Amarillo (Abajo Der)
        g.fillRect(w - w / 3 - 15, h - h / 3 - 15, w / 3, h / 3);

        // Triángulo central (Meta)
        g.setColor(Color.DARK_GRAY);
        g.fillOval(w / 2 - 35, h / 2 - 35, 70, 70);
        g.setColor(Color.WHITE);
        g.drawString("META", w / 2 - 16, h / 2 + 5);
    }

    private JLabel crearCardJugador(String texto, Color color) {
        JLabel lbl = new JLabel(texto, SwingConstants.CENTER);
        lbl.setFont(new Font("Arial", Font.BOLD, 13));
        lbl.setForeground(Color.WHITE);
        lbl.setOpaque(true);
        lbl.setBackground(color);
        lbl.setPreferredSize(new Dimension(160, 40));
        return lbl;
    }

    private JLabel crearLabelTexto(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setForeground(Color.WHITE);
        lbl.setFont(new Font("Arial", Font.BOLD, 14));
        return lbl;
    }

    private void estilarBoton(JButton btn) {
        btn.setFont(new Font("Arial", Font.BOLD, 14));
        btn.setBackground(Color.WHITE);
        btn.setForeground(new Color(110, 40, 200));
        btn.setFocusPainted(false);
    }

    public JButton getBtnCrearPartidaMenu() { return btnCrearPartidaMenu; }

    public JButton getBtnUnirsePartidaMenu() { return btnUnirsePartidaMenu; }

    public JButton getBtnConfirmarCrear() { return btnConfirmarCrear; }

    public JButton getBtnConfirmarUnirse() { return btnConfirmarUnirse; }

    public JButton getBtnDado() { return btnDado; }

    public JButton getBtnAccionLobby() { return btnAccionLobby; }

    public String getNombreUsuarioCrear() { return txtNombreCrear.getText(); }

    public String getNombreUsuarioUnirse() { return txtNombreUnirse.getText(); }

    public String getIpServidor() { return txtIpUnirse.getText(); }

    public ModeloJuego getModelo() { return modelo; }

    public void setModelo(ModeloJuego modelo) { this.modelo = modelo; }

    public int getCantFichasSeleccionadas() {
        return (Integer) cbCantFichas.getSelectedItem();
    }

    public int getCasillasSegurasSeleccionadas() {
        return (Integer) cbCasillasSeguras.getSelectedItem();
    }

    public int getNumSalidaSeleccionado() {
        return (Integer) cbNumSalida.getSelectedItem();
    }
}