package com.pos.controller;
import com.pos.dto.venta.VentaCreateDTO;
import com.pos.dto.venta.VentaResponseDTO;
import com.pos.entity.Usuario;
import com.pos.service.VentaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.pos.entity.Venta;
import lombok.RequiredArgsConstructor;
import com.pos.repository.UsuarioRepository;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/ventas")
@RequiredArgsConstructor
public class VentaController {

    private final VentaService ventaService;
    private final UsuarioRepository usuarioRepository;

    /* ===================== REGISTRAR ===================== */

    @PreAuthorize("hasAnyRole('CAJA','DOMI')")
    @PostMapping
    public ResponseEntity<VentaResponseDTO> registrar(
            @Valid @RequestBody VentaCreateDTO dto,
            @AuthenticationPrincipal UserDetails userDetails
    ) {

        Usuario usuario = usuarioRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Venta venta = ventaService.registrarVenta(dto, usuario);

        return ResponseEntity.ok(toDTO(venta));
    }

    /* ===================== CANCELAR ===================== */
    // ❌ No entra dinero, devuelve inventario
    @PreAuthorize("hasAnyRole('CAJA','DOMI')")
    @PostMapping("/{id}/cancelar")
    public ResponseEntity<VentaResponseDTO> cancelar(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {

        Usuario usuario = usuarioRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Venta venta = ventaService.cancelarVenta(id, usuario);

        return ResponseEntity.ok(toDTO(venta));
    }

    /* ===================== ANULAR ===================== */
    // 💰 Ya estaba despachada → devuelve dinero + inventario
    @PreAuthorize("hasRole('CAJA')")
    @PostMapping("/{id}/anular")
    public ResponseEntity<VentaResponseDTO> anular(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {

        Usuario usuario = usuarioRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Venta venta = ventaService.anularVenta(id, usuario);

        return ResponseEntity.ok(toDTO(venta));
    }

    /* ===================== MAPPER ===================== */

    private VentaResponseDTO toDTO(Venta venta) {
        return new VentaResponseDTO(
                venta.getId(),
                venta.getFecha(),
                venta.getTipoVenta(),
                venta.getEstado(),
                venta.getClienteNombre(),
                venta.getTotal(),
                venta.getFormaPago()
        );
    }
}
