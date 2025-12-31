package com.pos.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pos.entity.Categoria;

import java.util.List;
import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findAllByActivaTrue();

    Optional<Categoria> findByNombre(String nombre);
}
