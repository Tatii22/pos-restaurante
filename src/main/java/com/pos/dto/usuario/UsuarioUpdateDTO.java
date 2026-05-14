package com.pos.dto.usuario;


@Getter
@Setter
public class UsuarioUpdateDTO {


    @NotBlank(message = "El rol es obligatorio")
    private String rol;


    // Opcional: si viene vacío o null, no se cambia la contraseña.
}


