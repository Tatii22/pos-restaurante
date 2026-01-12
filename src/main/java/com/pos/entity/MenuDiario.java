package com.pos.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(
    name = "menus_diarios",
    uniqueConstraints = @UniqueConstraint(columnNames = "fecha")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuDiario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 📅 Fecha del menú
    @Column(nullable = false)
    private LocalDate fecha;

    // 🧑 CAJA que creó el menú
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    // 🔓 Activo (solo uno activo por día)
    @Builder.Default
    @Column(nullable = false)
    private Boolean activo = true;
}
