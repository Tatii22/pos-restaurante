package com.pos.dto.gastoAdmin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record GastoAdminCreateDTO(

        @NotNull(message = "La fecha es obligatoria")
        LocalDate fecha,

        @NotBlank(message = "La descripcion es obligatoria")
        @Size(max = 500, message = "La descripcion no puede superar 500 caracteres")
        String descripcion,

        BigDecimal monto,

        BigDecimal montoEfectivo,

        BigDecimal montoTransferencia,

        @NotNull(message = "Debes seleccionar un tipo de gasto")
        Long tipoGastoId
) {
}

