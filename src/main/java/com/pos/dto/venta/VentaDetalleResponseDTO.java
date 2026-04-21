package com.pos.dto.venta;

import com.pos.entity.EstadoVenta;
import com.pos.entity.FormaPago;
import com.pos.entity.TipoVenta;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record VentaDetalleResponseDTO(
        Long id,
        LocalDateTime fecha,
        TipoVenta tipoVenta,
        EstadoVenta estado,
        String clienteNombre,
        String telefono,
        String direccion,
        BigDecimal valorDomicilio,
        Boolean paraLlevar,
        BigDecimal descuentoPorcentaje,
        BigDecimal descuentoValor,
        BigDecimal total,
        FormaPago formaPago,
        BigDecimal pagoEfectivo,
        BigDecimal pagoTransferencia,
        LocalDateTime fechaAnulacion,
        String motivoAnulacion,
        List<VentaItemResponseDTO> detalles
) {
}
