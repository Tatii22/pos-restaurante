package com.pos.service;

import com.pos.dto.fiado.AbonoFiadoCreateDTO;
import com.pos.dto.fiado.AbonoFiadoResponseDTO;
import com.pos.dto.fiado.ClienteCreateDTO;
import com.pos.dto.fiado.ClienteDetalleDTO;
import com.pos.dto.fiado.ClienteResponseDTO;
import com.pos.dto.fiado.ClienteSearchDTO;
import com.pos.entity.AbonoFiado;
import com.pos.entity.Cliente;
import com.pos.entity.CondicionPago;
import com.pos.entity.EstadoTurno;
import com.pos.entity.EstadoVenta;
import com.pos.entity.FormaPago;
import com.pos.entity.TurnoCaja;
import com.pos.entity.Usuario;
import com.pos.entity.Venta;
import com.pos.exception.BadRequestException;
import com.pos.repository.AbonoFiadoRepository;
import com.pos.repository.ClienteRepository;
import com.pos.repository.TurnoCajaRepository;
import com.pos.repository.VentaRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FiadoService {

    private final ClienteRepository clienteRepository;
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
     * Una sola query agregada elimina el problema N+1.
     */
    public List<ClienteResponseDTO> listarClientes(boolean soloConDeuda) {
        Map<Long, BigDecimal> deudaMap = new HashMap<>();
        Map<Long, Long> conteoMap = new HashMap<>();
        for (Object[] row : ventaRepository.sumarDeudaAgrupadaPorCliente()) {
            Long clienteId = (Long) row[0];
            BigDecimal deuda = (BigDecimal) row[1];
            Long conteo = (Long) row[2];
            deudaMap.put(clienteId, deuda);
            conteoMap.put(clienteId, conteo);
        }

        return clienteRepository.findAllByOrderByNombreAsc().stream()
                .map(c -> toResumenDTO(c, deudaMap, conteoMap))
                .filter(dto -> !soloConDeuda || dto.deudaTotal().compareTo(BigDecimal.ZERO) > 0)
                .toList();
    }

    /**
     * Búsqueda de clientes por nombre o teléfono (parcial, case-insensitive).
     * Devuelve hasta 20 resultados con deuda en tiempo real.
     * Usado por componentes de listado completo.
     */
    public List<ClienteResponseDTO> buscarClientes(String q) {
        if (q == null || q.isBlank())
            return List.of();
        String query = q.trim().replaceAll("[^\\w\\s]", "");
        if (query.isBlank())
            return List.of();

        Map<Long, BigDecimal> deudaMap = new HashMap<>();
        Map<Long, Long> conteoMap = new HashMap<>();
        for (Object[] row : ventaRepository.sumarDeudaAgrupadaPorCliente()) {
            deudaMap.put((Long) row[0], (BigDecimal) row[1]);
            conteoMap.put((Long) row[0], (Long) row[2]);
        }

        return clienteRepository.buscarPorTexto(query).stream()
                .map(c -> toResumenDTO(c, deudaMap, conteoMap))
                .toList();
    }

    /**
     * Búsqueda ligera para autocomplete en UI.
     * Devuelve DTO mínimo con deuda actual.
     */
    public List<ClienteSearchDTO> buscarClientesLigero(String q) {
        if (q == null || q.isBlank())
            return List.of();
        String query = q.trim().replaceAll("[^\\w\\s]", "");
        if (query.isBlank())
            return List.of();

        Map<Long, BigDecimal> deudaMap = new HashMap<>();
        for (Object[] row : ventaRepository.sumarDeudaAgrupadaPorCliente()) {
            deudaMap.put((Long) row[0], (BigDecimal) row[1]);
        }

        return clienteRepository.buscarPorTexto(query).stream()
                .map(c -> {
                    BigDecimal deuda = deudaMap.getOrDefault(c.getId(), BigDecimal.ZERO);
                    return new ClienteSearchDTO(
                            c.getId(), c.getNombre(), c.getTelefono(),
                            c.getDireccionPredeterminada(),
                            deuda,
                            deuda.compareTo(BigDecimal.ZERO) > 0);
                })
                .toList();
    }

    @Transactional
    public ClienteDetalleDTO obtenerDetalle(Long clienteId) {
        Cliente cliente = obtenerCliente(clienteId);
        BigDecimal deudaTotal = calcularDeudaTotal(cliente);

        List<AbonoFiadoResponseDTO> abonos = abonoFiadoRepository
                .findByClienteOrderByFechaDesc(cliente)
                .stream()
                .map(a -> toAbonoDto(a, BigDecimal.ZERO))
                .toList();

        List<com.pos.dto.venta.VentaResponseDTO> ventasPendientes = ventaRepository
                .findByClienteAndEstadoAndSaldoPendienteGreaterThanOrderByFechaAsc(
                        cliente, EstadoVenta.DESPACHADA, BigDecimal.ZERO)
                .stream()
                .map(v -> {
                    var pago = ventaPagoDetalleService.obtener(v.getId());
                    return new com.pos.dto.venta.VentaResponseDTO(
                            v.getId(), v.getFecha(), v.getTipoVenta(), v.getEstado(),
                            v.getClienteNombre(), v.getTelefono(), v.getDireccion(),
                            v.getValorDomicilio(), v.getParaLlevar(),
                            v.getDescuentoPorcentaje(), v.getDescuentoValor(),
                            v.getTotal(), v.getFormaPago(),
                            pago != null ? pago.pagoEfectivo() : BigDecimal.ZERO,
                            pago != null ? pago.pagoTransferencia() : BigDecimal.ZERO,
                            v.getCondicionPago(), v.getSaldoPendiente(),
                            v.getCliente() != null ? v.getCliente().getId() : null);
                })
                .toList();

        return new ClienteDetalleDTO(
                cliente.getId(), cliente.getNombre(), cliente.getTelefono(),
                cliente.getDireccionPredeterminada(), cliente.getNotas(),
                deudaTotal.compareTo(BigDecimal.ZERO) > 0,
                deudaTotal, ventasPendientes, abonos);
    }

    // ─────────────────────────────────────────────────────────────────
    // MUTACIONES
    // ─────────────────────────────────────────────────────────────────

    @Transactional
    public ClienteResponseDTO crearCliente(ClienteCreateDTO dto) {
        String telefono = normalizarTelefono(dto.telefono());

        return clienteRepository.findByTelefono(telefono)
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
                    Cliente guardado = clienteRepository.save(existente);
                    return toResumenDTO(guardado, BigDecimal.ZERO, 0L);
                })
                .orElseGet(() -> {
                    LocalDateTime ahora = LocalDateTime.now();
                    Cliente nuevo = clienteRepository.save(
                            Cliente.builder()
                                    .nombre(normalizarNombre(dto.nombre()))
                                    .telefono(telefono)
                                    .direccionPredeterminada(
                                            dto.direccionPredeterminada() != null
                                                    && !dto.direccionPredeterminada().isBlank()
                                                            ? dto.direccionPredeterminada().trim()
                                                            : null)
                                    .notas(dto.notas() != null && !dto.notas().isBlank() ? dto.notas().trim() : null)
                                    .activo(true)
                                    .fechaCreacion(ahora)
                                    .fechaActualizacion(ahora)
                                    .build());
                    return toResumenDTO(nuevo, BigDecimal.ZERO, 0L);
                });
    }

    @Transactional
    public AbonoFiadoResponseDTO registrarAbono(AbonoFiadoCreateDTO dto, Usuario usuario) {
        Cliente cliente = obtenerCliente(dto.clienteId());
        BigDecimal montoEfectivo = nonNegative(dto.montoEfectivo());
        BigDecimal montoTransferencia = nonNegative(dto.montoTransferencia());

        if (montoEfectivo.add(montoTransferencia).compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Debes registrar un abono mayor a 0");
        }

        BigDecimal deudaActual = calcularDeudaTotal(cliente);
        if (deudaActual.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("El cliente no tiene saldo pendiente");
        }

        log.info("[ABONO] clienteId={} deudaActual={} montoEfectivo={} montoTransferencia={}",
                cliente.getId(), deudaActual, montoEfectivo, montoTransferencia);

        PagoAbono pago = calcularPagoAbono(deudaActual, montoEfectivo, montoTransferencia);

        Optional<TurnoCaja> optTurno = turnoCajaRepository
                .findByEstadoInForUpdate(List.of(EstadoTurno.ABIERTO));
        if (optTurno.isEmpty() && !"ADMIN".equals(usuario.getRol().getNombre())) {
            throw new BadRequestException("No hay turno activo para registrar el abono");
        }
        TurnoCaja turno = optTurno.orElse(null);

        // FIFO: ventas más antiguas primero
        BigDecimal restante = pago.totalAplicado();
        List<Venta> ventasPendientes = ventaRepository.findPendientesByClienteForUpdate(
                cliente, EstadoVenta.DESPACHADA, BigDecimal.ZERO);

        for (Venta venta : ventasPendientes) {
            if (restante.compareTo(BigDecimal.ZERO) <= 0)
                break;
            BigDecimal aplicado = venta.getSaldoPendiente().min(restante);
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

        log.info("[ABONO] Aplicado: total={} restante={} aplicadoTotal={} saldoPendienteQueda={}",
                pago.totalAplicado(), restante, aplicadoTotal,
                ventasPendientes.stream().map(Venta::getSaldoPendiente).reduce(BigDecimal.ZERO, BigDecimal::add));

        AbonoFiado abono = abonoFiadoRepository.save(
                AbonoFiado.builder()
                        .fecha(LocalDateTime.now())
                        .monto(aplicadoTotal)
                        .montoEfectivo(pago.efectivoAplicado())
                        .montoTransferencia(pago.transferenciaAplicada())
                        .formaPago(resolverFormaPago(pago.efectivoAplicado(), pago.transferenciaAplicada()))
                        .observacion(normalizarObservacion(dto.observacion()))
                        .cliente(cliente)
                        .usuario(usuario)
                        .turno(turno)
                        .build());

        if (turno != null) {
            turno.setTotalVentas(turno.getTotalVentas().add(aplicadoTotal));
            turnoCajaRepository.save(turno);
        }
        movimientoFinancieroService.registrarAbono(abono);
        auditService.record(
                "ABONO_FIADO_REGISTRADO", "AbonoFiado", abono.getId(),
                usuario, turno, abono.getObservacion(),
                auditService.change("clienteId", null, cliente.getId()),
                auditService.change("monto", null, aplicadoTotal),
                auditService.change("efectivoAplicado", null, pago.efectivoAplicado()),
                auditService.change("transferenciaAplicada", null, pago.transferenciaAplicada()),
                auditService.change("cambioEfectivo", null, pago.cambioEfectivo()));

        return toAbonoDto(abono, pago.cambioEfectivo());
    }

    // ─────────────────────────────────────────────────────────────────
    // HELPERS LLAMADOS DESDE VentaService
    // ─────────────────────────────────────────────────────────────────

    /**
     * Resuelve o crea el cliente para una venta LOCAL fiada.
     *
     * Prioridad:
     * 1. Si viene clienteId → usar ese cliente directamente.
     * 2. Si viene teléfono → buscar por teléfono; si existe, actualizar nombre;
     * si no existe, crear nuevo.
     */
    @Transactional
    public Cliente resolverClienteVenta(Boolean fiado, Long clienteId,
            String clienteNombre, String clienteTelefono) {
        if (!Boolean.TRUE.equals(fiado))
            return null;

        if (clienteId != null) {
            Cliente c = obtenerCliente(clienteId);
            log.info("[RESOLVER_CLIENTE] Por ID: clienteId={} nombre={}", c.getId(), c.getNombre());
            return c;
        }

        String telefono = normalizarTelefono(clienteTelefono);
        String nombre = normalizarNombre(clienteNombre);

        return clienteRepository.findByTelefono(telefono)
                .map(existente -> {
                    if (nombre != null && !nombre.isBlank()) {
                        existente.setNombre(nombre);
                    }
                    existente.setActivo(true);
                    existente.setFechaActualizacion(LocalDateTime.now());
                    return clienteRepository.save(existente);
                })
                .orElseGet(() -> {
                    LocalDateTime ahora = LocalDateTime.now();
                    return clienteRepository.save(
                            Cliente.builder()
                                    .nombre(nombre)
                                    .telefono(telefono)
                                    .activo(true)
                                    .fechaCreacion(ahora)
                                    .fechaActualizacion(ahora)
                                    .build());
                });
    }

    /**
     * Resuelve o crea el cliente para cualquier domicilio (contado, transferencia o
     * fiado).
     * Actualiza la dirección predeterminada con la última usada.
     * Retorna null si no viene ni teléfono ni nombre (domicilio anónimo).
     */
    @Transactional
    public Cliente resolverOActualizarCliente(String nombre, String telefono, String direccion) {
        // Sin teléfono: domicilio anónimo permitido para contado
        if (telefono == null || telefono.isBlank())
            return null;

        String telNorm = normalizarTelefono(telefono);
        String nomNorm = (nombre != null && !nombre.isBlank()) ? normalizarNombre(nombre) : null;
        String dirNorm = (direccion != null && !direccion.isBlank()) ? direccion.trim() : null;

        return clienteRepository.findByTelefono(telNorm)
                .map(existente -> {
                    if (nomNorm != null)
                        existente.setNombre(nomNorm);
                    if (dirNorm != null)
                        existente.setDireccionPredeterminada(dirNorm);
                    existente.setActivo(true);
                    existente.setFechaActualizacion(LocalDateTime.now());
                    return clienteRepository.save(existente);
                })
                .orElseGet(() -> {
                    LocalDateTime ahora = LocalDateTime.now();
                    return clienteRepository.save(
                            Cliente.builder()
                                    .nombre(nomNorm != null ? nomNorm : "Cliente")
                                    .telefono(telNorm)
                                    .direccionPredeterminada(dirNorm)
                                    .activo(true)
                                    .fechaCreacion(ahora)
                                    .fechaActualizacion(ahora)
                                    .build());
                });
    }

    public BigDecimal calcularDeudaTotal(Cliente cliente) {
        return ventaRepository.sumarSaldoPendientePorCliente(
                cliente, CondicionPago.FIADO, EstadoVenta.DESPACHADA);
    }

    public Cliente obtenerCliente(Long clienteId) {
        return clienteRepository.findById(clienteId)
                .orElseThrow(() -> new BadRequestException("Cliente no encontrado"));
    }

    // ─────────────────────────────────────────────────────────────────
    // HELPERS PRIVADOS
    // ─────────────────────────────────────────────────────────────────

    private ClienteResponseDTO toResumenDTO(Cliente c,
            Map<Long, BigDecimal> deudaMap,
            Map<Long, Long> conteoMap) {
        return toResumenDTO(c,
                deudaMap.getOrDefault(c.getId(), BigDecimal.ZERO),
                conteoMap.getOrDefault(c.getId(), 0L));
    }

    private ClienteResponseDTO toResumenDTO(Cliente c, BigDecimal deuda, long conteo) {
        boolean tieneDeuda = deuda.compareTo(BigDecimal.ZERO) > 0;
        return new ClienteResponseDTO(
                c.getId(), c.getNombre(), c.getTelefono(),
                c.getDireccionPredeterminada(), c.getNotas(),
                c.getActivo(), tieneDeuda, deuda, conteo);
    }

    private AbonoFiadoResponseDTO toAbonoDto(AbonoFiado abono, BigDecimal cambio) {
        return new AbonoFiadoResponseDTO(
                abono.getId(), abono.getFecha(),
                abono.getMonto(), abono.getMontoEfectivo(), abono.getMontoTransferencia(),
                abono.getFormaPago(), abono.getObservacion(),
                abono.getUsuario() != null ? abono.getUsuario().getUsername() : "-",
                abono.getTurno() != null ? abono.getTurno().getId() : null,
                cambio != null ? cambio : BigDecimal.ZERO);
    }

    // ─────────────────────────────────────────────────────────────────
    // LÓGICA FINANCIERA PRIVADA
    // ─────────────────────────────────────────────────────────────────

    private PagoAbono calcularPagoAbono(BigDecimal deudaActual,
            BigDecimal montoEfectivo,
            BigDecimal montoTransferencia) {
        BigDecimal deuda = deudaActual == null ? BigDecimal.ZERO : deudaActual;
        BigDecimal efectivoRecibido = nonNegative(montoEfectivo);
        BigDecimal transferenciaRecibida = nonNegative(montoTransferencia);

        if (transferenciaRecibida.compareTo(deuda) > 0) {
            throw new BadRequestException("La transferencia no puede superar la deuda pendiente");
        }

        BigDecimal faltante = deuda.subtract(transferenciaRecibida);
        BigDecimal efectivoAplicado = efectivoRecibido.min(faltante);
        BigDecimal totalAplicado = efectivoAplicado.add(transferenciaRecibida);

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
            BigDecimal cambioEfectivo) {
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
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0)
            return BigDecimal.ZERO;
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
        if (value == null || value.isBlank())
            return null;
        String clean = value.trim().replace("\r", " ").replace("\n", " ");
        return clean.length() > 255 ? clean.substring(0, 255) : clean;
    }
}
