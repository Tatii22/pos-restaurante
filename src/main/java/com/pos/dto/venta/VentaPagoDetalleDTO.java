package com.pos.dto.venta;

import java.math.BigDecimal;

public record VentaPagoDetalleDTO(
        BigDecimal pagoEfectivo,
        BigDecimal pagoTransferencia,
        BigDecimal recibidoEfectivo,
        BigDecimal recibidoTransferencia,
        BigDecimal cambioEfectivo
) {

    public VentaPagoDetalleDTO(BigDecimal pagoEfectivo, BigDecimal pagoTransferencia) {
        this(pagoEfectivo, pagoTransferencia, pagoEfectivo, pagoTransferencia, BigDecimal.ZERO);
    }
}
