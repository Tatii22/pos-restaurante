package com.pos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.pos.entity.InventarioDiario;

import java.util.List;

public interface InventarioDiarioRepository extends JpaRepository<InventarioDiario, Long> {

    @Query("""
        SELECT i
        FROM InventarioDiario i
        WHERE i.menuDiario.id = :menuId
          AND i.agotado = false
    """)
    List<InventarioDiario> findDisponiblesByMenuId(Long menuId);

    @Query("""
        SELECT i
        FROM InventarioDiario i
        WHERE i.stockActual <= i.stockMinimo
          AND i.agotado = false
    """)
    List<InventarioDiario> findConStockBajo();
}
