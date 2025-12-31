package com.pos.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pos.entity.MenuDiario;

import java.time.LocalDate;
import java.util.Optional;

public interface MenuDiarioRepository extends JpaRepository<MenuDiario, Long> {

    Optional<MenuDiario> findByFechaAndActivoTrue(LocalDate fecha);
}
