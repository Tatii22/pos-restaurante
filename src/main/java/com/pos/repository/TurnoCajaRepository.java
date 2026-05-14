package com.pos.repository;

import com.pos.entity.TurnoCaja;
import com.pos.entity.EstadoTurno;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

public interface TurnoCajaRepository extends JpaRepository<TurnoCaja, Long> {

    // 🔴 Regla clave: solo puede existir UN turno ABIERTO
    Optional<TurnoCaja> findByEstado(EstadoTurno estado);

    
    Optional<TurnoCaja> findByEstadoIn(List<EstadoTurno> estados);

    boolean existsByEstadoIn(List<EstadoTurno> estados);

    // 🟢 Para saber si ya hay turno abierto
    boolean existsByEstado(EstadoTurno estado);

    List<TurnoCaja> findByFechaAperturaBetweenOrderByFechaAperturaDesc(LocalDateTime inicio, LocalDateTime fin);
}
