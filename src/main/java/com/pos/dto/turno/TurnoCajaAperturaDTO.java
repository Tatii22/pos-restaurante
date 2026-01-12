package com.pos.dto.turno;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class TurnoCajaAperturaDTO {

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal montoInicial;
}
