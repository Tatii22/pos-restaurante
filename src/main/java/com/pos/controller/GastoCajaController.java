package com.pos.controller;

import com.pos.dto.turno.GastoCajaCreateDTO;
import com.pos.dto.turno.GastoCajaResponseDTO;
import com.pos.entity.GastoCaja;
import com.pos.entity.Usuario;
import com.pos.repository.UsuarioRepository;
import com.pos.service.GastoCajaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/gastos-caja")
@RequiredArgsConstructor
public class GastoCajaController {

    private final GastoCajaService gastoCajaService;
    private final UsuarioRepository usuarioRepository;

    @PostMapping
    @PreAuthorize("hasRole('CAJA')")
    public ResponseEntity<GastoCaja> registrar(
            @Valid @RequestBody GastoCajaCreateDTO dto,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Usuario usuario = usuarioRepository
                .findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        GastoCaja gasto = gastoCajaService.registrar(dto, usuario);

        return ResponseEntity.ok(gasto);
    }

    @GetMapping
    @PreAuthorize("hasRole('CAJA')")
    public ResponseEntity<List<GastoCajaResponseDTO>> listarTurnoActivo(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Usuario usuario = usuarioRepository
                .findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return ResponseEntity.ok(gastoCajaService.listarTurnoActivo(usuario));
    }
}
