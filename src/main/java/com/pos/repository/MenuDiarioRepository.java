package com.pos.repository;




public interface MenuDiarioRepository extends JpaRepository<MenuDiario, Long> {

    Optional<MenuDiario> findByFechaAndActivoTrue(LocalDate fecha);

    Optional<MenuDiario> findByFecha(LocalDate fecha);
}

