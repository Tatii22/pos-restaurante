package com.pos.controller;


@RestController
@RequestMapping("/configuracion")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ConfiguracionController {

    private final ConfiguracionService configuracionService;

    @GetMapping
    public ResponseEntity<AdminConfigDTO> obtener() {
        return ResponseEntity.ok(configuracionService.obtener());
    }

    @PutMapping
    public ResponseEntity<AdminConfigDTO> guardar(@RequestBody AdminConfigDTO dto) {
        return ResponseEntity.ok(configuracionService.guardar(dto));
    }
}

