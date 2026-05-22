package com.pos.service;

import com.pos.entity.*;
import com.pos.repository.MovimientoFinancieroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MovimientoFinancieroService {

    public static final List<MovimientoFinancieroTipo> TIPOS_INGRESO_COBRADO = List.of(
            MovimientoFinancieroTipo.VENTA_CONTADO,
            MovimientoFinancieroTipo.ABONO_FIADO,
            MovimientoFinancieroTipo.ANULACION_VENTA
    );

    private final MovimientoFinancieroRepository repository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void registrarVentaContado(Venta venta, Usuario usuario, BigDecimal efectivo, BigDecimal transferencia) {
        registrar(venta.getFecha(), MovimientoFinancieroTipo.VENTA_CONTADO, MedioFinanciero.EFECTIVO, efectivo, 1,
                venta.getTurno(), venta, null, null, null, usuario, "Venta contado efectivo");
        registrar(venta.getFecha(), MovimientoFinancieroTipo.VENTA_CONTADO, MedioFinanciero.TRANSFERENCIA, transferencia, 1,
                venta.getTurno(), venta, null, null, null, usuario, "Venta contado transferencia");
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void registrarVentaFiada(Venta venta, Usuario usuario) {
        registrar(venta.getFecha(), MovimientoFinancieroTipo.VENTA_FIADA, MedioFinanciero.CARTERA, venta.getTotal(), 1,
                venta.getTurno(), venta, null, null, null, usuario, "Venta fiada");
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void registrarAbono(AbonoFiado abono) {
        registrar(abono.getFecha(), MovimientoFinancieroTipo.ABONO_FIADO, MedioFinanciero.EFECTIVO, abono.getMontoEfectivo(), 1,
                abono.getTurno(), null, abono, null, null, abono.getUsuario(), "Abono fiado efectivo");
        registrar(abono.getFecha(), MovimientoFinancieroTipo.ABONO_FIADO, MedioFinanciero.TRANSFERENCIA, abono.getMontoTransferencia(), 1,
                abono.getTurno(), null, abono, null, null, abono.getUsuario(), "Abono fiado transferencia");
        registrar(abono.getFecha(), MovimientoFinancieroTipo.ABONO_FIADO, MedioFinanciero.CARTERA, abono.getMonto(), -1,
                abono.getTurno(), null, abono, null, null, abono.getUsuario(), "Reduccion cartera por abono");
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void registrarGastoCaja(GastoCaja gasto) {
        registrar(gasto.getFecha(), MovimientoFinancieroTipo.GASTO_CAJA, MedioFinanciero.EFECTIVO, gasto.getMontoEfectivo(), -1,
                gasto.getTurno(), null, null, gasto, null, gasto.getUsuario(), "Gasto caja efectivo");
        registrar(gasto.getFecha(), MovimientoFinancieroTipo.GASTO_CAJA, MedioFinanciero.TRANSFERENCIA, gasto.getMontoTransferencia(), -1,
                gasto.getTurno(), null, null, gasto, null, gasto.getUsuario(), "Gasto caja transferencia");
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void registrarGastoAdmin(GastoAdmin gasto) {
        LocalDateTime fecha = gasto.getFecha().atStartOfDay();
        registrar(fecha, MovimientoFinancieroTipo.GASTO_ADMIN, MedioFinanciero.EFECTIVO, gasto.getMontoEfectivo(), -1,
                null, null, null, null, gasto, gasto.getUsuario(), "Gasto administrativo efectivo");
        registrar(fecha, MovimientoFinancieroTipo.GASTO_ADMIN, MedioFinanciero.TRANSFERENCIA, gasto.getMontoTransferencia(), -1,
                null, null, null, null, gasto, gasto.getUsuario(), "Gasto administrativo transferencia");
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void registrarEliminacionGastoCaja(GastoCaja gasto, Usuario usuario) {
        registrar(LocalDateTime.now(), MovimientoFinancieroTipo.ELIMINACION_GASTO_CAJA, MedioFinanciero.EFECTIVO, gasto.getMontoEfectivo(), 1,
                gasto.getTurno(), null, null, gasto, null, usuario, "Reverso eliminacion gasto caja efectivo");
        registrar(LocalDateTime.now(), MovimientoFinancieroTipo.ELIMINACION_GASTO_CAJA, MedioFinanciero.TRANSFERENCIA, gasto.getMontoTransferencia(), 1,
                gasto.getTurno(), null, null, gasto, null, usuario, "Reverso eliminacion gasto caja transferencia");
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void registrarEliminacionGastoAdmin(GastoAdmin gasto, Usuario usuario) {
        registrar(LocalDateTime.now(), MovimientoFinancieroTipo.ELIMINACION_GASTO_ADMIN, MedioFinanciero.EFECTIVO, gasto.getMontoEfectivo(), 1,
                null, null, null, null, gasto, usuario, "Reverso eliminacion gasto admin efectivo");
        registrar(LocalDateTime.now(), MovimientoFinancieroTipo.ELIMINACION_GASTO_ADMIN, MedioFinanciero.TRANSFERENCIA, gasto.getMontoTransferencia(), 1,
                null, null, null, null, gasto, usuario, "Reverso eliminacion gasto admin transferencia");
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void registrarAnulacionVenta(Venta venta, Usuario usuario, BigDecimal efectivo, BigDecimal transferencia) {
        registrar(LocalDateTime.now(), MovimientoFinancieroTipo.ANULACION_VENTA, MedioFinanciero.EFECTIVO, efectivo, -1,
                venta.getTurno(), venta, null, null, null, usuario, "Anulacion venta efectivo");
        registrar(LocalDateTime.now(), MovimientoFinancieroTipo.ANULACION_VENTA, MedioFinanciero.TRANSFERENCIA, transferencia, -1,
                venta.getTurno(), venta, null, null, null, usuario, "Anulacion venta transferencia");
    }

    @Transactional(readOnly = true)
    public List<MovimientoFinanciero> porTurno(TurnoCaja turno) {
        return repository.findByTurnoOrderByFechaAsc(turno);
    }

    @Transactional(readOnly = true)
    public List<MovimientoFinanciero> porPeriodo(LocalDateTime inicio, LocalDateTime fin) {
        return repository.findByFechaBetweenOrderByFechaAsc(inicio, fin);
    }

    @Transactional(readOnly = true)
    public BigDecimal sumarTurnoMedioTipos(TurnoCaja turno, MedioFinanciero medio, Collection<MovimientoFinancieroTipo> tipos) {
        return repository.sumarNetoPorTurnoMedioTipos(turno, medio, tipos);
    }

    @Transactional(readOnly = true)
    public BigDecimal sumarPeriodoMedioTipos(LocalDateTime inicio, LocalDateTime fin, MedioFinanciero medio, Collection<MovimientoFinancieroTipo> tipos) {
        return repository.sumarNetoPorPeriodoMedioTipos(inicio, fin, medio, tipos);
    }

    private void registrar(
            LocalDateTime fecha,
            MovimientoFinancieroTipo tipo,
            MedioFinanciero medio,
            BigDecimal monto,
            int direccion,
            TurnoCaja turno,
            Venta venta,
            AbonoFiado abono,
            GastoCaja gastoCaja,
            GastoAdmin gastoAdmin,
            Usuario usuario,
            String descripcion
    ) {
        BigDecimal valor = safe(monto);
        if (valor.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        repository.save(MovimientoFinanciero.builder()
                .fecha(fecha != null ? fecha : LocalDateTime.now())
                .tipo(tipo)
                .medio(medio)
                .monto(valor)
                .direccion(direccion < 0 ? -1 : 1)
                .turno(turno)
                .venta(venta)
                .abonoFiado(abono)
                .gastoCaja(gastoCaja)
                .gastoAdmin(gastoAdmin)
                .usuario(usuario)
                .descripcion(descripcion)
                .build());
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : value;
    }
}
