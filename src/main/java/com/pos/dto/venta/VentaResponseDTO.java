package com.pos.dto.venta;

import com.pos.entity.EstadoVenta;
import com.pos.entity.FormaPago;
import com.pos.entity.TipoVenta;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record VentaResponseDTO(
        Long id,
        LocalDateTime fecha,
        TipoVenta tipoVenta,
        EstadoVenta estado,
        String clienteNombre,
        String telefono,
        String direccion,
        BigDecimal valorDomicilio,
        BigDecimal descuentoPorcentaje,
        BigDecimal descuentoValor,
        BigDecimal total,
        FormaPago formaPago
) {}
