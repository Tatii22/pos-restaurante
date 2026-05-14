package com.pos.dto.usuario;
import jakarta.validation.constraints.NotBlank;


@Getter
@Setter
public class UsuarioCreateDTO {



    @NotBlank(message = "El rol es obligatorio")
    private String rol; // CAJA o DOMI
}



