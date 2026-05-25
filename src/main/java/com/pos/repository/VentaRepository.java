package com.pos.repository;

import com.pos.entity.EstadoVenta;
import com.pos.entity.CondicionPago;
import com.pos.entity.Cliente;
import com.pos.entity.TipoVenta;
import com.pos.entity.TurnoCaja;
import com.pos.entity.Venta;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface VentaRepository extends JpaRepository<Venta, Long>, JpaSpecificationExecutor<Venta> {

    @Query("""
        select distinct v
        from Venta v
        left join fetch v.detalles d
        left join fetch d.producto
        where v.id = :id
    """)
    java.util.Optional<Venta> findByIdWithDetalles(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select distinct v
        from Venta v
        left join fetch v.detalles d
        left join fetch d.producto
        where v.id = :id
    """)
    java.util.Optional<Venta> findByIdWithDetallesForUpdate(@Param("id") Long id);

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

    List<Venta> findByClienteAndEstadoAndSaldoPendienteGreaterThanOrderByFechaAsc(
            Cliente cliente,
            EstadoVenta estado,
            BigDecimal saldoPendiente
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select v
        from Venta v
        where v.cliente = :cliente
          and v.estado = :estado
          and v.saldoPendiente > :saldoPendiente
        order by v.fecha asc
    """)
    List<Venta> findPendientesByClienteForUpdate(
            @Param("cliente") Cliente cliente,
            @Param("estado") EstadoVenta estado,
            @Param("saldoPendiente") BigDecimal saldoPendiente
    );

    List<Venta> findByClienteOrderByFechaDesc(Cliente cliente);

    @Query("""
        SELECT COALESCE(SUM(v.saldoPendiente), 0)
        FROM Venta v
        WHERE v.cliente = :cliente
          AND v.condicionPago = :condicionPago
          AND v.estado = :estado
          AND v.saldoPendiente > 0
    """)
    BigDecimal sumarSaldoPendientePorCliente(
            @Param("cliente") Cliente cliente,
            @Param("condicionPago") CondicionPago condicionPago,
            @Param("estado") EstadoVenta estado
    );

    /**
     * Devuelve deuda total y cantidad de ventas pendientes agrupadas por cliente,
     * en una sola consulta. Elimina el problema N+1 de listarClientes.
     * Cada elemento del resultado es un Object[] con:
     *   [0] = cliente.id (Long)
     *   [1] = SUM(saldoPendiente) (BigDecimal)
     *   [2] = COUNT(*) (Long)
     */
    @Query("""
        SELECT v.cliente.id,
               COALESCE(SUM(v.saldoPendiente), 0),
               COUNT(v)
        FROM Venta v
        WHERE v.condicionPago = com.pos.entity.CondicionPago.FIADO
          AND v.estado       = com.pos.entity.EstadoVenta.DESPACHADA
          AND v.saldoPendiente > 0
        GROUP BY v.cliente.id
    """)
    List<Object[]> sumarDeudaAgrupadaPorCliente();
}
