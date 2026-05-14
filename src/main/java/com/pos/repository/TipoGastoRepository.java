package com.pos.repository;



public interface TipoGastoRepository extends JpaRepository<TipoGasto, Long> {

    boolean existsByNombreIgnoreCase(String nombre);
}



