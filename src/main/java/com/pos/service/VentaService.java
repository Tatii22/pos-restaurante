package com.pos.service;
import com.pos.dto.venta.VentaCreateDTO;
import com.pos.dto.venta.VentaDetalleCreateDTO;
import com.pos.entity.*;
import com.pos.exception.BadRequestException;
import com.pos.repository.ProductoRepository;
import com.pos.repository.TurnoCajaRepository;
import com.pos.repository.VentaRepository;
import com.pos.repository.InventarioDiarioRepository;
import jakarta.transaction.Transactional;
import com.pos.repository.MenuDiarioRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;



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

        MenuDiario menuActivo = menuDiarioRepository
                .findByFechaAndActivoTrue(LocalDate.now())
                .orElseThrow(() -> new RuntimeException("No hay menú activo hoy"));

        if (!usuario.getRol().getNombre().equals("CAJA")) {
            throw new BadRequestException("Solo CAJA puede registrar ventas");
        }

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

        for (VentaDetalleCreateDTO d : dto.detalles()) {

            Producto producto = productoRepository.findById(d.productoId())
                    .orElseThrow(() -> new BadRequestException("Producto no existe"));

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

            InventarioDiario inv = inventarioDiarioRepository
                    .findByProductoAndMenuDiario(producto, menuActivo)
                    .orElseThrow(() -> new RuntimeException("Producto no está en inventario"));

            if (inv.getStockActual() < d.cantidad()) {
                throw new RuntimeException("Stock insuficiente para " + producto.getNombre());
            }

            inv.setStockActual(inv.getStockActual() - d.cantidad());

            if (inv.getStockActual() < inv.getStockMinimo()) {
                inv.setAgotado(true);
            }

            inventarioDiarioRepository.save(inv);
        }

        venta.setTotal(total);
        venta.setDetalles(detalles);

        // 🔥 AQUÍ ESTÁ LA CLAVE
        turno.setTotalVentas(
                turno.getTotalVentas().add(total)
        );

        turnoCajaRepository.save(turno);

        return ventaRepository.save(venta);
    }

}

