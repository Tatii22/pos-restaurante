package com.pos.entity;

import jakarta.persistence.*;
import lombok.*;


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


    // 📅 Fecha del menú
    @Column(nullable = false)
    private LocalDate fecha;

    // 🧑 CAJA que creó el menú

    // 🔓 Activo (solo uno activo por día)
    @Builder.Default
    @Column(nullable = false)
    private Boolean activo = true;
}


