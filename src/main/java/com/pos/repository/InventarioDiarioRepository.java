package com.pos.repository;

import com.pos.entity.InventarioDiario;
import org.springframework.data.jpa.repository.JpaRepository;
import com.pos.entity.Producto;

import java.util.List;
import com.pos.entity.MenuDiario;


import java.util.Optional;

public interface InventarioDiarioRepository
        extends JpaRepository<InventarioDiario, Long> {

    Optional<InventarioDiario> findByProductoAndMenuDiario(
            Producto producto,
            MenuDiario menuDiario
    );

    boolean existsByProductoAndMenuDiario(
            Producto producto,
            MenuDiario menuDiario
    );

    List<InventarioDiario> findByMenuDiario(MenuDiario menuDiario);
}

