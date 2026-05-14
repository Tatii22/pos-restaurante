package com.pos.entity;



@Entity
@Table(name = "venta_pago_detalle")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VentaPagoDetalle {


    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(
            name = "venta_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_venta_pago_detalle_venta")
    )



}


