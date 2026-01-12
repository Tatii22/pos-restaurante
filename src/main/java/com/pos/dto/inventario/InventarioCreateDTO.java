package com.pos.dto.inventario;

import jakarta.persistence.Column;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record InventarioCreateDTO(

        @NotNull Long productoId,

        @NotNull @Positive Integer stockInicial

) {}
