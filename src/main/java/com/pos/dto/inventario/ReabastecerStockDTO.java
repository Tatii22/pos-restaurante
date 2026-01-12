package com.pos.dto.inventario;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReabastecerStockDTO(

        @NotNull @Positive Integer cantidad
) {}
