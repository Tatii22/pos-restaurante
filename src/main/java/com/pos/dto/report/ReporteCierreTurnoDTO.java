package com.pos.dto.report;
import com.pos.dto.turno.GastoCajaResponseDTO;
import com.pos.dto.venta.VentaResponseDTO;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class ReporteCierreTurnoDTO {

    private Long turnoId;
    private LocalDateTime apertura;
    private LocalDateTime cierre;

    private BigDecimal totalVentas;
    private BigDecimal totalEfectivo;
    private BigDecimal totalTransferencia;
    private BigDecimal totalGastos;

    private BigDecimal netoEnCaja;

    private List<VentaResponseDTO> ventas;
    private List<GastoCajaResponseDTO> gastos;
}
