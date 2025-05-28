package com.example.hereapp_backend.dataAccess;

import com.example.hereapp_backend.models.Usuario;
import com.example.hereapp_backend.models.UsuarioDTO;
import com.example.hereapp_backend.repository.UsuarioRepository;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

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

    public void registrarMifare(String uidMifare, String emailUsuario) {
        try {
            // Verificar si el usuario que solicita el registro es administrador
            if (!esUsuarioAdmin(emailUsuario)) {
                throw new RuntimeException("No tiene permisos para registrar tarjetas. Solo los administradores pueden realizar esta acción.");
            }

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

    // ===== MÉTODOS INTEGRADOS DE UsuarioService =====

    public Integer obtenerUsuarioId(String email) {
        try {
            String sql = "SELECT usuario_id FROM usuario WHERE email = ?";
            return jdbcTemplate.queryForObject(sql, Integer.class, email);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean tieneRol(Integer userId, String rolNombre) {
        try {
            String sql = """
                SELECT COUNT(*) 
                FROM usuario_roles ur 
                WHERE ur.usuario_id = ? AND ur.rol_nombre = ?
                """;

            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId, rolNombre);
            return count > 0;
        } catch (Exception e) {
            System.err.println("Error verificando rol " + rolNombre + " para usuario " + userId + ": " + e.getMessage());
            return false;
        }
    }

    public boolean esUsuarioAdmin(String emailUsuario) {
        try {
            Integer userId = obtenerUsuarioId(emailUsuario);
            if (userId == null) {
                return false;
            }

            return tieneRol(userId, "ADMIN");
        } catch (Exception e) {
            System.err.println("Error verificando si usuario es admin: " + e.getMessage());
            return false;
        }
    }

    public String determinarRolUsuario(Integer userId) {
        try {
            String sql = """
                SELECT ur.rol_nombre
                FROM usuario_roles ur 
                WHERE ur.usuario_id = ?
                """;

            List<Map<String, Object>> rolesResult = jdbcTemplate.queryForList(sql, userId);

            boolean esProfesor = false;
            boolean esAlumno = false;
            boolean esAdmin = false;

            for (Map<String, Object> row : rolesResult) {
                String rolNombre = (String) row.get("rol_nombre");
                if (rolNombre != null) {
                    switch (rolNombre.toUpperCase()) {
                        case "PROFESOR":
                            esProfesor = true;
                            break;
                        case "ALUMNO":
                            esAlumno = true;
                            break;
                        case "ADMIN":
                            esAdmin = true;
                            break;
                    }
                }
            }

            // Devolver el rol prioritario para procesamiento de asistencia
            if (esProfesor) {
                return "profesor";
            } else if (esAlumno) {
                return "alumno";
            } else if (esAdmin) {
                return "admin";
            }

            return "otro";

        } catch (Exception e) {
            System.err.println("Error determinando rol del usuario " + userId + ": " + e.getMessage());
            return "otro";
        }
    }
}