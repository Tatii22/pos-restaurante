package com.pos.dto.venta;
import java.util.List;

public record VentaCreateDTO(
        String formaPago,
        String clienteNombre,
        List<VentaDetalleCreateDTO> detalles
) {}

