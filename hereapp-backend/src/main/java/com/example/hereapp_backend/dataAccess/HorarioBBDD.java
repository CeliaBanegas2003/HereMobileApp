package com.example.hereapp_backend.dataAccess;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class HorarioBBDD {

    private final JdbcTemplate jdbcTemplate;

    public HorarioBBDD(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Busca el horario actual del profesor en el día especificado
     * con un margen de ±20 minutos desde la hora actual
     */
    public Map<String, Object> buscarHorarioActual(Integer profesorId, String diaSemana) {
        String sql = """
            SELECT horario_id, asignatura_id, grupo_id, hora_inicio, hora_fin
            FROM horario
            WHERE profesor_id = ?
              AND dia_semana = ?
              AND TIME(CONVERT_TZ(NOW(), 'UTC', 'Europe/Madrid')) 
                  BETWEEN SUBTIME(hora_inicio, '00:20:00') 
                  AND ADDTIME(hora_inicio, '00:20:00')
            LIMIT 1
            """;

        List<Map<String, Object>> horarios = jdbcTemplate.queryForList(sql, profesorId, diaSemana);

        if (horarios.isEmpty()) {
            System.out.println("No se encontraron horarios para profesor " + profesorId +
                    " en día " + diaSemana);

            // Debug: mostrar todos los horarios del profesor
            mostrarHorariosProfesor(profesorId);

            return null;
        }

        return horarios.get(0);
    }

    /**
     * Obtiene todos los horarios de un profesor
     */
    public List<Map<String, Object>> obtenerHorariosProfesor(Integer profesorId) {
        String sql = """
            SELECT h.*, a.nombre_asignatura, g.grupo_nombre
            FROM horario h
            INNER JOIN asignatura a ON h.asignatura_id = a.asignatura_id
            INNER JOIN grupo g ON h.grupo_id = g.grupo_id
            WHERE h.profesor_id = ?
            ORDER BY 
                FIELD(h.dia_semana, 'lunes', 'martes', 'miércoles', 'jueves', 'viernes', 'sábado', 'domingo'),
                h.hora_inicio
            """;
        return jdbcTemplate.queryForList(sql, profesorId);
    }

    /**
     * Obtiene los horarios de un profesor para un día específico
     */
    public List<Map<String, Object>> obtenerHorariosPorDia(Integer profesorId, String diaSemana) {
        String sql = """
            SELECT h.*, a.nombre_asignatura, g.grupo_nombre
            FROM horario h
            INNER JOIN asignatura a ON h.asignatura_id = a.asignatura_id
            INNER JOIN grupo g ON h.grupo_id = g.grupo_id
            WHERE h.profesor_id = ? AND h.dia_semana = ?
            ORDER BY h.hora_inicio
            """;
        return jdbcTemplate.queryForList(sql, profesorId, diaSemana);
    }

    /**
     * Verifica si un profesor tiene clase en un horario específico
     */
    public boolean tieneClaseEnHorario(Integer profesorId, String diaSemana,
                                       String horaInicio, String horaFin) {
        try {
            String sql = """
                SELECT COUNT(*)
                FROM horario
                WHERE profesor_id = ?
                  AND dia_semana = ?
                  AND ((hora_inicio >= ? AND hora_inicio < ?)
                       OR (hora_fin > ? AND hora_fin <= ?)
                       OR (hora_inicio <= ? AND hora_fin >= ?))
                """;
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class,
                    profesorId, diaSemana,
                    horaInicio, horaFin,
                    horaInicio, horaFin,
                    horaInicio, horaFin);
            return count != null && count > 0;
        } catch (Exception e) {
            System.err.println("Error verificando horario: " + e.getMessage());
            return false;
        }
    }

    /**
     * Obtiene el día de la semana actual en español
     */
    public String obtenerDiaSemanaActual() {
        DayOfWeek dayOfWeek = LocalDate.now().getDayOfWeek();
        return dayOfWeek.getDisplayName(TextStyle.FULL, new Locale("es", "ES"));
    }

    /**
     * Método privado para debug: muestra todos los horarios de un profesor
     */
    private void mostrarHorariosProfesor(Integer profesorId) {
        String debugSql = "SELECT * FROM horario WHERE profesor_id = ?";
        List<Map<String, Object>> todosHorarios = jdbcTemplate.queryForList(debugSql, profesorId);
        System.out.println("Horarios del profesor " + profesorId + ": " + todosHorarios);
    }
}