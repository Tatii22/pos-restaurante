package com.pos.service;

import com.pos.entity.EstadoTurno;
import com.pos.entity.EstadoVenta;
import com.pos.entity.MedioFinanciero;
import com.pos.entity.TipoVenta;
import com.pos.entity.TurnoCaja;
import com.pos.entity.Usuario;
import com.pos.exception.BadRequestException;
import com.pos.repository.TurnoCajaRepository;
import com.pos.repository.GastoAdminRepository;
import com.pos.repository.UsuarioRepository;
import com.pos.repository.VentaRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TurnoCajaService {

    private final TurnoCajaRepository turnoCajaRepository;
    private final UsuarioRepository usuarioRepository;
    private final VentaRepository ventaRepository;
    private final GastoAdminRepository gastoAdminRepository;
    private final CalculosFinancierosService calculosFinancierosService;
    private final AuditService auditService;
    private final MenuDiarioService menuDiarioService;
    @Transactional
    public synchronized TurnoCaja abrirTurno(BigDecimal montoInicial, String username) {

        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("Usuario no existe"));

        if (!usuario.getRol().getNombre().equals("CAJA")) {
            throw new BadRequestException("Solo un usuario CAJA puede abrir turno");
        }

        if (turnoCajaRepository.existsByEstadoIn(
                List.of(EstadoTurno.ABIERTO, EstadoTurno.SIMULADO))) {
            throw new BadRequestException("Ya existe un turno activo");
        }
        if (montoInicial == null || montoInicial.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("El monto inicial no puede ser negativo");
        }
        TurnoCaja turno = TurnoCaja.builder()
                .fechaApertura(LocalDateTime.now())
                .montoInicial(montoInicial)
                .totalVentas(BigDecimal.ZERO)
                .totalGastos(BigDecimal.ZERO)
                .estado(EstadoTurno.ABIERTO)
                .usuario(usuario)
                .build();

        TurnoCaja guardado = turnoCajaRepository.save(turno);
        menuDiarioService.reiniciarMenuParaNuevoTurno(usuario);
        decorarMetricasCierre(guardado);
        auditService.record(
                "TURNO_ABIERTO",
                "TurnoCaja",
                guardado.getId(),
                usuario,
                guardado,
                null,
                auditService.change("montoInicial", null, guardado.getMontoInicial()),
                auditService.change("estado", null, guardado.getEstado())
        );
        return guardado;
    }

    @Transactional
    public TurnoCaja simularCierre(BigDecimal efectivoContado, BigDecimal transferenciasVerificadas, Usuario usuario) {

        if (!usuario.getRol().getNombre().equals("CAJA")) {
            throw new BadRequestException("Solo CAJA puede simular cierre");
        }

        TurnoCaja turno = turnoCajaRepository
                .findByEstadoInForUpdate(List.of(EstadoTurno.ABIERTO, EstadoTurno.SIMULADO))
                .orElseThrow(() -> new BadRequestException("No hay turno abierto"));

        decorarMetricasCierre(turno);
        BigDecimal esperadoFisico       = turno.getEsperado();
        BigDecimal transferenciasNetas   = turno.getTransferenciasNetas();
        BigDecimal totalOperativo       = turno.getTotalOperativoTurno();

        BigDecimal diferenciaEfectivo = efectivoContado.subtract(esperadoFisico);
        BigDecimal diferenciaTransferencias = transferenciasVerificadas.subtract(transferenciasNetas);
        BigDecimal totalVerificado = efectivoContado.add(transferenciasVerificadas);
        BigDecimal diferenciaTotal = totalVerificado.subtract(totalOperativo);

        BigDecimal faltanteFisico = diferenciaEfectivo;

        turno.setFaltante(faltanteFisico);

        turno.setEfectivoContado(efectivoContado);
        turno.setTransferenciasVerificadas(transferenciasVerificadas);
        turno.setDiferenciaEfectivo(diferenciaEfectivo);
        turno.setDiferenciaTransferencias(diferenciaTransferencias);
        turno.setTotalVerificado(totalVerificado);
        turno.setDiferenciaTotal(diferenciaTotal);

        turno.setEstado(EstadoTurno.ABIERTO);

        TurnoCaja guardado = turnoCajaRepository.save(turno);
        decorarMetricasCierre(guardado);

        auditService.record(
                "TURNO_CIERRE_SIMULADO",
                "TurnoCaja",
                guardado.getId(),
                usuario,
                guardado,
                null,
                auditService.change("efectivoContado", null, efectivoContado),
                auditService.change("transferenciasVerificadas", null, transferenciasVerificadas),
                auditService.change("diferenciaEfectivo", null, diferenciaEfectivo),
                auditService.change("diferenciaTransferencias", null, diferenciaTransferencias),
                auditService.change("diferenciaTotal", null, diferenciaTotal)
        );
        return guardado;
    }

    @Transactional
    public TurnoCaja cerrarTurno(BigDecimal efectivoContado, BigDecimal transferenciasVerificadas, Usuario usuario) {

        if (!usuario.getRol().getNombre().equals("CAJA")) {
            throw new BadRequestException("Solo CAJA puede cerrar turno");
        }

        TurnoCaja turno = turnoCajaRepository
                .findByEstadoInForUpdate(List.of(EstadoTurno.ABIERTO, EstadoTurno.SIMULADO))
                .orElseThrow(() -> new BadRequestException("No hay turno para cerrar"));

        boolean hayDomiciliosPendientes = ventaRepository.existsByTurnoAndTipoVentaAndEstado(
                turno, TipoVenta.DOMICILIO, EstadoVenta.EN_PROCESO
        );
        if (hayDomiciliosPendientes) {
            throw new BadRequestException("No puedes cerrar turno: hay domicilios del turno pendientes por despachar");
        }

        if (efectivoContado == null || efectivoContado.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("El efectivo físico contado no puede ser negativo");
        }
        if (transferenciasVerificadas == null || transferenciasVerificadas.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Las transferencias verificadas no pueden ser negativas");
        }

        decorarMetricasCierre(turno);
        BigDecimal esperadoFisico     = turno.getEsperado();
        BigDecimal transferenciasNetas = turno.getTransferenciasNetas();
        BigDecimal totalOperativo     = turno.getTotalOperativoTurno();

        BigDecimal diferenciaEfectivo = efectivoContado.subtract(esperadoFisico);
        BigDecimal diferenciaTransferencias = transferenciasVerificadas.subtract(transferenciasNetas);
        BigDecimal totalVerificado = efectivoContado.add(transferenciasVerificadas);
        BigDecimal diferenciaTotal = totalVerificado.subtract(totalOperativo);

        BigDecimal faltanteFisico = diferenciaEfectivo;

        turno.setMontoFinal(efectivoContado);
        turno.setFaltante(faltanteFisico);

        turno.setEfectivoContado(efectivoContado);
        turno.setTransferenciasVerificadas(transferenciasVerificadas);
        turno.setDiferenciaEfectivo(diferenciaEfectivo);
        turno.setDiferenciaTransferencias(diferenciaTransferencias);
        turno.setTotalVerificado(totalVerificado);
        turno.setDiferenciaTotal(diferenciaTotal);

        turno.setFechaCierre(LocalDateTime.now());
        turno.setEstado(EstadoTurno.CERRADO);

        TurnoCaja guardado = turnoCajaRepository.save(turno);
        decorarMetricasCierre(guardado);

        auditService.record(
                "TURNO_CERRADO",
                "TurnoCaja",
                guardado.getId(),
                usuario,
                guardado,
                null,
                auditService.change("estado", EstadoTurno.ABIERTO, EstadoTurno.CERRADO),
                auditService.change("efectivoContado", null, efectivoContado),
                auditService.change("transferenciasVerificadas", null, transferenciasVerificadas),
                auditService.change("diferenciaEfectivo", null, diferenciaEfectivo),
                auditService.change("diferenciaTransferencias", null, diferenciaTransferencias),
                auditService.change("diferenciaTotal", null, diferenciaTotal)
        );
        return guardado;
    }

    public TurnoCaja obtenerTurnoActivo() {
        return turnoCajaRepository
                .findByEstadoIn(List.of(EstadoTurno.ABIERTO, EstadoTurno.SIMULADO))
                .map(this::decorarMetricasCierre)
                .orElse(null);
    }

    public List<TurnoCaja> listarPorRango(LocalDate fechaInicio, LocalDate fechaFin, String username) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("Usuario no existe"));

        if (!"ADMIN".equals(usuario.getRol().getNombre())) {
            throw new BadRequestException("Solo ADMIN puede consultar historico de turnos");
        }
        if (fechaInicio == null || fechaFin == null) {
            throw new BadRequestException("Las fechas son obligatorias");
        }
        if (fechaInicio.isAfter(fechaFin)) {
            throw new BadRequestException("La fecha inicio no puede ser mayor a la fecha fin");
        }

        LocalDateTime inicio = fechaInicio.atStartOfDay();
        LocalDateTime fin = fechaFin.atTime(23, 59, 59);
        return turnoCajaRepository.findByFechaAperturaBetweenOrderByFechaAperturaDesc(inicio, fin)
                .stream()
                .map(this::decorarMetricasCierre)
                .toList();
    }

    private TurnoCaja decorarMetricasCierre(TurnoCaja turno) {
        if (turno == null || turno.getId() == null) return turno;

        // ── 1. Datos brutos del ledger financiero ───────────────────────────
        BigDecimal recaudoEfe     = calculosFinancierosService.sumarRecaudoTurno(turno, MedioFinanciero.EFECTIVO);
        BigDecimal recaudoTransf  = calculosFinancierosService.sumarRecaudoTurno(turno, MedioFinanciero.TRANSFERENCIA);
        BigDecimal gastosEfe      = calculosFinancierosService.sumarGastosPorTurno(turno, MedioFinanciero.EFECTIVO);
        BigDecimal gastosTransf   = calculosFinancierosService.sumarGastosPorTurno(turno, MedioFinanciero.TRANSFERENCIA);

        // ── 2. Métricas financieras (independientes de montoInicial) ────────
        BigDecimal recaudoBruto   = recaudoEfe.add(recaudoTransf);
        BigDecimal gastosCaja     = gastosEfe.add(gastosTransf);

        LocalDate desdeFecha = turno.getFechaApertura().toLocalDate();
        LocalDate hastaFecha = turno.getFechaCierre() != null
                ? turno.getFechaCierre().toLocalDate()
                : LocalDate.now();
        BigDecimal gastosAdmin = gastoAdminRepository
                .findByFechaBetween(desdeFecha, hastaFecha)
                .stream()
                .map(g -> g.getMonto() != null ? g.getMonto() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal gananciaFinanciera = recaudoBruto.subtract(gastosCaja).subtract(gastosAdmin);

        // ── 3. Métricas operativas (conciliación de caja con montoInicial) ──
        BigDecimal saldoCajaEsperado   = turno.getMontoInicial().add(recaudoEfe).subtract(gastosEfe);
        BigDecimal transferenciasNetas = recaudoTransf.subtract(gastosTransf);
        BigDecimal totalOperativoTurno = saldoCajaEsperado.add(transferenciasNetas);

        // ── 4. Volcar al entity ──────────────────────────────────────────────
        turno.setRecaudoBruto(recaudoBruto);
        turno.setTotalGastos(gastosCaja);
        turno.setTotalGastosAdmin(gastosAdmin);
        turno.setGananciaNeta(gananciaFinanciera);

        turno.setEsperado(saldoCajaEsperado);
        turno.setTransferenciasNetas(transferenciasNetas);
        turno.setTotalOperativoTurno(totalOperativoTurno);

        return turno;
    }
}
