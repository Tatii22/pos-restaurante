package com.pos.mapper;
import com.pos.dto.menu.MenuDiarioResponseDTO;
import com.pos.entity.MenuDiario;

public class MenuDiarioMapper {

    public static MenuDiarioResponseDTO toDTO(MenuDiario menu) {
        return new MenuDiarioResponseDTO(
                menu.getId(),
                menu.getFecha(),
                menu.getActivo()
        );
    }
}
