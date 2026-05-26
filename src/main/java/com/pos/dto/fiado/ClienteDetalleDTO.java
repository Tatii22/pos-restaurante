package com.pos.dto.fiado;

import com.pos.dto.venta.VentaResponseDTO;

import java.math.BigDecimal;
import java.util.List;

public record ClienteDetalleDTO(
        Long id,
        String nombre,
        String telefono,
        String direccionPredeterminada,
        String notas,
        Boolean tieneDeuda,
        BigDecimal deudaTotal,
        List<VentaResponseDTO> ventasPendientes,
        List<AbonoFiadoResponseDTO> abonos
) {
}
