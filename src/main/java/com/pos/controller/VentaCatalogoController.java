package com.pos.controller;
import com.pos.dto.producto.ProductosVentaHoyDTO;
import com.pos.service.ProductoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/ventas")
@RequiredArgsConstructor
public class VentaCatalogoController {

    private final ProductoService productoService;

    @GetMapping("/catalogo-hoy")
    @PreAuthorize("hasAnyRole('CAJA','DOMI')")
    public ResponseEntity<ProductosVentaHoyDTO> catalogoHoy() {
        return ResponseEntity.ok(
                productoService.productosVentaHoy()
        );
    }
}

