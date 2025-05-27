package com.example.hereapp_backend.controllers;

import com.example.hereapp_backend.dataAccess.UsuarioBBDD;
import com.example.hereapp_backend.models.MifareRegistro;
import com.example.hereapp_backend.models.UsuarioDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/usuario")
public class UsuarioController {

    private final UsuarioBBDD usuarioBBDD;

    public UsuarioController(UsuarioBBDD usuarioBBDD) {
        this.usuarioBBDD = usuarioBBDD;
    }

    @GetMapping
    public ResponseEntity<UsuarioDTO> getUsuarioPorEmail(@RequestParam String email) {
        try {
            UsuarioDTO dto = usuarioBBDD.getByEmail(email);
            return ResponseEntity.ok(dto);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PostMapping("/registrar-mifare")
    public ResponseEntity<String> registrarMifare(@RequestBody MifareRegistro registro) {
        try {
            // Validar que el UID no esté vacío
            if (registro.getUidMifare() == null || registro.getUidMifare().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("El UID de la tarjeta no puede estar vacío");
            }

            // Validar que el email del usuario esté presente
            if (registro.getEmailUsuario() == null || registro.getEmailUsuario().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("El email del usuario no puede estar vacío");
            }

            // Llamada al método que registra el mifare en la base de datos
            usuarioBBDD.registrarMifare(registro.getUidMifare().trim(), registro.getEmailUsuario().trim());
            return ResponseEntity.ok("Tarjeta registrada correctamente");
        } catch (RuntimeException ex) {
            // Manejo específico para diferentes tipos de errores
            if (ex.getMessage().contains("ya está registrada")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ex.getMessage());
            } else if (ex.getMessage().contains("No tiene permisos")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ex.getMessage());
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al registrar mifare: " + ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error inesperado al registrar mifare: " + ex.getMessage());
        }
    }
}