package com.pos.mapper;

import com.pos.dto.inventario.InventarioResponseDTO;
import com.pos.entity.InventarioDiario;

public class InventarioMapper {

    public static InventarioResponseDTO toDTO(InventarioDiario inventario) {
        return new InventarioResponseDTO(
                inventario.getId(),
                inventario.getFecha(),
                inventario.getProducto().getId(),
                inventario.getProducto().getNombre(),
                inventario.getStockInicial(),
                inventario.getStockActual(),
                inventario.getStockMinimo(),
                inventario.getAgotado()
        );
    }
}
