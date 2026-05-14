package com.pos.controller;

import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/gastos-caja")
@RequiredArgsConstructor
public class GastoCajaController {

    private final GastoCajaService gastoCajaService;
    private final UsuarioRepository usuarioRepository;

    @PostMapping
    @PreAuthorize("hasRole('CAJA')")
    public ResponseEntity<GastoCajaResponseDTO> registrar(
            @Valid @RequestBody GastoCajaCreateDTO dto,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Usuario usuario = usuarioRepository
                .findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        GastoCaja gasto = gastoCajaService.registrar(dto, usuario);

        return ResponseEntity.ok(toResponse(gasto));
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

    @GetMapping("/rango")
    @PreAuthorize("hasAnyRole('CAJA','ADMIN')")
    public ResponseEntity<List<GastoCajaResponseDTO>> listarPorRango(
            @RequestParam LocalDate inicio,
            @RequestParam LocalDate fin,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Usuario usuario = usuarioRepository
                .findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return ResponseEntity.ok(gastoCajaService.listarPorRango(inicio, fin, usuario));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Usuario usuario = usuarioRepository
                .findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        gastoCajaService.eliminarPorId(id, usuario);
        return ResponseEntity.noContent().build();
    }

    private GastoCajaResponseDTO toResponse(GastoCaja gasto) {
        return new GastoCajaResponseDTO(
                gasto.getId(),
                gasto.getFecha(),
                gasto.getDescripcion(),
                gasto.getMonto(),
                gasto.getMontoEfectivo(),
                gasto.getMontoTransferencia()
        );
    }
}

