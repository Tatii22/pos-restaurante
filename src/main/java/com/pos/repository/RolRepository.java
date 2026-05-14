package com.pos.repository;




public interface RolRepository extends JpaRepository<Rol, Long> {

    Optional<Rol> findByNombre(String nombre);

    boolean existsByNombreIgnoreCase(String nombre);
}

