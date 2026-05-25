package com.pos.dto.venta;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VentaFiadoDTO(
        Long clienteId,
        String clienteNombre,
        @Pattern(regexp = "^\\d{7,15}$", message = "El telefono del cliente debe tener entre 7 y 15 digitos")
        String clienteTelefono
) {}
