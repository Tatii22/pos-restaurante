package com.pos.mapper;

import com.pos.dto.turno.TurnoCajaResponseDTO;
import com.pos.entity.TurnoCaja;

import java.math.BigDecimal;

public class TurnoCajaMapper {

    public static TurnoCajaResponseDTO toDTO(TurnoCaja turno) {
        BigDecimal montoInicial            = safe(turno.getMontoInicial());
        BigDecimal recaudoBruto            = safe(turno.getRecaudoBruto());
        BigDecimal gastosCaja              = safe(turno.getTotalGastos());
        BigDecimal gastosAdmin             = safe(turno.getTotalGastosAdmin());
        BigDecimal gananciaNeta            = safe(turno.getGananciaNeta());
        BigDecimal esperado                = safe(turno.getEsperado());
        BigDecimal transferenciasNetas      = safe(turno.getTransferenciasNetas());
        BigDecimal totalOperativoTurno     = safe(turno.getTotalOperativoTurno());

        // ── Métricas derivadas (operativas, no financieras) ─────────────
        BigDecimal efectivoOperativo       = esperado.subtract(montoInicial);
        BigDecimal transferenciasOperativas = transferenciasNetas;
        BigDecimal totalOperativoNeto      = efectivoOperativo.add(transferenciasOperativas);
        BigDecimal totalGastosCombinados   = gastosCaja.add(gastosAdmin);

        return new TurnoCajaResponseDTO(
                turno.getId(),
                turno.getNumeroTurno(),
                turno.getFechaApertura(),
                turno.getFechaCierre(),
                montoInicial,
                recaudoBruto,
                safe(turno.getTotalVentas()),
                gastosCaja,
                gastosAdmin,
                totalGastosCombinados,
                gananciaNeta,
                esperado,
                safe(turno.getFaltante()),
                transferenciasNetas,
                totalOperativoTurno,
                efectivoOperativo,
                transferenciasOperativas,
                totalOperativoNeto,
                safe(turno.getEfectivoContado()),
                safe(turno.getTransferenciasVerificadas()),
                safe(turno.getDiferenciaEfectivo()),
                safe(turno.getDiferenciaTransferencias()),
                safe(turno.getTotalVerificado()),
                safe(turno.getDiferenciaTotal()),
                turno.getEstado(),
                turno.getUsuario() != null ? turno.getUsuario().getUsername() : "—",
                safe(turno.getUmbralDescuadre()),
                turno.getObservacionCierre()
        );
    }

    private static BigDecimal safe(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
