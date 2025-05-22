package com.example.hereapp_backend.models;

public class Asistencia {
    private String uidMifare;
    private String emailUsuario;

    // Constructores
    public Asistencia() {}

    public Asistencia(String uidMifare, String emailUsuario) {
        this.uidMifare = uidMifare;
        this.emailUsuario = emailUsuario;
    }

    // Getters y setters
    public String getUidMifare() {
        return uidMifare;
    }

    public void setUidMifare(String uidMifare) {
        this.uidMifare = uidMifare;
    }

    public String getEmailUsuario() {
        return emailUsuario;
    }

    public void setEmailUsuario(String emailUsuario) {
        this.emailUsuario = emailUsuario;
    }
}