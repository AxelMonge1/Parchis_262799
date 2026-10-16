package org.itson.model;

import org.itson.model.enums.EstadoFicha;
import java.util.List;

public class Jugador {
    private int id;
    private String nombre;
    private String avatar;
    private int slotTablero;
    private List<Ficha> fichas;
    private boolean estaListo;
    private boolean esHost;
    private boolean estaConectado;
    private int casillasFaltantesTotales;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public int getSlotTablero() {
        return slotTablero;
    }

    public void setSlotTablero(int slotTablero) {
        this.slotTablero = slotTablero;
    }

    public List<Ficha> getFichas() {
        return fichas;
    }

    public void setFichas(List<Ficha> fichas) {
        this.fichas = fichas;
    }

    public boolean estaListo() {
        return estaListo;
    }

    public void setEstaListo(boolean estaListo) {
        this.estaListo = estaListo;
    }

    public boolean esHost() {
        return esHost;
    }

    public void setEsHost(boolean esHost) {
        this.esHost = esHost;
    }

    public boolean estaActivo() {
        return estaConectado;
    }

    public void setEstaConectado(boolean estaConectado) {
        this.estaConectado = estaConectado;
    }

    public int getCasillasFaltantesTotales() {
        return casillasFaltantesTotales;
    }

    public void setCasillasFaltantesTotales(int casillasFaltantesTotales) {
        this.casillasFaltantesTotales = casillasFaltantesTotales;
    }

    public boolean tieneFichasDisponibles(){
        return !fichas.isEmpty();
    }

    public void actualizarCasillasFaltantes(){
        final int CASILLAS_TOTALES_META = 72;
        int totalFaltantes = 0;

        for(Ficha ficha : this.fichas){
            if(ficha.getEstado() == EstadoFicha.EN_CASA){
                totalFaltantes += CASILLAS_TOTALES_META;
            } else if (ficha.estaEnMeta()) {
                totalFaltantes += 0;
            } else {
                int faltantesFicha = CASILLAS_TOTALES_META - ficha.getPasosRecorridos();
                totalFaltantes += faltantesFicha;
            }
        }
        this.casillasFaltantesTotales = totalFaltantes;
    }

    public Ficha obtenerFichaPorId(int id){
        for(Ficha ficha : fichas){
            if(ficha.getId() == id){
                return ficha;
            }
        }
        return null;
    }
}