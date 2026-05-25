package com.pos.controller;

import com.pos.dto.fiado.AbonoFiadoCreateDTO;
import com.pos.dto.fiado.AbonoFiadoResponseDTO;
import com.pos.dto.fiado.DeudorCreateDTO;
import com.pos.dto.fiado.DeudorDetalleDTO;
import com.pos.dto.fiado.DeudorResponseDTO;
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
@Tag(name = "Fiados", description = "Gestion de deudores, ventas fiadas y abonos")
public class FiadoController {

    private final FiadoService fiadoService;
    private final UsuarioRepository usuarioRepository;

    @PreAuthorize("hasAnyRole('CAJA','DOMI','ADMIN')")
    @GetMapping("/deudores")
    public ResponseEntity<List<DeudorResponseDTO>> listarDeudores(
            @RequestParam(defaultValue = "false") boolean soloConDeuda
    ) {
        return ResponseEntity.ok(fiadoService.listarDeudores(soloConDeuda));
    }

    @PreAuthorize("hasAnyRole('CAJA','DOMI','ADMIN')")
    @GetMapping("/deudores/buscar")
    public ResponseEntity<List<DeudorResponseDTO>> buscarDeudores(
            @RequestParam(name = "q", defaultValue = "") String query
    ) {
        return ResponseEntity.ok(fiadoService.buscarClientes(query));
    }

    @PreAuthorize("hasAnyRole('CAJA','DOMI','ADMIN')")
    @GetMapping("/deudores/{id}")
    public ResponseEntity<DeudorDetalleDTO> obtenerDetalle(@PathVariable Long id) {
        return ResponseEntity.ok(fiadoService.obtenerDetalle(id));
    }

    @PreAuthorize("hasAnyRole('CAJA','DOMI','ADMIN')")
    @PostMapping("/deudores")
    public ResponseEntity<DeudorResponseDTO> crearDeudor(@Valid @RequestBody DeudorCreateDTO dto) {
        return ResponseEntity.ok(fiadoService.crearDeudor(dto));
    }

    @PreAuthorize("hasAnyRole('CAJA','DOMI','ADMIN')")
    @PostMapping("/abonos")
    public ResponseEntity<AbonoFiadoResponseDTO> registrarAbono(
            @Valid @RequestBody AbonoFiadoCreateDTO dto,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Usuario usuario = usuarioRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return ResponseEntity.ok(fiadoService.registrarAbono(dto, usuario));
    }
}
