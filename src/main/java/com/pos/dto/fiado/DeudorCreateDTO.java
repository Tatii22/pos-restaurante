package com.pos.dto.fiado;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record DeudorCreateDTO(
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,
        @NotBlank(message = "El telefono es obligatorio")
        @Pattern(regexp = "\\d{7,15}", message = "El telefono debe tener entre 7 y 15 digitos")
        String telefono,
        String direccionPredeterminada,
        String notas
) {
}
