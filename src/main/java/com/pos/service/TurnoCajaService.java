package com.pos.service;

import com.pos.entity.EstadoTurno;
import com.pos.entity.EstadoVenta;
import com.pos.entity.MedioFinanciero;
import com.pos.entity.MovimientoFinancieroTipo;
import com.pos.entity.TipoVenta;
import com.pos.entity.TurnoCaja;
import com.pos.entity.Usuario;
import com.pos.exception.BadRequestException;
import com.pos.repository.TurnoCajaRepository;
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
    private final MovimientoFinancieroService movimientoFinancieroService;
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

        BigDecimal esperadoFisico = calcularEfectivoEsperado(turno);
        BigDecimal transferenciasNetas = calcularTransferenciasNetas(turno);
        BigDecimal totalOperativo = esperadoFisico.add(transferenciasNetas);

        BigDecimal diferenciaEfectivo = efectivoContado.subtract(esperadoFisico);
        BigDecimal diferenciaTransferencias = transferenciasVerificadas.subtract(transferenciasNetas);
        BigDecimal totalVerificado = efectivoContado.add(transferenciasVerificadas);
        BigDecimal diferenciaTotal = totalVerificado.subtract(totalOperativo);

        // Mantener compatibilidad con campo legacy "faltante" (diferencia físico)
        BigDecimal faltanteFisico = diferenciaEfectivo;

        turno.setEsperado(esperadoFisico);
        turno.setFaltante(faltanteFisico);
        turno.setTransferenciasNetas(transferenciasNetas);
        turno.setTotalOperativoTurno(totalOperativo);

        // Conciliación dual
        turno.setEfectivoContado(efectivoContado);
        turno.setTransferenciasVerificadas(transferenciasVerificadas);
        turno.setDiferenciaEfectivo(diferenciaEfectivo);
        turno.setDiferenciaTransferencias(diferenciaTransferencias);
        turno.setTotalVerificado(totalVerificado);
        turno.setDiferenciaTotal(diferenciaTotal);

        // La simulacion solo recalcula cifras; el turno debe seguir operativo.
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

        BigDecimal esperadoFisico = calcularEfectivoEsperado(turno);
        BigDecimal transferenciasNetas = calcularTransferenciasNetas(turno);
        BigDecimal totalOperativo = esperadoFisico.add(transferenciasNetas);

        BigDecimal diferenciaEfectivo = efectivoContado.subtract(esperadoFisico);
        BigDecimal diferenciaTransferencias = transferenciasVerificadas.subtract(transferenciasNetas);
        BigDecimal totalVerificado = efectivoContado.add(transferenciasVerificadas);
        BigDecimal diferenciaTotal = totalVerificado.subtract(totalOperativo);

        // Compatibilidad legacy
        BigDecimal faltanteFisico = diferenciaEfectivo;

        turno.setEsperado(esperadoFisico);
        turno.setMontoFinal(efectivoContado); // legacy: usamos el físico como "montoFinal" para reportes antiguos
        turno.setFaltante(faltanteFisico);
        turno.setTransferenciasNetas(transferenciasNetas);
        turno.setTotalOperativoTurno(totalOperativo);

        // Conciliación dual
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

    private BigDecimal calcularEfectivoEsperado(TurnoCaja turno) {
        BigDecimal movimientosEfectivo = movimientoFinancieroService.sumarTurnoMedioTipos(
                turno,
                MedioFinanciero.EFECTIVO,
                List.of(
                        MovimientoFinancieroTipo.VENTA_CONTADO,
                        MovimientoFinancieroTipo.ABONO_FIADO,
                        MovimientoFinancieroTipo.GASTO_CAJA,
                        MovimientoFinancieroTipo.ELIMINACION_GASTO_CAJA,
                        MovimientoFinancieroTipo.ANULACION_VENTA
                )
        );
        return turno.getMontoInicial().add(movimientosEfectivo);
    }

    private BigDecimal calcularTransferenciasNetas(TurnoCaja turno) {
        return movimientoFinancieroService.sumarTurnoMedioTipos(
                turno,
                MedioFinanciero.TRANSFERENCIA,
                List.of(
                        MovimientoFinancieroTipo.VENTA_CONTADO,
                        MovimientoFinancieroTipo.ABONO_FIADO,
                        MovimientoFinancieroTipo.GASTO_CAJA,
                        MovimientoFinancieroTipo.ELIMINACION_GASTO_CAJA,
                        MovimientoFinancieroTipo.ANULACION_VENTA
                )
        );
    }

    private TurnoCaja decorarMetricasCierre(TurnoCaja turno) {
        if (turno == null || turno.getId() == null) {
            return turno;
        }
        BigDecimal cajaFisica = calcularEfectivoEsperado(turno);
        BigDecimal transferencias = calcularTransferenciasNetas(turno);
        turno.setEsperado(turno.getEsperado() != null ? turno.getEsperado() : cajaFisica);
        turno.setTransferenciasNetas(transferencias);
        turno.setTotalOperativoTurno(cajaFisica.add(transferencias));
        return turno;
    }
}
