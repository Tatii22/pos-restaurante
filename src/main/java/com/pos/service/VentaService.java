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

    @Transactional
    public Venta registrarVenta(VentaCreateDTO dto, Usuario usuario) {

        // 🔐 Validación de rol
        if (!usuario.getRol().getNombre().equals("CAJA")) {
            throw new BadRequestException("Solo CAJA puede registrar ventas");
        }

        // 🔐 Turno activo
        TurnoCaja turno = turnoCajaRepository
                .findByEstadoIn(List.of(EstadoTurno.ABIERTO, EstadoTurno.SIMULADO))
                .orElseThrow(() -> new BadRequestException("No hay turno activo"));

        Venta venta = new Venta();
        venta.setFecha(LocalDateTime.now());
        venta.setClienteNombre(dto.clienteNombre());
        venta.setFormaPago(dto.formaPago());
        venta.setUsuario(usuario);
        venta.setTurno(turno);

        BigDecimal total = BigDecimal.ZERO;
        List<VentaDetalle> detalles = new ArrayList<>();

        // 🟢 Solo se carga si hay productos de menú diario
        MenuDiario menuActivo = null;

        for (VentaDetalleCreateDTO d : dto.detalles()) {

            Producto producto = productoRepository.findById(d.productoId())
                    .orElseThrow(() -> new BadRequestException("Producto no existe"));

            // 🧠 Cargar menú diario solo si es necesario
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

            // ⚠️ Inventario SOLO para productos de menú diario
            if (producto.getTipoVenta() == TipoVentaProducto.MENU_DIARIO) {

                InventarioDiario inv = inventarioDiarioRepository
                        .findByProductoAndMenuDiario(producto, menuActivo)
                        .orElseThrow(() -> new BadRequestException(
                                "Producto " + producto.getNombre() + " no está en el menú diario"
                        ));

                if (inv.getStockActual() < d.cantidad()) {
                    throw new BadRequestException(
                            "Stock insuficiente para " + producto.getNombre()
                    );
                }

                inv.setStockActual(inv.getStockActual() - d.cantidad());

                if (inv.getStockActual() <= inv.getStockMinimo()) {
                    inv.setAgotado(true);
                }

                inventarioDiarioRepository.save(inv);
            }
        }

        venta.setTotal(total);
        venta.setDetalles(detalles);

        // 💰 Impacto en turno
        turno.setTotalVentas(
                turno.getTotalVentas().add(total)
        );

        turnoCajaRepository.save(turno);

        return ventaRepository.save(venta);
    }
}
