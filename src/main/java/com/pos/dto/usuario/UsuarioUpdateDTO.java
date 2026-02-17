package com.pos.dto.usuario;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UsuarioUpdateDTO {

    @NotBlank
    private String username;

    @NotBlank
    private String rol;

    private Boolean activo;

    // Opcional: si viene vacío o null, no se cambia la contraseña.
    private String password;
}

