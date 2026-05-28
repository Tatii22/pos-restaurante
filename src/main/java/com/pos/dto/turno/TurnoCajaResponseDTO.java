package com.pos.dto.turno;

import com.pos.entity.EstadoTurno;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class TurnoCajaResponseDTO {

    private Long id;
    private Integer numeroTurno;
    private LocalDateTime fechaApertura;
    private LocalDateTime fechaCierre;

    private BigDecimal montoInicial;

    // Recaudo bruto del turno: todo el dinero recibido, sin descontar gastos
    private BigDecimal recaudoBruto;

    // Total ventas del turno (alias de recaudoBruto para compatibilidad frontend)
    private BigDecimal totalVentas;

    // Gastos solo de caja (del ledger del turno)
    private BigDecimal totalGastos;

    // Gastos admin del período del turno (por fecha, no por turno_id)
    private BigDecimal totalGastosAdmin;

    // Gastos totales = totalGastos + totalGastosAdmin
    private BigDecimal totalGastosCombinados;

    // Ganancia neta real = totalVentas - totalGastosCombinados
    private BigDecimal gananciaNeta;

    private BigDecimal esperado;
    private BigDecimal faltante;
    private BigDecimal transferenciasNetas;
    private BigDecimal totalOperativoTurno;

    // Métricas operativas puras (sin base de caja)
    private BigDecimal efectivoOperativo;
    private BigDecimal transferenciasOperativas;
    private BigDecimal totalOperativoNeto;

    // Conciliación dual (arqueo y cierre)
    private BigDecimal efectivoContado;
    private BigDecimal transferenciasVerificadas;
    private BigDecimal diferenciaEfectivo;
    private BigDecimal diferenciaTransferencias;
    private BigDecimal totalVerificado;
    private BigDecimal diferenciaTotal;

    private EstadoTurno estado;
    private String usuario;

    // Umbral de descuadre del servidor (para validación en frontend)
    private BigDecimal umbralDescuadre;

    // Justificación del cajero cuando el turno cerró descuadrado
    private String observacionCierre;
}
