package com.pos.dto.producto;



@Data
public class ProductoUpdateDTO {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotNull(message = "El precio es obligatorio")
    @Positive(message = "El precio debe ser mayor a 0")
    private BigDecimal precio;

    @NotNull(message = "El estado activo es obligatorio")
    private Boolean activo;


}


