package com.pos.dto.fiado;

import com.pos.dto.venta.VentaResponseDTO;

import java.math.BigDecimal;
import java.util.List;

public record DeudorDetalleDTO(
        Long id,
        String nombre,
        String telefono,
        BigDecimal deudaTotal,
        List<VentaResponseDTO> ventasPendientes,
        List<AbonoFiadoResponseDTO> abonos
) {
}
