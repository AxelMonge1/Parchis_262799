package org.itson.view;

import org.itson.controller.ControladorJuego;
import org.itson.model.ModeloJuego;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VistaJuego extends JFrame {
    private ModeloJuego modelo;
    private ControladorJuego controlador;
    private Map<Integer, Color> coloresLocales;
    private CardLayout cardLayout;
    private JPanel panelContenedor;

    public static final String PANTALLA_MENU = "MENU";
    public static final String PANTALLA_CREAR = "CREAR";
    public static final String PANTALLA_UNIRSE = "UNIRSE";
    public static final String PANTALLA_JUEGO = "JUEGO";

    private JPanel panelMenu;
    private JPanel panelCrearPartida;
    private JPanel panelUnirsePartida;
    private JPanel panelTableroJuego;

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
    private JLabel[] lblJugadores;
    private JButton btnDado;
    private JButton btnAccionLobby;
    private JLabel lblEstado;

    private final List<FichaHitbox> fichasHitboxes = new ArrayList<>();
    private static final int[][] POS_CASILLAS = new int[69][2];

    static {
        inicializarCoordenadasCasillas();
    }

    public VistaJuego() {
        this.coloresLocales = new HashMap<>();
        coloresLocales.put(1, new Color(225, 40, 40));
        coloresLocales.put(2, new Color(40, 145, 235));
        coloresLocales.put(3, new Color(95, 185, 55));
        coloresLocales.put(4, new Color(245, 210, 25));

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

    private void crearPanelMenu() {
        panelMenu = new JPanel(new GridBagLayout());
        panelMenu.setBackground(new Color(110, 40, 200));

        JPanel container = new JPanel(new GridLayout(4, 1, 15, 15));
        container.setOpaque(false);

        JLabel lblTitulo = new JLabel("PARCHÍS", SwingConstants.CENTER);
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 42));
        lblTitulo.setForeground(Color.WHITE);

        btnCrearPartidaMenu = new JButton("Crear partida");
        btnUnirsePartidaMenu = new JButton("Unirse a partida");
        JButton btnPersonalizacion = new JButton("Personalización");
        btnPersonalizacion.setEnabled(false);

        estilarBoton(btnCrearPartidaMenu);
        estilarBoton(btnUnirsePartidaMenu);
        estilarBoton(btnPersonalizacion);

        btnCrearPartidaMenu.addActionListener(e -> mostrarPantalla(PANTALLA_CREAR));
        btnUnirsePartidaMenu.addActionListener(e -> mostrarPantalla(PANTALLA_UNIRSE));

        container.add(lblTitulo);
        container.add(btnCrearPartidaMenu);
        container.add(btnUnirsePartidaMenu);
        container.add(btnPersonalizacion);

        panelMenu.add(container);
    }

    private void crearPanelCrearPartida() {
        panelCrearPartida = new JPanel(new GridBagLayout());
        panelCrearPartida.setBackground(new Color(110, 40, 200));

        JPanel form = new JPanel(new GridLayout(6, 2, 10, 15));
        form.setOpaque(false);

        JButton btnVolver = new JButton("Volver");
        estilarBoton(btnVolver);
        btnVolver.addActionListener(e -> mostrarPantalla(PANTALLA_MENU));

        cbCantFichas = new JComboBox<>(new Integer[]{3, 4, 5, 6});
        cbCantFichas.setSelectedItem(4);

        cbCasillasSeguras = new JComboBox<>(new Integer[]{1, 2, 3, 4, 5});
        cbCasillasSeguras.setSelectedItem(2);

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

    private void crearPanelTableroJuego() {
        panelTableroJuego = new JPanel(new BorderLayout());
        panelTableroJuego.setBackground(new Color(110, 40, 200));

        panelTableroRender = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                dibujarTableroGrafico(g);
            }
        };
        panelTableroRender.setPreferredSize(new Dimension(480, 480));
        panelTableroRender.setBackground(Color.WHITE);

        panelTableroRender.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int mx = e.getX();
                int my = e.getY();

                for (FichaHitbox hb : fichasHitboxes) {
                    if (hb.contains(mx, my)) {
                        if (controlador != null) {
                            controlador.procesarAccionJugador(hb.idJugador, hb.idFicha);
                        }
                        break;
                    }
                }
            }
        });

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

        btnDado = new JButton("🎲 Lanzar Dado");
        estilarBoton(btnDado);

        btnAccionLobby = new JButton("Listo / Comenzar");
        estilarBoton(btnAccionLobby);

        lblEstado = new JLabel("Esperando inicio...", SwingConstants.CENTER);
        lblEstado.setFont(new Font("Arial", Font.BOLD, 14));
        lblEstado.setForeground(Color.WHITE);

        JPanel panelInferior = new JPanel(new GridLayout(2, 1, 5, 5));
        panelInferior.setOpaque(false);

        JPanel panelBotones = new JPanel();
        panelBotones.setOpaque(false);
        panelBotones.add(btnDado);
        panelBotones.add(btnAccionLobby);

        panelInferior.add(panelBotones);
        panelInferior.add(lblEstado);

        panelTableroJuego.add(panelNorte, BorderLayout.NORTH);
        panelTableroJuego.add(panelTableroRender, BorderLayout.CENTER);
        panelTableroJuego.add(panelSur, BorderLayout.SOUTH);
        panelTableroJuego.add(panelInferior, BorderLayout.PAGE_END);
    }

    private void dibujarTableroGrafico(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        fichasHitboxes.clear();

        int w = panelTableroRender.getWidth();
        int h = panelTableroRender.getHeight();

        int size = Math.min(w, h) - 10;
        int offsetX = (w - size) / 2;
        int offsetY = (h - size) / 2;

        double u = size / 19.0;

        Color colorRojo = coloresLocales.get(1);
        Color colorAzul = coloresLocales.get(2);
        Color colorVerde = coloresLocales.get(3);
        Color colorAmarillo = coloresLocales.get(4);
        Color colorBorde = new Color(100, 50, 20);

        g2.setColor(Color.WHITE);
        g2.fillRect(offsetX, offsetY, size, size);

        dibujarCasa(g2, offsetX, offsetY, u, 0, 0, colorRojo, 1);
        dibujarCasa(g2, offsetX, offsetY, u, 11, 0, colorAzul, 2);
        dibujarCasa(g2, offsetX, offsetY, u, 0, 11, colorVerde, 3);
        dibujarCasa(g2, offsetX, offsetY, u, 11, 11, colorAmarillo, 4);

        dibujarMetaCentral(g2, offsetX, offsetY, u, colorRojo, colorAzul, colorAmarillo, colorVerde);

        dibujarAlaSuperior(g2, offsetX, offsetY, u, colorRojo);
        dibujarAlaDerecha(g2, offsetX, offsetY, u, colorAzul);
        dibujarAlaInferior(g2, offsetX, offsetY, u, colorAmarillo);
        dibujarAlaIzquierda(g2, offsetX, offsetY, u, colorVerde);

        dibujarFichasModelo(g2, offsetX, offsetY, u);

        g2.setColor(colorBorde);
        g2.setStroke(new BasicStroke(4f));
        g2.drawRect(offsetX, offsetY, size, size);
    }

    private void dibujarCasa(Graphics2D g2, int ox, int oy, double u, int gridX, int gridY, Color color, int idJugador) {
        int x0 = (int) Math.round(ox + gridX * u);
        int y0 = (int) Math.round(oy + gridY * u);
        int boxSize = (int) Math.round(8 * u);

        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRect(x0, y0, boxSize, boxSize);

        int cx = x0 + boxSize / 2;
        int cy = y0 + boxSize / 2;

        int rOuter = (int) Math.round(3.6 * u);
        g2.setColor(color);
        g2.fillOval(cx - rOuter, cy - rOuter, rOuter * 2, rOuter * 2);
        g2.setColor(Color.BLACK);
        g2.drawOval(cx - rOuter, cy - rOuter, rOuter * 2, rOuter * 2);

        int rInner = (int) Math.round(2.6 * u);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2.0f));
        g2.drawOval(cx - rInner, cy - rInner, rInner * 2, rInner * 2);

        int rCenter = (int) Math.round(0.85 * u);
        g2.setColor(Color.WHITE);
        g2.fillOval(cx - rCenter, cy - rCenter, rCenter * 2, rCenter * 2);
        g2.setColor(Color.BLACK);
        g2.drawOval(cx - rCenter, cy - rCenter, rCenter * 2, rCenter * 2);

        double dist = 1.6 * u;
        int[][] posSlots = {
                {(int) Math.round(-dist * 0.707), (int) Math.round(-dist * 0.707)},
                {(int) Math.round(dist * 0.707), (int) Math.round(-dist * 0.707)},
                {(int) Math.round(-dist * 0.707), (int) Math.round(dist * 0.707)},
                {(int) Math.round(dist * 0.707), (int) Math.round(dist * 0.707)}
        };

        int rSlot = (int) Math.round(0.45 * u);
        for (int[] p : posSlots) {
            int sx = cx + p[0];
            int sy = cy + p[1];
            g2.setColor(Color.WHITE);
            g2.fillOval(sx - rSlot, sy - rSlot, rSlot * 2, rSlot * 2);
            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawOval(sx - rSlot, sy - rSlot, rSlot * 2, rSlot * 2);
        }

        if (modelo != null && modelo.getPosicionesFichas().containsKey(idJugador)) {
            Map<Integer, Integer> fichas = modelo.getPosicionesFichas().get(idJugador);
            for (int f = 1; f <= 4; f++) {
                int pos = fichas.getOrDefault(f, -1);
                if (pos == -1 && f <= posSlots.length) {
                    int sx = cx + posSlots[f - 1][0];
                    int sy = cy + posSlots[f - 1][1];
                    dibujarFichaVisual(g2, sx, sy, u, idJugador, f);
                }
            }
        } else {
            for (int f = 1; f <= 4; f++) {
                int sx = cx + posSlots[f - 1][0];
                int sy = cy + posSlots[f - 1][1];
                dibujarFichaVisual(g2, sx, sy, u, idJugador, f);
            }
        }
    }

    private void dibujarFichasModelo(Graphics2D g2, int ox, int oy, double u) {
        if (modelo == null || modelo.getPosicionesFichas() == null) return;

        Map<Integer, Integer> ocupacionCasillas = new HashMap<>();

        modelo.getPosicionesFichas().forEach((idJugador, fichas) -> {
            if (fichas == null) return;

            fichas.forEach((idFicha, pos) -> {
                if (pos != null && pos >= 1 && pos <= 68) {
                    int[] gridCoords = POS_CASILLAS[pos];
                    if (gridCoords != null) {
                        int gridX = gridCoords[0];
                        int gridY = gridCoords[1];

                        int count = ocupacionCasillas.getOrDefault(pos, 0);
                        ocupacionCasillas.put(pos, count + 1);

                        int offsetPx = count * 3;

                        int cx = (int) Math.round(ox + (gridX + 0.5) * u) + offsetPx;
                        int cy = (int) Math.round(oy + (gridY + 0.5) * u) + offsetPx;

                        dibujarFichaVisual(g2, cx, cy, u, idJugador, idFicha);
                    }
                }
            });
        });
    }

    private void dibujarFichaVisual(Graphics2D g2, int cx, int cy, double u, int idJugador, int idFicha) {
        int r = (int) Math.round(u * 0.42);
        Color colorJugador = coloresLocales.getOrDefault(idJugador, Color.RED);

        g2.setColor(new Color(0, 0, 0, 70));
        g2.fillOval(cx - r + 2, cy - r + 2, r * 2, r * 2);

        g2.setColor(colorJugador);
        g2.fillOval(cx - r, cy - r, r * 2, r * 2);

        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2.0f));
        g2.drawOval(cx - r + 3, cy - r + 3, (r - 3) * 2, (r - 3) * 2);

        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(1.8f));
        g2.drawOval(cx - r, cy - r, r * 2, r * 2);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, Math.max(10, (int) (u * 0.38))));
        FontMetrics fm = g2.getFontMetrics();
        String txt = String.valueOf(idFicha);
        int tx = cx - fm.stringWidth(txt) / 2;
        int ty = cy + (fm.getAscent() - fm.getDescent()) / 2;
        g2.drawString(txt, tx, ty);

        fichasHitboxes.add(new FichaHitbox(idJugador, idFicha, cx, cy, r));
    }

    private void dibujarMetaCentral(Graphics2D g2, int ox, int oy, double u, Color red, Color blue, Color yellow, Color green) {
        int x1 = (int) Math.round(ox + 8 * u);
        int x2 = (int) Math.round(ox + 11 * u);
        int y1 = (int) Math.round(oy + 8 * u);
        int y2 = (int) Math.round(oy + 11 * u);

        int cx = (int) Math.round(ox + 9.5 * u);
        int cy = (int) Math.round(oy + 9.5 * u);

        Polygon polyTop = new Polygon(new int[]{x1, x2, cx}, new int[]{y1, y1, cy}, 3);
        g2.setColor(red);
        g2.fillPolygon(polyTop);

        Polygon polyRight = new Polygon(new int[]{x2, x2, cx}, new int[]{y1, y2, cy}, 3);
        g2.setColor(blue);
        g2.fillPolygon(polyRight);

        Polygon polyBottom = new Polygon(new int[]{x2, x1, cx}, new int[]{y2, y2, cy}, 3);
        g2.setColor(yellow);
        g2.fillPolygon(polyBottom);

        Polygon polyLeft = new Polygon(new int[]{x1, x1, cx}, new int[]{y2, y1, cy}, 3);
        g2.setColor(green);
        g2.fillPolygon(polyLeft);

        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawPolygon(polyTop);
        g2.drawPolygon(polyRight);
        g2.drawPolygon(polyBottom);
        g2.drawPolygon(polyLeft);
        g2.drawRect(x1, y1, x2 - x1, y2 - y1);
    }

    private void dibujarAlaSuperior(Graphics2D g2, int ox, int oy, double u, Color red) {
        int[] numIzq = {35, 36, 37, 38, 39, 40, 41, 42};
        int[] numDer = {33, 32, 31, 30, 29, 28, 27, 26};

        for (int r = 0; r < 8; r++) {
            Color bgIzq = (r == 4) ? red : Color.WHITE;
            dibujarCasilla(g2, ox, oy, u, 8, r, bgIzq, String.valueOf(numIzq[r]), false);

            if (r == 0) {
                dibujarCasilla(g2, ox, oy, u, 9, r, Color.WHITE, "34", true);
            } else {
                dibujarCasilla(g2, ox, oy, u, 9, r, red, "", false);
            }

            dibujarCasilla(g2, ox, oy, u, 10, r, Color.WHITE, String.valueOf(numDer[r]), false);
        }
    }

    private void dibujarAlaDerecha(Graphics2D g2, int ox, int oy, double u, Color blue) {
        int[] numTop = {25, 24, 23, 22, 21, 20, 19, 18};
        int[] numBot = {9, 10, 11, 12, 13, 14, 15, 16};

        for (int c = 0; c < 8; c++) {
            double gx = 11 + c;

            Color bgTop = (c == 3) ? blue : Color.WHITE;
            dibujarCasilla(g2, ox, oy, u, gx, 8, bgTop, String.valueOf(numTop[c]), false);

            if (c == 7) {
                dibujarCasilla(g2, ox, oy, u, gx, 9, Color.WHITE, "17", true);
            } else {
                dibujarCasilla(g2, ox, oy, u, gx, 9, blue, "", false);
            }

            dibujarCasilla(g2, ox, oy, u, gx, 10, Color.WHITE, String.valueOf(numBot[c]), false);
        }
    }

    private void dibujarAlaInferior(Graphics2D g2, int ox, int oy, double u, Color yellow) {
        int[] numDer = {8, 7, 6, 5, 4, 3, 2, 1};
        int[] numIzq = {60, 61, 62, 63, 64, 65, 66, 67};

        for (int r = 0; r < 8; r++) {
            double gy = 11 + r;

            dibujarCasilla(g2, ox, oy, u, 8, gy, Color.WHITE, String.valueOf(numIzq[r]), false);

            if (r == 7) {
                dibujarCasilla(g2, ox, oy, u, 9, gy, Color.WHITE, "68", true);
            } else {
                dibujarCasilla(g2, ox, oy, u, 9, gy, yellow, "", false);
            }

            Color bgDer = (r == 3) ? yellow : Color.WHITE;
            dibujarCasilla(g2, ox, oy, u, 10, gy, bgDer, String.valueOf(numDer[r]), false);
        }
    }

    private void dibujarAlaIzquierda(Graphics2D g2, int ox, int oy, double u, Color green) {
        int[] numTop = {50, 49, 48, 47, 46, 45, 44, 43};
        int[] numBot = {52, 53, 54, 55, 56, 57, 58, 59};

        for (int c = 0; c < 8; c++) {
            double gx = c;

            dibujarCasilla(g2, ox, oy, u, gx, 8, Color.WHITE, String.valueOf(numTop[c]), false);

            if (c == 0) {
                dibujarCasilla(g2, ox, oy, u, gx, 9, Color.WHITE, "51", true);
            } else {
                dibujarCasilla(g2, ox, oy, u, gx, 9, green, "", false);
            }

            Color bgBot = (c == 4) ? green : Color.WHITE;
            dibujarCasilla(g2, ox, oy, u, gx, 10, bgBot, String.valueOf(numBot[c]), false);
        }
    }

    private void dibujarCasilla(Graphics2D g2, int ox, int oy, double u, double gridX, double gridY, Color bg, String texto, boolean esCirculoSeguro) {
        int rx = (int) Math.round(ox + gridX * u);
        int ry = (int) Math.round(oy + gridY * u);
        int rw = (int) Math.round(u);
        int rh = (int) Math.round(u);

        g2.setColor(bg);
        g2.fillRect(rx, ry, rw, rh);

        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(1.0f));
        g2.drawRect(rx, ry, rw, rh);

        if (esCirculoSeguro) {
            int diameter = (int) Math.round(u * 0.82);
            int cx = rx + (rw - diameter) / 2;
            int cy = ry + (rh - diameter) / 2;

            g2.setColor(new Color(215, 215, 215));
            g2.fillOval(cx, cy, diameter, diameter);
            g2.setColor(Color.BLACK);
            g2.drawOval(cx, cy, diameter, diameter);
        }

        if (texto != null && !texto.isEmpty()) {
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("SansSerif", Font.PLAIN, Math.max(9, (int) (u * 0.36))));
            FontMetrics fm = g2.getFontMetrics();
            int tx = rx + (rw - fm.stringWidth(texto)) / 2;
            int ty = ry + (rh + fm.getAscent() - fm.getDescent()) / 2;
            g2.drawString(texto, tx, ty);
        }
    }

    public void renderizarTablero() {
        if (panelTableroRender != null) {
            panelTableroRender.repaint();
        }
    }

    public void actualizarFichas() {
        renderizarTablero();
    }

    public void mostrarResultadoDado(int valor) {
        btnDado.setText("🎲 Dado: " + valor);
        actualizarTextoEstado("Resultado del dado: " + valor);
    }

    public void mostrarTurnoActual(int idJugador) {
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
            actualizarTextoEstado("Esperando confirmación de todos los jugadores...");
        });
    }

    public void prepararInterfazParaInicio(int miId, int turnoActual) {
        SwingUtilities.invokeLater(() -> {
            mostrarPantalla(PANTALLA_JUEGO);

            btnAccionLobby.setText("En Partida");
            btnAccionLobby.setEnabled(false);

            boolean esMiTurno = (miId == turnoActual);
            btnDado.setEnabled(esMiTurno);

            String mensajeEstado = esMiTurno
                    ? "¡Es TU turno! Haz clic en Lanzar Dado."
                    : "Turno del Jugador " + turnoActual + ". Esperando su jugada...";

            actualizarTextoEstado(mensajeEstado);
            mostrarTurnoActual(turnoActual);
            renderizarTablero();

            panelContenedor.revalidate();
            panelContenedor.repaint();
        });
    }

    public void actualizarTextoEstado(String mensaje) {
        if (lblEstado != null) {
            lblEstado.setText(mensaje);
        }
    }

    public void mostrarMensaje(String mensaje) {
        actualizarTextoEstado(mensaje);
    }

    public void mostrarTablaPosiciones(String resumen) {
        JOptionPane.showMessageDialog(this, resumen, "Tabla de Posiciones - Fin del Juego", JOptionPane.PLAIN_MESSAGE);
    }

    public void mostrarPantalla(String nombrePantalla) {
        cardLayout.show(panelContenedor, nombrePantalla);
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

    public void setBotonDadoHabilitado(boolean habilitado) {
        SwingUtilities.invokeLater(() -> btnDado.setEnabled(habilitado));
    }

    private static void inicializarCoordenadasCasillas() {
        int[][] coords = {
                {10, 18}, {10, 17}, {10, 16}, {10, 15}, {10, 14}, {10, 13}, {10, 12}, {10, 11},
                {11, 10}, {12, 10}, {13, 10}, {14, 10}, {15, 10}, {16, 10}, {17, 10}, {18, 10},
                {18, 9},  {18, 8},  {17, 8},  {16, 8},  {15, 8},  {14, 8},  {13, 8},  {12, 8},  {11, 8},
                {10, 7},  {10, 6},  {10, 5},  {10, 4},  {10, 3},  {10, 2},  {10, 1},  {10, 0},  {9, 0},
                {8, 0},   {8, 1},   {8, 2},   {8, 3},   {8, 4},   {8, 5},   {8, 6},   {8, 7},
                {7, 8},   {6, 8},   {5, 8},   {4, 8},   {3, 8},   {2, 8},   {1, 8},   {0, 8},
                {0, 9},   {0, 10},  {1, 10},  {2, 10},  {3, 10},  {4, 10},  {5, 10},  {6, 10},  {7, 10},
                {8, 11},  {8, 12},  {8, 13},  {8, 14},  {8, 15},  {8, 16},  {8, 17},  {8, 18},  {9, 18}
        };

        for (int i = 0; i < coords.length; i++) {
            POS_CASILLAS[i + 1] = coords[i];
        }
    }

    private static class FichaHitbox {
        int idJugador;
        int idFicha;
        int x, y, radius;

        public FichaHitbox(int idJugador, int idFicha, int x, int y, int radius) {
            this.idJugador = idJugador;
            this.idFicha = idFicha;
            this.x = x;
            this.y = y;
            this.radius = radius;
        }

        public boolean contains(int mx, int my) {
            int dx = mx - x;
            int dy = my - y;
            return (dx * dx + dy * dy) <= (radius * radius);
        }
    }

    public void setControlador(ControladorJuego controlador) { this.controlador = controlador; }
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
    public int getCantFichasSeleccionadas() { return (Integer) cbCantFichas.getSelectedItem(); }
    public int getCasillasSegurasSeleccionadas() { return (Integer) cbCasillasSeguras.getSelectedItem(); }
    public int getNumSalidaSeleccionado() { return (Integer) cbNumSalida.getSelectedItem(); }
}