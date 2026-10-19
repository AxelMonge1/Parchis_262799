package org.itson.model;

import java.util.HashMap;
import java.util.Map;

public class ModeloJuego {
    private int turnoActual = 1;
    private int totalJugadores = 4;
    private boolean dadoLanzado = false;
    private int numSalida = 5;
    private int pasosExtraPendientes = 0;
    private Dado dado;

    private Map<Integer, Map<Integer, Integer>> posicionesFichas;

    private static final Map<Integer, Integer> CASILLAS_SALIDA = Map.of(
            1, 39,
            2, 22,
            3, 56,
            4, 5
    );

    public ModeloJuego() {
        this.dado = new Dado();
        this.posicionesFichas = new HashMap<>();

        for (int j = 1; j <= 4; j++) {
            Map<Integer, Integer> fichas = new HashMap<>();
            for (int f = 1; f <= 4; f++) {
                fichas.put(f, -1);
            }
            posicionesFichas.put(j, fichas);
        }
    }

    public boolean puedeMoverFicha(int idJugador, int idFicha) {
        if (!dadoLanzado || idJugador != turnoActual) {
            return false;
        }

        Map<Integer, Integer> fichasJugador = posicionesFichas.get(idJugador);
        if (fichasJugador == null || !fichasJugador.containsKey(idFicha)) {
            return false;
        }

        int posActual = fichasJugador.get(idFicha);
        int valorDado = dado.getValorActual();

        if (posActual == -1) {
            return valorDado == numSalida;
        }

        return posActual > 0;
    }

    public boolean tieneMovimientosValidos(int idJugador) {
        if (!dadoLanzado) return false;

        Map<Integer, Integer> fichas = posicionesFichas.get(idJugador);
        if (fichas == null) return false;

        for (Map.Entry<Integer, Integer> entry : fichas.entrySet()) {
            if (puedeMoverFicha(idJugador, entry.getKey())) {
                return true;
            }
        }
        return false;
    }

    public boolean moverFicha(int idFicha, int pasos) {
        if (!puedeMoverFicha(turnoActual, idFicha)) {
            return false;
        }

        Map<Integer, Integer> fichasJugador = posicionesFichas.get(turnoActual);
        int posActual = fichasJugador.get(idFicha);

        if (posActual == -1) {
            int casillaSalida = CASILLAS_SALIDA.getOrDefault(turnoActual, 1);
            fichasJugador.put(idFicha, casillaSalida);
            System.out.println("Ficha " + idFicha + " del Jugador " + turnoActual + " SALIÓ a la casilla " + casillaSalida);
        } else {
            int nuevaPos = posActual + pasos;
            if (nuevaPos > 68) {
                nuevaPos = ((nuevaPos - 1) % 68) + 1;
            }
            fichasJugador.put(idFicha, nuevaPos);
            System.out.println("Ficha " + idFicha + " del Jugador " + turnoActual + " avanzó a la casilla " + nuevaPos);
        }

        this.dadoLanzado = false;
        return true;
    }

    public void cambiarTurno() {
        this.dadoLanzado = false;
        this.pasosExtraPendientes = 0;
        int limiteJugadores = Math.max(1, totalJugadores);
        this.turnoActual = (this.turnoActual % limiteJugadores) + 1;
        System.out.println("Turno cambiado -> Ahora es turno del Jugador " + this.turnoActual + " (Total jugadores: " + limiteJugadores + ")");
    }

    public String obtenerEstadoSerializado() {
        StringBuilder sb = new StringBuilder();
        sb.append(turnoActual).append(";")
                .append(dado.getValorActual()).append(";")
                .append(dadoLanzado).append(";")
                .append(numSalida).append(";")
                .append(totalJugadores).append("#");

        posicionesFichas.forEach((jugadorId, fichas) -> {
            sb.append(jugadorId).append(":");
            fichas.forEach((fichaId, pos) -> {
                sb.append(fichaId).append(",").append(pos).append("|");
            });
            sb.append(";");
        });

        return sb.toString();
    }

    public void actualizarDesdeRed(String datos) {
        if (datos == null || datos.trim().isEmpty() || "null".equalsIgnoreCase(datos)) return;

        try {
            String[] partes = datos.split("#");
            if (partes.length >= 1) {
                String[] cabecera = partes[0].split(";");
                if (cabecera.length >= 3) {
                    this.turnoActual = Integer.parseInt(cabecera[0]);
                    int valor = Integer.parseInt(cabecera[1]);
                    this.dado.setValorActual(valor);
                    this.dadoLanzado = Boolean.parseBoolean(cabecera[2]);

                    if (cabecera.length >= 4) {
                        this.numSalida = Integer.parseInt(cabecera[3]);
                    }
                    if (cabecera.length >= 5) {
                        this.totalJugadores = Integer.parseInt(cabecera[4]);
                    }
                }
            }

            if (partes.length >= 2) {
                String[] jugadores = partes[1].split(";");
                for (String jugData : jugadores) {
                    if (jugData.trim().isEmpty()) continue;
                    String[] jugPartes = jugData.split(":");
                    if (jugPartes.length < 2) continue;

                    int idJugador = Integer.parseInt(jugPartes[0]);
                    String[] fichasData = jugPartes[1].split("\\|");

                    Map<Integer, Integer> fichasMap = posicionesFichas.computeIfAbsent(idJugador, k -> new HashMap<>());
                    for (String fichaData : fichasData) {
                        if (fichaData.trim().isEmpty()) continue;
                        String[] fPos = fichaData.split(",");
                        if (fPos.length >= 2) {
                            int idFicha = Integer.parseInt(fPos[0]);
                            int pos = Integer.parseInt(fPos[1]);
                            fichasMap.put(idFicha, pos);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error deserializando estado del modelo: " + e.getMessage());
        }
    }

    public static class Dado {
        private int valorActual = 0;

        public int lanzar() {
            this.valorActual = (int) (Math.random() * 6) + 1;
            return this.valorActual;
        }

        public int getValorActual() { return valorActual; }
        public void setValorActual(int valor) { this.valorActual = valor; }
    }

    public int getTurnoActual() { return turnoActual; }
    public void setTurnoActual(int turnoActual) { this.turnoActual = turnoActual; }
    public int getTotalJugadores() { return totalJugadores; }
    public void setTotalJugadores(int totalJugadores) { this.totalJugadores = totalJugadores; }
    public boolean isDadoLanzado() { return dadoLanzado; }
    public void setDadoLanzado(boolean dadoLanzado) { this.dadoLanzado = dadoLanzado; }
    public int getNumSalida() { return numSalida; }
    public void setNumSalida(int numSalida) { this.numSalida = numSalida; }
    public int getPasosExtraPendientes() { return pasosExtraPendientes; }
    public void setPasosExtraPendientes(int pasosExtraPendientes) { this.pasosExtraPendientes = pasosExtraPendientes; }
    public Dado getDado() { return dado; }
    public Map<Integer, Map<Integer, Integer>> getPosicionesFichas() { return posicionesFichas; }
}