package com.example.hereapp_backend.dataAccess;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Service
public class SesionBBDD {

    private final JdbcTemplate jdbcTemplate;
    private final AsistenciaAlumnoBBDD asistenciaAlumnoBBDD;
    private final HorarioBBDD horarioBBDD;

    public SesionBBDD(JdbcTemplate jdbcTemplate, AsistenciaAlumnoBBDD asistenciaAlumnoBBDD,
                      HorarioBBDD horarioBBDD) {
        this.jdbcTemplate = jdbcTemplate;
        this.asistenciaAlumnoBBDD = asistenciaAlumnoBBDD;
        this.horarioBBDD = horarioBBDD;
    }

    public Map<String, Object> obtenerSesionAbiertaProfesor(Integer profesorId) {
        String sql = """
            SELECT sesion_id, asignatura_id, grupo_id, fecha_creacion
            FROM sesion
            WHERE profesor_id = ?
              AND hora_salida_profesor IS NULL
            ORDER BY sesion_id DESC
            LIMIT 1
            """;

        List<Map<String, Object>> sesiones = jdbcTemplate.queryForList(sql, profesorId);
        return sesiones.isEmpty() ? null : sesiones.get(0);
    }

    public int calcularMinutosSesion(Integer sesionId) {
        String sql = """
            SELECT TIMESTAMPDIFF(MINUTE, 
                CONCAT(fecha_creacion, ' ', hora_entrada_profesor), 
                CONVERT_TZ(NOW(), 'UTC', 'Europe/Madrid')) AS diffMin 
            FROM sesion 
            WHERE sesion_id = ?
            """;
        Integer diffMin = jdbcTemplate.queryForObject(sql, Integer.class, sesionId);
        return diffMin != null ? diffMin : 0;
    }

    public void cerrarSesion(Integer sesionId) {
        jdbcTemplate.update("""
            UPDATE sesion
            SET hora_salida_profesor = TIME(CONVERT_TZ(NOW(), 'UTC', 'Europe/Madrid'))
            WHERE sesion_id = ?
            """, sesionId);
    }

    public String crearNuevaSesion(Integer profesorId, Integer tarjetaId) {
        String diaSemana = horarioBBDD.obtenerDiaSemanaActual();
        LocalTime horaActual = LocalTime.now();

        // Buscar asignatura en horario ±20 minutos usando HorarioBBDD
        Map<String, Object> horario = horarioBBDD.buscarHorarioActual(profesorId, diaSemana);

        if (horario == null) {
            return "No hay clase programada para este horario";
        }

        Integer asignaturaId = (Integer) horario.get("asignatura_id");
        Integer grupoId = (Integer) horario.get("grupo_id");

        // Crear sesión
        String sql = """
            INSERT INTO sesion
            (fecha_creacion, usuario_id, profesor_id, hora_entrada_profesor, asignatura_id, grupo_id, tarjeta_id)
            VALUES (DATE(CONVERT_TZ(NOW(), 'UTC', 'Europe/Madrid')), ?, ?, TIME(CONVERT_TZ(NOW(), 'UTC', 'Europe/Madrid')), ?, ?, ?)
            """;

        jdbcTemplate.update(sql, profesorId, profesorId, asignaturaId, grupoId, tarjetaId);

        // Obtener el ID de la sesión creada
        Integer nuevaSesionId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);

        // Crear registros de asistencia para alumnos matriculados
        asistenciaAlumnoBBDD.crearRegistrosAlumnosMatriculados(nuevaSesionId, asignaturaId, grupoId);

        return "Nueva sesión creada";
    }

    public Object obtenerHoraSalidaProfesor(Integer sesionId) {
        String sql = "SELECT hora_salida_profesor FROM sesion WHERE sesion_id = ?";
        List<Map<String, Object>> sesiones = jdbcTemplate.queryForList(sql, sesionId);

        if (!sesiones.isEmpty()) {
            return sesiones.get(0).get("hora_salida_profesor");
        }
        return null;
    }
}