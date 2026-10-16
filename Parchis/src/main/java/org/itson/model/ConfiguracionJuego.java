package org.itson.model;

public class ConfiguracionJuego {
    private int fichasPorJugador;
    private int casillasSegurasPorAla;
    private int numeroSalidaCasa;

    public ConfiguracionJuego() {
        cargarConfiguracionPredeterminada();
    }

    public ConfiguracionJuego(int fichasPorJugador, int casillasSegurasPorAla, int numeroSalidaCasa) {
        this.fichasPorJugador = fichasPorJugador;
        this.casillasSegurasPorAla = casillasSegurasPorAla;
        this.numeroSalidaCasa = numeroSalidaCasa;
    }

    public void cargarConfiguracionPredeterminada(){
        this.fichasPorJugador = 4;
        this.casillasSegurasPorAla = 12;
        this.numeroSalidaCasa = 5;
    }

    public int getFichasPorJugador() {
        return fichasPorJugador;
    }

    public int getCasillasSegurasPorAla() {
        return casillasSegurasPorAla;
    }

    public int getNumeroSalidaCasa() {
        return numeroSalidaCasa;
    }

    public void setFichasPorJugador(int fichasPorJugador) {
        this.fichasPorJugador = fichasPorJugador;
    }

    public void setCasillasSegurasPorAla(int casillasSegurasPorAla) {
        this.casillasSegurasPorAla = casillasSegurasPorAla;
    }

    public void setNumeroSalidaCasa(int numeroSalidaCasa) {
        this.numeroSalidaCasa = numeroSalidaCasa;
    }
}