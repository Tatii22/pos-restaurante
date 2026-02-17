package com.pos.service;


import com.pos.entity.*;
import com.pos.exception.BadRequestException;
import com.pos.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;



import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class InventarioDiarioService {

    private final InventarioDiarioRepository inventarioRepository;
    private final MenuDiarioRepository menuDiarioRepository;
    private final ProductoService productoService;

    public InventarioDiario crearInventario(
            Long productoId,
            Integer stockInicial
    ) {
        LocalDate hoy = LocalDate.now();

        MenuDiario menu = menuDiarioRepository
                .findByFechaAndActivoTrue(hoy)
                .orElseThrow(() ->
                        new BadRequestException("No hay menu activo hoy")
                );

        Producto producto = productoService.obtenerPorId(productoId);

        if (inventarioRepository.existsByProductoAndMenuDiario(producto, menu)) {
            throw new BadRequestException("Producto ya existe en el menu de hoy");
        }

        InventarioDiario inv = InventarioDiario.builder()
                .fecha(hoy)
                .producto(producto)
                .menuDiario(menu) // 🔥 OBLIGATORIO
                .stockInicial(stockInicial)
                .stockActual(stockInicial)
                // stockMinimo = 5 por default
                .agotado(stockInicial <= 0)
                .build();

        return inventarioRepository.save(inv);
    }

    public List<InventarioDiario> listarHoy() {

        MenuDiario menu = menuDiarioRepository
                .findByFechaAndActivoTrue(LocalDate.now())
                .orElse(null);

        if (menu == null) {
            return List.of();
        }

        return inventarioRepository.findByMenuDiario(menu);
    }


    public InventarioDiario reabastecer(Long id, Integer cantidad) {

        InventarioDiario inv = inventarioRepository.findById(id)
                .orElseThrow(() ->
                        new BadRequestException("Inventario no encontrado")
                );

        inv.setStockActual(inv.getStockActual() + cantidad);

        inv.setAgotado(inv.getStockActual() <= 0);


        return inventarioRepository.save(inv);
    }
}



