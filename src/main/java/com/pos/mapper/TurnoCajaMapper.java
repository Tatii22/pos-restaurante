package com.pos.mapper;

import com.pos.dto.turno.TurnoCajaResponseDTO;
import com.pos.entity.TurnoCaja;

public class TurnoCajaMapper {

    public static TurnoCajaResponseDTO toDTO(TurnoCaja turno) {
        return new TurnoCajaResponseDTO(
                turno.getId(),
                turno.getFechaApertura(),
                turno.getFechaCierre(),
                turno.getMontoInicial(),
                turno.getTotalVentas(),
                turno.getTotalGastos(),
                turno.getEsperado(),
                turno.getFaltante(),
                turno.getTransferenciasNetas() != null ? turno.getTransferenciasNetas() : java.math.BigDecimal.ZERO,
                turno.getTotalOperativoTurno() != null ? turno.getTotalOperativoTurno() : turno.getEsperado(),
                // Métricas operativas puras (sin base)
                (turno.getEsperado() != null ? turno.getEsperado() : java.math.BigDecimal.ZERO)
                        .subtract(turno.getMontoInicial() != null ? turno.getMontoInicial() : java.math.BigDecimal.ZERO),
                turno.getTransferenciasNetas() != null ? turno.getTransferenciasNetas() : java.math.BigDecimal.ZERO,
                // totalOperativoNeto = (esperado - inicial) + transferenciasNetas
                (turno.getEsperado() != null ? turno.getEsperado() : java.math.BigDecimal.ZERO)
                        .subtract(turno.getMontoInicial() != null ? turno.getMontoInicial() : java.math.BigDecimal.ZERO)
                        .add(turno.getTransferenciasNetas() != null ? turno.getTransferenciasNetas() : java.math.BigDecimal.ZERO),
                // Conciliación dual (arqueo)
                turno.getEfectivoContado(),
                turno.getTransferenciasVerificadas(),
                turno.getDiferenciaEfectivo(),
                turno.getDiferenciaTransferencias(),
                turno.getTotalVerificado(),
                turno.getDiferenciaTotal(),
                turno.getEstado(),
                turno.getUsuario().getUsername()
        );
    }
}
