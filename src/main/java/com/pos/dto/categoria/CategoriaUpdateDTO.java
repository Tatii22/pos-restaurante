package com.pos.dto.categoria;


@Data
public class CategoriaUpdateDTO {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

}


