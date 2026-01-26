package com.pos.dto.producto;
import lombok.Builder;
import java.util.List;

@Builder
public record ProductosVentaHoyDTO(
        List<ProductoVentaDTO> menuDiario,
        List<ProductoVentaDTO> siempreDisponibles
) {}

