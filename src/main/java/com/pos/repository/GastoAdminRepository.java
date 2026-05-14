package com.pos.repository;



public interface GastoAdminRepository extends JpaRepository<GastoAdmin, Long> {

    List<GastoAdmin> findByFecha(LocalDate fecha);

    List<GastoAdmin> findByTipoId(Long tipoId);

    List<GastoAdmin> findByFechaBetween(
            LocalDate fechaInicio,
            LocalDate fechaFin
    );
}

