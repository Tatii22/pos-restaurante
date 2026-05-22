package com.pos.dto.report;

import com.pos.dto.venta.VentaResponseDTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReporteVentaDTO {

    private LocalDate fechaInicio;
    private LocalDate fechaFin;

    private Long totalVentas;

    private BigDecimal totalBruto;
    private BigDecimal totalDescuentos;
    private BigDecimal totalNeto;

    private BigDecimal totalEfectivo;
    private BigDecimal totalTransferencia;
    private BigDecimal totalAbonos;
    private BigDecimal totalAbonosEfectivo;
    private BigDecimal totalAbonosTransferencia;
    private Long totalVentasContado;
    private Long totalVentasFiadas;
    private BigDecimal totalMontoContado;
    private BigDecimal totalMontoFiado;
    private BigDecimal carteraGenerada;
    private BigDecimal carteraPendiente;
    private BigDecimal recaudoReal;
    private List<VentaResponseDTO> ventas;

    
}
