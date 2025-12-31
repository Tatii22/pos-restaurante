package com.pos.dto.producto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProductoResponseDTO {

    private Long id;
    private String nombre;
    private BigDecimal precio;
    private Boolean activo;
    private Long categoriaId;
    private String categoriaNombre;
}
