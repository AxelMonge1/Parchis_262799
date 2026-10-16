package org.itson.model;

import java.util.List;
import java.util.Map;

public class Tablero {
    private List<Casilla> casillas;
    private Map<Integer, List<Casilla>> pasillosMeta;
    private Map<Integer, Casilla> casillasSalida;
    private Map<Integer, Casilla> casillasMeta;

    public Tablero() {
    }

    public Tablero(List<Casilla> casillas, Map<Integer, List<Casilla>> pasillosMeta, Map<Integer, Casilla> casillasSalida) {
        this.casillas = casillas;
        this.pasillosMeta = pasillosMeta;
        this.casillasSalida = casillasSalida;
    }

    public List<Casilla> getCasillas() {
        return casillas;
    }

    public void setCasillas(List<Casilla> casillas) {
        this.casillas = casillas;
    }

    public Map<Integer, List<Casilla>> getPasillosMeta() {
        return pasillosMeta;
    }

    public void setPasillosMeta(Map<Integer, List<Casilla>> pasillosMeta) {
        this.pasillosMeta = pasillosMeta;
    }

    public Map<Integer, Casilla> getCasillasSalida() {
        return casillasSalida;
    }

    public void setCasillasSalida(Map<Integer, Casilla> casillasSalida) {
        this.casillasSalida = casillasSalida;
    }

    public Casilla obtenerCasilla(int id){
        for(Casilla casilla: casillas){
            if(casilla.getId() == id){
                return casilla;
            }
        }

        for(List<Casilla> pasillo : this.pasillosMeta.values()){
            for(Casilla casilla: pasillo){
                if(casilla.getId() == id){
                    return casilla;
                }
            }
        }

        return null;
    }

    private int obtenerIndiceSalida(int idJugador) {
        switch(idJugador) {
            case 1: return 4;
            case 2: return 21;
            case 3: return 38;
            case 4: return 55;
            default: return 0;
        }
    }

    private Casilla obtenerCasillaMeta(int idJugador){
        return this.casillasMeta.get(idJugador);
    }

    public Casilla calcularSiguienteCasilla(int idJugador, int pasosRecorridos) {
        if (pasosRecorridos == 72) {
            return obtenerCasillaMeta(idJugador);
        }

        if (pasosRecorridos > 64) {
            int indiceEnPasillo = pasosRecorridos - 65;
            return this.pasillosMeta.get(idJugador).get(indiceEnPasillo);
        }

        int casillaSalida = obtenerIndiceSalida(idJugador);
        int indiceDestino = (casillaSalida + pasosRecorridos - 1) % this.casillas.size();

        return this.casillas.get(indiceDestino);
    }
}
