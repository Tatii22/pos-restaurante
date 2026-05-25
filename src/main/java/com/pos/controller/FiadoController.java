package com.pos.controller;

import com.pos.dto.fiado.AbonoFiadoCreateDTO;
import com.pos.dto.fiado.AbonoFiadoResponseDTO;
import com.pos.dto.fiado.ClienteSearchDTO;
import com.pos.dto.fiado.ClienteCreateDTO;
import com.pos.dto.fiado.ClienteDetalleDTO;
import com.pos.dto.fiado.ClienteResponseDTO;
import com.pos.entity.Usuario;
import com.pos.repository.UsuarioRepository;
import com.pos.service.FiadoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/fiados")
@RequiredArgsConstructor
@Tag(name = "Fiados", description = "Gestion de clientes frecuentes, ventas fiadas y abonos")
public class FiadoController {

    private final FiadoService fiadoService;
    private final UsuarioRepository usuarioRepository;

    // ─────────────────────────────────────────────────────────────────
    // NUEVOS ENDPOINTS LIMPIOS (recomendados)
    // ─────────────────────────────────────────────────────────────────
    @PreAuthorize("hasAnyRole('CAJA','DOMI','ADMIN')")
    @GetMapping("/clientes")
    public ResponseEntity<List<ClienteResponseDTO>> listarClientes(
            @RequestParam(defaultValue = "false") boolean soloConDeuda) {
        return ResponseEntity.ok(fiadoService.listarClientes(soloConDeuda));
    }

    @PreAuthorize("hasAnyRole('CAJA','DOMI','ADMIN')")
    @GetMapping("/clientes/buscar")
    public ResponseEntity<List<ClienteResponseDTO>> buscarClientes(
            @RequestParam(name = "q", defaultValue = "") String query) {
        return ResponseEntity.ok(fiadoService.buscarClientes(query));
    }

    // ─────────────────────────────────────────────────────────────────
    // ENDPOINTS LEGACY (deprecados - mantener temporalmente para compatibilidad)
    // TODO: Eliminar después de completar la migración del frontend (2026-Q3)
    // ─────────────────────────────────────────────────────────────────
    @Deprecated(since = "2026-05", forRemoval = true)
    @PreAuthorize("hasAnyRole('CAJA','DOMI','ADMIN')")
    @GetMapping("/deudores")
    public ResponseEntity<List<ClienteResponseDTO>> listarDeudoresLegacy(
            @RequestParam(defaultValue = "false") boolean soloConDeuda) {
        return ResponseEntity.ok(fiadoService.listarClientes(soloConDeuda));
    }

    @Deprecated(since = "2026-05", forRemoval = true)
    @PreAuthorize("hasAnyRole('CAJA','DOMI','ADMIN')")
    @GetMapping("/deudores/buscar")
    public ResponseEntity<List<ClienteResponseDTO>> buscarDeudoresLegacy(
            @RequestParam(name = "q", defaultValue = "") String query) {
        return ResponseEntity.ok(fiadoService.buscarClientes(query));
    }

    /**
     * Búsqueda optimizada para autocomplete en componentes de UI.
     * Devuelve DTO ligero con ordenamiento inteligente.
     * Pensado para búsqueda rápida en VentasPage, DomiciliosPage, etc.
     */
    @PreAuthorize("hasAnyRole('CAJA','DOMI','ADMIN')")
    @GetMapping("/clientes/buscar")
    public ResponseEntity<List<ClienteSearchDTO>> buscarClientesLigero(
            @RequestParam(name = "q", defaultValue = "") String query) {
        return ResponseEntity.ok(fiadoService.buscarClientesLigero(query));
    }

    @PreAuthorize("hasAnyRole('CAJA','DOMI','ADMIN')")
    @GetMapping("/clientes/{id}")
    public ResponseEntity<ClienteDetalleDTO> obtenerCliente(@PathVariable Long id) {
        return ResponseEntity.ok(fiadoService.obtenerDetalle(id));
    }

    @PreAuthorize("hasAnyRole('CAJA','DOMI','ADMIN')")
    @PostMapping("/clientes")
    public ResponseEntity<ClienteResponseDTO> crearCliente(@Valid @RequestBody ClienteCreateDTO dto) {
        return ResponseEntity.ok(fiadoService.crearCliente(dto));
    }

    // ── Legacy deprecated ─────────────────────────────────────────────
    @Deprecated(since = "2026-05", forRemoval = true)
    @PreAuthorize("hasAnyRole('CAJA','DOMI','ADMIN')")
    @GetMapping("/deudores/{id}")
    public ResponseEntity<ClienteDetalleDTO> obtenerDetalleLegacy(@PathVariable Long id) {
        return ResponseEntity.ok(fiadoService.obtenerDetalle(id));
    }

    @Deprecated(since = "2026-05", forRemoval = true)
    @PreAuthorize("hasAnyRole('CAJA','DOMI','ADMIN')")
    @PostMapping("/deudores")
    public ResponseEntity<ClienteResponseDTO> crearDeudorLegacy(@Valid @RequestBody ClienteCreateDTO dto) {
        return ResponseEntity.ok(fiadoService.crearCliente(dto));
    }

    @PreAuthorize("hasAnyRole('CAJA','DOMI','ADMIN')")
    @PostMapping("/abonos")
    public ResponseEntity<AbonoFiadoResponseDTO> registrarAbono(
            @Valid @RequestBody AbonoFiadoCreateDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        Usuario usuario = usuarioRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return ResponseEntity.ok(fiadoService.registrarAbono(dto, usuario));
    }
}
