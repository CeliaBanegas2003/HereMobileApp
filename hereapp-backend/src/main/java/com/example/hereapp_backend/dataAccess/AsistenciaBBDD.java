package com.example.hereapp_backend.dataAccess;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class AsistenciaBBDD {

    private final JdbcTemplate jdbcTemplate;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    public AsistenciaBBDD(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String procesarAsistenciaNFC(String uidMifare, String emailUsuario) {
        // 1. Verificar que la tarjeta esté registrada
        if (!existeTarjetaMifare(uidMifare)) {
            throw new RuntimeException("La tarjeta no está registrada en el sistema");
        }

        // 2. Obtener el usuario y determinar el rol
        Integer userId = obtenerUsuarioId(emailUsuario);
        if (userId == null) {
            throw new RuntimeException("Usuario no encontrado");
        }

        String rol = determinarRolUsuario(emailUsuario);

        // 3. Procesar según el rol
        if ("alumno".equals(rol)) {
            return procesarAlumno(userId);
        } else if ("profesor".equals(rol)) {
            return procesarProfesor(userId);
        } else {
            return "Rol no reconocido para el procesamiento de asistencia";
        }
    }

    private boolean existeTarjetaMifare(String uidMifare) {
        try {
            String sql = "SELECT COUNT(*) FROM tarjetas_mifare WHERE uid_mifare = ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, uidMifare);
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private Integer obtenerUsuarioId(String email) {
        try {
            String sql = "SELECT usuario_id FROM usuario WHERE email = ?";
            return jdbcTemplate.queryForObject(sql, Integer.class, email);
        } catch (Exception e) {
            return null;
        }
    }


    private String procesarAlumno(Integer alumnoId) {
        // Verificar que el usuario tenga rol alumno
        String sql = "SELECT COUNT(*) FROM usuario_roles ur JOIN rol r ON ur.roles_rol = r.rol WHERE ur.usuario_usuario_id = ? AND r.rol = 'ALUMNO'";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, alumnoId);
        if (count == null || count == 0) {
            return "No se encontró alumno con ese ID";
        }

        // Buscar registro existente de hoy sin hora_salida_alumno
        sql = """
            SELECT asistencia_alumno_id, sesion_id, hora_entrada_alumno, tipo_asistencia_id
            FROM asistencia_alumno
            WHERE alumno_id = ?
              AND fecha_asistencia_alumno = CURDATE()
              AND hora_salida_alumno IS NULL
            ORDER BY asistencia_alumno_id DESC
            LIMIT 1
            """;

        List<Map<String, Object>> asistencias = jdbcTemplate.queryForList(sql, alumnoId);

        if (!asistencias.isEmpty()) {
            Map<String, Object> asistencia = asistencias.get(0);
            Long asistenciaId = ((Number) asistencia.get("asistencia_alumno_id")).longValue();
            Object horaEntrada = asistencia.get("hora_entrada_alumno");
            Object sesionId = asistencia.get("sesion_id");

            // Primer pase: no tenía hora_entrada
            if (horaEntrada == null) {
                jdbcTemplate.update("""
                    UPDATE asistencia_alumno
                    SET hora_entrada_alumno = CURTIME(),
                        tipo_asistencia_id = 5
                    WHERE asistencia_alumno_id = ?
                    """, asistenciaId);
                return "Entrada registrada correctamente";
            }

            // Segundo pase: verificar si han pasado >= 5 minutos
            int minutosDesdeEntrada = calcularMinutosDesdeEntrada(asistenciaId);
            if (minutosDesdeEntrada >= 5) {
                // Cerrar asistencia
                jdbcTemplate.update("""
                    UPDATE asistencia_alumno
                    SET hora_salida_alumno = CURTIME()
                    WHERE asistencia_alumno_id = ?
                    """, asistenciaId);

                // Verificar sesión del profesor y marcar asistencia si corresponde
                if (sesionId != null) {
                    verificarYMarcarAsistencia(asistenciaId, (Integer) sesionId);
                }

                return "Salida registrada correctamente tras " + minutosDesdeEntrada + " minutos";
            } else {
                return "Asistencia reciente (" + minutosDesdeEntrada + " min), no se puede registrar salida aún";
            }
        }

        // Crear nuevo registro de entrada
        jdbcTemplate.update("""
            INSERT INTO asistencia_alumno
            (alumno_id, fecha_asistencia_alumno, hora_entrada_alumno, tipo_asistencia_id)
            VALUES (?, CURDATE(), CURTIME(), 5)
            """, alumnoId);

        return "Nueva entrada registrada";
    }

    private String procesarProfesor(Integer profesorId) {
        // Verificar que el usuario tenga rol profesor
        String sql = "SELECT COUNT(*) FROM usuario WHERE usuario_id = ? AND roles LIKE '%PROFESOR%'";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, profesorId);
        if (count == null || count == 0) {
            return "No se encontró profesor con ese ID";
        }

        // Verificar sesión abierta del profesor
        sql = """
            SELECT sesion_id, asignatura_id, grupo_id, fecha_creacion
            FROM sesion
            WHERE profesor_id = ?
              AND hora_salida_profesor IS NULL
            ORDER BY sesion_id DESC
            LIMIT 1
            """;

        List<Map<String, Object>> sesiones = jdbcTemplate.queryForList(sql, profesorId);

        if (!sesiones.isEmpty()) {
            Map<String, Object> sesion = sesiones.get(0);
            Integer sesionId = (Integer) sesion.get("sesion_id");
            Integer asignaturaId = (Integer) sesion.get("asignatura_id");
            Integer grupoId = (Integer) sesion.get("grupo_id");
            LocalDate fechaCreacion = ((java.sql.Date) sesion.get("fecha_creacion")).toLocalDate();

            int minutosDesdeInicio = calcularMinutosSesion(sesionId);

            if (minutosDesdeInicio >= 5) {
                // Cerrar sesión
                jdbcTemplate.update("""
                    UPDATE sesion
                    SET hora_salida_profesor = CURTIME()
                    WHERE sesion_id = ?
                    """, sesionId);

                // Marcar asistencias y finalizar después de 10 minutos
                marcarAsistenciasYProgramarFinalizacion(sesionId, asignaturaId, grupoId, fechaCreacion);

                return "Sesión cerrada tras " + minutosDesdeInicio + " minutos";
            } else {
                return "Sesión reciente (" + minutosDesdeInicio + " min), no se puede cerrar aún";
            }
        }

        // Crear nueva sesión
        return crearNuevaSesion(profesorId);
    }

    private String crearNuevaSesion(Integer profesorId) {
        String diaSemana = obtenerDiaSemana();
        LocalTime horaActual = LocalTime.now();

        // Buscar asignatura en horario ±20 minutos
        String sql = """
            SELECT asignatura_id, grupo_id
            FROM horario
            WHERE profesor_id = ?
              AND dia_semana = ?
              AND TIME(hora_inicio) BETWEEN SUBTIME(?, '00:20:00') AND ADDTIME(?, '00:20:00')
            LIMIT 1
            """;

        List<Map<String, Object>> horarios = jdbcTemplate.queryForList(sql,
                profesorId, diaSemana, horaActual, horaActual);

        if (horarios.isEmpty()) {
            return "No hay clase programada para este horario";
        }

        Map<String, Object> horario = horarios.get(0);
        Integer asignaturaId = (Integer) horario.get("asignatura_id");
        Integer grupoId = (Integer) horario.get("grupo_id");

        // Crear sesión
        sql = """
            INSERT INTO sesion
            (fecha_creacion, profesor_id, hora_entrada_profesor, asignatura_id, grupo_id)
            VALUES (CURDATE(), ?, CURTIME(), ?, ?)
            """;

        jdbcTemplate.update(sql, profesorId, asignaturaId, grupoId);

        // Obtener el ID de la sesión creada
        Integer nuevaSesionId = jdbcTemplate.queryForObject(
                "SELECT LAST_INSERT_ID()", Integer.class);

        // Crear registros de asistencia para alumnos matriculados
        crearRegistrosAlumnosMatriculados(nuevaSesionId, asignaturaId, grupoId);

        return "Nueva sesión creada con ID: " + nuevaSesionId;
    }

    private void crearRegistrosAlumnosMatriculados(Integer sesionId, Integer asignaturaId, Integer grupoId) {
        // Obtener alumnos matriculados
        String sql = "SELECT alumno_id FROM matricula WHERE asignatura_id = ? AND grupo_id = ?";
        List<Map<String, Object>> matriculados = jdbcTemplate.queryForList(sql, asignaturaId, grupoId);

        for (Map<String, Object> matricula : matriculados) {
            Integer alumnoId = (Integer) matricula.get("alumno_id");

            // Verificar si ya existe registro para hoy
            String checkSql = """
                SELECT asistencia_alumno_id, sesion_id 
                FROM asistencia_alumno 
                WHERE alumno_id = ? AND fecha_asistencia_alumno = CURDATE() 
                ORDER BY asistencia_alumno_id DESC LIMIT 1
                """;

            List<Map<String, Object>> existentes = jdbcTemplate.queryForList(checkSql, alumnoId);

            if (!existentes.isEmpty()) {
                Map<String, Object> existente = existentes.get(0);
                Long asistenciaId = ((Number) existente.get("asistencia_alumno_id")).longValue();
                Object sesionIdExistente = existente.get("sesion_id");

                // Actualizar solo si no tiene sesión asignada
                if (sesionIdExistente == null) {
                    jdbcTemplate.update("""
                        UPDATE asistencia_alumno 
                        SET sesion_id = ?, asignatura_id = ?, grupo_id = ? 
                        WHERE asistencia_alumno_id = ?
                        """, sesionId, asignaturaId, grupoId, asistenciaId);
                }
            } else {
                // Crear nuevo registro con tipo_asistencia_id = 2 (no asiste)
                jdbcTemplate.update("""
                    INSERT INTO asistencia_alumno 
                    (alumno_id, asignatura_id, grupo_id, sesion_id, fecha_asistencia_alumno, tipo_asistencia_id) 
                    VALUES (?, ?, ?, ?, CURDATE(), 2)
                    """, alumnoId, asignaturaId, grupoId, sesionId);
            }
        }
    }

    private int calcularMinutosDesdeEntrada(Long asistenciaId) {
        String sql = """
            SELECT TIMESTAMPDIFF(MINUTE, 
                CONCAT(fecha_asistencia_alumno, ' ', hora_entrada_alumno), 
                NOW()) AS diffMin 
            FROM asistencia_alumno 
            WHERE asistencia_alumno_id = ?
            """;
        Integer diffMin = jdbcTemplate.queryForObject(sql, Integer.class, asistenciaId);
        return diffMin != null ? diffMin : 0;
    }

    private int calcularMinutosSesion(Integer sesionId) {
        String sql = """
            SELECT TIMESTAMPDIFF(MINUTE, 
                CONCAT(fecha_creacion, ' ', hora_entrada_profesor), 
                NOW()) AS diffMin 
            FROM sesion 
            WHERE sesion_id = ?
            """;
        Integer diffMin = jdbcTemplate.queryForObject(sql, Integer.class, sesionId);
        return diffMin != null ? diffMin : 0;
    }

    private void verificarYMarcarAsistencia(Long asistenciaId, Integer sesionId) {
        // Obtener hora de salida del profesor
        String sql = "SELECT hora_salida_profesor FROM sesion WHERE sesion_id = ?";
        List<Map<String, Object>> sesiones = jdbcTemplate.queryForList(sql, sesionId);

        if (!sesiones.isEmpty()) {
            Object horaSalidaProfesor = sesiones.get(0).get("hora_salida_profesor");
            if (horaSalidaProfesor != null) {
                // Verificar si la salida del alumno fue dentro de los 15 minutos
                String diffSql = """
                    SELECT TIMESTAMPDIFF(MINUTE, ?, NOW()) AS diffMin
                    """;
                Integer diffMin = jdbcTemplate.queryForObject(diffSql, Integer.class, horaSalidaProfesor);

                if (diffMin != null && diffMin >= -15) {
                    jdbcTemplate.update("""
                        UPDATE asistencia_alumno
                        SET tipo_asistencia_id = 1
                        WHERE asistencia_alumno_id = ?
                        """, asistenciaId);
                }
            }
        }
    }

    private void marcarAsistenciasYProgramarFinalizacion(Integer sesionId, Integer asignaturaId,
                                                         Integer grupoId, LocalDate fechaCreacion) {
        // Marcar asistencias inmediatas (si salieron ≤15 min antes)
        marcarAsistenciasInmediatas(sesionId, asignaturaId, grupoId, fechaCreacion);

        // Programar finalización tras 10 minutos
        scheduler.schedule(() -> {
            finalizarAsistenciasSesion(sesionId, asignaturaId, grupoId, fechaCreacion);
        }, 10, TimeUnit.MINUTES);
    }

    private void marcarAsistenciasInmediatas(Integer sesionId, Integer asignaturaId,
                                             Integer grupoId, LocalDate fechaCreacion) {
        // Obtener hora de salida del profesor
        String sql = "SELECT hora_salida_profesor FROM sesion WHERE sesion_id = ?";
        List<Map<String, Object>> sesiones = jdbcTemplate.queryForList(sql, sesionId);

        if (!sesiones.isEmpty()) {
            Object horaSalidaProfesor = sesiones.get(0).get("hora_salida_profesor");
            if (horaSalidaProfesor != null) {
                // Marcar como asiste si salieron ≤15 min antes
                String updateSql = """
                    UPDATE asistencia_alumno 
                    SET tipo_asistencia_id = 1, sesion_id = ?
                    WHERE fecha_asistencia_alumno = ?
                      AND asignatura_id = ?
                      AND grupo_id = ?
                      AND hora_salida_alumno IS NOT NULL
                      AND TIMESTAMPDIFF(MINUTE, ?, hora_salida_alumno) >= -15
                    """;
                jdbcTemplate.update(updateSql, sesionId, fechaCreacion, asignaturaId, grupoId, horaSalidaProfesor);
            }
        }
    }

    private void finalizarAsistenciasSesion(Integer sesionId, Integer asignaturaId,
                                            Integer grupoId, LocalDate fechaCreacion) {
        try {
            // Marcar registros cerrados sin sesion_id
            String sql1 = """
                UPDATE asistencia_alumno 
                SET sesion_id = ?
                WHERE fecha_asistencia_alumno = ?
                  AND asignatura_id = ?
                  AND grupo_id = ?
                  AND sesion_id IS NULL
                  AND hora_entrada_alumno IS NOT NULL
                  AND hora_salida_alumno IS NOT NULL
                """;
            jdbcTemplate.update(sql1, sesionId, fechaCreacion, asignaturaId, grupoId);

            // Clasificar asistencias completadas
            clasificarAsistenciasCompletadas(sesionId, asignaturaId, grupoId, fechaCreacion);

            // Marcar solo entrada como asistencia media (tipo 3)
            String sql2 = """
                UPDATE asistencia_alumno 
                SET sesion_id = ?, tipo_asistencia_id = 3
                WHERE fecha_asistencia_alumno = ?
                  AND asignatura_id = ?
                  AND grupo_id = ?
                  AND sesion_id IS NULL
                  AND hora_entrada_alumno IS NOT NULL
                  AND hora_salida_alumno IS NULL
                """;
            jdbcTemplate.update(sql2, sesionId, fechaCreacion, asignaturaId, grupoId);

            // Insertar registros de no asistencia para quien no apareció
            insertarNoAsistencias(sesionId, asignaturaId, grupoId, fechaCreacion);

        } catch (Exception e) {
            System.err.println("Error finalizando asistencias de sesión " + sesionId + ": " + e.getMessage());
        }
    }

    private void clasificarAsistenciasCompletadas(Integer sesionId, Integer asignaturaId,
                                                  Integer grupoId, LocalDate fechaCreacion) {
        // Obtener asistencias completadas que necesitan clasificación
        String sql = """
            SELECT a.asistencia_alumno_id, a.hora_salida_alumno, s.hora_salida_profesor
            FROM asistencia_alumno a
            JOIN sesion s ON s.sesion_id = ?
            WHERE a.fecha_asistencia_alumno = ?
              AND a.asignatura_id = ?
              AND a.grupo_id = ?
              AND a.hora_entrada_alumno IS NOT NULL
              AND a.hora_salida_alumno IS NOT NULL
              AND a.tipo_asistencia_id = 5
            """;

        List<Map<String, Object>> asistencias = jdbcTemplate.queryForList(sql,
                sesionId, fechaCreacion, asignaturaId, grupoId);

        for (Map<String, Object> asistencia : asistencias) {
            Long asistenciaId = ((Number) asistencia.get("asistencia_alumno_id")).longValue();
            Object horaSalidaAlumno = asistencia.get("hora_salida_alumno");
            Object horaSalidaProfesor = asistencia.get("hora_salida_profesor");

            if (horaSalidaAlumno != null && horaSalidaProfesor != null) {
                // Calcular diferencia en minutos
                String diffSql = """
                    SELECT ABS(TIMESTAMPDIFF(MINUTE, ?, ?)) AS diffMin
                    """;
                Integer diffMin = jdbcTemplate.queryForObject(diffSql, Integer.class,
                        horaSalidaProfesor, horaSalidaAlumno);

                // Asiste (1) si ≤15 min, media asistencia (3) si >15 min
                int tipoAsistencia = (diffMin != null && diffMin <= 15) ? 1 : 3;

                jdbcTemplate.update("""
                    UPDATE asistencia_alumno 
                    SET tipo_asistencia_id = ?
                    WHERE asistencia_alumno_id = ?
                    """, tipoAsistencia, asistenciaId);
            }
        }
    }

    private void insertarNoAsistencias(Integer sesionId, Integer asignaturaId,
                                       Integer grupoId, LocalDate fechaCreacion) {
        // Obtener alumnos matriculados
        String sql = "SELECT alumno_id FROM matricula WHERE asignatura_id = ? AND grupo_id = ?";
        List<Map<String, Object>> matriculados = jdbcTemplate.queryForList(sql, asignaturaId, grupoId);

        for (Map<String, Object> matricula : matriculados) {
            Integer alumnoId = (Integer) matricula.get("alumno_id");

            // Verificar si ya tiene registro para este día
            String checkSql = """
                SELECT COUNT(*) FROM asistencia_alumno 
                WHERE alumno_id = ? 
                  AND asignatura_id = ? 
                  AND grupo_id = ? 
                  AND DATE(fecha_asistencia_alumno) = ?
                """;
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class,
                    alumnoId, asignaturaId, grupoId, fechaCreacion);

            if (count == null || count == 0) {
                // Insertar registro de no asistencia
                jdbcTemplate.update("""
                    INSERT INTO asistencia_alumno 
                    (alumno_id, asignatura_id, grupo_id, sesion_id, fecha_asistencia_alumno, tipo_asistencia_id) 
                    VALUES (?, ?, ?, ?, ?, 2)
                    """, alumnoId, asignaturaId, grupoId, sesionId, fechaCreacion);
            }
        }
    }

    private String obtenerDiaSemana() {
        DayOfWeek dayOfWeek = LocalDate.now().getDayOfWeek();
        return dayOfWeek.getDisplayName(TextStyle.FULL, new Locale("es", "ES"));
    }
}