package com.pos.repository;



public interface TurnoCajaRepository extends JpaRepository<TurnoCaja, Long> {

    // 🔴 Regla clave: solo puede existir UN turno ABIERTO
    Optional<TurnoCaja> findByEstado(EstadoTurno estado);

    
    Optional<TurnoCaja> findByEstadoIn(List<EstadoTurno> estados);

    boolean existsByEstadoIn(List<EstadoTurno> estados);

    // 🟢 Para saber si ya hay turno abierto
    boolean existsByEstado(EstadoTurno estado);

    List<TurnoCaja> findByFechaAperturaBetweenOrderByFechaAperturaDesc(LocalDateTime inicio, LocalDateTime fin);
}

