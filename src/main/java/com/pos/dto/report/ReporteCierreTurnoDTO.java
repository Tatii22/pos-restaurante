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
    private BigDecimal totalGastosEfectivo;
    private BigDecimal totalGastosTransferencia;
    private BigDecimal gananciaEfectivo;
    private BigDecimal gananciaTransferencia;
    private BigDecimal netoEnCaja;

    // === Valores calculados por ledger (usados como Efectivo / Transferencias / Total en reportes) ===
    private BigDecimal cajaFisicaEsperada;   // = Efectivo final físico real (base + neto efectivo)
    private BigDecimal transferenciasNetas;  // = Transferencias netas reales
    private BigDecimal totalOperativoTurno;  // = Total = Efectivo + Transferencias

    // Campos legacy de comparación (ya no se usan en tablas principales)
    private BigDecimal cajaContada;
    private BigDecimal diferenciaCaja;

    // Conciliación dual (si el turno ya fue cerrado)
    private BigDecimal efectivoContado;
    private BigDecimal transferenciasVerificadas;
    private BigDecimal diferenciaEfectivo;
    private BigDecimal diferenciaTransferencias;
    private BigDecimal totalVerificado;
    private BigDecimal diferenciaTotal;
    private BigDecimal totalAbonos;
    private BigDecimal totalAbonosEfectivo;
    private BigDecimal totalAbonosTransferencia;
    private List<VentaResponseDTO> ventas;
    private List<GastoCajaResponseDTO> gastos;
}
