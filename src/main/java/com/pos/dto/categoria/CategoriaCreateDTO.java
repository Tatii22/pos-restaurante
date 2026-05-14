package com.pos.dto.categoria;


@Data
public class CategoriaCreateDTO {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

}


