package com.pos.dto.turno;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record GastoCajaCreateDTO(

        @NotBlank(message = "La descripcion del gasto es obligatoria")
        String descripcion,

        BigDecimal monto,

        BigDecimal montoEfectivo,

        BigDecimal montoTransferencia,

        @NotNull(message = "Debes seleccionar un tipo de gasto")
        Long tipoGastoId
) {
}
