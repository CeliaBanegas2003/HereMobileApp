package com.example.hereapp_backend.dataAccess;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class AsistenciaBBDD {

    private final UsuarioBBDD usuarioBBDD;
    private final TarjetaBBDD tarjetaBBDD;
    private final SesionBBDD sesionBBDD;
    private final AsistenciaAlumnoBBDD asistenciaAlumnoBBDD;
    private final JdbcTemplate jdbcTemplate;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    public AsistenciaBBDD(UsuarioBBDD usuarioBBDD,
                          TarjetaBBDD tarjetaBBDD,
                          SesionBBDD sesionBBDD,
                          AsistenciaAlumnoBBDD asistenciaAlumnoBBDD,
                          JdbcTemplate jdbcTemplate) {
        this.usuarioBBDD = usuarioBBDD;
        this.tarjetaBBDD = tarjetaBBDD;
        this.sesionBBDD = sesionBBDD;
        this.asistenciaAlumnoBBDD = asistenciaAlumnoBBDD;
        this.jdbcTemplate = jdbcTemplate;
    }

    public String procesarAsistenciaNFC(String uidMifare, String emailUsuario) {
        // 1. Verificar que la tarjeta esté registrada
        if (!tarjetaBBDD.existeTarjetaMifare(uidMifare)) {
            throw new RuntimeException("La tarjeta no está registrada en el sistema");
        }

        // 2. Obtener el usuario y la tarjeta
        Integer userId = usuarioBBDD.obtenerUsuarioId(emailUsuario);
        if (userId == null) {
            throw new RuntimeException("Usuario no encontrado");
        }

        Integer tarjetaId = tarjetaBBDD.obtenerTarjetaId(uidMifare);
        if (tarjetaId == null) {
            throw new RuntimeException("Error obteniendo ID de tarjeta");
        }

        // 3. Determinar cómo procesar según los roles del usuario
        // Prioridad: Si tiene rol PROFESOR (aunque también tenga ADMIN), procesar como profesor
        if (usuarioBBDD.tieneRol(userId, "PROFESOR")) {
            return procesarProfesor(userId, tarjetaId);
        } else if (usuarioBBDD.tieneRol(userId, "ALUMNO")) {
            return procesarAlumno(userId, tarjetaId);
        } else if (usuarioBBDD.tieneRol(userId, "ADMIN")) {
            // Solo admin sin otros roles
            return "Usuario administrador: utilice la función de registro de tarjetas";
        } else {
            return "Rol no reconocido para el procesamiento de asistencia";
        }
    }

    private String procesarAlumno(Integer alumnoId, Integer tarjetaId) {
        // Verificar que el usuario tenga rol alumno
        if (!usuarioBBDD.tieneRol(alumnoId, "ALUMNO")) {
            return "No se encontró alumno con ese ID";
        }

        // Buscar asistencia abierta del alumno (sin salida registrada)
        Map<String, Object> asistenciaAbierta = asistenciaAlumnoBBDD.obtenerAsistenciaAbierta(alumnoId);

        if (asistenciaAbierta != null) {
            Long asistenciaId = ((Number) asistenciaAbierta.get("asistencia_alumno_id")).longValue();
            Object horaEntrada = asistenciaAbierta.get("hora_entrada_alumno");
            Object sesionId = asistenciaAbierta.get("sesion_id");

            // Primer pase: no tenía hora_entrada
            if (horaEntrada == null) {
                asistenciaAlumnoBBDD.registrarEntrada(asistenciaId, tarjetaId);
                return "Entrada registrada correctamente";
            }

            // Segundo pase: verificar si han pasado >= 5 minutos para poder registrar salida
            int minutosDesdeEntrada = asistenciaAlumnoBBDD.calcularMinutosDesdeEntrada(asistenciaId);
            if (minutosDesdeEntrada >= 5) {
                // Registrar salida del alumno (NO crear registros de matriculados)
                asistenciaAlumnoBBDD.registrarSalida(asistenciaId);

                // Verificar sesión del profesor y marcar asistencia si corresponde
                if (sesionId != null) {
                    verificarYMarcarAsistencia(asistenciaId, (Integer) sesionId);
                }

                return "Salida registrada correctamente tras " + minutosDesdeEntrada + " minutos";
            } else {
                return "Asistencia reciente (" + minutosDesdeEntrada + " min), no se puede registrar salida aún";
            }
        }

        // Si no hay asistencia abierta, crear nueva entrada
        asistenciaAlumnoBBDD.crearNuevaEntrada(alumnoId, tarjetaId);
        return "Nueva entrada registrada";
    }

    private String procesarProfesor(Integer profesorId, Integer tarjetaId) {
        // Verificar que el usuario tenga rol profesor
        if (!usuarioBBDD.tieneRol(profesorId, "PROFESOR")) {
            return "No se encontró profesor con ese ID";
        }

        Map<String, Object> sesionAbierta = sesionBBDD.obtenerSesionAbiertaProfesor(profesorId);

        if (sesionAbierta != null) {
            // CERRAR SESIÓN EXISTENTE
            Integer sesionId = (Integer) sesionAbierta.get("sesion_id");
            Integer asignaturaId = (Integer) sesionAbierta.get("asignatura_id");
            Integer grupoId = (Integer) sesionAbierta.get("grupo_id");
            LocalDate fechaCreacion = ((java.sql.Date) sesionAbierta.get("fecha_creacion")).toLocalDate();

            int minutosDesdeInicio = sesionBBDD.calcularMinutosSesion(sesionId);

            if (minutosDesdeInicio >= 5) {
                // Cerrar sesión del profesor prueba
                sesionBBDD.cerrarSesion(sesionId);

                // Marcar asistencias y programar finalización
                marcarAsistenciasYProgramarFinalizacion(sesionId, asignaturaId, grupoId, fechaCreacion);

                return "Sesión cerrada tras " + minutosDesdeInicio + " minutos";
            } else {
                return "Sesión reciente (" + minutosDesdeInicio + " min), no se puede cerrar aún";
            }
        } else {
            // CREAR NUEVA SESIÓN
            // Solo aquí se deben crear los registros de alumnos matriculados
            String resultado = sesionBBDD.crearNuevaSesion(profesorId, tarjetaId);

            // Obtener la nueva sesión creada para poder crear los registros de matriculados
            Map<String, Object> nuevaSesion = sesionBBDD.obtenerSesionAbiertaProfesor(profesorId);
            if (nuevaSesion != null) {
                Integer sesionId = (Integer) nuevaSesion.get("sesion_id");
                Integer asignaturaId = (Integer) nuevaSesion.get("asignatura_id");
                Integer grupoId = (Integer) nuevaSesion.get("grupo_id");

                // SOLO cuando se crea una nueva sesión, crear registros de alumnos matriculados
                asistenciaAlumnoBBDD.crearRegistrosAlumnosMatriculados(sesionId, asignaturaId, grupoId);
            }

            return resultado;
        }
    }

    private void verificarYMarcarAsistencia(Long asistenciaId, Integer sesionId) {
        Object horaSalidaProfesor = sesionBBDD.obtenerHoraSalidaProfesor(sesionId);

        if (horaSalidaProfesor != null) {
            // Verificar si la salida del alumno fue dentro de los 15 minutos
            String diffSql = """
                SELECT TIMESTAMPDIFF(MINUTE, ?, TIME(CONVERT_TZ(NOW(), 'UTC', 'Europe/Madrid'))) AS diffMin
                """;
            Integer diffMin = jdbcTemplate.queryForObject(diffSql, Integer.class, horaSalidaProfesor);

            if (diffMin != null && diffMin >= -15) {
                asistenciaAlumnoBBDD.marcarAsistencia(asistenciaId, 1);
            }
        }
    }

    private void marcarAsistenciasYProgramarFinalizacion(Integer sesionId, Integer asignaturaId,
                                                         Integer grupoId, LocalDate fechaCreacion) {
        // Marcar asistencias inmediatas
        Object horaSalidaProfesor = sesionBBDD.obtenerHoraSalidaProfesor(sesionId);
        asistenciaAlumnoBBDD.marcarAsistenciasInmediatas(sesionId, asignaturaId, grupoId,
                fechaCreacion, horaSalidaProfesor);

        // Programar finalización tras 10 minutos
        scheduler.schedule(() -> {
            finalizarAsistenciasSesion(sesionId, asignaturaId, grupoId, fechaCreacion);
        }, 10, TimeUnit.MINUTES);
    }

    private void finalizarAsistenciasSesion(Integer sesionId, Integer asignaturaId,
                                            Integer grupoId, LocalDate fechaCreacion) {
        try {
            // Actualizar registros con sesión
            asistenciaAlumnoBBDD.actualizarRegistrosConSesion(sesionId, asignaturaId, grupoId, fechaCreacion);

            // Clasificar asistencias completadas
            Object horaSalidaProfesor = sesionBBDD.obtenerHoraSalidaProfesor(sesionId);
            asistenciaAlumnoBBDD.clasificarAsistenciasCompletadas(sesionId, asignaturaId, grupoId,
                    fechaCreacion, horaSalidaProfesor);

            // Marcar solo entrada como asistencia media
            asistenciaAlumnoBBDD.marcarSoloEntradaComoMedia(sesionId, asignaturaId, grupoId, fechaCreacion);

            // Insertar registros de no asistencia
            asistenciaAlumnoBBDD.insertarNoAsistencias(sesionId, asignaturaId, grupoId, fechaCreacion);

        } catch (Exception e) {
            System.err.println("Error finalizando asistencias de sesión " + sesionId + ": " + e.getMessage());
        }
    }

    // Método delegado para compatibilidad con otros controladores
    public boolean esUsuarioAdmin(String emailUsuario) {
        return usuarioBBDD.esUsuarioAdmin(emailUsuario);
    }
}