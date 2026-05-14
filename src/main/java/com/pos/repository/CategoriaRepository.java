package com.pos.repository;




public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findAllByActivaTrue();

    Optional<Categoria> findByNombre(String nombre);

    boolean existsByNombreIgnoreCase(String nombre);
}

