package org.itson.conection.auxiliar;

public class PaqueteRed {
    private TipoPaquete tipo;
    private int idEmisor;
    private String mensaje;

    public PaqueteRed() {
    }

    public PaqueteRed(TipoPaquete tipo, int idEmisor, String mensaje) {
        this.tipo = tipo;
        this.idEmisor = idEmisor;
        this.mensaje = mensaje;
    }

    public String serializar() {
        return tipo.name() + ";" + idEmisor + ";" + mensaje;
    }

    public int getIdEmisor() {
        return idEmisor;
    }

    public TipoPaquete getTipo() { return tipo; }

    public String getMensaje() { return mensaje; }

    public void setTipo(TipoPaquete tipo) {
        this.tipo = tipo;
    }

    public void setIdEmisor(int idEmisor) {
        this.idEmisor = idEmisor;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}