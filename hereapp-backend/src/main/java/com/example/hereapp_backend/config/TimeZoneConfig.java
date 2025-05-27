package com.example.hereapp_backend.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import java.util.TimeZone;

@Configuration
public class TimeZoneConfig {

    @PostConstruct
    public void init() {
        // Establecer la zona horaria de Madrid para toda la aplicación
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Madrid"));
        System.out.println("Zona horaria establecida: " + TimeZone.getDefault().getID());
        System.out.println("Hora actual del sistema: " + new java.util.Date());
    }
}