package com.pos.controller;

import com.pos.dto.gastoAdmin.GastoAdminCreateDTO;
import com.pos.entity.GastoAdmin;
import com.pos.entity.Usuario;
import com.pos.service.GastoAdminService;
import com.pos.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/gastos-admin")
@RequiredArgsConstructor
public class GastoAdminController {

    private final GastoAdminService gastoAdminService;
    private final UsuarioService usuarioService;

    // ✅ Registrar gasto administrativo
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GastoAdmin> registrar(
            @Valid @RequestBody GastoAdminCreateDTO dto,
            Authentication auth
    ) {
        Usuario usuario = usuarioService.obtenerPorUsername(auth.getName());
        return ResponseEntity.ok(
                gastoAdminService.registrar(dto, usuario)
        );
    }

    // 📅 Listar por fecha
    @GetMapping("/fecha/{fecha}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<GastoAdmin>> listarPorFecha(
            @PathVariable LocalDate fecha
    ) {
        return ResponseEntity.ok(
                gastoAdminService.listarPorFecha(fecha)
        );
    }

    // 📆 Listar por rango
    @GetMapping("/rango")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<GastoAdmin>> listarPorRango(
            @RequestParam LocalDate inicio,
            @RequestParam LocalDate fin
    ) {
        return ResponseEntity.ok(
                gastoAdminService.listarPorRango(inicio, fin)
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long id,
            Authentication auth
    ) {
        Usuario usuario = usuarioService.obtenerPorUsername(auth.getName());
        gastoAdminService.eliminarPorId(id, usuario);
        return ResponseEntity.noContent().build();
    }
}
