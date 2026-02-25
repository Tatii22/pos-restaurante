package com.pos.service;

import com.pos.dto.producto.ProductoVentaDTO;
import com.pos.dto.producto.ProductosVentaHoyDTO;
import com.pos.entity.InventarioDiario;
import com.pos.entity.MenuDiario;
import com.pos.entity.Producto;
import com.pos.entity.TipoVentaProducto;
import com.pos.repository.InventarioDiarioRepository;
import com.pos.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.pos.exception.ResourceNotFoundException;
import com.pos.exception.BadRequestException;
import com.pos.repository.MenuDiarioRepository;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final InventarioDiarioRepository inventarioDiarioRepository;
    private final MenuDiarioRepository menuDiarioRepository;

    public Producto crear(Producto producto) {
        if (producto == null) {
            throw new BadRequestException("Producto no puede ser nulo");
        }
        return productoRepository.save(producto);
    }

    public List<Producto> listar() {
        return productoRepository.findAllWithCategoria();
    }

    public Producto obtenerPorId(Long id) {
        if (id == null) {
            throw new BadRequestException("ID no puede ser nulo");
        }
        return productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
    }


    public Producto actualizar(Long id, Producto producto) {
        Producto existente = obtenerPorId(id);
        existente.setNombre(producto.getNombre());
        existente.setPrecio(producto.getPrecio());
        existente.setActivo(producto.getActivo());
        existente.setCategoria(producto.getCategoria());
        existente.setTipoVenta(producto.getTipoVenta());
        return productoRepository.save(existente);
    }

    public void eliminar(Long id) {
        if (id == null) {
            throw new BadRequestException("ID no puede ser nulo");
        }
        productoRepository.deleteById(id);
    }

   public ProductosVentaHoyDTO productosVentaHoy() {

        // 1️⃣ Menú activo hoy
        MenuDiario menuActivo = menuDiarioRepository
                .findByFechaAndActivoTrue(LocalDate.now())
                .orElse(null);

        // 2️⃣ Inventario del menú (si existe)
        List<ProductoVentaDTO> menuDiario = List.of();

        if (menuActivo != null) {
            menuDiario = inventarioDiarioRepository
                    .findByMenuDiarioWithProducto(menuActivo)
                    .stream()
                    .map(inv -> new ProductoVentaDTO(
                            inv.getProducto().getId(),
                            inv.getProducto().getNombre(),
                            inv.getProducto().getPrecio(),
                            inv.getAgotado(),
                            inv.getProducto().getCategoria() != null
                                    ? inv.getProducto().getCategoria().getNombre()
                                    : "Sin categoria"
                    ))
                    .toList();
        }

        // 3️⃣ Siempre disponibles
        List<ProductoVentaDTO> siempreDisponibles =
                productoRepository
                        .findByTipoVentaAndActivoTrueWithCategoria(
                                TipoVentaProducto.SIEMPRE_DISPONIBLE
                        )
                        .stream()
                        .map(p -> new ProductoVentaDTO(
                                p.getId(),
                                p.getNombre(),
                                p.getPrecio(),
                                false,
                                p.getCategoria() != null
                                        ? p.getCategoria().getNombre()
                                        : "Sin categoria"
                        ))
                        .toList();

        return ProductosVentaHoyDTO.builder()
                .menuDiario(menuDiario)
                .siempreDisponibles(siempreDisponibles)
                .build();
    }

    
}
