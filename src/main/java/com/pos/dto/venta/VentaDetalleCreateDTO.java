package com.pos.dto.venta;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record VentaDetalleCreateDTO(
        @NotNull(message = "El producto es obligatorio")
        Long productoId,
        @NotNull(message = "La cantidad es obligatoria")
        @Positive(message = "La cantidad debe ser mayor a 0")
        Integer cantidad,
        @Size(max = 255, message = "La observacion no puede superar 255 caracteres")
        String observacion
) {}
