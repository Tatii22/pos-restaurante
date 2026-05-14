package com.pos.controller;

import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/gastos-admin")
@RequiredArgsConstructor
public class GastoAdminController {

    private final GastoAdminService gastoAdminService;
    private final UsuarioService usuarioService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GastoAdminResponseDTO> registrar(
            @Valid @RequestBody GastoAdminCreateDTO dto,
            Authentication auth
    ) {
        Usuario usuario = usuarioService.obtenerPorUsername(auth.getName());
        return ResponseEntity.ok(toResponse(gastoAdminService.registrar(dto, usuario)));
    }

    @GetMapping("/fecha/{fecha}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<GastoAdminResponseDTO>> listarPorFecha(
            @PathVariable LocalDate fecha
    ) {
        return ResponseEntity.ok(
                gastoAdminService.listarPorFecha(fecha).stream()
                        .map(this::toResponse)
                        .toList()
        );
    }

    @GetMapping("/rango")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<GastoAdminResponseDTO>> listarPorRango(
            @RequestParam LocalDate inicio,
            @RequestParam LocalDate fin
    ) {
        return ResponseEntity.ok(
                gastoAdminService.listarPorRango(inicio, fin).stream()
                        .map(this::toResponse)
                        .toList()
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

    private GastoAdminResponseDTO toResponse(GastoAdmin gasto) {
        return new GastoAdminResponseDTO(
                gasto.getId(),
                gasto.getFecha(),
                gasto.getDescripcion(),
                gasto.getMonto(),
                gasto.getMontoEfectivo(),
                gasto.getMontoTransferencia(),
                gasto.getTipo().getNombre(),
                gasto.getUsuario().getUsername()
        );
    }
}

