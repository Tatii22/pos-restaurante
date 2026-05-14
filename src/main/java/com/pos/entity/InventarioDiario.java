package com.pos.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(
    name = "inventarios_diarios",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"producto_id", "fecha"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventarioDiario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 📅 FECHA DEL INVENTARIO
    @Column(nullable = false)
    private LocalDate fecha;

    // 🍔 PRODUCTO
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    // 📦 STOCK
    @Column(nullable = false)
    private Integer stockInicial;

    @Column(nullable = false)
    private Integer stockActual;

    @Builder.Default
    @Column(nullable = false)
    private Integer stockMinimo = 5;

    // 🚨 ALERTA
    @Builder.Default
    @Column(nullable = false)
    private Boolean agotado = false;

    @ManyToOne
    @JoinColumn(name = "menu_diario_id", nullable = false)
    private MenuDiario menuDiario;
}

