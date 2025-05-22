package com.example.hereapp_backend.models;

public class MifareRegistro {
    private String uidMifare;

    // Constructores
    public MifareRegistro() {}

    public MifareRegistro(String uidMifare) {
        this.uidMifare = uidMifare;
    }

    // Getters y setters
    public String getUidMifare() {
        return uidMifare;
    }

    public void setUidMifare(String uidMifare) {
        this.uidMifare = uidMifare;
    }
}