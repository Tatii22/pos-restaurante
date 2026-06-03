package com.pos.controller;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.pos.dto.venta.VentaCreateDTO;
import com.pos.dto.venta.VentaCocinaPreviewDTO;
import com.pos.dto.venta.AnularVentaDTO;
import com.pos.dto.venta.VentaDetalleResponseDTO;
import com.pos.dto.venta.VentaDespachoDTO;
import com.pos.dto.venta.VentaResponseDTO;
import com.pos.dto.venta.VentaValorDomicilioDTO;
import com.pos.dto.venta.VentaFiadoDTO;
import com.pos.entity.Usuario;
import com.pos.service.VentaService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.extern.slf4j.Slf4j;
import com.pos.entity.Venta;
import lombok.RequiredArgsConstructor;
import com.pos.repository.UsuarioRepository;
import jakarta.validation.Valid;
import com.pos.entity.EstadoVenta;
import com.pos.entity.TipoVenta;

import java.time.LocalDate;
@Slf4j
@RestController
@RequestMapping("/ventas")
@RequiredArgsConstructor
@Tag(name = "Ventas", description = "Operaciones de ventas locales y domicilio")
public class VentaController {

    private final VentaService ventaService;
    private final UsuarioRepository usuarioRepository;

    /* ===================== REGISTRAR ===================== */

    @PreAuthorize("hasAnyRole('CAJA','DOMI','MESERO')")
    @PostMapping
    @Operation(summary = "Registrar venta")
    public ResponseEntity<VentaResponseDTO> registrar(
            @Valid @RequestBody VentaCreateDTO dto,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        log.info("Registrando venta: tipoVenta={}, formaPago={}, fiado={}, clienteId={}, formaPago={}",
                dto.tipoVenta(), dto.formaPago(), dto.fiado(), dto.clienteId(), dto.formaPago());

        Usuario usuario = usuarioRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Venta venta = ventaService.registrarVenta(dto, usuario);

        return ResponseEntity.ok(ventaService.construirRespuesta(venta));
    }

    /* ===================== CANCELAR ===================== */
    // ❌ No entra dinero, devuelve inventario
    @PreAuthorize("hasAnyRole('CAJA','DOMI')")
    @PostMapping("/{id}/cancelar")
    @Operation(summary = "Cancelar venta en proceso")
    public ResponseEntity<VentaResponseDTO> cancelar(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {

        Usuario usuario = usuarioRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Venta venta = ventaService.cancelarVenta(id, usuario);

        return ResponseEntity.ok(ventaService.construirRespuesta(venta));
    }

    /* ===================== ANULAR ===================== */
    // 💰 Ya estaba despachada → devuelve dinero + inventario
    @PreAuthorize("hasRole('CAJA')")
    @PostMapping("/{id}/anular")
    @Operation(summary = "Anular venta despachada")
    public ResponseEntity<VentaResponseDTO> anular(
            @PathVariable Long id,
            @RequestBody(required = false) AnularVentaDTO dto,
            @AuthenticationPrincipal UserDetails userDetails
    ) {

        Usuario usuario = usuarioRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Venta venta = ventaService.anularVenta(id, dto != null ? dto.motivo() : null, usuario);

        return ResponseEntity.ok(ventaService.construirRespuesta(venta));
    }

    @PreAuthorize("hasAnyRole('CAJA','DOMI','ADMIN','MESERO')")
    @GetMapping("/{id}")
    @Operation(summary = "Obtener venta por id")
    public ResponseEntity<VentaDetalleResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(ventaService.obtenerDetallePorId(id));
    }

    @PreAuthorize("hasAnyRole('CAJA','DOMI','ADMIN','MESERO')")
    @GetMapping
    @Operation(summary = "Listar ventas operativas con filtros y paginación")
    public ResponseEntity<Page<VentaResponseDTO>> listarOperativas(
            @Parameter(description = "Estado de venta") 
            @RequestParam(required = false) EstadoVenta estado,
            @Parameter(description = "Tipo de venta")
            @RequestParam(required = false) TipoVenta tipoVenta,
            @Parameter(description = "Id de turno")
            @RequestParam(required = false) Long turnoId,
            @Parameter(description = "Fecha inicio (yyyy-MM-dd)")
            @RequestParam(required = false) LocalDate fechaInicio,
            @Parameter(description = "Fecha fin (yyyy-MM-dd)")
            @RequestParam(required = false) LocalDate fechaFin,
            @Parameter(description = "Filtro por nombre de cliente")
            @RequestParam(required = false) String clienteNombre,
            @Parameter(description = "Filtro por teléfono")
            @RequestParam(required = false) String telefono,
            @Parameter(description = "Página (base 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página")
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<VentaResponseDTO> ventas = ventaService
                .listarOperativas(estado, tipoVenta, turnoId, fechaInicio, fechaFin, clienteNombre, telefono, page, size);
        return ResponseEntity.ok(ventas);
    }

    @PreAuthorize("hasAnyRole('CAJA','DOMI')")
    @PostMapping("/{id}/imprimir-factura")
    @Operation(summary = "Imprimir factura de venta en proceso (domicilio)")
    public ResponseEntity<VentaResponseDTO> imprimirFacturaEnProceso(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Usuario usuario = usuarioRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Venta venta = ventaService.imprimirFacturaEnProceso(id, usuario);
        return ResponseEntity.ok(ventaService.construirRespuesta(venta));
    }

    @PreAuthorize("hasAnyRole('CAJA','DOMI')")
    @PostMapping("/{id}/imprimir-cocina")
    @Operation(summary = "Imprimir ticket de cocina de venta en proceso (domicilio)")
    public ResponseEntity<VentaResponseDTO> imprimirCocinaEnProceso(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Usuario usuario = usuarioRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Venta venta = ventaService.imprimirTicketCocinaEnProceso(id, usuario);
        return ResponseEntity.ok(ventaService.construirRespuesta(venta));
    }

    @PreAuthorize("hasAnyRole('CAJA','MESERO')")
    @PostMapping("/imprimir-cocina-preview")
    @Operation(summary = "Imprimir ticket de cocina previo al pago (sin registrar venta)")
    public ResponseEntity<Void> imprimirCocinaPreview(
            @Valid @RequestBody VentaCocinaPreviewDTO dto,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Usuario usuario = usuarioRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        ventaService.imprimirTicketCocinaPreview(dto, usuario);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('CAJA')")
    @PostMapping("/{id}/despachar")
    @Operation(summary = "Despachar venta domicilio en proceso")
    public ResponseEntity<VentaResponseDTO> despachar(
            @PathVariable Long id,
            @Valid @RequestBody VentaDespachoDTO dto,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Usuario usuario = usuarioRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Venta venta = ventaService.despacharVenta(id, dto, usuario);
        return ResponseEntity.ok(ventaService.construirRespuesta(venta));
    }

    @PreAuthorize("hasAnyRole('CAJA','DOMI')")
    @PutMapping("/{id}/valor-domicilio")
    @Operation(summary = "Actualizar valor de domicilio en venta en proceso")
    public ResponseEntity<VentaResponseDTO> actualizarValorDomicilio(
            @PathVariable Long id,
            @Valid @RequestBody VentaValorDomicilioDTO dto,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Usuario usuario = usuarioRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Venta venta = ventaService.actualizarValorDomicilio(id, dto.valorDomicilio(), usuario);
        return ResponseEntity.ok(ventaService.construirRespuesta(venta));
    }

    @PreAuthorize("hasAnyRole('CAJA','DOMI','ADMIN')")
    @PostMapping("/{id}/reimprimir-factura")
    @Operation(summary = "Reimprimir factura de cualquier venta registrada")
    public ResponseEntity<VentaResponseDTO> reimprimirFactura(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Usuario usuario = usuarioRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Venta venta = ventaService.reimprimirFactura(id, usuario);
        return ResponseEntity.ok(ventaService.construirRespuesta(venta));
    }

    @PreAuthorize("hasAnyRole('CAJA','DOMI')")
    @PutMapping("/{id}/fiado")
    @Operation(summary = "Marcar domicilio en proceso como fiado")
    public ResponseEntity<VentaResponseDTO> marcarFiado(
            @PathVariable Long id,
            @Valid @RequestBody VentaFiadoDTO dto,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Usuario usuario = usuarioRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Venta venta = ventaService.marcarVentaDomicilioComoFiado(id, dto, usuario);
        return ResponseEntity.ok(ventaService.construirRespuesta(venta));
    }

    /* ===================== MESERO ===================== */

    @PreAuthorize("hasAnyRole('CAJA','MESERO')")
    @GetMapping("/pendientes-meseros")
    @Operation(summary = "Listar ventas de meseros pendientes de entrega a caja")
    public ResponseEntity<List<VentaResponseDTO>> listarPendientesMeseros(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Usuario usuario = usuarioRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        List<Venta> ventas = ventaService.listarPendientesMeseros(usuario);
        return ResponseEntity.ok(ventas);
    }

    @PreAuthorize("hasRole('CAJA')")
    @PostMapping("/confirmar-entrega-caja")
    @Operation(summary = "Confirmar que el mesero entrego el efectivo a caja")
    public ResponseEntity<Void> confirmarEntregaCaja(
            @RequestBody List<Long> ventaIds,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Usuario usuario = usuarioRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        ventaService.confirmarEntregaCaja(ventaIds, usuario);
        return ResponseEntity.ok().build();
    }
}
