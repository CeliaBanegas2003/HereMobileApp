package com.example.hereapp_backend.repository;

import com.example.hereapp_backend.models.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Usuario findByEmailIgnoreCase(String email);
}
