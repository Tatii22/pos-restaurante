package com.pos.controller;


import org.springframework.web.bind.annotation.*;



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


