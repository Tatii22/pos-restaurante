package com.pos.service;

import com.pos.dto.venta.VentaCreateDTO;
import com.pos.dto.venta.VentaCocinaPreviewDTO;
import com.pos.dto.venta.VentaDetalleCreateDTO;
import com.pos.entity.*;
import com.pos.exception.BadRequestException;
import com.pos.repository.*;
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

    /* ===================== REGISTRAR ===================== */

    public Venta obtenerPorId(Long ventaId) {
        return ventaRepository.findById(ventaId)
                .orElseThrow(() -> new BadRequestException("Venta no encontrada"));
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
            int size
    ) {
        Specification<Venta> spec = Specification.where(null);

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
                    fechaInicio.atStartOfDay()
            ));
        }
        if (fechaFin != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(
                    root.get("fecha"),
                    fechaFin.atTime(23, 59, 59)
            ));
        }
        if (clienteNombre != null && !clienteNombre.isBlank()) {
            String pattern = "%" + clienteNombre.toLowerCase() + "%";
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("clienteNombre")), pattern));
        }
        if (telefono != null && !telefono.isBlank()) {
            String pattern = "%" + telefono.toLowerCase() + "%";
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("telefono")), pattern));
        }

        return ventaRepository.findAll(
                spec,
                PageRequest.of(Math.max(page, 0), Math.max(size, 1), org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "fecha"))
        );
    }

    @Transactional
    public Venta registrarVenta(VentaCreateDTO dto, Usuario usuario) {

        TurnoCaja turno = turnoCajaRepository
                .findByEstadoIn(List.of(EstadoTurno.ABIERTO, EstadoTurno.SIMULADO))
                .orElseThrow(() -> new BadRequestException("No hay turno activo"));

        // 🔐 Rol vs tipo venta
        if (dto.tipoVenta() == TipoVenta.LOCAL &&
                !usuario.getRol().getNombre().equals("CAJA")) {
            throw new BadRequestException("Solo CAJA puede registrar ventas locales");
        }

        if (dto.tipoVenta() == TipoVenta.DOMICILIO &&
                !usuario.getRol().getNombre().equals("DOMI")) {
            throw new BadRequestException("Solo DOMI puede registrar ventas a domicilio");
        }

        if (dto.tipoVenta() == TipoVenta.DOMICILIO && turno.getEstado() != EstadoTurno.ABIERTO) {
            throw new BadRequestException("Para crear domicilios el turno debe estar ABIERTO");
        }

        // 🧾 Validaciones DOMICILIO
        if (dto.tipoVenta() == TipoVenta.DOMICILIO) {
            if (dto.telefono() == null || dto.telefono().isBlank()) {
                throw new BadRequestException("El teléfono es obligatorio");
            }
            if (dto.direccion() == null || dto.direccion().isBlank()) {
                throw new BadRequestException("La dirección es obligatoria");
            }
        }

        Venta venta = new Venta();
        venta.setFecha(LocalDateTime.now());
        venta.setTipoVenta(dto.tipoVenta());
        venta.setFormaPago(dto.formaPago());
        venta.setUsuario(usuario);
        venta.setTurno(turno);

        venta.setEstado(
                dto.tipoVenta() == TipoVenta.LOCAL
                        ? EstadoVenta.DESPACHADA
                        : EstadoVenta.EN_PROCESO
        );

        venta.setClienteNombre(dto.clienteNombre());
        venta.setTelefono(dto.telefono());
        venta.setDireccion(dto.direccion());
        venta.setValorDomicilio(dto.valorDomicilio());

        BigDecimal total = BigDecimal.ZERO;
        List<VentaDetalle> detalles = new ArrayList<>();
        MenuDiario menuActivo = null;

        for (VentaDetalleCreateDTO d : dto.detalles()) {

            Producto producto = productoRepository.findById(d.productoId())
                    .orElseThrow(() -> new BadRequestException("Producto no existe"));

            if (producto.getTipoVenta() == TipoVentaProducto.MENU_DIARIO && menuActivo == null) {
                menuActivo = menuDiarioRepository
                        .findByFechaAndActivoTrue(LocalDate.now())
                        .orElseThrow(() -> new BadRequestException("No hay menú activo hoy"));
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

            // 🔻 Impacto inventario
            if (producto.getTipoVenta() == TipoVentaProducto.MENU_DIARIO) {
                InventarioDiario inv = inventarioDiarioRepository
                        .findByProductoAndMenuDiario(producto, menuActivo)
                        .orElseThrow(() -> new BadRequestException("Producto no está en el menú"));

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
        // 💸 Aplicar descuento si existe
        if (dto.descuentoPorcentaje() != null &&
            dto.descuentoPorcentaje().compareTo(BigDecimal.ZERO) > 0) {

            // (opcional pero recomendado)
            if (!usuario.getRol().getNombre().equals("CAJA")) {
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


        // 💰 LOCAL entra inmediatamente a caja
        if (dto.tipoVenta() == TipoVenta.LOCAL) {
            turno.setTotalVentas(turno.getTotalVentas().add(total));
            turnoCajaRepository.save(turno);
        }

        Venta ventaGuardada = ventaRepository.save(venta);
        ventaPagoDetalleService.guardar(ventaGuardada.getId(), dto.pagoEfectivo(), dto.pagoTransferencia());

        if (ventaGuardada.getEstado() == EstadoVenta.DESPACHADA && isFacturaAutoEnabled()) {
            imprimirFacturaSeguro(ventaGuardada, dto.pagoEfectivo(), dto.pagoTransferencia());
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

    /* ===================== DESPACHAR DOMICILIO ===================== */

    @Transactional
    public Venta despacharVenta(Long ventaId, Usuario usuario) {

        Venta venta = ventaRepository.findById(ventaId)
                .orElseThrow(() -> new BadRequestException("Venta no encontrada"));

        if (venta.getTipoVenta() != TipoVenta.DOMICILIO) {
            throw new BadRequestException("Solo domicilios se despachan");
        }

        if (venta.getEstado() != EstadoVenta.EN_PROCESO) {
            throw new BadRequestException("La venta no está en proceso");
        }

        if (!usuario.getRol().getNombre().equals("CAJA")) {
            throw new BadRequestException("Solo CAJA puede despachar");
        }

        TurnoCaja turno = venta.getTurno();

        venta.setEstado(EstadoVenta.DESPACHADA);
        turno.setTotalVentas(turno.getTotalVentas().add(venta.getTotal()));

        turnoCajaRepository.save(turno);
        Venta ventaGuardada = ventaRepository.save(venta);
        if (isFacturaAutoEnabled()) {
            imprimirFacturaSeguro(ventaGuardada);
        }
        return ventaGuardada;
    }

    /* ===================== CANCELAR (SIN COBRO) ===================== */

    @Transactional
    public Venta cancelarVenta(Long ventaId, Usuario usuario) {

        Venta venta = ventaRepository.findById(ventaId)
                .orElseThrow(() -> new BadRequestException("Venta no encontrada"));

        // 🔒 Estado válido
        if (venta.getEstado() != EstadoVenta.EN_PROCESO) {
            throw new BadRequestException("Solo se pueden cancelar ventas en proceso");
        }

        // 🔐 Permisos por tipo de venta
        if (venta.getTipoVenta() == TipoVenta.LOCAL &&
                !usuario.getRol().getNombre().equals("CAJA")) {
            throw new BadRequestException("Solo CAJA puede cancelar ventas locales");
        }

        if (venta.getTipoVenta() == TipoVenta.DOMICILIO &&
                !usuario.getRol().getNombre().equals("CAJA") &&
                !usuario.getRol().getNombre().equals("DOMI")) {
            throw new BadRequestException("No autorizado para cancelar este pedido");
        }

        // 🔄 Devolver inventario
        devolverInventario(venta);

        venta.setEstado(EstadoVenta.CANCELADA);
        return ventaRepository.save(venta);
    }


    /* ===================== ANULAR (CON DEVOLUCIÓN) ===================== */

    @Transactional
    public Venta anularVenta(Long ventaId, Usuario usuario) {

        Venta venta = ventaRepository.findById(ventaId)
                .orElseThrow(() -> new BadRequestException("Venta no encontrada"));

        if (venta.getEstado() != EstadoVenta.DESPACHADA) {
            throw new BadRequestException("Solo se pueden anular ventas despachadas");
        }

        if (!usuario.getRol().getNombre().equals("CAJA")) {
            throw new BadRequestException("Solo CAJA puede anular ventas");
        }

        TurnoCaja turno = venta.getTurno();
        turno.setTotalVentas(turno.getTotalVentas().subtract(venta.getTotal()));

        devolverInventario(venta);

        venta.setEstado(EstadoVenta.ANULADA);

        turnoCajaRepository.save(turno);
        return ventaRepository.save(venta);
    }

    @Transactional
    public Venta actualizarValorDomicilio(Long ventaId, BigDecimal valorDomicilio, Usuario usuario) {
        Venta venta = ventaRepository.findById(ventaId)
                .orElseThrow(() -> new BadRequestException("Venta no encontrada"));

        validarOperacionEnProcesoDomicilio(venta, usuario);

        if (valorDomicilio == null || valorDomicilio.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("El valor del domicilio es inválido");
        }

        venta.setValorDomicilio(valorDomicilio);
        recalcularTotales(venta);

        return ventaRepository.save(venta);
    }

    public Venta imprimirFacturaEnProceso(Long ventaId, Usuario usuario) {
        Venta venta = ventaRepository.findById(ventaId)
                .orElseThrow(() -> new BadRequestException("Venta no encontrada"));

        validarOperacionEnProcesoDomicilio(venta, usuario);
        imprimirFacturaSeguro(venta);
        return venta;
    }

    public Venta imprimirTicketCocinaEnProceso(Long ventaId, Usuario usuario) {
        Venta venta = ventaRepository.findById(ventaId)
                .orElseThrow(() -> new BadRequestException("Venta no encontrada"));

        validarOperacionEnProcesoDomicilio(venta, usuario);
        imprimirTicketCocinaSeguro(venta);
        return venta;
    }

    /* ===================== INVENTARIO ===================== */

    private void devolverInventario(Venta venta) {

        MenuDiario menuActivo = menuDiarioRepository
                .findByFechaAndActivoTrue(venta.getFecha().toLocalDate())
                .orElse(null);

        if (menuActivo == null) return;

        for (VentaDetalle detalle : venta.getDetalles()) {

            Producto producto = detalle.getProducto();

            if (producto.getTipoVenta() == TipoVentaProducto.MENU_DIARIO) {

                InventarioDiario inv = inventarioDiarioRepository
                        .findByProductoAndMenuDiario(producto, menuActivo)
                        .orElseThrow(() -> new BadRequestException("Inventario no encontrado"));

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
            throw new BadRequestException("Esta operación aplica solo a ventas domicilio");
        }
        if (venta.getEstado() != EstadoVenta.EN_PROCESO) {
            throw new BadRequestException("Solo se permite cuando la venta está en proceso");
        }
        if (usuario == null || usuario.getRol() == null) {
            throw new BadRequestException("Usuario no válido");
        }
        if (!esPedidoPorCaja(usuario) && !"DOMI".equals(usuario.getRol().getNombre())) {
            throw new BadRequestException("No autorizado para esta operación");
        }
    }

    private void recalcularTotales(Venta venta) {
        BigDecimal subtotalProductos = venta.getDetalles() == null
                ? BigDecimal.ZERO
                : venta.getDetalles().stream()
                .map(VentaDetalle::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal base = subtotalProductos.add(
                venta.getValorDomicilio() == null ? BigDecimal.ZERO : venta.getValorDomicilio()
        );

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
}

