package com.pos.entity;

import jakarta.persistence.*;
import lombok.*;


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


    // 📅 FECHA DEL INVENTARIO
    @Column(nullable = false)
    private LocalDate fecha;

    // 🍔 PRODUCTO

    // 📦 STOCK



    // 🚨 ALERTA

}



