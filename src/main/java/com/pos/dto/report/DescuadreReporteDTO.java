package com.pos.dto.report;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DescuadreReporteDTO(
    Long turnoId,
    Integer numeroTurno,
    LocalDateTime fechaApertura,
    BigDecimal diferenciaTotal,
    String observacionCierre
) {}
