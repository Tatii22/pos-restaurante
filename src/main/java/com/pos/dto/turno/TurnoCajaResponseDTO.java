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
    private LocalDateTime fechaApertura;
    private LocalDateTime fechaCierre;

    private BigDecimal montoInicial;
    private BigDecimal totalVentas;
    private BigDecimal totalGastos;
    private BigDecimal esperado;
    private BigDecimal faltante;
    private BigDecimal transferenciasNetas;
    private BigDecimal totalOperativoTurno;

    // === Métricas operativas puras (sin baseCaja) para reportes históricos ===
    // Efectivo operativo = (ventasEfectivo + abonosE - gastosE) = esperado - montoInicial
    private BigDecimal efectivoOperativo;
    private BigDecimal transferenciasOperativas; // = transferenciasNetas (ya es neto)
    private BigDecimal totalOperativoNeto;       // = efectivoOperativo + transferenciasOperativas

    // Conciliación dual (arqueo y cierre) - NO usar en tabla principal de reportes
    private BigDecimal efectivoContado;
    private BigDecimal transferenciasVerificadas;
    private BigDecimal diferenciaEfectivo;
    private BigDecimal diferenciaTransferencias;
    private BigDecimal totalVerificado;
    private BigDecimal diferenciaTotal;

    private EstadoTurno estado;

    private String usuario;
}
