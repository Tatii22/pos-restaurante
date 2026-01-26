package com.pos.controller;


import com.pos.service.MenuDiarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import com.pos.entity.Usuario;
import com.pos.mapper.MenuDiarioMapper;
import com.pos.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

import com.pos.dto.menu.MenuDiarioResponseDTO;
import com.pos.entity.MenuDiario;


@RestController
@RequestMapping("/menu-diario")
@RequiredArgsConstructor
public class MenuDiarioController {

    private final MenuDiarioService menuDiarioService;
    private final UsuarioRepository usuarioRepository;

    @PostMapping
    @PreAuthorize("hasRole('CAJA')")
    public ResponseEntity<MenuDiario> crearMenu(
            @AuthenticationPrincipal UserDetails userDetails
    ) {

        Usuario usuario = usuarioRepository
                .findByUsername(userDetails.getUsername())
                .orElseThrow(() ->
                        new RuntimeException("Usuario no encontrado")
                );

        return ResponseEntity.ok(
                menuDiarioService.crearMenuHoy(usuario)
        );
    }

    @GetMapping("/activo")
    @PreAuthorize("hasRole('CAJA')")
    public ResponseEntity<MenuDiarioResponseDTO> obtenerActivo() {
        
        MenuDiario menu = menuDiarioService.obtenerMenuActivo();
        
        return ResponseEntity.ok(
                MenuDiarioMapper.toDTO(menu)
        );

    }


}



