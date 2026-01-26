package com.pos.entity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


@Entity
@Table(name = "ventas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoVenta tipoVenta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoVenta estado;

    @Column(length = 100)
    private String clienteNombre;

    @Column(length = 20)
    private String telefono;

    @Column(length = 255)
    private String direccion;

    @Column(precision = 10, scale = 2)
    private BigDecimal valorDomicilio;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(precision = 5, scale = 2)
    private BigDecimal descuentoPorcentaje;

    @Column(precision = 10, scale = 2)
    private BigDecimal descuentoValor;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FormaPago formaPago;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private TurnoCaja turno;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<VentaDetalle> detalles;

}

