package com.pos.service;

import com.pos.dto.fiado.AbonoFiadoCreateDTO;
import com.pos.dto.fiado.AbonoFiadoResponseDTO;
import com.pos.dto.fiado.DeudorCreateDTO;
import com.pos.dto.fiado.DeudorDetalleDTO;
import com.pos.dto.fiado.DeudorResponseDTO;
import com.pos.entity.AbonoFiado;
import com.pos.entity.CondicionPago;
import com.pos.entity.Deudor;
import com.pos.entity.EstadoTurno;
import com.pos.entity.EstadoVenta;
import com.pos.entity.FormaPago;
import com.pos.entity.TurnoCaja;
import com.pos.entity.Usuario;
import com.pos.entity.Venta;
import com.pos.exception.BadRequestException;
import com.pos.repository.AbonoFiadoRepository;
import com.pos.repository.DeudorRepository;
import com.pos.repository.TurnoCajaRepository;
import com.pos.repository.VentaRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FiadoService {

    private final DeudorRepository deudorRepository;
    private final VentaRepository ventaRepository;
    private final AbonoFiadoRepository abonoFiadoRepository;
    private final TurnoCajaRepository turnoCajaRepository;
    private final VentaPagoDetalleService ventaPagoDetalleService;
    private final MovimientoFinancieroService movimientoFinancieroService;
    private final AuditService auditService;

    // ─────────────────────────────────────────────────────────────────
    // CONSULTAS
    // ─────────────────────────────────────────────────────────────────

    /**
     * Lista todos los clientes con su deuda total y número de ventas pendientes.
     * Una sola query agregada elimina el problema N+1 original.
     */
    public List<DeudorResponseDTO> listarDeudores(boolean soloConDeuda) {
        Map<Long, BigDecimal> deudaMap = new HashMap<>();
        Map<Long, Long> conteoMap = new HashMap<>();
        for (Object[] row : ventaRepository.sumarDeudaAgrupadaPorDeudor()) {
            Long   deudorId = (Long)       row[0];
            BigDecimal deuda = (BigDecimal) row[1];
            Long   conteo   = (Long)       row[2];
            deudaMap.put(deudorId, deuda);
            conteoMap.put(deudorId, conteo);
        }

        return deudorRepository.findAllByOrderByNombreAsc().stream()
                .map(d -> toResumenDTO(d, deudaMap, conteoMap))
                .filter(dto -> !soloConDeuda || dto.deudaTotal().compareTo(BigDecimal.ZERO) > 0)
                .toList();
    }

    /**
     * Búsqueda rápida de clientes por nombre o teléfono (parcial, case-insensitive).
     * Devuelve hasta 20 resultados. Incluye deuda en tiempo real.
     * Usado por el componente ClienteSearch en toda la app.
     */
    public List<DeudorResponseDTO> buscarClientes(String q) {
        if (q == null || q.isBlank()) return List.of();
        String query = q.trim().replaceAll("[^\\w\\s]", "");
        if (query.isBlank()) return List.of();

        // Carga deudas solo para los candidatos, no para toda la tabla
        Map<Long, BigDecimal> deudaMap = new HashMap<>();
        Map<Long, Long> conteoMap = new HashMap<>();
        for (Object[] row : ventaRepository.sumarDeudaAgrupadaPorDeudor()) {
            deudaMap.put((Long) row[0], (BigDecimal) row[1]);
            conteoMap.put((Long) row[0], (Long) row[2]);
        }

        return deudorRepository.buscarPorTexto(query).stream()
                .map(d -> toResumenDTO(d, deudaMap, conteoMap))
                .toList();
    }

    @Transactional
    public DeudorDetalleDTO obtenerDetalle(Long deudorId) {
        Deudor deudor = obtenerDeudor(deudorId);
        BigDecimal deudaTotal = calcularDeudaTotal(deudor);

        List<AbonoFiadoResponseDTO> abonos = abonoFiadoRepository
                .findByDeudorOrderByFechaDesc(deudor)
                .stream()
                .map(a -> toAbonoDto(a, BigDecimal.ZERO))
                .toList();

        List<com.pos.dto.venta.VentaResponseDTO> ventasPendientes =
                ventaRepository.findByDeudorAndEstadoAndSaldoPendienteGreaterThanOrderByFechaAsc(
                        deudor, EstadoVenta.DESPACHADA, BigDecimal.ZERO)
                        .stream()
                        .map(v -> {
                            var pago = ventaPagoDetalleService.obtener(v.getId());
                            return new com.pos.dto.venta.VentaResponseDTO(
                                    v.getId(), v.getFecha(), v.getTipoVenta(), v.getEstado(),
                                    v.getClienteNombre(), v.getTelefono(), v.getDireccion(),
                                    v.getValorDomicilio(), v.getParaLlevar(),
                                    v.getDescuentoPorcentaje(), v.getDescuentoValor(),
                                    v.getTotal(), v.getFormaPago(),
                                    pago != null ? pago.pagoEfectivo()      : BigDecimal.ZERO,
                                    pago != null ? pago.pagoTransferencia() : BigDecimal.ZERO,
                                    v.getCondicionPago(), v.getSaldoPendiente(),
                                    v.getDeudor() != null ? v.getDeudor().getId() : null
                            );
                        })
                        .toList();

        return new DeudorDetalleDTO(
                deudor.getId(), deudor.getNombre(), deudor.getTelefono(),
                deudor.getDireccionPredeterminada(), deudor.getNotas(),
                deudaTotal.compareTo(BigDecimal.ZERO) > 0,
                deudaTotal, ventasPendientes, abonos
        );
    }

    // ─────────────────────────────────────────────────────────────────
    // MUTACIONES
    // ─────────────────────────────────────────────────────────────────

    @Transactional
    public DeudorResponseDTO crearDeudor(DeudorCreateDTO dto) {
        String telefono = normalizarTelefono(dto.telefono());

        // Si ya existe por teléfono, actualizarlo en vez de duplicar
        return deudorRepository.findByTelefono(telefono)
                .map(existente -> {
                    existente.setNombre(normalizarNombre(dto.nombre()));
                    existente.setActivo(true);
                    if (dto.direccionPredeterminada() != null && !dto.direccionPredeterminada().isBlank()) {
                        existente.setDireccionPredeterminada(dto.direccionPredeterminada().trim());
                    }
                    if (dto.notas() != null && !dto.notas().isBlank()) {
                        existente.setNotas(dto.notas().trim());
                    }
                    existente.setFechaActualizacion(LocalDateTime.now());
                    Deudor guardado = deudorRepository.save(existente);
                    return toResumenDTO(guardado, BigDecimal.ZERO, 0L);
                })
                .orElseGet(() -> {
                    LocalDateTime ahora = LocalDateTime.now();
                    Deudor nuevo = deudorRepository.save(
                            Deudor.builder()
                                    .nombre(normalizarNombre(dto.nombre()))
                                    .telefono(telefono)
                                    .direccionPredeterminada(
                                            dto.direccionPredeterminada() != null && !dto.direccionPredeterminada().isBlank()
                                                    ? dto.direccionPredeterminada().trim() : null)
                                    .notas(dto.notas() != null && !dto.notas().isBlank() ? dto.notas().trim() : null)
                                    .activo(true)
                                    .fechaCreacion(ahora)
                                    .fechaActualizacion(ahora)
                                    .build()
                    );
                    return toResumenDTO(nuevo, BigDecimal.ZERO, 0L);
                });
    }

    @Transactional
    public AbonoFiadoResponseDTO registrarAbono(AbonoFiadoCreateDTO dto, Usuario usuario) {
        Deudor deudor = obtenerDeudor(dto.deudorId());
        BigDecimal montoEfectivo      = nonNegative(dto.montoEfectivo());
        BigDecimal montoTransferencia = nonNegative(dto.montoTransferencia());

        if (montoEfectivo.add(montoTransferencia).compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Debes registrar un abono mayor a 0");
        }

        BigDecimal deudaActual = calcularDeudaTotal(deudor);
        if (deudaActual.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("El cliente no tiene saldo pendiente");
        }

        PagoAbono pago = calcularPagoAbono(deudaActual, montoEfectivo, montoTransferencia);

        TurnoCaja turno = turnoCajaRepository
                .findByEstadoInForUpdate(List.of(EstadoTurno.ABIERTO, EstadoTurno.SIMULADO))
                .orElseThrow(() -> new BadRequestException("No hay turno activo para registrar el abono"));

        // FIFO: ventas más antiguas primero
        BigDecimal restante = pago.totalAplicado();
        List<Venta> ventasPendientes = ventaRepository.findPendientesByDeudorForUpdate(
                deudor, EstadoVenta.DESPACHADA, BigDecimal.ZERO);

        for (Venta venta : ventasPendientes) {
            if (restante.compareTo(BigDecimal.ZERO) <= 0) break;
            BigDecimal aplicado   = venta.getSaldoPendiente().min(restante);
            BigDecimal nuevoSaldo = venta.getSaldoPendiente().subtract(aplicado);
            if (nuevoSaldo.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalStateException("Inconsistencia: saldo negativo en venta " + venta.getId());
            }
            venta.setSaldoPendiente(nuevoSaldo);
            ventaRepository.save(venta);
            restante = restante.subtract(aplicado);
        }

        BigDecimal aplicadoTotal = pago.totalAplicado().subtract(restante);
        if (aplicadoTotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("No fue posible aplicar el abono");
        }

        AbonoFiado abono = abonoFiadoRepository.save(
                AbonoFiado.builder()
                        .fecha(LocalDateTime.now())
                        .monto(aplicadoTotal)
                        .montoEfectivo(pago.efectivoAplicado())
                        .montoTransferencia(pago.transferenciaAplicada())
                        .formaPago(resolverFormaPago(pago.efectivoAplicado(), pago.transferenciaAplicada()))
                        .observacion(normalizarObservacion(dto.observacion()))
                        .deudor(deudor)
                        .usuario(usuario)
                        .turno(turno)
                        .build()
        );

        turno.setTotalVentas(turno.getTotalVentas().add(aplicadoTotal));
        turnoCajaRepository.save(turno);
        movimientoFinancieroService.registrarAbono(abono);
        auditService.record(
                "ABONO_FIADO_REGISTRADO", "AbonoFiado", abono.getId(),
                usuario, turno, abono.getObservacion(),
                auditService.change("deudorId",              null, deudor.getId()),
                auditService.change("monto",                 null, aplicadoTotal),
                auditService.change("efectivoAplicado",      null, pago.efectivoAplicado()),
                auditService.change("transferenciaAplicada", null, pago.transferenciaAplicada()),
                auditService.change("cambioEfectivo",        null, pago.cambioEfectivo())
        );

        return toAbonoDto(abono, pago.cambioEfectivo());
    }

    // ─────────────────────────────────────────────────────────────────
    // HELPERS INTERNOS LLAMADOS DESDE VentaService
    // ─────────────────────────────────────────────────────────────────

    /**
     * Resuelve el cliente para una venta fiada.
     *
     * Prioridad:
     *   1. Si viene deudorId → usar ese cliente directamente (sin duplicar).
     *   2. Si viene deudorTelefono → buscar por teléfono. Si existe, actualizar nombre;
     *      si no existe, crear nuevo.
     *
     * Cuando la venta tiene dirección (domicilio), se actualiza también
     * la dirección predeterminada del cliente.
     */
    @Transactional
    public Deudor resolverDeudorVenta(Boolean fiado, Long deudorId,
                                      String deudorNombre, String deudorTelefono) {
        return resolverDeudorVenta(fiado, deudorId, deudorNombre, deudorTelefono, null);
    }

    @Transactional
    public Deudor resolverDeudorVenta(Boolean fiado, Long deudorId,
                                      String deudorNombre, String deudorTelefono,
                                      String direccion) {
        if (!Boolean.TRUE.equals(fiado)) return null;

        // Camino 1: viene ID explícito → reutilizar sin tocar nombre (evita sobreescrituras)
        if (deudorId != null) {
            Deudor existente = obtenerDeudor(deudorId);
            actualizarDireccionSiAplica(existente, direccion);
            return existente;
        }

        // Camino 2: viene teléfono → buscar o crear
        String telefono = normalizarTelefono(deudorTelefono);
        String nombre   = normalizarNombre(deudorNombre);

        return deudorRepository.findByTelefono(telefono)
                .map(existente -> {
                    // Actualizar nombre solo si el nuevo no está en blanco
                    if (nombre != null && !nombre.isBlank()) {
                        existente.setNombre(nombre);
                    }
                    existente.setActivo(true);
                    actualizarDireccionSiAplica(existente, direccion);
                    existente.setFechaActualizacion(LocalDateTime.now());
                    return deudorRepository.save(existente);
                })
                .orElseGet(() -> {
                    LocalDateTime ahora = LocalDateTime.now();
                    String dir = (direccion != null && !direccion.isBlank()) ? direccion.trim() : null;
                    return deudorRepository.save(
                            Deudor.builder()
                                    .nombre(nombre)
                                    .telefono(telefono)
                                    .direccionPredeterminada(dir)
                                    .activo(true)
                                    .fechaCreacion(ahora)
                                    .fechaActualizacion(ahora)
                                    .build()
                    );
                });
    }

    public BigDecimal calcularDeudaTotal(Deudor deudor) {
        return ventaRepository.sumarSaldoPendientePorDeudor(
                deudor, CondicionPago.FIADO, EstadoVenta.DESPACHADA);
    }

    public Deudor obtenerDeudor(Long deudorId) {
        return deudorRepository.findById(deudorId)
                .orElseThrow(() -> new BadRequestException("Cliente no encontrado"));
    }

    // ─────────────────────────────────────────────────────────────────
    // HELPERS PRIVADOS
    // ─────────────────────────────────────────────────────────────────

    private void actualizarDireccionSiAplica(Deudor deudor, String direccion) {
        if (direccion != null && !direccion.isBlank()) {
            String dir = direccion.trim();
            if (!dir.equals(deudor.getDireccionPredeterminada())) {
                deudor.setDireccionPredeterminada(dir);
            }
        }
    }

    private DeudorResponseDTO toResumenDTO(Deudor d,
                                           Map<Long, BigDecimal> deudaMap,
                                           Map<Long, Long> conteoMap) {
        return toResumenDTO(d,
                deudaMap.getOrDefault(d.getId(), BigDecimal.ZERO),
                conteoMap.getOrDefault(d.getId(), 0L));
    }

    private DeudorResponseDTO toResumenDTO(Deudor d, BigDecimal deuda, long conteo) {
        boolean esDeudor = deuda.compareTo(BigDecimal.ZERO) > 0;
        return new DeudorResponseDTO(
                d.getId(), d.getNombre(), d.getTelefono(),
                d.getDireccionPredeterminada(), d.getNotas(),
                d.getActivo(), esDeudor, deuda, conteo
        );
    }

    private AbonoFiadoResponseDTO toAbonoDto(AbonoFiado abono, BigDecimal cambio) {
        return new AbonoFiadoResponseDTO(
                abono.getId(), abono.getFecha(),
                abono.getMonto(), abono.getMontoEfectivo(), abono.getMontoTransferencia(),
                abono.getFormaPago(), abono.getObservacion(),
                abono.getUsuario() != null ? abono.getUsuario().getUsername() : "-",
                abono.getTurno()   != null ? abono.getTurno().getId()         : null,
                cambio != null ? cambio : BigDecimal.ZERO
        );
    }

    // ─────────────────────────────────────────────────────────────────
    // LÓGICA FINANCIERA PRIVADA
    // ─────────────────────────────────────────────────────────────────

    private PagoAbono calcularPagoAbono(BigDecimal deudaActual,
                                        BigDecimal montoEfectivo,
                                        BigDecimal montoTransferencia) {
        BigDecimal deuda              = deudaActual == null ? BigDecimal.ZERO : deudaActual;
        BigDecimal efectivoRecibido   = nonNegative(montoEfectivo);
        BigDecimal transferenciaRecibida = nonNegative(montoTransferencia);

        if (transferenciaRecibida.compareTo(deuda) > 0) {
            throw new BadRequestException("La transferencia no puede superar la deuda pendiente");
        }

        BigDecimal faltante        = deuda.subtract(transferenciaRecibida);
        BigDecimal efectivoAplicado = efectivoRecibido.min(faltante);
        BigDecimal totalAplicado    = efectivoAplicado.add(transferenciaRecibida);

        if (totalAplicado.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("No fue posible aplicar el abono");
        }

        BigDecimal cambio = efectivoRecibido.subtract(efectivoAplicado);
        if (cambio.compareTo(deuda.add(BigDecimal.valueOf(100_000))) > 0) {
            throw new BadRequestException("El efectivo recibido supera un cambio razonable para el abono");
        }

        return new PagoAbono(efectivoAplicado, transferenciaRecibida, cambio);
    }

    private record PagoAbono(
            BigDecimal efectivoAplicado,
            BigDecimal transferenciaAplicada,
            BigDecimal cambioEfectivo
    ) {
        BigDecimal totalAplicado() {
            return efectivoAplicado.add(transferenciaAplicada);
        }
    }

    private FormaPago resolverFormaPago(BigDecimal montoEfectivo, BigDecimal montoTransferencia) {
        if (montoTransferencia.compareTo(BigDecimal.ZERO) > 0
                && montoEfectivo.compareTo(BigDecimal.ZERO) == 0) {
            return FormaPago.TRANSFERENCIA;
        }
        return FormaPago.EFECTIVO;
    }

    // ─────────────────────────────────────────────────────────────────
    // NORMALIZACIÓN
    // ─────────────────────────────────────────────────────────────────

    private BigDecimal nonNegative(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) return BigDecimal.ZERO;
        return value;
    }

    private String normalizarNombre(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("El nombre del cliente es obligatorio");
        }
        String clean = value.trim().replaceAll("\\s+", " ");
        return clean.length() > 100 ? clean.substring(0, 100) : clean;
    }

    private String normalizarTelefono(String value) {
        String clean = value == null ? "" : value.replaceAll("\\D", "");
        if (clean.length() < 7 || clean.length() > 15) {
            throw new BadRequestException("El telefono debe tener entre 7 y 15 digitos");
        }
        return clean;
    }

    private String normalizarObservacion(String value) {
        if (value == null || value.isBlank()) return null;
        String clean = value.trim().replace("\r", " ").replace("\n", " ");
        return clean.length() > 255 ? clean.substring(0, 255) : clean;
    }
}
