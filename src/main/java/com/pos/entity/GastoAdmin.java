package com.pos.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "gastos_admin")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GastoAdmin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false, length = 150)
    private String descripcion;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(precision = 12, scale = 2)
    private BigDecimal montoEfectivo;

    @Column(precision = 12, scale = 2)
    private BigDecimal montoTransferencia;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tipo_gasto_id", nullable = false)
    private TipoGasto tipo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
}

