package com.pos.dto.venta;

import com.pos.entity.FormaPago;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record VentaDespachoDTO(
        FormaPago formaPago,
        @PositiveOrZero(message = "El pago en efectivo no puede ser negativo")
        BigDecimal pagoEfectivo,
        @PositiveOrZero(message = "El pago por transferencia no puede ser negativo")
        BigDecimal pagoTransferencia
) {
}
