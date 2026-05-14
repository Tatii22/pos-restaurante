package com.pos.entity;


import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "turnos_caja")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TurnoCaja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fechaApertura;

    private LocalDateTime fechaCierre;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montoInicial;

    @Column(precision = 12, scale = 2)
    private BigDecimal montoFinal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalVentas;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalGastos;

    @Column(precision = 12, scale = 2)
    private BigDecimal esperado;

    @Column(precision = 12, scale = 2)
    private BigDecimal faltante;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoTurno estado;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
}
