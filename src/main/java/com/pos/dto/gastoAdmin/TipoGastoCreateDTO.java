package com.pos.dto.gastoAdmin;
import jakarta.validation.constraints.NotBlank;


public record TipoGastoCreateDTO(
        @NotBlank
        String nombre
) {}
