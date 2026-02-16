package com.pos.dto.venta;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record VentaCocinaPreviewDTO(
        String clienteNombre,
        @NotEmpty(message = "Debe enviar al menos un producto para cocina")
        List<VentaDetalleCreateDTO> detalles
) {}

