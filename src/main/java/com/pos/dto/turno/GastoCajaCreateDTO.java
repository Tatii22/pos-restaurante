package com.pos.dto.turno;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record GastoCajaCreateDTO(

        @NotBlank(message = "La descripcion del gasto es obligatoria")
        @Size(max = 500, message = "La descripcion no puede superar 500 caracteres")
        String descripcion,

        BigDecimal monto,

        BigDecimal montoEfectivo,

        BigDecimal montoTransferencia,

        @NotNull(message = "Debes seleccionar un tipo de gasto")
        Long tipoGastoId
) {
}
