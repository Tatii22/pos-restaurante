package com.pos.repository;

import com.pos.entity.EstadoVenta;
import com.pos.entity.GastoCaja;
import com.pos.entity.TurnoCaja;
import com.pos.entity.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface VentaRepository extends JpaRepository<Venta, Long> {

    @Query("""
        SELECT COALESCE(SUM(v.total), 0)
        FROM Venta v
        WHERE v.turno.id = :turnoId
    """)
    BigDecimal sumarTotalPorTurno(@Param("turnoId") Long turnoId);

    List<Venta> findByFechaBetweenAndEstadoIn(
            LocalDateTime inicio,
            LocalDateTime fin,
            List<EstadoVenta> estados
    );

    // Filtrar ventas despachadas por turno y rango de fechas
    List<Venta> findByTurnoAndEstadoAndFechaBetween(
            TurnoCaja turno,
            EstadoVenta estado,
            LocalDateTime inicio,
            LocalDateTime fin
    );
}
