package com.pos.repository;

import com.pos.entity.TipoGasto;
import org.springframework.data.jpa.repository.JpaRepository;


public interface TipoGastoRepository extends JpaRepository<TipoGasto, Long> {

    boolean existsByNombreIgnoreCase(String nombre);
}


