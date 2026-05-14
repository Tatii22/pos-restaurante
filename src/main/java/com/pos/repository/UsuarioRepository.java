package com.pos.repository;



public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsername(String username);

    boolean existsByRol_Nombre(String rolNombre);
}

