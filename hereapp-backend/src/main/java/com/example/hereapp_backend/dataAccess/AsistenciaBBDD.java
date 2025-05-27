package com.example.hereapp_backend.dataAccess;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
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

    // Método para verificar si un usuario es administrador (para uso en otros controladores)
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

    public String procesarAsistenciaNFC(String uidMifare, String emailUsuario) {
        // 1. Verificar que la tarjeta esté registrada
        if (!existeTarjetaMifare(uidMifare)) {
            throw new RuntimeException("La tarjeta no está registrada en el sistema");
        }

        // 2. Obtener el usuario y la tarjeta
        Integer userId = obtenerUsuarioId(emailUsuario);
        if (userId == null) {
            throw new RuntimeException("Usuario no encontrado");
        }

        Integer tarjetaId = obtenerTarjetaId(uidMifare);
        if (tarjetaId == null) {
            throw new RuntimeException("Error obteniendo ID de tarjeta");
        }

        // 3. Determinar cómo procesar según los roles del usuario
        // Prioridad: Si tiene rol PROFESOR (aunque también tenga ADMIN), procesar como profesor
        if (tieneRol(userId, "PROFESOR")) {
            return procesarProfesor(userId, tarjetaId);
        } else if (tieneRol(userId, "ALUMNO")) {
            return procesarAlumno(userId, tarjetaId);
        } else if (tieneRol(userId, "ADMIN")) {
            // Solo admin sin otros roles
            return "Usuario administrador: utilice la función de registro de tarjetas";
        } else {
            return "Rol no reconocido para el procesamiento de asistencia";
        }
    }

    private boolean existeTarjetaMifare(String uidMifare) {
        try {
            String sql = "SELECT COUNT(*) FROM tarjetas_mifare WHERE uid_mifare = ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, uidMifare);
            return count > 0;
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

    private Integer obtenerTarjetaId(String uidMifare) {
        try {
            String sql = "SELECT tarjeta_id FROM tarjetas_mifare WHERE uid_mifare = ?";
            return jdbcTemplate.queryForObject(sql, Integer.class, uidMifare);
        } catch (Exception e) {
            return null;
        }
    }

    // Método mejorado para verificar si un usuario tiene un rol específico
    private boolean tieneRol(Integer userId, String rolNombre) {
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

    // Método corregido para determinar el rol del usuario basado en la estructura real de BD
    private String determinarRolUsuario(Integer userId) {
        try {
            // Consultar los roles desde la tabla usuario_roles usando la estructura correcta del diagrama
            String sql = """
                SELECT ur.rol_nombre
                FROM usuario_roles ur 
                WHERE ur.usuario_id = ?
                """;

            List<Map<String, Object>> rolesResult = jdbcTemplate.queryForList(sql, userId);

            // Verificar cada rol encontrado - prioridad: PROFESOR > ALUMNO > ADMIN
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

    private String procesarAlumno(Integer alumnoId, Integer tarjetaId) {
        // Verificar que el usuario tenga rol alumno (verificación adicional por seguridad)
        if (!tieneRol(alumnoId, "ALUMNO")) {
            return "No se encontró alumno con ese ID";
        }

        // CORRECCIÓN: Cambiar alumno_id por usuario_id en la consulta
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

        if (!asistencias.isEmpty()) {
            Map<String, Object> asistencia = asistencias.getFirst();
            Long asistenciaId = ((Number) asistencia.get("asistencia_alumno_id")).longValue();
            Object horaEntrada = asistencia.get("hora_entrada_alumno");
            Object sesionId = asistencia.get("sesion_id");

            // Primer pase: no tenía hora_entrada
            if (horaEntrada == null) {
                jdbcTemplate.update("""
                    UPDATE asistencia_alumno
                    SET hora_entrada_alumno = TIME(CONVERT_TZ(NOW(), 'UTC', 'Europe/Madrid')),
                        tipo_asistencia_id = 5,
                        tarjeta_id = ?
                    WHERE asistencia_alumno_id = ?
                    """, tarjetaId, asistenciaId);
                return "Entrada registrada correctamente";
            }

            // Segundo pase: verificar si han pasado >= 5 minutos
            int minutosDesdeEntrada = calcularMinutosDesdeEntrada(asistenciaId);
            if (minutosDesdeEntrada >= 5) {
                // Cerrar asistencia
                jdbcTemplate.update("""
                    UPDATE asistencia_alumno
                    SET hora_salida_alumno = TIME(CONVERT_TZ(NOW(), 'UTC', 'Europe/Madrid'))
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

        // CORRECCIÓN: Cambiar alumno_id por usuario_id en el INSERT
        jdbcTemplate.update("""
            INSERT INTO asistencia_alumno
            (usuario_id, fecha_asistencia_alumno, hora_entrada_alumno, tipo_asistencia_id, tarjeta_id)
            VALUES (?, DATE(CONVERT_TZ(NOW(), 'UTC', 'Europe/Madrid')), TIME(CONVERT_TZ(NOW(), 'UTC', 'Europe/Madrid')), 5, ?)
            """, alumnoId, tarjetaId);

        return "Nueva entrada registrada";
    }

    private String procesarProfesor(Integer profesorId, Integer tarjetaId) {
        // Verificar que el usuario tenga rol profesor
        if (!tieneRol(profesorId, "PROFESOR")) {
            return "No se encontró profesor con ese ID";
        }

        // Verificar sesión abierta del profesor
        String sql = """
            SELECT sesion_id, asignatura_id, grupo_id, fecha_creacion
            FROM sesion
            WHERE profesor_id = ?
              AND hora_salida_profesor IS NULL
            ORDER BY sesion_id DESC
            LIMIT 1
            """;

        List<Map<String, Object>> sesiones = jdbcTemplate.queryForList(sql, profesorId);

        if (!sesiones.isEmpty()) {
            Map<String, Object> sesion = sesiones.getFirst();
            Integer sesionId = (Integer) sesion.get("sesion_id");
            Integer asignaturaId = (Integer) sesion.get("asignatura_id");
            Integer grupoId = (Integer) sesion.get("grupo_id");
            LocalDate fechaCreacion = ((java.sql.Date) sesion.get("fecha_creacion")).toLocalDate();

            int minutosDesdeInicio = calcularMinutosSesion(sesionId);

            if (minutosDesdeInicio >= 5) {
                // Cerrar sesión
                jdbcTemplate.update("""
                    UPDATE sesion
                    SET hora_salida_profesor = TIME(CONVERT_TZ(NOW(), 'UTC', 'Europe/Madrid'))
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
        return crearNuevaSesion(profesorId, tarjetaId);
    }

    private String crearNuevaSesion(Integer profesorId, Integer tarjetaId) {
        String diaSemana = obtenerDiaSemana();
        LocalTime horaActual = LocalTime.now();

        // Debug: mostrar valores actuales
        System.out.println("Creando nueva sesión para profesor: " + profesorId);
        System.out.println("Día de la semana: " + diaSemana);
        System.out.println("Hora actual: " + horaActual);

        // Buscar asignatura en horario ±20 minutos
        String sql = """
            SELECT horario_id, asignatura_id, grupo_id, hora_inicio, hora_fin
            FROM horario
            WHERE profesor_id = ?
              AND dia_semana = ?
              AND TIME(CONVERT_TZ(NOW(), 'UTC', 'Europe/Madrid')) BETWEEN SUBTIME(hora_inicio, '00:20:00') AND ADDTIME(hora_inicio, '00:20:00')
            LIMIT 1
            """;

        List<Map<String, Object>> horarios = jdbcTemplate.queryForList(sql,
                profesorId, diaSemana);

        if (horarios.isEmpty()) {
            System.out.println("No se encontraron horarios para profesor " + profesorId +
                    " en día " + diaSemana + " cerca de la hora " + horaActual);

            // Query adicional para debug - mostrar todos los horarios del profesor
            String debugSql = "SELECT * FROM horario WHERE profesor_id = ?";
            List<Map<String, Object>> todosHorarios = jdbcTemplate.queryForList(debugSql, profesorId);
            System.out.println("Horarios del profesor " + profesorId + ": " + todosHorarios);

            return "No hay clase programada para este horario";
        }

        Map<String, Object> horario = horarios.getFirst();
        Integer asignaturaId = (Integer) horario.get("asignatura_id");
        Integer grupoId = (Integer) horario.get("grupo_id");

        System.out.println("Horario encontrado: " + horario);

        // Crear sesión con usuario_id y tarjeta_id usando zona horaria correcta
        sql = """
            INSERT INTO sesion
            (fecha_creacion, usuario_id, profesor_id, hora_entrada_profesor, asignatura_id, grupo_id, tarjeta_id)
            VALUES (DATE(CONVERT_TZ(NOW(), 'UTC', 'Europe/Madrid')), ?, ?, TIME(CONVERT_TZ(NOW(), 'UTC', 'Europe/Madrid')), ?, ?, ?)
            """;

        jdbcTemplate.update(sql, profesorId, profesorId, asignaturaId, grupoId, tarjetaId);

        // Obtener el ID de la sesión creada
        Integer nuevaSesionId = jdbcTemplate.queryForObject(
                "SELECT LAST_INSERT_ID()", Integer.class);

        // Crear registros de asistencia para alumnos matriculados
        crearRegistrosAlumnosMatriculados(nuevaSesionId, asignaturaId, grupoId);

        return "Nueva sesión creada con ID: " + nuevaSesionId;
    }

    private void crearRegistrosAlumnosMatriculados(Integer sesionId, Integer asignaturaId, Integer grupoId) {
        try {
            // Obtener alumnos matriculados
            String sql = "SELECT usuario_id FROM matricula WHERE asignatura_id = ? AND grupo_id = ?";
            List<Map<String, Object>> matriculados = jdbcTemplate.queryForList(sql, asignaturaId, grupoId);

            System.out.println("Alumnos matriculados encontrados: " + matriculados.size());

            for (Map<String, Object> matricula : matriculados) {
                Integer alumnoId = (Integer) matricula.get("usuario_id");

                // Buscar CUALQUIER registro del alumno de hoy, sin importar si tiene entrada o no
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

                        // Actualizar el registro existente
                        jdbcTemplate.update("""
                            UPDATE asistencia_alumno 
                            SET sesion_id = ?, asignatura_id = ?, grupo_id = ? 
                            WHERE asistencia_alumno_id = ?
                            """, sesionId, asignaturaId, grupoId, asistenciaId);

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
                        // Crear nuevo registro solo si no existe uno para esta sesión
                        jdbcTemplate.update("""
                            INSERT INTO asistencia_alumno 
                            (usuario_id, asignatura_id, grupo_id, sesion_id, fecha_asistencia_alumno, tipo_asistencia_id) 
                            VALUES (?, ?, ?, ?, DATE(CONVERT_TZ(NOW(), 'UTC', 'Europe/Madrid')), 2)
                            """, alumnoId, asignaturaId, grupoId, sesionId);

                        System.out.println("Creado nuevo registro para alumno " + alumnoId);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error creando registros de alumnos matriculados: " + e.getMessage());
            e.printStackTrace();
            // No lanzar la excepción para que no afecte a la creación de sesión
        }
    }

    private int calcularMinutosDesdeEntrada(Long asistenciaId) {
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

    private int calcularMinutosSesion(Integer sesionId) {
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

    private void verificarYMarcarAsistencia(Long asistenciaId, Integer sesionId) {
        // Obtener hora de salida del profesor
        String sql = "SELECT hora_salida_profesor FROM sesion WHERE sesion_id = ?";
        List<Map<String, Object>> sesiones = jdbcTemplate.queryForList(sql, sesionId);

        if (!sesiones.isEmpty()) {
            Object horaSalidaProfesor = sesiones.get(0).get("hora_salida_profesor");
            if (horaSalidaProfesor != null) {
                // Verificar si la salida del alumno fue dentro de los 15 minutos
                String diffSql = """
                    SELECT TIMESTAMPDIFF(MINUTE, ?, TIME(CONVERT_TZ(NOW(), 'UTC', 'Europe/Madrid'))) AS diffMin
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
        try {
            // Obtener alumnos matriculados
            String sql = "SELECT usuario_id FROM matricula WHERE asignatura_id = ? AND grupo_id = ?";
            List<Map<String, Object>> matriculados = jdbcTemplate.queryForList(sql, asignaturaId, grupoId);

            for (Map<String, Object> matricula : matriculados) {
                Integer alumnoId = (Integer) matricula.get("usuario_id");

                // CORRECCIÓN: Cambiar alumno_id por usuario_id en la consulta
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
                    // CORRECCIÓN: Cambiar alumno_id por usuario_id en el INSERT
                    jdbcTemplate.update("""
                        INSERT INTO asistencia_alumno 
                        (usuario_id, asignatura_id, grupo_id, sesion_id, fecha_asistencia_alumno, tipo_asistencia_id) 
                        VALUES (?, ?, ?, ?, ?, 2)
                        """, alumnoId, asignaturaId, grupoId, sesionId, fechaCreacion);
                }
            }
        } catch (Exception e) {
            System.err.println("Error insertando no asistencias: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String obtenerDiaSemana() {
        DayOfWeek dayOfWeek = LocalDate.now().getDayOfWeek();
        return dayOfWeek.getDisplayName(TextStyle.FULL, new Locale("es", "ES"));
    }
}