package com.pos.repository;

import com.pos.entity.TurnoCaja;
import com.pos.entity.EstadoTurno;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

public interface TurnoCajaRepository extends JpaRepository<TurnoCaja, Long> {

    // 🔴 Regla clave: solo puede existir UN turno ABIERTO
    Optional<TurnoCaja> findByEstado(EstadoTurno estado);

    
    Optional<TurnoCaja> findByEstadoIn(List<EstadoTurno> estados);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select t
            from TurnoCaja t
            where t.estado in :estados
            """)
    Optional<TurnoCaja> findByEstadoInForUpdate(List<EstadoTurno> estados);

    boolean existsByEstadoIn(List<EstadoTurno> estados);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select t
            from TurnoCaja t
            where t.id = :id
            """)
    Optional<TurnoCaja> findByIdForUpdate(Long id);

    // 🟢 Para saber si ya hay turno abierto
    boolean existsByEstado(EstadoTurno estado);

    List<TurnoCaja> findByFechaAperturaBetweenOrderByFechaAperturaDesc(LocalDateTime inicio, LocalDateTime fin);

    List<TurnoCaja> findByEstadoAndFechaCierreBetween(EstadoTurno estado, LocalDateTime inicio, LocalDateTime fin);

    /**
     * Turnos cerrados cuya apertura cae en el rango.
     * Criterio unificado con listarPorRango para que Dashboard y Turnos
     * sumen exactamente los mismos turnos dado el mismo rango de fechas.
     */
    List<TurnoCaja> findByEstadoAndFechaAperturaBetween(EstadoTurno estado, LocalDateTime inicio, LocalDateTime fin);

    @Query(value = "SELECT COALESCE(MAX(numero_turno), 0) FROM turnos_caja WHERE YEAR(fecha_apertura) = :year AND MONTH(fecha_apertura) = :month", nativeQuery = true)
    Integer findMaxNumeroTurnoPorMes(@Param("year") int year, @Param("month") int month);
}
