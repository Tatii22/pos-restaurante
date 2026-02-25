package com.pos.repository;

import com.pos.entity.InventarioDiario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.pos.entity.Producto;

import java.util.List;
import com.pos.entity.MenuDiario;


import java.util.Optional;
import java.time.LocalDate;

public interface InventarioDiarioRepository
        extends JpaRepository<InventarioDiario, Long> {

    @Query("""
            select i
            from InventarioDiario i
            join fetch i.producto p
            left join fetch p.categoria
            where i.id = :id
            """)
    Optional<InventarioDiario> findByIdWithProducto(Long id);

    Optional<InventarioDiario> findByProductoAndMenuDiario(
            Producto producto,
            MenuDiario menuDiario
    );

    boolean existsByProductoAndMenuDiario(
            Producto producto,
            MenuDiario menuDiario
    );

    List<InventarioDiario> findByMenuDiario(MenuDiario menuDiario);

    @Query("""
            select i
            from InventarioDiario i
            join fetch i.producto p
            left join fetch p.categoria
            where i.menuDiario = :menuDiario
            """)
    List<InventarioDiario> findByMenuDiarioWithProducto(MenuDiario menuDiario);

    void deleteByMenuDiario(MenuDiario menuDiario);

    void deleteByFecha(LocalDate fecha);
}

