package com.pos.repository;





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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select i
            from InventarioDiario i
            where i.producto = :producto
              and i.menuDiario = :menuDiario
            """)
    Optional<InventarioDiario> findByProductoAndMenuDiarioForUpdate(
            @Param("producto") Producto producto,
            @Param("menuDiario") MenuDiario menuDiario
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


