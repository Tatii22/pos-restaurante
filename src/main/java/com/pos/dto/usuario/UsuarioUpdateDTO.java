package com.pos.dto.usuario;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UsuarioUpdateDTO {

    @NotBlank(message = "El usuario es obligatorio")
    private String username;

    @NotBlank(message = "El rol es obligatorio")
    private String rol;

    private Boolean activo;

    // Opcional: si viene vacío o null, no se cambia la contraseña.
    private String password;
}
