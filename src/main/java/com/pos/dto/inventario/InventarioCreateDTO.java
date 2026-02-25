package com.pos.dto.inventario;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record InventarioCreateDTO(

        @NotNull(message = "El producto es obligatorio")
        Long productoId,

        @NotNull(message = "El stock inicial es obligatorio")
        @Positive(message = "El stock inicial debe ser mayor a 0")
        Integer stockInicial

) {}
