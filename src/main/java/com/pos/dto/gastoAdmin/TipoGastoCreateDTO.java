package com.pos.dto.gastoAdmin;
import jakarta.validation.constraints.NotBlank;


public record TipoGastoCreateDTO(
        @NotBlank(message = "El nombre del tipo de gasto es obligatorio")
        String nombre
) {}
