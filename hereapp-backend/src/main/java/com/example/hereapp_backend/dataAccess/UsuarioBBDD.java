package com.example.hereapp_backend.dataAccess;

import com.example.hereapp_backend.models.Usuario;
import com.example.hereapp_backend.models.UsuarioDTO;
import com.example.hereapp_backend.repository.UsuarioRepository;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class UsuarioBBDD {

    private final UsuarioRepository repo;
    private final JdbcTemplate jdbcTemplate;

    public UsuarioBBDD(UsuarioRepository repo, JdbcTemplate jdbcTemplate) {
        this.repo = repo;
        this.jdbcTemplate = jdbcTemplate;
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

    public void registrarMifare(String uidMifare) {
        try {
            // Verificar si la tarjeta ya existe
            if (existeTarjetaMifare(uidMifare)) {
                throw new RuntimeException("La tarjeta ya está registrada en el sistema");
            }

            // Insertar directamente en la tabla tarjetas_mifare
            String sql = "INSERT INTO tarjetas_mifare (uid_mifare) VALUES (?)";
            jdbcTemplate.update(sql, uidMifare);
        } catch (RuntimeException e) {
            // Re-lanzar excepciones de negocio (como tarjeta ya registrada)
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error al registrar tarjeta: " + e.getMessage());
        }
    }

    private boolean existeTarjetaMifare(String uidMifare) {
        try {
            String sql = "SELECT COUNT(*) FROM tarjetas_mifare WHERE uid_mifare = ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, uidMifare);
            return count != null && count > 0;
        } catch (Exception e) {
            // Si hay error en la consulta, asumimos que no existe para no bloquear el registro
            return false;
        }
    }
}