package com.pos.dto.venta;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record VentaValorDomicilioDTO(
        @NotNull(message = "El valor del domicilio es obligatorio")
        @PositiveOrZero(message = "El valor del domicilio no puede ser negativo")
        BigDecimal valorDomicilio
) {}
