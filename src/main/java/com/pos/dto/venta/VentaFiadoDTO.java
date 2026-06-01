package com.pos.dto.venta;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record VentaFiadoDTO(
        Long clienteId,
        @Size(max = 60, message = "El nombre del cliente no puede superar 60 caracteres")
        String clienteNombre,
        @Pattern(regexp = "^\\d{7,15}$", message = "El teléfono del cliente debe tener entre 7 y 15 dígitos")
        String clienteTelefono,
        BigDecimal pagoEfectivo,
        BigDecimal pagoTransferencia
) {}
