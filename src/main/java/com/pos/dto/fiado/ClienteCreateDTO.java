package com.pos.dto.fiado;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ClienteCreateDTO(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 60, message = "El nombre no puede superar 60 caracteres")
        String nombre,
        @NotBlank(message = "El telefono es obligatorio")
        @Pattern(regexp = "\\d{7,15}", message = "El telefono debe tener entre 7 y 15 digitos")
        String telefono,
        @Size(max = 120, message = "La direccion no puede superar 120 caracteres")
        String direccionPredeterminada,
        @Size(max = 500, message = "Las notas no pueden superar 500 caracteres")
        String notas
) {
}
