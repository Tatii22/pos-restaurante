package com.pos.repository;



public interface VentaRepository extends JpaRepository<Venta, Long>, JpaSpecificationExecutor<Venta> {

    @Query("""
        select distinct v
        from Venta v
        left join fetch v.detalles d
        left join fetch d.producto
        where v.id = :id
    """)
    java.util.Optional<Venta> findByIdWithDetalles(@Param("id") Long id);

    @Query("""
        SELECT COALESCE(SUM(v.total), 0)
        FROM Venta v
        WHERE v.turno.id = :turnoId
          AND v.estado = :estado
    """)
    BigDecimal sumarTotalPorTurnoPorEstado(
            @Param("turnoId") Long turnoId,
            @Param("estado") EstadoVenta estado
    );

    List<Venta> findByFechaBetweenAndEstadoIn(
            LocalDateTime inicio,
            LocalDateTime fin,
            List<EstadoVenta> estados
    );

    List<Venta> findByTurnoAndEstadoAndFechaBetween(
            TurnoCaja turno,
            EstadoVenta estado,
            LocalDateTime inicio,
            LocalDateTime fin
    );

    boolean existsByTurnoAndTipoVentaAndEstado(
            TurnoCaja turno,
            TipoVenta tipoVenta,
            EstadoVenta estado
    );
}

