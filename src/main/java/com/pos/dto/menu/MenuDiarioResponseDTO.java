package com.pos.dto.menu;

import java.time.LocalDate;

public record MenuDiarioResponseDTO(
        Long id,
        LocalDate fecha,
        Boolean activo
) {}

