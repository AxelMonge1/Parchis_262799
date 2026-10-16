package org.itson.model;

import org.itson.model.enums.TipoCasilla;

import java.util.List;

public class Casilla {
    private int id;
    private TipoCasilla tipo;
    private List<Ficha> fichasPresentes;
    private int capacidadMaxima;

    public Casilla() {
    }

    public Casilla(int id, int capacidadMaxima, List<Ficha> fichasPresentes, TipoCasilla tipo) {
        this.id = id;
        this.capacidadMaxima = capacidadMaxima;
        this.fichasPresentes = fichasPresentes;
        this.tipo = tipo;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public TipoCasilla getTipo() {
        return tipo;
    }

    public void setTipo(TipoCasilla tipo) {
        this.tipo = tipo;
    }

    public List<Ficha> getFichasPresentes() {
        return fichasPresentes;
    }

    public void setFichasPresentes(List<Ficha> fichasPresentes) {
        this.fichasPresentes = fichasPresentes;
    }

    public int getCapacidadMaxima() {
        return capacidadMaxima;
    }

    public void setCapacidadMaxima(int capacidadMaxima) {
        this.capacidadMaxima = capacidadMaxima;
    }

    public boolean agregarFicha(Ficha ficha){
        if(estaLlena()){
            return false;
        }else{
            fichasPresentes.add(ficha);
            return true;
        }
    }

    public boolean removerFicha(){
        if(fichasPresentes.isEmpty()){
            return false;
        }else{
            fichasPresentes.removeLast();
            return true;
        }
    }

    public boolean estaLlena(){
        return fichasPresentes.size() == 2;
    }

    public boolean esSegura(){
        return tipo == TipoCasilla.SEGURO;
    }
}
