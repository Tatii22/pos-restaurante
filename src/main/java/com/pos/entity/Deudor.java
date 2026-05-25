package com.pos.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "deudores", uniqueConstraints = {
        @UniqueConstraint(name = "uk_deudor_telefono", columnNames = "telefono")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Deudor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 20)
    private String telefono;

    /**
     * Dirección predeterminada del cliente.
     * Se autocompleta en nuevos domicilios y se actualiza con la última dirección usada.
     */
    @Column(length = 255)
    private String direccionPredeterminada;

    /**
     * Notas libres visibles para el operador: "timbre roto", "pago siempre en efectivo", etc.
     */
    @Column(length = 255)
    private String notas;

    @Column(nullable = false)
    private Boolean activo;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(nullable = false)
    private LocalDateTime fechaActualizacion;
}
