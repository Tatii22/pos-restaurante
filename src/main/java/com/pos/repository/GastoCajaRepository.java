package com.pos.repository;

import com.pos.entity.GastoCaja;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import com.pos.entity.TurnoCaja;

public interface GastoCajaRepository extends JpaRepository<GastoCaja, Long> {

    List<GastoCaja> findByTurno(TurnoCaja turno);

    List<GastoCaja> findByTurnoAndFechaBetween(TurnoCaja turno, LocalDateTime inicio, LocalDateTime fin);

    // Para traer todos los gastos entre dos fechas sin importar el turno
    List<GastoCaja> findByFechaBetween(LocalDateTime inicio, LocalDateTime fin);

}

