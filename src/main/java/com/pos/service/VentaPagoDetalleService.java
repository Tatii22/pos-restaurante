package com.pos.service;

import com.pos.dto.venta.VentaPagoDetalleDTO;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class VentaPagoDetalleService {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    void init() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS venta_pago_detalle (
                  venta_id BIGINT NOT NULL PRIMARY KEY,
                  pago_efectivo DECIMAL(12,2) NOT NULL DEFAULT 0,
                  pago_transferencia DECIMAL(12,2) NOT NULL DEFAULT 0,
                  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                )
                """);
    }

    public void guardar(Long ventaId, BigDecimal pagoEfectivo, BigDecimal pagoTransferencia) {
        if (ventaId == null) {
            return;
        }
        BigDecimal efectivo = nonNegative(pagoEfectivo);
        BigDecimal transferencia = nonNegative(pagoTransferencia);

        jdbcTemplate.update(
                """
                        INSERT INTO venta_pago_detalle (venta_id, pago_efectivo, pago_transferencia)
                        VALUES (?, ?, ?)
                        ON DUPLICATE KEY UPDATE
                          pago_efectivo = VALUES(pago_efectivo),
                          pago_transferencia = VALUES(pago_transferencia)
                        """,
                ventaId, efectivo, transferencia
        );
    }

    public VentaPagoDetalleDTO obtener(Long ventaId) {
        if (ventaId == null) {
            return null;
        }
        return jdbcTemplate.query(
                """
                        SELECT pago_efectivo, pago_transferencia
                          FROM venta_pago_detalle
                         WHERE venta_id = ?
                        """,
                rs -> rs.next()
                        ? new VentaPagoDetalleDTO(rs.getBigDecimal("pago_efectivo"), rs.getBigDecimal("pago_transferencia"))
                        : null,
                ventaId
        );
    }

    private BigDecimal nonNegative(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return value;
    }
}
