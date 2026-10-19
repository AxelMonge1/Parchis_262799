package org.itson.model;

import org.itson.model.enums.EstadoFicha;
import org.itson.model.enums.EstadoJuego;

import java.util.List;

public class ModeloJuego {
    private List<Jugador> jugadores;
    private Tablero tablero;
    private Dado dado;
    private ConfiguracionJuego configuracionJuego;
    private int turnoActual;
    private int contadorSeisSeguidos;
    private int pasosExtraPendientes;
    private EstadoJuego estado;
    private long versionEstado;

    public ModeloJuego() {
        this.dado = new Dado();
    }

    public ModeloJuego(List<Jugador> jugadores, Tablero tablero, Dado dado, ConfiguracionJuego configuracionJuego, int turnoActual, int contadorSeisSeguidos, int pasosExtraPendientes, EstadoJuego estado, long versionEstado) {
        this.jugadores = jugadores;
        this.tablero = tablero;
        this.dado = dado;
        this.configuracionJuego = configuracionJuego;
        this.turnoActual = turnoActual;
        this.contadorSeisSeguidos = contadorSeisSeguidos;
        this.pasosExtraPendientes = pasosExtraPendientes;
        this.estado = estado;
        this.versionEstado = versionEstado;
    }

    public List<Jugador> getJugadores() {
        return jugadores;
    }

    public void setJugadores(List<Jugador> jugadores) {
        this.jugadores = jugadores;
    }

    public Tablero getTablero() {
        return tablero;
    }

    public void setTablero(Tablero tablero) {
        this.tablero = tablero;
    }

    public Dado getDado() {
        return dado;
    }

    public void setDado(Dado dado) {
        this.dado = dado;
    }

    public ConfiguracionJuego getConfiguracionJuego() {
        return configuracionJuego;
    }

    public void setConfiguracionJuego(ConfiguracionJuego configuracionJuego) {
        this.configuracionJuego = configuracionJuego;
    }

    public int getTurnoActual() {
        return turnoActual;
    }

    public void setTurnoActual(int turnoActual) {
        this.turnoActual = turnoActual;
    }

    public int getContadorSeisSeguidos() {
        return contadorSeisSeguidos;
    }

    public void setContadorSeisSeguidos(int contadorSeisSeguidos) {
        this.contadorSeisSeguidos = contadorSeisSeguidos;
    }

    public int getPasosExtraPendientes() {
        return pasosExtraPendientes;
    }

    public void setPasosExtraPendientes(int pasosExtraPendientes) {
        this.pasosExtraPendientes = pasosExtraPendientes;
    }

    public EstadoJuego obtenerEstado() {
        return estado;
    }

    public void setEstado(EstadoJuego estado) {
        this.estado = estado;
    }

    public long getVersionEstado() {
        return versionEstado;
    }

    public void setVersionEstado(long versionEstado) {
        this.versionEstado = versionEstado;
    }

    public int lanzarDado(){
        return dado.lanzar();
    }

    public boolean moverFicha(int idFicha, int pasos){
        Jugador jugador = this.jugadores.get(this.turnoActual);
        Ficha ficha = jugador.obtenerFichaPorId(idFicha);

        if(ficha.getEstado() == EstadoFicha.EN_CASA){
            return false;
        }

        final int CASILLAS_TOTALES_META = 72;
        int pasosPrevistos = ficha.getPasosRecorridos() + pasos;
        if(pasosPrevistos > CASILLAS_TOTALES_META){
            int rebote = pasosPrevistos - CASILLAS_TOTALES_META;
            ficha.setPasosRecorridos(CASILLAS_TOTALES_META - rebote);
        } else {
            ficha.setPasosRecorridos(pasosPrevistos);
        }

        Casilla nuevaCasilla = tablero.calcularSiguienteCasilla(jugador.getId(), ficha.getPasosRecorridos());
        ficha.setPosicionActual(nuevaCasilla);
        evaluarCapturaOCoronacion(ficha);
        this.versionEstado++;

        return true;
    }

    public void evaluarCapturaOCoronacion(Ficha fichaMovida) {
        if (fichaMovida.getPasosRecorridos() == 72) {
            fichaMovida.setEstado(EstadoFicha.EN_META);
            this.pasosExtraPendientes += 10;
            return;
        }

        Casilla actual = fichaMovida.getPosicionActual();
        if (!actual.esSegura()) {
            for (Jugador jugador : this.jugadores) {
                if (jugador.getId() == fichaMovida.getIdJugador()) {
                    continue;
                }

                for (Ficha fichaRival : jugador.getFichas()) {
                    if (fichaRival.getPosicionActual() != null && fichaRival.getPosicionActual().getId() == actual.getId()) {
                        fichaRival.setEstado(EstadoFicha.EN_CASA);
                        fichaRival.setPasosRecorridos(0);
                        this.pasosExtraPendientes += 20;
                        return;
                    }
                }
            }
        }
    }

    public void cambiarTurno(){
        this.contadorSeisSeguidos = 0;
        this.pasosExtraPendientes = 0;
        this.turnoActual = (this.turnoActual + 1) % this.jugadores.size();
    }

    public boolean esFinDelJuego() {
        for (Jugador jugador : this.jugadores) {
            boolean todasEnMeta = true;

            for (Ficha ficha : jugador.getFichas()) {
                if (ficha.getEstado() != EstadoFicha.EN_META) {
                    todasEnMeta = false;
                    break;
                }
            }
            if (todasEnMeta) {
                return true;
            }
        }
        return false;
    }

    /**
     * Convierte el estado actual del juego a una cadena de texto para enviarla por red.
     */
    public String obtenerEstadoSerializado() {
        // Ejemplo de formato: "TURNO;ULTIMO_DADO"
        // Adapta los atributos a los nombres reales de tu ModeloJuego
        return this.turnoActual + ";" + this.dado.getValorActual();
    }

    /**
     * Recibe la cadena serializada desde la red y actualiza el estado local del modelo.
     */
    public void actualizarDesdeRed(String datosEstado) {
        if (datosEstado == null || datosEstado.trim().isEmpty() || "null".equalsIgnoreCase(datosEstado)) {
            return;
        }

        try {
            String[] partes = datosEstado.split(";");

            if (partes.length >= 2) {
                this.turnoActual = Integer.parseInt(partes[0]);
                this.dado.setValorActual(Integer.parseInt(partes[1]));
            }

            // Si tu modelo maneja posiciones de fichas en la cadena, se parsean aquí

        } catch (Exception e) {
            System.err.println("Error deserializando el estado en ModeloJuego: " + e.getMessage());
        }
    }
}