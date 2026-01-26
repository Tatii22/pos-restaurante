package com.pos.service;

import com.pos.entity.MenuDiario;

import com.pos.repository.MenuDiarioRepository;

import com.pos.entity.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import com.pos.exception.BadRequestException;


@Service
@RequiredArgsConstructor
public class MenuDiarioService {

    private final MenuDiarioRepository menuDiarioRepository;

    public MenuDiario crearMenuHoy(Usuario usuario) {

        LocalDate hoy = LocalDate.now();

        menuDiarioRepository.findByFechaAndActivoTrue(hoy)
                .ifPresent(m -> {
                    throw new RuntimeException("Ya existe un menú activo hoy");
                });

        MenuDiario menu = MenuDiario.builder()
                .fecha(hoy)
                .usuario(usuario) 
                .activo(true)
                .build();

        return menuDiarioRepository.save(menu);
    }
    public MenuDiario obtenerMenuActivo() {
        return menuDiarioRepository
                .findByFechaAndActivoTrue(LocalDate.now())
                .orElseThrow(() -> new BadRequestException("No hay menú activo hoy"));
    }

}