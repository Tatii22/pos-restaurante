package com.pos.repository;

import com.pos.entity.AbonoFiado;
import com.pos.entity.Deudor;
import com.pos.entity.TurnoCaja;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AbonoFiadoRepository extends JpaRepository<AbonoFiado, Long> {
    List<AbonoFiado> findByDeudorOrderByFechaDesc(Deudor deudor);
    List<AbonoFiado> findByTurnoOrderByFechaAsc(TurnoCaja turno);
    List<AbonoFiado> findByFechaBetweenOrderByFechaAsc(LocalDateTime inicio, LocalDateTime fin);
}
