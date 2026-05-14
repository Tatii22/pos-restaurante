package com.pos.mapper;


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

