package com.pos.controller;
import com.pos.dto.venta.VentaCreateDTO;
import com.pos.dto.venta.VentaResponseDTO;
import com.pos.entity.Usuario;
import com.pos.service.VentaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.pos.entity.Venta;
import lombok.RequiredArgsConstructor;
import com.pos.repository.UsuarioRepository;

@RestController
@RequestMapping("/api/ventas")
@RequiredArgsConstructor
public class VentaController {

    private final VentaService ventaService;
    private final UsuarioRepository usuarioRepository;

    @PreAuthorize("hasRole('CAJA')")
    @PostMapping
    public ResponseEntity<VentaResponseDTO> registrar(
            @RequestBody VentaCreateDTO dto,
            @AuthenticationPrincipal UserDetails userDetails
    ) {

        Usuario usuario = usuarioRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Venta venta = ventaService.registrarVenta(dto, usuario);

        return ResponseEntity.ok(
                new VentaResponseDTO(
                        venta.getId(),
                        venta.getFecha(),
                        venta.getClienteNombre(),
                        venta.getTotal(),
                        venta.getFormaPago()
                )
        );
    }
}

