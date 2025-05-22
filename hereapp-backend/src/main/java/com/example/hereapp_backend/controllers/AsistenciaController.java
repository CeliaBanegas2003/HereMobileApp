package com.example.hereapp_backend.controllers;

import com.example.hereapp_backend.dataAccess.AsistenciaBBDD;
import com.example.hereapp_backend.models.Asistencia;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/asistencia")
public class AsistenciaController {

    private final AsistenciaBBDD asistenciaBBDD;

    public AsistenciaController(AsistenciaBBDD asistenciaBBDD) {
        this.asistenciaBBDD = asistenciaBBDD;
    }

    @PostMapping("/procesar-nfc")
    public ResponseEntity<String> procesarNFC(@RequestBody Asistencia request) {
        try {
            // Validar que los datos no estén vacíos
            if (request.getUidMifare() == null || request.getUidMifare().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("El UID de la tarjeta no puede estar vacío");
            }

            if (request.getEmailUsuario() == null || request.getEmailUsuario().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("El email del usuario no puede estar vacío");
            }

            // Procesar la asistencia
            String resultado = asistenciaBBDD.procesarAsistenciaNFC(
                    request.getUidMifare().trim(),
                    request.getEmailUsuario().trim()
            );

            return ResponseEntity.ok(resultado);

        } catch (RuntimeException ex) {
            // Manejo específico para tarjetas no registradas
            if (ex.getMessage().contains("no está registrada")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ex.getMessage());
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al procesar asistencia: " + ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error inesperado al procesar asistencia: " + ex.getMessage());
        }
    }
}