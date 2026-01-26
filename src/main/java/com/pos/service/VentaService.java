package com.pos.service;

import com.pos.dto.venta.VentaCreateDTO;
import com.pos.dto.venta.VentaDetalleCreateDTO;
import com.pos.entity.*;
import com.pos.exception.BadRequestException;
import com.pos.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
@Service
@RequiredArgsConstructor
public class VentaService {

    private final VentaRepository ventaRepository;
    private final ProductoRepository productoRepository;
    private final TurnoCajaRepository turnoCajaRepository;
    private final InventarioDiarioRepository inventarioDiarioRepository;
    private final MenuDiarioRepository menuDiarioRepository;

    /* ===================== REGISTRAR ===================== */

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
                inv.setAgotado(inv.getStockActual() <= inv.getStockMinimo());
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

        return ventaRepository.save(venta);
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
        return ventaRepository.save(venta);
    }

    /* ===================== CANCELAR (SIN COBRO) ===================== */

    @Transactional
    public Venta cancelarVenta(Long ventaId, Usuario usuario) {

        Venta venta = ventaRepository.findById(ventaId)
                .orElseThrow(() -> new BadRequestException("Venta no encontrada"));

        if (venta.getEstado() == EstadoVenta.DESPACHADA) {
            throw new BadRequestException("Use ANULAR para ventas despachadas");
        }

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
}
