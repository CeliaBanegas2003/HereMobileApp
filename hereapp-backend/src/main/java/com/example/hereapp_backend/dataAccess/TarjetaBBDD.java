package com.example.hereapp_backend.dataAccess;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class TarjetaBBDD {

    private final JdbcTemplate jdbcTemplate;

    public TarjetaBBDD(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean existeTarjetaMifare(String uidMifare) {
        try {
            String sql = "SELECT COUNT(*) FROM tarjetas_mifare WHERE uid_mifare = ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, uidMifare);
            return count > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public Integer obtenerTarjetaId(String uidMifare) {
        try {
            String sql = "SELECT tarjeta_id FROM tarjetas_mifare WHERE uid_mifare = ?";
            return jdbcTemplate.queryForObject(sql, Integer.class, uidMifare);
        } catch (Exception e) {
            return null;
        }
    }

    // Método para obtener la tarjeta asociada a un usuario
    public Integer obtenerTarjetaDeUsuario(Integer usuarioId) {
        try {
            // Opción 1: Si hay una tabla que relaciona usuarios con tarjetas
            String sql = """
                SELECT t.tarjeta_id 
                FROM tarjetas_mifare t
                WHERE t.usuario_id = ?
                ORDER BY t.tarjeta_id DESC
                LIMIT 1
                """;

            return jdbcTemplate.queryForObject(sql, Integer.class, usuarioId);
        } catch (Exception e) {
            // Si no se encuentra tarjeta, retornar null
            System.out.println("No se encontró tarjeta para usuario " + usuarioId);
            return null;
        }
    }
}