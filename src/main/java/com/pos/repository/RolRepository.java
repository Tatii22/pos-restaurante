package com.pos.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pos.entity.Rol;

import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Long> {

    Optional<Rol> findByNombre(String nombre);
}
