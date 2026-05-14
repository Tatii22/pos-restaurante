package com.pos.dto.categoria;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CategoriaResponseDTO {

    private Long id;
    private String nombre;
    private Boolean activa;
}
