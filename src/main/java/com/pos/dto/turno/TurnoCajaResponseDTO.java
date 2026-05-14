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

    private EstadoTurno estado;

    private String usuario;
}
