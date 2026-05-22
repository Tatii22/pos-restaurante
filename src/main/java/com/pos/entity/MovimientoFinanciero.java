package com.pos.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimientos_financieros", indexes = {
        @Index(name = "idx_mov_fin_fecha", columnList = "fecha"),
        @Index(name = "idx_mov_fin_turno", columnList = "turno_id"),
        @Index(name = "idx_mov_fin_venta", columnList = "venta_id"),
        @Index(name = "idx_mov_fin_tipo_medio", columnList = "tipo,medio")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimientoFinanciero {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private MovimientoFinancieroTipo tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MedioFinanciero medio;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false)
    private Integer direccion;

    @ManyToOne(fetch = FetchType.LAZY)
    private TurnoCaja turno;

    @ManyToOne(fetch = FetchType.LAZY)
    private Venta venta;

    @ManyToOne(fetch = FetchType.LAZY)
    private AbonoFiado abonoFiado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private GastoCaja gastoCaja;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private GastoAdmin gastoAdmin;

    @ManyToOne(fetch = FetchType.LAZY)
    private Usuario usuario;

    @Column(length = 255)
    private String descripcion;
}
