package com.pos.service;


import com.pos.entity.*;
import com.pos.exception.BadRequestException;
import com.pos.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;



import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class InventarioDiarioService {

    private final InventarioDiarioRepository inventarioRepository;
    private final MenuDiarioRepository menuDiarioRepository;
    private final ProductoService productoService;
    private final FechaOperativaService fechaOperativaService;
    private final AuditService auditService;
    private final ActorResolver actorResolver;

    public InventarioDiario crearInventario(
            Long productoId,
            Integer stockInicial
    ) {
        LocalDate hoy = fechaOperativaService.obtenerFechaOperativa();

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

        InventarioDiario guardado = inventarioRepository.save(inv);
        auditService.record(
                "INVENTARIO_CREADO",
                "InventarioDiario",
                guardado.getId(),
                actorResolver.currentActorOrNull(),
                null,
                null,
                auditService.change("stockInicial", null, guardado.getStockInicial()),
                auditService.change("stockActual", null, guardado.getStockActual())
        );
        return guardado;
    }

    public List<InventarioDiario> listarHoy() {

        MenuDiario menu = menuDiarioRepository
                .findByFechaAndActivoTrue(fechaOperativaService.obtenerFechaOperativa())
                .orElse(null);

        if (menu == null) {
            return List.of();
        }

        return inventarioRepository.findByMenuDiarioWithProducto(menu);
    }


    @Transactional
    public InventarioDiario reabastecer(Long id, Integer cantidad) {
        if (cantidad == null || cantidad <= 0) {
            throw new BadRequestException("La cantidad a reabastecer debe ser mayor a cero");
        }

        InventarioDiario inv = inventarioRepository.findByIdWithProducto(id)
                .orElseThrow(() ->
                        new BadRequestException("Inventario no encontrado")
                );

        Integer stockAnterior = inv.getStockActual();
        inv.setStockActual(inv.getStockActual() + cantidad);

        inv.setAgotado(inv.getStockActual() <= 0);


        InventarioDiario guardado = inventarioRepository.save(inv);
        auditService.record(
                "INVENTARIO_REABASTECIDO",
                "InventarioDiario",
                guardado.getId(),
                actorResolver.currentActorOrNull(),
                null,
                null,
                auditService.change("stockActual", stockAnterior, guardado.getStockActual()),
                auditService.change("cantidad", null, cantidad)
        );
        return guardado;
    }
}



