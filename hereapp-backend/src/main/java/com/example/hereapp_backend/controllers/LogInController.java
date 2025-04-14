package com.example.hereapp_backend.controllers;

import com.example.hereapp_backend.Request.LogInRequest;
import com.example.hereapp_backend.dataAccess.LogInBBDD;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class LogInController {

    private final LogInBBDD logInBBDD;

    public LogInController(LogInBBDD logInBBDD) {
        this.logInBBDD = logInBBDD;
    }

    @PostMapping("/tipo")
    public ResponseEntity<String> loginTipo(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String contrasena = body.get("contrasena");


        String rol = logInBBDD.validarTipoUsuario(email, contrasena);


        if (rol == null) {
            return ResponseEntity.ok("NONE");
        } else {
            return ResponseEntity.ok(rol);
        }
    }
}
