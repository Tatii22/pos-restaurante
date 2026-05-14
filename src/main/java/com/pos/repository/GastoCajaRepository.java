package com.pos.repository;



public interface GastoCajaRepository extends JpaRepository<GastoCaja, Long> {

    List<GastoCaja> findByTurno(TurnoCaja turno);
    List<GastoCaja> findByTurnoOrderByFechaDesc(TurnoCaja turno);

    List<GastoCaja> findByTurnoAndFechaBetween(TurnoCaja turno, LocalDateTime inicio, LocalDateTime fin);

    // Para traer todos los gastos entre dos fechas sin importar el turno
    List<GastoCaja> findByFechaBetween(LocalDateTime inicio, LocalDateTime fin);

}


