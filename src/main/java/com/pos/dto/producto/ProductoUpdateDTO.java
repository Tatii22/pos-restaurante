package com.pos.dto.producto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class ProductoUpdateDTO {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotNull
    @Positive
    private BigDecimal precio;

    @NotNull
    private Boolean activo;

    @NotNull
    private Long categoriaId;
}
