package com.pos.service;

import com.pos.entity.MedioFinanciero;
import com.pos.entity.MovimientoFinancieroTipo;
import com.pos.entity.TurnoCaja;
import com.pos.repository.MovimientoFinancieroRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CalculosFinancierosService {

    public static final List<MovimientoFinancieroTipo> TIPOS_RECAUDO = List.of(
            MovimientoFinancieroTipo.VENTA_CONTADO,
            MovimientoFinancieroTipo.ABONO_FIADO,
            MovimientoFinancieroTipo.ABONO_CON_VENTA_FIADA,
            MovimientoFinancieroTipo.ANULACION_VENTA
    );

    public static final List<MovimientoFinancieroTipo> TIPOS_GASTO_CAJA = List.of(
            MovimientoFinancieroTipo.GASTO_CAJA,
            MovimientoFinancieroTipo.ELIMINACION_GASTO_CAJA
    );

    public static final List<MovimientoFinancieroTipo> TIPOS_GASTO_ADMIN = List.of(
            MovimientoFinancieroTipo.GASTO_ADMIN,
            MovimientoFinancieroTipo.ELIMINACION_GASTO_ADMIN
    );

    public static final List<MovimientoFinancieroTipo> TIPOS_RECAUDO_TURNO = List.of(
            MovimientoFinancieroTipo.VENTA_CONTADO,
            MovimientoFinancieroTipo.ABONO_CON_VENTA_FIADA,
            MovimientoFinancieroTipo.ANULACION_VENTA
    );

    public static final List<MedioFinanciero> MEDIOS_EFECTIVO_TRANSFERENCIA = List.of(
            MedioFinanciero.EFECTIVO,
            MedioFinanciero.TRANSFERENCIA
    );

    private final MovimientoFinancieroRepository repository;

    public CalculosFinancierosService(MovimientoFinancieroRepository repository) {
        this.repository = repository;
    }

    public BigDecimal sumarRecaudoPorTurno(TurnoCaja turno, MedioFinanciero medio) {
        return repository.sumarNetoPorTurnoMedioTipos(turno, medio, TIPOS_RECAUDO);
    }

    public BigDecimal sumarRecaudoTurno(TurnoCaja turno, MedioFinanciero medio) {
        return repository.sumarNetoPorTurnoMedioTipos(turno, medio, TIPOS_RECAUDO_TURNO);
    }

    public BigDecimal sumarRecaudoTotal(List<TurnoCaja> turnos, MedioFinanciero medio) {
        BigDecimal total = BigDecimal.ZERO;
        for (TurnoCaja turno : turnos) {
            total = total.add(sumarRecaudoPorTurno(turno, medio));
        }
        return total;
    }

    public BigDecimal sumarGastosPorTurno(TurnoCaja turno, MedioFinanciero medio) {
        BigDecimal neto = repository.sumarNetoPorTurnoMedioTipos(turno, medio, TIPOS_GASTO_CAJA);
        return neto.negate();
    }

    public BigDecimal sumarGastosTotal(List<TurnoCaja> turnos, MedioFinanciero medio) {
        BigDecimal total = BigDecimal.ZERO;
        for (TurnoCaja turno : turnos) {
            total = total.add(sumarGastosPorTurno(turno, medio));
        }
        return total;
    }

    public BigDecimal sumarGastosAdminPeriodo(LocalDateTime inicio, LocalDateTime fin, MedioFinanciero medio) {
        BigDecimal neto = repository.sumarNetoPorPeriodoMedioTipos(inicio, fin, medio, TIPOS_GASTO_ADMIN);
        return neto.negate();
    }

}
