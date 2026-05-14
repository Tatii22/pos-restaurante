package com.pos.mapper;

public class MenuDiarioMapper {

    public static MenuDiarioResponseDTO toDTO(MenuDiario menu) {
        return new MenuDiarioResponseDTO(
                menu.getId(),
                menu.getFecha(),
                menu.getActivo()
        );
    }
}

