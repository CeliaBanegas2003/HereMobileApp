package com.example.hereapp_backend.controllers;

import com.example.hereapp_backend.dataAccess.UsuarioBBDD;
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
}
