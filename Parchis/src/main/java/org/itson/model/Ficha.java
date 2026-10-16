package org.itson.model;

import org.itson.model.enums.EstadoFicha;
import org.itson.model.enums.TipoCasilla;

public class Ficha {
    private int id;
    private int idJugador;
    private EstadoFicha estado;
    private Casilla posicionActual;
    private int pasosRecorridos;

    public Ficha() {
    }

    public Ficha(int id, int idJugador, EstadoFicha estado, Casilla posicionActual, int pasosRecorridos) {
        this.id = id;
        this.idJugador = idJugador;
        this.estado = estado;
        this.posicionActual = posicionActual;
        this.pasosRecorridos = pasosRecorridos;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getIdJugador() {
        return idJugador;
    }

    public void setIdJugador(int idJugador) {
        this.idJugador = idJugador;
    }

    public EstadoFicha getEstado() {
        return estado;
    }

    public void setEstado(EstadoFicha estado) {
        this.estado = estado;
    }

    public Casilla getPosicionActual() {
        return posicionActual;
    }

    public void setPosicionActual(Casilla posicionActual){
        this.posicionActual = posicionActual;
    }

    public int getPasosRecorridos() {
        return pasosRecorridos;
    }

    public void setPasosRecorridos(int pasosRecorridos) {
        this.pasosRecorridos = pasosRecorridos;
    }

    public void moverA(Casilla nuevaCasilla){
        this.posicionActual = nuevaCasilla;
    }

    public void regresarACasa(){
        this.posicionActual = null;
    }

    public boolean estaEnMeta(){
        return this.posicionActual.getTipo() == TipoCasilla.META;
    }
}
