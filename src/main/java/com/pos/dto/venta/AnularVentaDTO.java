package com.pos.dto.venta;

import jakarta.validation.constraints.Size;

public record AnularVentaDTO(
        @Size(max = 500, message = "El motivo no puede superar 500 caracteres")
        String motivo
) {
}
