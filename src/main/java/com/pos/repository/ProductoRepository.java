package com.pos.repository;




public interface ProductoRepository extends JpaRepository<Producto, Long> {

    @Query("select p from Producto p left join fetch p.categoria")
    List<Producto> findAllWithCategoria();

    @Query("""
            select p
            from Producto p
            left join fetch p.categoria
            where p.tipoVenta = :tipo and p.activo = true
            """)
    List<Producto> findByTipoVentaAndActivoTrueWithCategoria(@Param("tipo") TipoVentaProducto tipo);

    List<Producto> findAllByActivoTrue();

    List<Producto> findAllByCategoriaIdAndActivoTrue(Long categoriaId);
    
    List<Producto> findByTipoVentaAndActivoTrue(TipoVentaProducto tipo);

}

