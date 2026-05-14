package com.pos.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDTO {
    @NotBlank(message = "El usuario es obligatorio")
    private String username;
    @NotBlank(message = "La contrasena es obligatoria")
    private String password;
}
