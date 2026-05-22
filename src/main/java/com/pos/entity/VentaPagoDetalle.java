package com.pos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "venta_pago_detalle")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VentaPagoDetalle {

    @Id
    @Column(name = "venta_id")
    private Long ventaId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(
            name = "venta_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_venta_pago_detalle_venta")
    )
    private Venta venta;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal pagoEfectivo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal pagoTransferencia;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal recibidoEfectivo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal recibidoTransferencia;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal cambioEfectivo;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
