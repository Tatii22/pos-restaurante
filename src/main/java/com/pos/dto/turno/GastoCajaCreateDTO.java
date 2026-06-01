package com.pos.dto.turno;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record GastoCajaCreateDTO(

        @NotBlank(message = "La descripción del gasto es obligatoria")
        @Size(max = 500, message = "La descripción no puede superar 500 caracteres")
        String descripcion,

        @NotNull(message = "El monto total es obligatorio")
        @PositiveOrZero(message = "El monto total no puede ser negativo")
        BigDecimal monto,

        @NotNull(message = "El monto en efectivo es obligatorio")
        @PositiveOrZero(message = "El monto en efectivo no puede ser negativo")
        BigDecimal montoEfectivo,

        @NotNull(message = "El monto por transferencia es obligatorio")
        @PositiveOrZero(message = "El monto por transferencia no puede ser negativo")
        BigDecimal montoTransferencia,

        @NotNull(message = "Debes seleccionar un tipo de gasto")
        Long tipoGastoId
) {
}
