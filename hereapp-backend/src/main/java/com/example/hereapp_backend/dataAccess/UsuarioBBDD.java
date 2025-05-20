package com.example.hereapp_backend.dataAccess;

import com.example.hereapp_backend.models.Usuario;
import com.example.hereapp_backend.models.UsuarioDTO;
import com.example.hereapp_backend.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

@Service
public class UsuarioBBDD {

    private final UsuarioRepository repo;

    public UsuarioBBDD(UsuarioRepository repo) {
        this.repo = repo;
    }

    public UsuarioDTO getByEmail(String email) {
        // 1) Busca el usuario; si no existe, lanzamos excepción para que el controlador devuelva 404
        Usuario u = repo.findByEmailIgnoreCase(email.trim());
        if (u == null) {
            throw new RuntimeException("Usuario no encontrado");
        }
        // 2) Convierte la entidad a tu DTO existente (incluye nombre, apellido1, apellido2, email)
        return UsuarioDTO.fromUsuario(u);
    }
}
