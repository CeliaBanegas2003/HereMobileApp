package com.example.hereapp_backend.dataAccess;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class AsistenciaAlumnoBBDD {

    private final JdbcTemplate jdbcTemplate;
    private final TarjetaBBDD tarjetaBBDD;
    private final MatriculaBBDD matriculaBBDD;

    public AsistenciaAlumnoBBDD(JdbcTemplate jdbcTemplate, TarjetaBBDD tarjetaBBDD,
                                MatriculaBBDD matriculaBBDD) {
        this.jdbcTemplate = jdbcTemplate;
        this.tarjetaBBDD = tarjetaBBDD;
        this.matriculaBBDD = matriculaBBDD;
    }

    public Map<String, Object> obtenerAsistenciaAbierta(Integer alumnoId) {
        String sql = """
            SELECT asistencia_alumno_id, sesion_id, hora_entrada_alumno, tipo_asistencia_id
            FROM asistencia_alumno
            WHERE usuario_id = ?
              AND fecha_asistencia_alumno = DATE(CONVERT_TZ(NOW(), 'UTC', 'Europe/Madrid'))
              AND hora_salida_alumno IS NULL
            ORDER BY asistencia_alumno_id DESC
            LIMIT 1
            """;

        List<Map<String, Object>> asistencias = jdbcTemplate.queryForList(sql, alumnoId);
        return asistencias.isEmpty() ? null : asistencias.get(0);
    }

    public void registrarEntrada(Long asistenciaId, Integer tarjetaId) {
        jdbcTemplate.update("""
            UPDATE asistencia_alumno
            SET hora_entrada_alumno = TIME(CONVERT_TZ(NOW(), 'UTC', 'Europe/Madrid')),
                tipo_asistencia_id = 5,
                tarjeta_id = ?
            WHERE asistencia_alumno_id = ?
            """, tarjetaId, asistenciaId);
    }

    public void registrarSalida(Long asistenciaId) {
        jdbcTemplate.update("""
            UPDATE asistencia_alumno
            SET hora_salida_alumno = TIME(CONVERT_TZ(NOW(), 'UTC', 'Europe/Madrid'))
            WHERE asistencia_alumno_id = ?
            """, asistenciaId);
    }

    public void crearNuevaEntrada(Integer alumnoId, Integer tarjetaId) {
        jdbcTemplate.update("""
            INSERT INTO asistencia_alumno
            (usuario_id, fecha_asistencia_alumno, hora_entrada_alumno, tipo_asistencia_id, tarjeta_id)
            VALUES (?, DATE(CONVERT_TZ(NOW(), 'UTC', 'Europe/Madrid')), TIME(CONVERT_TZ(NOW(), 'UTC', 'Europe/Madrid')), 5, ?)
            """, alumnoId, tarjetaId);
    }

    public int calcularMinutosDesdeEntrada(Long asistenciaId) {
        String sql = """
            SELECT TIMESTAMPDIFF(MINUTE, 
                CONCAT(fecha_asistencia_alumno, ' ', hora_entrada_alumno), 
                CONVERT_TZ(NOW(), 'UTC', 'Europe/Madrid')) AS diffMin 
            FROM asistencia_alumno 
            WHERE asistencia_alumno_id = ?
            """;
        Integer diffMin = jdbcTemplate.queryForObject(sql, Integer.class, asistenciaId);
        return diffMin != null ? diffMin : 0;
    }

    public void crearRegistrosAlumnosMatriculados(Integer sesionId, Integer asignaturaId, Integer grupoId) {
        try {
            // Obtener alumnos matriculados usando MatriculaBBDD
            List<Map<String, Object>> matriculados = matriculaBBDD.obtenerAlumnosMatriculados(asignaturaId, grupoId);

            System.out.println("Alumnos matriculados encontrados: " + matriculados.size());

            for (Map<String, Object> matricula : matriculados) {
                Integer alumnoId = (Integer) matricula.get("usuario_id");

                // Buscar la tarjeta asociada al alumno
                Integer tarjetaAlumnoId = tarjetaBBDD.obtenerTarjetaDeUsuario(alumnoId);

                // Buscar registros existentes del alumno de hoy
                String checkSql = """
                    SELECT asistencia_alumno_id, sesion_id, hora_entrada_alumno, asignatura_id, grupo_id 
                    FROM asistencia_alumno 
                    WHERE usuario_id = ? 
                      AND fecha_asistencia_alumno = DATE(CONVERT_TZ(NOW(), 'UTC', 'Europe/Madrid'))
                    ORDER BY asistencia_alumno_id DESC
                    """;

                List<Map<String, Object>> existentes = jdbcTemplate.queryForList(checkSql, alumnoId);

                boolean registroActualizado = false;

                // Buscar si ya existe un registro que podamos actualizar
                for (Map<String, Object> existente : existentes) {
                    Long asistenciaId = ((Number) existente.get("asistencia_alumno_id")).longValue();
                    Object sesionIdExistente = existente.get("sesion_id");
                    Object asignaturaIdExistente = existente.get("asignatura_id");
                    Object grupoIdExistente = existente.get("grupo_id");

                    // Si el registro no tiene sesión asignada O si es de la misma asignatura/grupo
                    if (sesionIdExistente == null ||
                            (asignaturaId.equals(asignaturaIdExistente) && grupoId.equals(grupoIdExistente))) {

                        // Actualizar el registro existente incluyendo tarjeta_id si no lo tiene
                        jdbcTemplate.update("""
                            UPDATE asistencia_alumno 
                            SET sesion_id = ?, 
                                asignatura_id = ?, 
                                grupo_id = ?,
                                tarjeta_id = COALESCE(tarjeta_id, ?)
                            WHERE asistencia_alumno_id = ?
                            """, sesionId, asignaturaId, grupoId, tarjetaAlumnoId, asistenciaId);

                        registroActualizado = true;
                        System.out.println("Actualizado registro existente para alumno " + alumnoId);
                        break;
                    }
                }

                // Solo crear nuevo registro si no se actualizó ninguno existente
                if (!registroActualizado) {
                    // Verificar que no exista ya un registro para esta sesión específica
                    String checkDuplicadoSql = """
                        SELECT COUNT(*) 
                        FROM asistencia_alumno 
                        WHERE usuario_id = ? 
                          AND sesion_id = ?
                          AND fecha_asistencia_alumno = DATE(CONVERT_TZ(NOW(), 'UTC', 'Europe/Madrid'))
                        """;

                    Integer countDuplicado = jdbcTemplate.queryForObject(checkDuplicadoSql, Integer.class,
                            alumnoId, sesionId);

                    if (countDuplicado == null || countDuplicado == 0) {
                        // Crear nuevo registro incluyendo tarjeta_id
                        jdbcTemplate.update("""
                            INSERT INTO asistencia_alumno 
                            (usuario_id, asignatura_id, grupo_id, sesion_id, fecha_asistencia_alumno, tipo_asistencia_id, tarjeta_id) 
                            VALUES (?, ?, ?, ?, DATE(CONVERT_TZ(NOW(), 'UTC', 'Europe/Madrid')), 2, ?)
                            """, alumnoId, asignaturaId, grupoId, sesionId, tarjetaAlumnoId);

                        System.out.println("Creado nuevo registro para alumno " + alumnoId + " con tarjeta " + tarjetaAlumnoId);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error creando registros de alumnos matriculados: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void marcarAsistencia(Long asistenciaId, int tipoAsistencia) {
        jdbcTemplate.update("""
            UPDATE asistencia_alumno
            SET tipo_asistencia_id = ?
            WHERE asistencia_alumno_id = ?
            """, tipoAsistencia, asistenciaId);
    }

    public void insertarNoAsistencias(Integer sesionId, Integer asignaturaId,
                                      Integer grupoId, LocalDate fechaCreacion) {
        try {
            // Obtener alumnos matriculados usando MatriculaBBDD
            List<Map<String, Object>> matriculados = matriculaBBDD.obtenerAlumnosMatriculados(asignaturaId, grupoId);

            for (Map<String, Object> matricula : matriculados) {
                Integer alumnoId = (Integer) matricula.get("usuario_id");

                // Buscar la tarjeta asociada al alumno
                Integer tarjetaAlumnoId = tarjetaBBDD.obtenerTarjetaDeUsuario(alumnoId);

                // Verificar si ya existe un registro de asistencia
                String checkSql = """
                    SELECT COUNT(*) FROM asistencia_alumno 
                    WHERE usuario_id = ? 
                      AND asignatura_id = ? 
                      AND grupo_id = ? 
                      AND DATE(fecha_asistencia_alumno) = ?
                    """;
                Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class,
                        alumnoId, asignaturaId, grupoId, fechaCreacion);

                if (count == null || count == 0) {
                    // Insertar registro de no asistencia con tarjeta_id
                    jdbcTemplate.update("""
                        INSERT INTO asistencia_alumno 
                        (usuario_id, asignatura_id, grupo_id, sesion_id, fecha_asistencia_alumno, tipo_asistencia_id, tarjeta_id) 
                        VALUES (?, ?, ?, ?, ?, 2, ?)
                        """, alumnoId, asignaturaId, grupoId, sesionId, fechaCreacion, tarjetaAlumnoId);
                }
            }
        } catch (Exception e) {
            System.err.println("Error insertando no asistencias: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void clasificarAsistenciasCompletadas(Integer sesionId, Integer asignaturaId,
                                                 Integer grupoId, LocalDate fechaCreacion, Object horaSalidaProfesor) {
        // Obtener asistencias completadas que necesitan clasificación
        String sql = """
        SELECT asistencia_alumno_id, hora_salida_alumno
        FROM asistencia_alumno
        WHERE fecha_asistencia_alumno = ?
          AND asignatura_id = ?
          AND grupo_id = ?
          AND hora_entrada_alumno IS NOT NULL
          AND hora_salida_alumno IS NOT NULL
          AND tipo_asistencia_id = 5
        """;

        List<Map<String, Object>> asistencias = jdbcTemplate.queryForList(sql,
                fechaCreacion, asignaturaId, grupoId);

        for (Map<String, Object> asistencia : asistencias) {
            Long asistenciaId = ((Number) asistencia.get("asistencia_alumno_id")).longValue();
            Object horaSalidaAlumno = asistencia.get("hora_salida_alumno");

            if (horaSalidaAlumno != null && horaSalidaProfesor != null) {
                // Calcular diferencia en segundos y convertir a minutos
                String diffSql = """
                SELECT ABS(TIME_TO_SEC(?) - TIME_TO_SEC(?)) / 60 AS diffMin
                """;
                Integer diffMin = jdbcTemplate.queryForObject(diffSql, Integer.class,
                        horaSalidaProfesor, horaSalidaAlumno);

                // Asiste (1) si ≤15 min, media asistencia (3) si >15 min
                int tipoAsistencia = (diffMin != null && diffMin <= 15) ? 1 : 3;

                marcarAsistencia(asistenciaId, tipoAsistencia);
            }
        }
    }

    public void actualizarRegistrosConSesion(Integer sesionId, Integer asignaturaId,
                                             Integer grupoId, LocalDate fechaCreacion) {
        // Marcar registros cerrados sin sesion_id
        String sql = """
            UPDATE asistencia_alumno 
            SET sesion_id = ?
            WHERE fecha_asistencia_alumno = ?
              AND asignatura_id = ?
              AND grupo_id = ?
              AND sesion_id IS NULL
              AND hora_entrada_alumno IS NOT NULL
              AND hora_salida_alumno IS NOT NULL
            """;
        jdbcTemplate.update(sql, sesionId, fechaCreacion, asignaturaId, grupoId);
    }

    public void marcarSoloEntradaComoMedia(Integer sesionId, Integer asignaturaId,
                                           Integer grupoId, LocalDate fechaCreacion) {
        String sql = """
            UPDATE asistencia_alumno 
            SET sesion_id = ?, tipo_asistencia_id = 3
            WHERE fecha_asistencia_alumno = ?
              AND asignatura_id = ?
              AND grupo_id = ?
              AND sesion_id IS NULL
              AND hora_entrada_alumno IS NOT NULL
              AND hora_salida_alumno IS NULL
            """;
        jdbcTemplate.update(sql, sesionId, fechaCreacion, asignaturaId, grupoId);
    }

    public void marcarAsistenciasInmediatas(Integer sesionId, Integer asignaturaId,
                                            Integer grupoId, LocalDate fechaCreacion, Object horaSalidaProfesor) {
        if (horaSalidaProfesor != null) {
            // Marcar como asiste si salieron ≤15 min antes del profesor
            String updateSql = """
            UPDATE asistencia_alumno 
            SET tipo_asistencia_id = 1, sesion_id = ?
            WHERE fecha_asistencia_alumno = ?
              AND asignatura_id = ?
              AND grupo_id = ?
              AND hora_salida_alumno IS NOT NULL
              AND TIME_TO_SEC(hora_salida_alumno) >= TIME_TO_SEC(?) - 900
            """;
            // 900 segundos = 15 minutos
            jdbcTemplate.update(updateSql, sesionId, fechaCreacion, asignaturaId, grupoId, horaSalidaProfesor);
        }
    }
}