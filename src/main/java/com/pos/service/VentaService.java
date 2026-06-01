package com.pos.service;

import com.pos.dto.venta.VentaCocinaPreviewDTO;
import com.pos.dto.venta.VentaCreateDTO;
import com.pos.dto.venta.VentaDetalleResponseDTO;
import com.pos.dto.venta.VentaDespachoDTO;
import com.pos.dto.venta.VentaFiadoDTO;
import com.pos.dto.venta.VentaDetalleCreateDTO;
import com.pos.dto.venta.VentaItemResponseDTO;
import com.pos.dto.venta.VentaPagoDetalleDTO;
import com.pos.entity.*;
import com.pos.exception.BadRequestException;
import com.pos.repository.AbonoFiadoRepository;
import com.pos.repository.InventarioDiarioRepository;
import com.pos.repository.MenuDiarioRepository;
import com.pos.repository.ProductoRepository;
import com.pos.repository.TurnoCajaRepository;
import com.pos.repository.VentaRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VentaService {

    private final VentaRepository ventaRepository;
    private final ProductoRepository productoRepository;
    private final TurnoCajaRepository turnoCajaRepository;
    private final InventarioDiarioRepository inventarioDiarioRepository;
    private final MenuDiarioRepository menuDiarioRepository;
    private final ImpresoraTtermicaService impresoraTtermicaService;
    private final ConfiguracionService configuracionService;
    private final VentaPagoDetalleService ventaPagoDetalleService;
    private final FechaOperativaService fechaOperativaService;
    private final FiadoService fiadoService;
    private final MovimientoFinancieroService movimientoFinancieroService;
    private final AbonoFiadoRepository abonoFiadoRepository;
    private final AuditService auditService;

    public Venta obtenerPorId(Long ventaId) {
        return ventaRepository.findByIdWithDetalles(ventaId)
                .orElseThrow(() -> new BadRequestException("Venta no encontrada"));
    }

    public VentaDetalleResponseDTO obtenerDetallePorId(Long ventaId) {
        Venta venta = obtenerPorId(ventaId);
        VentaPagoDetalleDTO pago = ventaPagoDetalleService.obtener(venta.getId());
        FormaPago formaPago = resolverFormaPago(
                venta.getFormaPago(),
                pago != null ? pago.pagoEfectivo() : BigDecimal.ZERO,
                pago != null ? pago.pagoTransferencia() : BigDecimal.ZERO);

        return new VentaDetalleResponseDTO(
                venta.getId(),
                venta.getFecha(),
                venta.getTipoVenta(),
                venta.getEstado(),
                venta.getClienteNombre(),
                venta.getTelefono(),
                venta.getDireccion(),
                venta.getValorDomicilio(),
                venta.getParaLlevar(),
                venta.getDescuentoPorcentaje(),
                venta.getDescuentoValor(),
                venta.getTotal(),
                formaPago,
                pago != null ? pago.pagoEfectivo() : BigDecimal.ZERO,
                pago != null ? pago.pagoTransferencia() : BigDecimal.ZERO,
                venta.getCondicionPago(),
                venta.getSaldoPendiente(),
                venta.getCliente() != null ? venta.getCliente().getId() : null,
                venta.getFechaAnulacion(),
                venta.getMotivoAnulacion(),
                venta.getDetalles() == null
                        ? List.of()
                        : venta.getDetalles().stream()
                                .map(d -> new VentaItemResponseDTO(
                                        d.getProducto() != null ? d.getProducto().getId() : null,
                                        d.getProducto() != null ? d.getProducto().getNombre() : "Producto",
                                        d.getCantidad(),
                                        d.getPrecioUnitario(),
                                        d.getSubtotal(),
                                        d.getObservacion()))
                                .toList());
    }

    public com.pos.dto.venta.VentaResponseDTO construirRespuesta(Venta venta) {
        VentaPagoDetalleDTO pago = venta.getId() != null
                ? ventaPagoDetalleService.obtener(venta.getId())
                : null;
        FormaPago formaPago = resolverFormaPago(
                venta.getFormaPago(),
                pago != null ? pago.pagoEfectivo() : BigDecimal.ZERO,
                pago != null ? pago.pagoTransferencia() : BigDecimal.ZERO);

        return new com.pos.dto.venta.VentaResponseDTO(
                venta.getId(),
                venta.getFecha(),
                venta.getTipoVenta(),
                venta.getEstado(),
                venta.getClienteNombre(),
                venta.getTelefono(),
                venta.getDireccion(),
                venta.getValorDomicilio(),
                venta.getParaLlevar(),
                venta.getDescuentoPorcentaje(),
                venta.getDescuentoValor(),
                venta.getTotal(),
                formaPago,
                pago != null ? pago.pagoEfectivo() : BigDecimal.ZERO,
                pago != null ? pago.pagoTransferencia() : BigDecimal.ZERO,
                venta.getCondicionPago(),
                venta.getSaldoPendiente(),
                venta.getCliente() != null ? venta.getCliente().getId() : null);
    }

    public Page<Venta> listarOperativas(
            EstadoVenta estado,
            TipoVenta tipoVenta,
            Long turnoId,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            String clienteNombre,
            String telefono,
            int page,
            int size) {
        Specification<Venta> spec = Specification.unrestricted();

        if (estado != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("estado"), estado));
        }
        if (tipoVenta != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("tipoVenta"), tipoVenta));
        }
        if (turnoId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("turno").get("id"), turnoId));
        }
        if (fechaInicio != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(
                    root.get("fecha"),
                    fechaInicio.atStartOfDay()));
        }
        if (fechaFin != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(
                    root.get("fecha"),
                    fechaFin.atTime(23, 59, 59)));
        }
        if (clienteNombre != null && !clienteNombre.isBlank()) {
            String pattern = "%" + clienteNombre.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("clienteNombre")), pattern));
        }
        if (telefono != null && !telefono.isBlank()) {
            String pattern = "%" + telefono.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("telefono")), pattern));
        }

        Page<Venta> pageResult = ventaRepository.findAll(
                spec,
                PageRequest.of(
                        Math.max(page, 0),
                        Math.max(size, 1),
                        org.springframework.data.domain.Sort.by(
                                org.springframework.data.domain.Sort.Direction.DESC,
                                "fecha")));

        return pageResult;
    }

    @Transactional
    public Venta registrarVenta(VentaCreateDTO dto, Usuario usuario) {
        TurnoCaja turno = turnoCajaRepository
                .findByEstadoInForUpdate(List.of(EstadoTurno.ABIERTO))
                .orElseThrow(() -> new BadRequestException("No hay turno activo"));

        if (dto.tipoVenta() == TipoVenta.LOCAL && !"CAJA".equals(usuario.getRol().getNombre())) {
            throw new BadRequestException("Solo CAJA puede registrar ventas locales");
        }

        if (dto.tipoVenta() == TipoVenta.DOMICILIO && !"DOMI".equals(usuario.getRol().getNombre())) {
            throw new BadRequestException("Solo DOMI puede registrar ventas a domicilio");
        }

        if (dto.tipoVenta() == TipoVenta.DOMICILIO && turno.getEstado() != EstadoTurno.ABIERTO) {
            throw new BadRequestException("Para crear domicilios el turno debe estar ABIERTO");
        }

        if (dto.tipoVenta() == TipoVenta.DOMICILIO) {
            if (!Boolean.TRUE.equals(dto.fiado()) && (dto.telefono() == null || dto.telefono().isBlank())) {
                throw new BadRequestException("El telefono es obligatorio");
            }
            if (dto.direccion() == null || dto.direccion().isBlank()) {
                throw new BadRequestException("La direccion es obligatoria");
            }
        }

        Venta venta = new Venta();
        venta.setFecha(LocalDateTime.now());
        venta.setTipoVenta(dto.tipoVenta());

        BigDecimal pagoEfectivo = nonNegative(dto.pagoEfectivo());
        BigDecimal pagoTransferencia = nonNegative(dto.pagoTransferencia());
        boolean esFiado = Boolean.TRUE.equals(dto.fiado());

        Cliente cliente;
        if (dto.tipoVenta() == TipoVenta.DOMICILIO) {
            // Para cualquier domicilio (contado, transferencia o fiado) sincronizamos el
            // cliente maestro automáticamente
            cliente = fiadoService.resolverOActualizarCliente(
                    dto.clienteNombre(),
                    dto.telefono(),
                    dto.direccion());
        } else if (esFiado) {
            cliente = fiadoService.resolverClienteVenta(true, dto.clienteId(), dto.clienteNombre(), dto.telefono());
        } else {
            cliente = null;
        }

        log.info("[VENTA] Cliente resuelto: id={} nombre={} telefono={}",
                cliente != null ? cliente.getId() : null,
                cliente != null ? cliente.getNombre() : dto.clienteNombre(),
                cliente != null ? cliente.getTelefono() : dto.telefono());

        venta.setFormaPago(esFiado ? FormaPago.FIADO
                : resolverFormaPago(dto.formaPago(), pagoEfectivo, pagoTransferencia));
        venta.setCondicionPago(esFiado ? CondicionPago.FIADO : CondicionPago.CONTADO);
        venta.setUsuario(usuario);
        venta.setTurno(turno);
        venta.setEstado(dto.tipoVenta() == TipoVenta.LOCAL ? EstadoVenta.DESPACHADA : EstadoVenta.EN_PROCESO);
        venta.setCliente(cliente);
        venta.setClienteNombre(
                cliente != null
                        ? cliente.getNombre()
                        : dto.clienteNombre());
        venta.setTelefono(
                cliente != null
                        ? cliente.getTelefono()
                        : dto.telefono());
        venta.setDireccion(dto.direccion());
        venta.setValorDomicilio(dto.valorDomicilio());
        venta.setParaLlevar(dto.tipoVenta() == TipoVenta.LOCAL && Boolean.TRUE.equals(dto.paraLlevar()));

        BigDecimal total = BigDecimal.ZERO;
        List<VentaDetalle> detalles = new ArrayList<>();
        MenuDiario menuActivo = null;

        for (VentaDetalleCreateDTO d : dto.detalles()) {
            if (d.cantidad() == null || d.cantidad() <= 0) {
                throw new BadRequestException("La cantidad de cada producto debe ser mayor a cero");
            }

            Producto producto = productoRepository.findById(d.productoId())
                    .orElseThrow(() -> new BadRequestException("Producto no existe"));

            if (producto.getTipoVenta() == TipoVentaProducto.MENU_DIARIO && menuActivo == null) {
                menuActivo = menuDiarioRepository
                        .findByFechaAndActivoTrue(fechaOperativaService.obtenerFechaOperativa())
                        .orElseThrow(() -> new BadRequestException("No hay menu activo hoy"));
            }

            BigDecimal subtotal = producto.getPrecio()
                    .multiply(BigDecimal.valueOf(d.cantidad()));

            total = total.add(subtotal);

            VentaDetalle detalle = VentaDetalle.builder()
                    .venta(venta)
                    .producto(producto)
                    .cantidad(d.cantidad())
                    .precioUnitario(producto.getPrecio())
                    .subtotal(subtotal)
                    .observacion(d.observacion())
                    .build();

            detalles.add(detalle);

            if (producto.getTipoVenta() == TipoVentaProducto.MENU_DIARIO) {
                InventarioDiario inv = obtenerInventarioParaActualizar(
                        producto,
                        menuActivo,
                        "Producto no esta en el menu");

                if (inv.getStockActual() < d.cantidad()) {
                    throw new BadRequestException("Stock insuficiente");
                }

                inv.setStockActual(inv.getStockActual() - d.cantidad());
                inv.setAgotado(inv.getStockActual() <= 0);
                inventarioDiarioRepository.save(inv);
            }
        }

        if (dto.tipoVenta() == TipoVenta.DOMICILIO && dto.valorDomicilio() != null) {
            total = total.add(dto.valorDomicilio());
        }

        BigDecimal descuentoValor = BigDecimal.ZERO;
        if (dto.descuentoPorcentaje() != null && dto.descuentoPorcentaje().compareTo(BigDecimal.ZERO) > 0) {
            if (!"CAJA".equals(usuario.getRol().getNombre())) {
                throw new BadRequestException("Solo CAJA puede aplicar descuentos");
            }

            descuentoValor = total
                    .multiply(dto.descuentoPorcentaje())
                    .divide(BigDecimal.valueOf(100));

            total = total.subtract(descuentoValor);
        }

        venta.setDescuentoPorcentaje(dto.descuentoPorcentaje());
        venta.setDescuentoValor(descuentoValor);
        venta.setTotal(total);
        venta.setDetalles(detalles);

        boolean hayAbonoParcial = esFiado
                && (pagoEfectivo.compareTo(BigDecimal.ZERO) > 0 || pagoTransferencia.compareTo(BigDecimal.ZERO) > 0);
        boolean cobraContado = dto.tipoVenta() == TipoVenta.LOCAL && !esFiado;
        PagoAplicado pagoAplicado;
        if (cobraContado) {
            pagoAplicado = calcularPagoAplicado(total, pagoEfectivo, pagoTransferencia,
                    "El pago es insuficiente para registrar la venta");
        } else if (hayAbonoParcial) {
            pagoAplicado = calcularPagoFiadoAbono(total, pagoEfectivo, pagoTransferencia);
        } else {
            pagoAplicado = PagoAplicado.cero();
        }

        venta.setSaldoPendiente(
                hayAbonoParcial ? total.subtract(pagoAplicado.totalAplicado()) : (esFiado ? total : BigDecimal.ZERO));

        log.info(
                "[VENTA] tipo={} fiado={} total={} efectivo={} transf={} cobraContado={} hayAbonoParcial={} saldoPendiente={}",
                dto.tipoVenta(), esFiado, total, pagoEfectivo, pagoTransferencia, cobraContado, hayAbonoParcial,
                venta.getSaldoPendiente());

        if (dto.tipoVenta() == TipoVenta.LOCAL && (!esFiado || hayAbonoParcial)) {
            turno.setTotalVentas(turno.getTotalVentas().add(pagoAplicado.totalAplicado()));
            turnoCajaRepository.save(turno);
        }

        Venta ventaGuardada = ventaRepository.save(venta);
        ventaPagoDetalleService.guardar(
                ventaGuardada.getId(),
                pagoAplicado.efectivoAplicado(),
                pagoAplicado.transferenciaAplicada(),
                pagoAplicado.efectivoRecibido(),
                pagoAplicado.transferenciaRecibida(),
                pagoAplicado.cambioEfectivo());
        if (esFiado) {
            if (hayAbonoParcial) {
                movimientoFinancieroService.registrarVentaFiadaConAbono(ventaGuardada, usuario,
                        pagoAplicado.efectivoAplicado(), pagoAplicado.transferenciaAplicada());
            } else {
                movimientoFinancieroService.registrarVentaFiada(ventaGuardada, usuario);
            }
        } else if (ventaGuardada.getEstado() == EstadoVenta.DESPACHADA) {
            movimientoFinancieroService.registrarVentaContado(
                    ventaGuardada,
                    usuario,
                    pagoAplicado.efectivoAplicado(),
                    pagoAplicado.transferenciaAplicada());
        }
        if (hayAbonoParcial && ventaGuardada.getCliente() != null) {
            BigDecimal efectivoAplicado = pagoAplicado.efectivoAplicado();
            BigDecimal transferenciaAplicada = pagoAplicado.transferenciaAplicada();
            BigDecimal montoAbono = efectivoAplicado.add(transferenciaAplicada);
            log.info("[ABONO_INICIAL] ventaId={} clienteId={} monto={} efectivo={} transferencia={}",
                    ventaGuardada.getId(), ventaGuardada.getCliente().getId(), montoAbono, efectivoAplicado,
                    transferenciaAplicada);

            FormaPago formaPagoAbono = transferenciaAplicada.compareTo(BigDecimal.ZERO) > 0
                    && efectivoAplicado.compareTo(BigDecimal.ZERO) == 0
                            ? FormaPago.TRANSFERENCIA
                            : FormaPago.EFECTIVO;

            abonoFiadoRepository.save(
                    AbonoFiado.builder()
                            .fecha(LocalDateTime.now())
                            .monto(montoAbono)
                            .montoEfectivo(efectivoAplicado)
                            .montoTransferencia(transferenciaAplicada)
                            .formaPago(formaPagoAbono)
                            .cliente(ventaGuardada.getCliente())
                            .usuario(usuario)
                            .turno(turno)
                            .build());
        }

        auditService.record(
                esFiado ? "VENTA_FIADA_REGISTRADA" : "VENTA_REGISTRADA",
                "Venta",
                ventaGuardada.getId(),
                usuario,
                turno,
                null,
                auditService.change("total", null, ventaGuardada.getTotal()),
                auditService.change("estado", null, ventaGuardada.getEstado()),
                auditService.change("condicionPago", null, ventaGuardada.getCondicionPago()));

        if (ventaGuardada.getEstado() == EstadoVenta.DESPACHADA && isFacturaAutoEnabled()) {
            imprimirFacturaSeguro(ventaGuardada, pagoAplicado.efectivoAplicado(), pagoAplicado.transferenciaAplicada());
        }

        return ventaGuardada;
    }

    public void imprimirTicketCocinaPreview(VentaCocinaPreviewDTO dto, Usuario usuario) {
        if (usuario == null || usuario.getRol() == null || !"CAJA".equals(usuario.getRol().getNombre())) {
            throw new BadRequestException("Solo CAJA puede imprimir ticket de cocina previo");
        }

        if (!isCocinaAutoEnabled()) {
            return;
        }

        if (dto == null || dto.detalles() == null || dto.detalles().isEmpty()) {
            throw new BadRequestException("Debe enviar al menos un producto");
        }

        Venta ventaPreview = new Venta();
        ventaPreview.setId(System.currentTimeMillis());
        ventaPreview.setFecha(LocalDateTime.now());
        ventaPreview.setClienteNombre(dto.clienteNombre());
        ventaPreview.setParaLlevar(Boolean.TRUE.equals(dto.paraLlevar()));

        List<VentaDetalle> detalles = new ArrayList<>();
        for (VentaDetalleCreateDTO d : dto.detalles()) {
            if (d.productoId() == null || d.cantidad() == null || d.cantidad() <= 0) {
                throw new BadRequestException("Detalle de producto invalido");
            }

            Producto producto = productoRepository.findById(d.productoId())
                    .orElseThrow(() -> new BadRequestException("Producto no existe"));

            VentaDetalle detalle = VentaDetalle.builder()
                    .venta(ventaPreview)
                    .producto(producto)
                    .cantidad(d.cantidad())
                    .precioUnitario(producto.getPrecio())
                    .subtotal(producto.getPrecio().multiply(BigDecimal.valueOf(d.cantidad())))
                    .observacion(d.observacion())
                    .build();
            detalles.add(detalle);
        }

        ventaPreview.setDetalles(detalles);
        imprimirTicketCocinaSeguro(ventaPreview);
    }

    @Transactional
    public Venta despacharVenta(Long ventaId, VentaDespachoDTO dto, Usuario usuario) {
        Venta venta = ventaRepository.findByIdWithDetallesForUpdate(ventaId)
                .orElseThrow(() -> new BadRequestException("Venta no encontrada"));

        if (venta.getTipoVenta() != TipoVenta.DOMICILIO) {
            throw new BadRequestException("Solo domicilios se despachan");
        }

        if (venta.getEstado() != EstadoVenta.EN_PROCESO) {
            throw new BadRequestException("La venta no esta en proceso");
        }

        if (!"CAJA".equals(usuario.getRol().getNombre())) {
            throw new BadRequestException("Solo CAJA puede despachar");
        }

        if (dto == null) {
            throw new BadRequestException("Debe informar el pago para despachar la venta");
        }

        TurnoCaja turno = venta.getTurno();
        BigDecimal pagoEfectivo = nonNegative(dto.pagoEfectivo());
        BigDecimal pagoTransferencia = nonNegative(dto.pagoTransferencia());

        boolean esFiado = venta.getCondicionPago() == CondicionPago.FIADO;
        PagoAplicado pagoAplicado = esFiado
                ? PagoAplicado.cero()
                : calcularPagoAplicado(venta.getTotal(), pagoEfectivo, pagoTransferencia,
                        "El pago es insuficiente para despachar la venta");

        log.info("[DESPACHO] ventaId={} esFiado={} total={} efectivo={} transf={} saldoPendiente={}",
                venta.getId(), esFiado, venta.getTotal(), pagoEfectivo, pagoTransferencia,
                esFiado ? venta.getTotal() : BigDecimal.ZERO);

        venta.setEstado(EstadoVenta.DESPACHADA);
        venta.setFormaPago(esFiado ? FormaPago.FIADO
                : resolverFormaPago(dto.formaPago(), pagoEfectivo, pagoTransferencia));
        venta.setSaldoPendiente(esFiado ? venta.getTotal() : BigDecimal.ZERO);
        if (!esFiado) {
            turno.setTotalVentas(turno.getTotalVentas().add(pagoAplicado.totalAplicado()));
        }

        turnoCajaRepository.save(turno);
        Venta ventaGuardada = ventaRepository.save(venta);
        ventaPagoDetalleService.guardar(
                ventaGuardada.getId(),
                pagoAplicado.efectivoAplicado(),
                pagoAplicado.transferenciaAplicada(),
                pagoAplicado.efectivoRecibido(),
                pagoAplicado.transferenciaRecibida(),
                pagoAplicado.cambioEfectivo());
        if (esFiado) {
            movimientoFinancieroService.registrarVentaFiada(ventaGuardada, usuario);
        } else {
            movimientoFinancieroService.registrarVentaContado(
                    ventaGuardada,
                    usuario,
                    pagoAplicado.efectivoAplicado(),
                    pagoAplicado.transferenciaAplicada());
        }
        auditService.record(
                "VENTA_DESPACHADA",
                "Venta",
                ventaGuardada.getId(),
                usuario,
                turno,
                null,
                auditService.change("estado", EstadoVenta.EN_PROCESO, EstadoVenta.DESPACHADA),
                auditService.change("pagoEfectivo", null, pagoAplicado.efectivoAplicado()),
                auditService.change("pagoTransferencia", null, pagoAplicado.transferenciaAplicada()));

        if (isFacturaAutoEnabled() && !esFiado) {
            imprimirFacturaSeguro(ventaGuardada, pagoAplicado.efectivoAplicado(), pagoAplicado.transferenciaAplicada());
        }

        return ventaGuardada;
    }

    @Transactional
    public Venta marcarVentaDomicilioComoFiado(Long ventaId, VentaFiadoDTO dto, Usuario usuario) {
        Venta venta = ventaRepository.findByIdWithDetallesForUpdate(ventaId)
                .orElseThrow(() -> new BadRequestException("Venta no encontrada"));

        if (venta.getTipoVenta() != TipoVenta.DOMICILIO) {
            throw new BadRequestException("Solo se pueden marcar como fiado los domicilios");
        }
        if (venta.getEstado() != EstadoVenta.EN_PROCESO) {
            throw new BadRequestException("Solo se pueden marcar como fiado los pedidos en proceso");
        }
        if (venta.getCondicionPago() == CondicionPago.FIADO) {
            return venta;
        }

        Cliente cliente;
        if (venta.getCliente() != null) {
            cliente = venta.getCliente();
            if (dto.clienteNombre() != null && !dto.clienteNombre().isBlank()
                    && !dto.clienteNombre().equals(cliente.getNombre())) {
                cliente.setNombre(dto.clienteNombre());
            }
        } else {
            cliente = fiadoService.resolverClienteVenta(true, dto.clienteId(), dto.clienteNombre(),
                    dto.clienteTelefono());
        }

        BigDecimal pagoEfectivo = nonNegative(dto.pagoEfectivo());
        BigDecimal pagoTransferencia = nonNegative(dto.pagoTransferencia());
        boolean hayAbono = pagoEfectivo.compareTo(BigDecimal.ZERO) > 0
                || pagoTransferencia.compareTo(BigDecimal.ZERO) > 0;
        TurnoCaja turno = venta.getTurno();

        venta.setCliente(cliente);
        venta.setCondicionPago(CondicionPago.FIADO);
        venta.setFormaPago(FormaPago.FIADO);
        venta.setEstado(EstadoVenta.DESPACHADA);

        PagoAplicado pagoAplicado;
        if (hayAbono) {
            pagoAplicado = calcularPagoFiadoAbono(venta.getTotal(), pagoEfectivo, pagoTransferencia);
            venta.setSaldoPendiente(venta.getTotal().subtract(pagoAplicado.totalAplicado()));
            turno.setTotalVentas(turno.getTotalVentas().add(pagoAplicado.totalAplicado()));
        } else {
            pagoAplicado = PagoAplicado.cero();
            venta.setSaldoPendiente(venta.getTotal());
        }

        if (cliente != null) {
            venta.setClienteNombre(cliente.getNombre());
            venta.setTelefono(cliente.getTelefono());
        }

        log.info("[MARCAR_FIADO] ventaId={} clienteId={} total={} saldoPendiente={} abono={}",
                venta.getId(), cliente != null ? cliente.getId() : null, venta.getTotal(),
                venta.getSaldoPendiente(), pagoAplicado.totalAplicado());

        turnoCajaRepository.save(turno);
        Venta ventaGuardada = ventaRepository.save(venta);
        ventaPagoDetalleService.guardar(
                ventaGuardada.getId(),
                pagoAplicado.efectivoAplicado(),
                pagoAplicado.transferenciaAplicada(),
                pagoAplicado.efectivoRecibido(),
                pagoAplicado.transferenciaRecibida(),
                pagoAplicado.cambioEfectivo());

        if (hayAbono) {
            movimientoFinancieroService.registrarVentaFiadaConAbono(ventaGuardada, usuario,
                    pagoAplicado.efectivoAplicado(), pagoAplicado.transferenciaAplicada());
            if (ventaGuardada.getCliente() != null) {
                BigDecimal montoAbono = pagoAplicado.totalAplicado();
                FormaPago formaPagoAbono = pagoAplicado.transferenciaAplicada().compareTo(BigDecimal.ZERO) > 0
                        && pagoAplicado.efectivoAplicado().compareTo(BigDecimal.ZERO) == 0
                                ? FormaPago.TRANSFERENCIA
                                : FormaPago.EFECTIVO;
                abonoFiadoRepository.save(
                        AbonoFiado.builder()
                                .fecha(LocalDateTime.now())
                                .monto(montoAbono)
                                .montoEfectivo(pagoAplicado.efectivoAplicado())
                                .montoTransferencia(pagoAplicado.transferenciaAplicada())
                                .formaPago(formaPagoAbono)
                                .cliente(ventaGuardada.getCliente())
                                .usuario(usuario)
                                .turno(turno)
                                .build());
            }
        } else {
            movimientoFinancieroService.registrarVentaFiada(ventaGuardada, usuario);
        }

        auditService.record(
                "VENTA_DOMICILIO_FIADO",
                "Venta",
                ventaGuardada.getId(),
                usuario,
                ventaGuardada.getTurno(),
                null,
                auditService.change("condicionPago", CondicionPago.CONTADO, CondicionPago.FIADO),
                auditService.change("formaPago", null, FormaPago.FIADO),
                auditService.change("estado", EstadoVenta.EN_PROCESO, EstadoVenta.DESPACHADA),
                auditService.change("clienteId", null, cliente != null ? cliente.getId() : null));
        return ventaGuardada;
    }

    @Transactional
    public Venta cancelarVenta(Long ventaId, Usuario usuario) {
        Venta venta = ventaRepository.findByIdWithDetallesForUpdate(ventaId)
                .orElseThrow(() -> new BadRequestException("Venta no encontrada"));

        if (venta.getEstado() != EstadoVenta.EN_PROCESO) {
            throw new BadRequestException("Solo se pueden cancelar ventas en proceso");
        }

        if (venta.getTipoVenta() == TipoVenta.LOCAL && !"CAJA".equals(usuario.getRol().getNombre())) {
            throw new BadRequestException("Solo CAJA puede cancelar ventas locales");
        }

        if (venta.getTipoVenta() == TipoVenta.DOMICILIO
                && !"CAJA".equals(usuario.getRol().getNombre())
                && !"DOMI".equals(usuario.getRol().getNombre())) {
            throw new BadRequestException("No autorizado para cancelar este pedido");
        }

        devolverInventario(venta);
        venta.setEstado(EstadoVenta.CANCELADA);
        Venta guardada = ventaRepository.save(venta);
        auditService.record(
                "VENTA_CANCELADA",
                "Venta",
                guardada.getId(),
                usuario,
                guardada.getTurno(),
                null,
                auditService.change("estado", EstadoVenta.EN_PROCESO, EstadoVenta.CANCELADA));
        return guardada;
    }

    @Transactional
    public Venta anularVenta(Long ventaId, Usuario usuario) {
        return anularVenta(ventaId, null, usuario);
    }

    @Transactional
    public Venta anularVenta(Long ventaId, String motivo, Usuario usuario) {
        Venta venta = ventaRepository.findByIdWithDetallesForUpdate(ventaId)
                .orElseThrow(() -> new BadRequestException("Venta no encontrada"));

        if (venta.getEstado() != EstadoVenta.DESPACHADA) {
            throw new BadRequestException("Solo se pueden anular ventas despachadas");
        }

        if (!"CAJA".equals(usuario.getRol().getNombre())) {
            throw new BadRequestException("Solo CAJA puede anular ventas");
        }

        TurnoCaja turno = venta.getTurno();
        VentaPagoDetalleDTO pago = ventaPagoDetalleService.obtener(venta.getId());
        BigDecimal efectivoAplicado = pago != null ? pago.pagoEfectivo() : BigDecimal.ZERO;
        BigDecimal transferenciaAplicada = pago != null ? pago.pagoTransferencia() : BigDecimal.ZERO;

        // Siempre ajustar turno y registrar movimiento (incluye fiados con abono)
        turno.setTotalVentas(turno.getTotalVentas().subtract(efectivoAplicado.add(transferenciaAplicada)));
        turnoCajaRepository.save(turno);

        devolverInventario(venta);

        venta.setEstado(EstadoVenta.ANULADA);
        venta.setFechaAnulacion(LocalDateTime.now());
        venta.setMotivoAnulacion(normalizeMotivoAnulacion(motivo));
        venta.setSaldoPendiente(BigDecimal.ZERO);

        Venta guardada = ventaRepository.save(venta);
        if (efectivoAplicado.compareTo(BigDecimal.ZERO) > 0 || transferenciaAplicada.compareTo(BigDecimal.ZERO) > 0) {
            movimientoFinancieroService.registrarAnulacionVenta(guardada, usuario, efectivoAplicado,
                    transferenciaAplicada);
        }
        auditService.record(
                "VENTA_ANULADA",
                "Venta",
                guardada.getId(),
                usuario,
                turno,
                guardada.getMotivoAnulacion(),
                auditService.change("estado", EstadoVenta.DESPACHADA, EstadoVenta.ANULADA),
                auditService.change("saldoPendiente", venta.getTotal(), BigDecimal.ZERO));
        return guardada;
    }

    @Transactional
    public Venta actualizarValorDomicilio(Long ventaId, BigDecimal valorDomicilio, Usuario usuario) {
        Venta venta = ventaRepository.findByIdWithDetallesForUpdate(ventaId)
                .orElseThrow(() -> new BadRequestException("Venta no encontrada"));

        validarOperacionEnProcesoDomicilio(venta, usuario);

        if (valorDomicilio == null || valorDomicilio.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("El valor del domicilio es invalido");
        }

        BigDecimal anterior = venta.getValorDomicilio();
        venta.setValorDomicilio(valorDomicilio);
        recalcularTotales(venta);

        Venta guardada = ventaRepository.save(venta);
        auditService.record(
                "VENTA_VALOR_DOMICILIO_ACTUALIZADO",
                "Venta",
                guardada.getId(),
                usuario,
                guardada.getTurno(),
                null,
                auditService.change("valorDomicilio", anterior, valorDomicilio),
                auditService.change("total", null, guardada.getTotal()));
        return guardada;
    }

    public Venta imprimirFacturaEnProceso(Long ventaId, Usuario usuario) {
        Venta venta = ventaRepository.findById(ventaId)
                .orElseThrow(() -> new BadRequestException("Venta no encontrada"));

        validarOperacionEnProcesoDomicilio(venta, usuario);
        imprimirFacturaSeguro(venta);
        auditService.record("VENTA_FACTURA_REIMPRESA", "Venta", venta.getId(), usuario, venta.getTurno(), null);
        return venta;
    }

    public Venta imprimirTicketCocinaEnProceso(Long ventaId, Usuario usuario) {
        Venta venta = ventaRepository.findById(ventaId)
                .orElseThrow(() -> new BadRequestException("Venta no encontrada"));

        validarOperacionEnProcesoDomicilio(venta, usuario);
        imprimirTicketCocinaSeguro(venta);
        auditService.record("VENTA_COCINA_REIMPRESA", "Venta", venta.getId(), usuario, venta.getTurno(), null);
        return venta;
    }

    public Venta reimprimirFactura(Long ventaId, Usuario usuario) {
        Venta venta = obtenerPorId(ventaId);
        imprimirFacturaSeguro(venta);
        auditService.record("VENTA_FACTURA_REIMPRESA", "Venta", venta.getId(), usuario, venta.getTurno(), null);
        return venta;
    }

    public Venta reimprimirTicketCocina(Long ventaId, Usuario usuario) {
        Venta venta = obtenerPorId(ventaId);
        imprimirTicketCocinaSeguro(venta);
        auditService.record("VENTA_COCINA_REIMPRESA", "Venta", venta.getId(), usuario, venta.getTurno(), null);
        return venta;
    }

    private void devolverInventario(Venta venta) {
        MenuDiario menuActivo = menuDiarioRepository
                .findByFechaAndActivoTrue(venta.getFecha().toLocalDate())
                .orElse(null);

        if (menuActivo == null) {
            return;
        }

        for (VentaDetalle detalle : venta.getDetalles()) {
            Producto producto = detalle.getProducto();

            if (producto.getTipoVenta() == TipoVentaProducto.MENU_DIARIO) {
                InventarioDiario inv = obtenerInventarioParaActualizar(
                        producto,
                        menuActivo,
                        "Inventario no encontrado");

                inv.setStockActual(inv.getStockActual() + detalle.getCantidad());
                inv.setAgotado(false);
                inventarioDiarioRepository.save(inv);
            }
        }
    }

    private void imprimirFacturaSeguro(Venta venta) {
        try {
            impresoraTtermicaService.imprimirFactura(venta);
        } catch (Exception ex) {
            log.warn("No se pudo imprimir factura de la venta {}", venta.getId(), ex);
        }
    }

    private void imprimirFacturaSeguro(Venta venta, BigDecimal pagoEfectivo, BigDecimal pagoTransferencia) {
        try {
            impresoraTtermicaService.imprimirFactura(venta, pagoEfectivo, pagoTransferencia);
        } catch (Exception ex) {
            log.warn("No se pudo imprimir factura de la venta {}", venta.getId(), ex);
        }
    }

    private void imprimirTicketCocinaSeguro(Venta venta) {
        try {
            impresoraTtermicaService.imprimirTicketCocina(venta);
        } catch (Exception ex) {
            log.warn("No se pudo imprimir ticket de cocina para la venta {}", venta.getId(), ex);
        }
    }

    private void validarOperacionEnProcesoDomicilio(Venta venta, Usuario usuario) {
        if (venta.getTipoVenta() != TipoVenta.DOMICILIO) {
            throw new BadRequestException("Esta operacion aplica solo a ventas domicilio");
        }
        if (venta.getEstado() != EstadoVenta.EN_PROCESO) {
            throw new BadRequestException("Solo se permite cuando la venta esta en proceso");
        }
        if (usuario == null || usuario.getRol() == null) {
            throw new BadRequestException("Usuario no valido");
        }
        if (!esPedidoPorCaja(usuario) && !"DOMI".equals(usuario.getRol().getNombre())) {
            throw new BadRequestException("No autorizado para esta operacion");
        }
    }

    private void recalcularTotales(Venta venta) {
        BigDecimal subtotalProductos = venta.getDetalles() == null
                ? BigDecimal.ZERO
                : venta.getDetalles().stream()
                        .map(VentaDetalle::getSubtotal)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal base = subtotalProductos.add(
                venta.getValorDomicilio() == null ? BigDecimal.ZERO : venta.getValorDomicilio());

        BigDecimal descuentoValor = BigDecimal.ZERO;
        if (venta.getDescuentoPorcentaje() != null
                && venta.getDescuentoPorcentaje().compareTo(BigDecimal.ZERO) > 0) {
            descuentoValor = base
                    .multiply(venta.getDescuentoPorcentaje())
                    .divide(BigDecimal.valueOf(100));
        }

        venta.setDescuentoValor(descuentoValor);
        venta.setTotal(base.subtract(descuentoValor));
    }

    private boolean esPedidoPorCaja(Usuario usuario) {
        return usuario != null
                && usuario.getRol() != null
                && "CAJA".equals(usuario.getRol().getNombre());
    }

    private boolean isFacturaAutoEnabled() {
        try {
            return configuracionService.obtener().imprimirFacturaAuto();
        } catch (Exception ex) {
            log.warn("No se pudo leer configuracion de impresion de factura, se usara habilitada por defecto", ex);
            return true;
        }
    }

    private boolean isCocinaAutoEnabled() {
        try {
            return configuracionService.obtener().imprimirCocinaAuto();
        } catch (Exception ex) {
            log.warn("No se pudo leer configuracion de impresion de cocina, se usara habilitada por defecto", ex);
            return true;
        }
    }

    /**
     * Resuelve la forma de pago real a partir de los montos recibidos.
     * Si se usa junto a una venta fiada, llamar directamente con FormaPago.FIADO.
     */
    private FormaPago resolverFormaPago(FormaPago formaPagoDeclarada, BigDecimal pagoEfectivo,
            BigDecimal pagoTransferencia) {
        // Si el caller ya sabe que es FIADO, respetarlo sin inspeccionar montos
        if (formaPagoDeclarada == FormaPago.FIADO)
            return FormaPago.FIADO;

        BigDecimal efectivo = pagoEfectivo == null ? BigDecimal.ZERO : pagoEfectivo;
        BigDecimal transferencia = pagoTransferencia == null ? BigDecimal.ZERO : pagoTransferencia;

        boolean tieneEfectivo = efectivo.compareTo(BigDecimal.ZERO) > 0;
        boolean tieneTransferencia = transferencia.compareTo(BigDecimal.ZERO) > 0;

        if (tieneTransferencia && !tieneEfectivo)
            return FormaPago.TRANSFERENCIA;
        if (tieneEfectivo && !tieneTransferencia)
            return FormaPago.EFECTIVO;
        if (tieneEfectivo && tieneTransferencia)
            return transferencia.compareTo(efectivo) >= 0
                    ? FormaPago.TRANSFERENCIA
                    : FormaPago.EFECTIVO;

        return formaPagoDeclarada != null ? formaPagoDeclarada : FormaPago.EFECTIVO;
    }

    private void validarPagoSuficiente(BigDecimal total, BigDecimal pagoEfectivo, BigDecimal pagoTransferencia,
            String mensaje) {
        BigDecimal totalPagado = nonNegative(pagoEfectivo).add(nonNegative(pagoTransferencia));
        if (totalPagado.compareTo(total) < 0) {
            throw new BadRequestException(mensaje);
        }
    }

    private BigDecimal nonNegative(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return value;
    }

    private InventarioDiario obtenerInventarioParaActualizar(Producto producto, MenuDiario menuDiario,
            String errorMessage) {
        return inventarioDiarioRepository.findByProductoAndMenuDiarioForUpdate(producto, menuDiario)
                .orElseThrow(() -> new BadRequestException(errorMessage));
    }

    private String normalizeMotivoAnulacion(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            return "Sin motivo registrado";
        }
        String clean = motivo.trim().replace("\r", " ").replace("\n", " ");
        return clean.length() > 255 ? clean.substring(0, 255) : clean;
    }

    /**
     * Cálculo específico para abono parcial en venta fiada.
     * No exige que el pago cubra el total — el restante queda como saldo pendiente.
     */
    private PagoAplicado calcularPagoFiadoAbono(BigDecimal total, BigDecimal pagoEfectivo,
            BigDecimal pagoTransferencia) {
        BigDecimal totalVenta = total == null ? BigDecimal.ZERO : total;
        BigDecimal efectivoRecibido = nonNegative(pagoEfectivo);
        BigDecimal transferenciaRecibida = nonNegative(pagoTransferencia);

        if (transferenciaRecibida.compareTo(totalVenta) > 0) {
            throw new BadRequestException("La transferencia no puede superar el total de la venta");
        }

        BigDecimal faltante = totalVenta.subtract(transferenciaRecibida);
        BigDecimal efectivoAplicado = efectivoRecibido.min(faltante);
        BigDecimal cambio = efectivoRecibido.subtract(efectivoAplicado);

        return new PagoAplicado(efectivoAplicado, transferenciaRecibida, efectivoRecibido, transferenciaRecibida,
                cambio);
    }

    private PagoAplicado calcularPagoAplicado(BigDecimal total, BigDecimal pagoEfectivo, BigDecimal pagoTransferencia,
            String mensajeInsuficiente) {
        BigDecimal totalVenta = total == null ? BigDecimal.ZERO : total;
        BigDecimal efectivoRecibido = nonNegative(pagoEfectivo);
        BigDecimal transferenciaRecibida = nonNegative(pagoTransferencia);

        if (transferenciaRecibida.compareTo(totalVenta) > 0) {
            throw new BadRequestException("La transferencia no puede superar el total de la venta");
        }

        BigDecimal faltanteDespuesTransferencia = totalVenta.subtract(transferenciaRecibida);
        if (efectivoRecibido.add(transferenciaRecibida).compareTo(totalVenta) < 0) {
            throw new BadRequestException(mensajeInsuficiente);
        }

        BigDecimal efectivoAplicado = efectivoRecibido.min(faltanteDespuesTransferencia);
        BigDecimal cambio = efectivoRecibido.subtract(efectivoAplicado);
        if (cambio.compareTo(totalVenta.max(BigDecimal.valueOf(100000))) > 0) {
            throw new BadRequestException("El efectivo recibido supera un cambio razonable para la venta");
        }

        return new PagoAplicado(
                efectivoAplicado,
                transferenciaRecibida,
                efectivoRecibido,
                transferenciaRecibida,
                cambio);
    }

    private record PagoAplicado(
            BigDecimal efectivoAplicado,
            BigDecimal transferenciaAplicada,
            BigDecimal efectivoRecibido,
            BigDecimal transferenciaRecibida,
            BigDecimal cambioEfectivo) {
        static PagoAplicado cero() {
            return new PagoAplicado(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO);
        }

        BigDecimal totalAplicado() {
            return efectivoAplicado.add(transferenciaAplicada);
        }
    }
}
