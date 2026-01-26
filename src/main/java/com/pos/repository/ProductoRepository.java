package com.pos.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pos.entity.Producto;
import com.pos.entity.TipoVentaProducto;

import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    List<Producto> findAllByActivoTrue();

    List<Producto> findAllByCategoriaIdAndActivoTrue(Long categoriaId);
    
    List<Producto> findByTipoVentaAndActivoTrue(TipoVentaProducto tipo);

}
