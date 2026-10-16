package org.itson.model;

public class Dado {
    private int valorActual;

    public int getValorActual() {
        return valorActual;
    }

    public int lanzar(){
         int num = (int) (Math.random() * 6) + 1;
         this.valorActual = num;
         return num;
     }
}
