package com.pos.controller;

import com.pos.service.InventarioDiarioService;
import lombok.RequiredArgsConstructor;
import com.pos.entity.InventarioDiario;
import com.pos.dto.inventario.*;
import com.pos.mapper.InventarioMapper;

import java.util.List;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;




@RestController
@RequestMapping("/inventario-diario")
@RequiredArgsConstructor
public class InventarioDiarioController {

    private final InventarioDiarioService inventarioService;

    @PostMapping
    @PreAuthorize("hasRole('CAJA')")
    public ResponseEntity<InventarioResponseDTO> crear(
            @RequestParam Long productoId,
            @RequestParam Integer stockInicial
    ) {
        InventarioDiario inv =
                inventarioService.crearInventario(productoId, stockInicial);

        return ResponseEntity.ok(InventarioMapper.toDTO(inv));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CAJA','DOMI')")
    public ResponseEntity<List<InventarioResponseDTO>> listar() {

        return ResponseEntity.ok(
                inventarioService.listarHoy()
                        .stream()
                        .map(InventarioMapper::toDTO)
                        .toList()
        );
    }

    @PostMapping("/{id}/reabastecer")
    @PreAuthorize("hasRole('CAJA')")
    public ResponseEntity<InventarioResponseDTO> reabastecer(
            @PathVariable Long id,
            @RequestParam Integer cantidad
    ) {
        InventarioDiario inv =
                inventarioService.reabastecer(id, cantidad);

        return ResponseEntity.ok(InventarioMapper.toDTO(inv));
    }
}


