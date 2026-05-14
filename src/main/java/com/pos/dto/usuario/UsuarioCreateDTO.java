package com.pos.dto.usuario;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class UsuarioCreateDTO {

    @NotBlank(message = "El usuario es obligatorio")
    private String username;

    @NotBlank(message = "La contrasena es obligatoria")
    private String password;

    @NotBlank(message = "El rol es obligatorio")
    private String rol; // CAJA o DOMI
}

