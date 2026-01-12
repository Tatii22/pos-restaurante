package com.pos.dto.usuario;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UsuarioResponseDTO {

    private Long id;
    private String username;
    private String rol;
    private Boolean activo;
}
