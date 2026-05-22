package com.pos.repository;

import com.pos.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface MovimientoFinancieroRepository extends JpaRepository<MovimientoFinanciero, Long> {

    List<MovimientoFinanciero> findByTurnoOrderByFechaAsc(TurnoCaja turno);

    List<MovimientoFinanciero> findByFechaBetweenOrderByFechaAsc(LocalDateTime inicio, LocalDateTime fin);

    boolean existsByVentaAndTipo(Venta venta, MovimientoFinancieroTipo tipo);

    @Query("""
            select coalesce(sum(m.monto * m.direccion), 0)
            from MovimientoFinanciero m
            where m.turno = :turno
              and m.medio = :medio
            """)
    BigDecimal sumarNetoPorTurnoYMedio(@Param("turno") TurnoCaja turno, @Param("medio") MedioFinanciero medio);

    @Query("""
            select coalesce(sum(m.monto * m.direccion), 0)
            from MovimientoFinanciero m
            where m.turno = :turno
              and m.medio = :medio
              and m.tipo in :tipos
            """)
    BigDecimal sumarNetoPorTurnoMedioTipos(
            @Param("turno") TurnoCaja turno,
            @Param("medio") MedioFinanciero medio,
            @Param("tipos") Collection<MovimientoFinancieroTipo> tipos
    );

    @Query("""
            select coalesce(sum(m.monto * m.direccion), 0)
            from MovimientoFinanciero m
            where m.fecha between :inicio and :fin
              and m.medio = :medio
              and m.tipo in :tipos
            """)
    BigDecimal sumarNetoPorPeriodoMedioTipos(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin,
            @Param("medio") MedioFinanciero medio,
            @Param("tipos") Collection<MovimientoFinancieroTipo> tipos
    );
}
