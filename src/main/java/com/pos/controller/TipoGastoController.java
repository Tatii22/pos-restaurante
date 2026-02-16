package com.pos.controller;

import com.pos.entity.TipoGasto;
import com.pos.service.TipoGastoService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.pos.dto.gastoAdmin.TipoGastoCreateDTO;
import java.util.List;



@RestController
@RequestMapping("/tipos-gasto")
@RequiredArgsConstructor
public class TipoGastoController {

    private final TipoGastoService tipoGastoService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TipoGasto> crear(@RequestBody TipoGastoCreateDTO dto) {
        return ResponseEntity.ok(
                tipoGastoService.crear(dto.nombre())
        );
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','CAJA')")
    public ResponseEntity<List<TipoGasto>> listar() {
        return ResponseEntity.ok(tipoGastoService.listar());
    }
}

