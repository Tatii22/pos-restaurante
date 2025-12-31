package com.pos.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "inventarios_diarios",
    uniqueConstraints = @UniqueConstraint(columnNames = {"menu_diario_id", "producto_id"})
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_diario_id", nullable = false)
    private MenuDiario menuDiario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(nullable = false)
    private Integer stockInicial;

    @Column(nullable = false)
    private Integer stockActual;

    @Column(nullable = false)
    private Integer stockMinimo;

    @Builder.Default
    @Column(nullable = false)
    private Boolean agotado = false;
}
