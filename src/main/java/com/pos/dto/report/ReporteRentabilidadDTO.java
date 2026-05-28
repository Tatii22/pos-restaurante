package com.pos.dto.report;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.pos.dto.gasto.GastoResponseDTO;
import com.pos.dto.venta.VentaResponseDTO;

@Getter
@Setter
public class ReporteRentabilidadDTO {

    private LocalDate fechaInicio;
    private LocalDate fechaFin;

    private BigDecimal totalVentas = BigDecimal.ZERO;
    private BigDecimal totalVentasEfectivo = BigDecimal.ZERO;
    private BigDecimal totalVentasTransferencia = BigDecimal.ZERO;

    private BigDecimal totalGastos = BigDecimal.ZERO;
    private BigDecimal totalGastosEfectivo = BigDecimal.ZERO;
    private BigDecimal totalGastosTransferencia = BigDecimal.ZERO;

    private BigDecimal gananciaNeta = BigDecimal.ZERO;
    private BigDecimal totalVentasComerciales = BigDecimal.ZERO;
    private BigDecimal ventasContado = BigDecimal.ZERO;
    private BigDecimal ventasFiadas = BigDecimal.ZERO;
    private BigDecimal carteraGenerada = BigDecimal.ZERO;
    private BigDecimal recaudoReal = BigDecimal.ZERO;

    private BigDecimal diferenciaAcumulada = BigDecimal.ZERO;
    private BigDecimal resultadoRealAjustado = BigDecimal.ZERO;
    private List<DescuadreReporteDTO> descuadres;

    private List<VentaResponseDTO> ventas;
    private List<GastoResponseDTO> gastos;

    
}

