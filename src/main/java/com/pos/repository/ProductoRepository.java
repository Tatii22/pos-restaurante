package com.pos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pos.entity.Producto;
import com.pos.entity.TipoVentaProducto;

import java.util.List;

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
