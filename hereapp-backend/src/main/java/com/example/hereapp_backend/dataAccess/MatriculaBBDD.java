package com.example.hereapp_backend.dataAccess;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class MatriculaBBDD {

    private final JdbcTemplate jdbcTemplate;

    public MatriculaBBDD(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Obtiene todos los alumnos matriculados en una asignatura y grupo específicos
     */
    public List<Map<String, Object>> obtenerAlumnosMatriculados(Integer asignaturaId, Integer grupoId) {
        String sql = "SELECT usuario_id FROM matricula WHERE asignatura_id = ? AND grupo_id = ?";
        return jdbcTemplate.queryForList(sql, asignaturaId, grupoId);
    }

    /**
     * Verifica si un alumno está matriculado en una asignatura y grupo específicos
     */
    public boolean estaMatriculado(Integer usuarioId, Integer asignaturaId, Integer grupoId) {
        try {
            String sql = """
                SELECT COUNT(*) 
                FROM matricula 
                WHERE usuario_id = ? 
                  AND asignatura_id = ? 
                  AND grupo_id = ?
                """;
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class,
                    usuarioId, asignaturaId, grupoId);
            return count != null && count > 0;
        } catch (Exception e) {
            System.err.println("Error verificando matrícula: " + e.getMessage());
            return false;
        }
    }

    /**
     * Obtiene todas las asignaturas en las que está matriculado un alumno
     */
    public List<Map<String, Object>> obtenerAsignaturasDeAlumno(Integer usuarioId) {
        String sql = """
            SELECT m.asignatura_id, m.grupo_id, a.nombre_asignatura, g.grupo_nombre
            FROM matricula m
            INNER JOIN asignatura a ON m.asignatura_id = a.asignatura_id
            INNER JOIN grupo g ON m.grupo_id = g.grupo_id
            WHERE m.usuario_id = ?
            """;
        return jdbcTemplate.queryForList(sql, usuarioId);
    }

    /**
     * Cuenta el número de alumnos matriculados en una asignatura y grupo
     */
    public int contarAlumnosMatriculados(Integer asignaturaId, Integer grupoId) {
        try {
            String sql = """
                SELECT COUNT(*) 
                FROM matricula 
                WHERE asignatura_id = ? 
                  AND grupo_id = ?
                """;
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class,
                    asignaturaId, grupoId);
            return count != null ? count : 0;
        } catch (Exception e) {
            System.err.println("Error contando alumnos matriculados: " + e.getMessage());
            return 0;
        }
    }
}