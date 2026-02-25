package com.pos.dto.venta;

import java.math.BigDecimal;

public record VentaPagoDetalleDTO(
        BigDecimal pagoEfectivo,
        BigDecimal pagoTransferencia
) {
}
