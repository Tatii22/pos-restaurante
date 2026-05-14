package com.pos.controller;


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


