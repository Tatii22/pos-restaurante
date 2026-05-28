package com.pos.service;

import com.pos.entity.EstadoTurno;
import com.pos.entity.EstadoVenta;
import com.pos.entity.MedioFinanciero;
import com.pos.entity.TipoVenta;
import com.pos.entity.TurnoCaja;
import com.pos.entity.Usuario;
import com.pos.exception.BadRequestException;
import com.pos.repository.GastoAdminRepository;
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

    /**
     * Diferencia máxima (en valor absoluto) que se acepta sin justificación.
     * Por encima de este umbral el cajero DEBE ingresar observacionCierre.
     * Valor: $15.000 COP — ajustar según política del negocio.
     */
    private static final BigDecimal UMBRAL_DESCUADRE = new BigDecimal("15000");

    private final TurnoCajaRepository turnoCajaRepository;
    private final UsuarioRepository usuarioRepository;
    private final VentaRepository ventaRepository;
    private final GastoAdminRepository gastoAdminRepository;
    private final CalculosFinancierosService calculosFinancierosService;
    private final AuditService auditService;
    private final MenuDiarioService menuDiarioService;

    // ─────────────────────────────────────────────────────────────────
    // APERTURA
    // ─────────────────────────────────────────────────────────────────

    @Transactional
    public synchronized TurnoCaja abrirTurno(BigDecimal montoInicial, String username) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("Usuario no existe"));

        if (!usuario.getRol().getNombre().equals("CAJA")) {
            throw new BadRequestException("Solo un usuario CAJA puede abrir turno");
        }
        if (turnoCajaRepository.existsByEstadoIn(List.of(EstadoTurno.ABIERTO, EstadoTurno.SIMULADO))) {
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

        // Asignar número secuencial del turno dentro del mes
        LocalDateTime now = LocalDateTime.now();
        int maxNro = turnoCajaRepository.findMaxNumeroTurnoPorMes(now.getYear(), now.getMonthValue());
        turno.setNumeroTurno(maxNro + 1);

        TurnoCaja guardado = turnoCajaRepository.save(turno);
        menuDiarioService.reiniciarMenuParaNuevoTurno(usuario);
        decorarMetricasCierre(guardado);
        auditService.record(
                "TURNO_ABIERTO", "TurnoCaja", guardado.getId(), usuario, guardado, null,
                auditService.change("montoInicial", null, guardado.getMontoInicial()),
                auditService.change("estado", null, guardado.getEstado())
        );
        return guardado;
    }

    // ─────────────────────────────────────────────────────────────────
    // SIMULACIÓN DE CIERRE — solo devuelve métricas, NO persiste arqueo
    // ─────────────────────────────────────────────────────────────────

    /**
     * Calcula y devuelve las métricas de arqueo SIN cerrar el turno ni persistir
     * los valores de conciliación. El cajero puede ejecutar esto N veces.
     * Solo el cerrarTurno() persiste los valores definitivos.
     */
    @Transactional
    public TurnoCaja simularCierre(BigDecimal efectivoContado,
                                   BigDecimal transferenciasVerificadas,
                                   Usuario usuario) {
        if (!usuario.getRol().getNombre().equals("CAJA")) {
            throw new BadRequestException("Solo CAJA puede simular cierre");
        }

        // Usar findByEstadoIn (lectura limpia, sin FOR UPDATE — solo consulta)
        TurnoCaja turno = turnoCajaRepository
                .findByEstadoIn(List.of(EstadoTurno.ABIERTO, EstadoTurno.SIMULADO))
                .orElseThrow(() -> new BadRequestException("No hay turno abierto"));

        decorarMetricasCierre(turno);

        // Calcular métricas de arqueo en memoria
        ArqueoResult arqueo = calcularArqueo(
                turno, efectivoContado, transferenciasVerificadas
        );

        // Volcar al objeto en memoria ÚNICAMENTE (sin save)
        aplicarArqueoATurno(turno, efectivoContado, transferenciasVerificadas, arqueo);

        // Auditar la simulación (sin tocar el estado ni los campos persistidos)
        auditService.record(
                "TURNO_CIERRE_SIMULADO", "TurnoCaja", turno.getId(), usuario, turno, null,
                auditService.change("efectivoContado",           null, efectivoContado),
                auditService.change("transferenciasVerificadas", null, transferenciasVerificadas),
                auditService.change("diferenciaEfectivo",        null, arqueo.diferenciaEfectivo()),
                auditService.change("diferenciaTransferencias",  null, arqueo.diferenciaTransferencias()),
                auditService.change("diferenciaTotal",           null, arqueo.diferenciaTotal())
        );

        return turno;  // objeto decorado, NO persistido con datos de arqueo
    }

    // ─────────────────────────────────────────────────────────────────
    // CIERRE REAL — persiste todo y cierra definitivamente
    // ─────────────────────────────────────────────────────────────────

    @Transactional
    public TurnoCaja cerrarTurno(BigDecimal efectivoContado,
                                 BigDecimal transferenciasVerificadas,
                                 String observacionCierre,
                                 Usuario usuario) {
        if (!usuario.getRol().getNombre().equals("CAJA")) {
            throw new BadRequestException("Solo CAJA puede cerrar turno");
        }

        TurnoCaja turno = turnoCajaRepository
                .findByEstadoInForUpdate(List.of(EstadoTurno.ABIERTO, EstadoTurno.SIMULADO))
                .orElseThrow(() -> new BadRequestException("No hay turno para cerrar"));

        // Bloqueo: no cerrar si hay domicilios pendientes
        if (ventaRepository.existsByTurnoAndTipoVentaAndEstado(
                turno, TipoVenta.DOMICILIO, EstadoVenta.EN_PROCESO)) {
            throw new BadRequestException(
                    "No puedes cerrar turno: hay domicilios del turno pendientes por despachar");
        }

        // Validaciones de entrada
        if (efectivoContado == null || efectivoContado.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("El efectivo físico contado no puede ser negativo");
        }
        if (transferenciasVerificadas == null
                || transferenciasVerificadas.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Las transferencias verificadas no pueden ser negativas");
        }

        decorarMetricasCierre(turno);
        ArqueoResult arqueo = calcularArqueo(turno, efectivoContado, transferenciasVerificadas);

        // ── FIX 3: Umbral de descuadre ────────────────────────────────────────
        BigDecimal difAbsoluta = arqueo.diferenciaTotal().abs();
        if (difAbsoluta.compareTo(UMBRAL_DESCUADRE) > 0) {
            String obsLimpia = observacionCierre == null ? "" : observacionCierre.trim();
            if (obsLimpia.isBlank()) {
                throw new BadRequestException(String.format(
                        "La caja presenta una diferencia de %s. " +
                        "Debes ingresar una observación para justificar el descuadre antes de cerrar.",
                        formatMoney(arqueo.diferenciaTotal())
                ));
            }
        }

        // ── Persistir cierre definitivo ──────────────────────────────────────
        aplicarArqueoATurno(turno, efectivoContado, transferenciasVerificadas, arqueo);

        String obsFinal = limpiarObservacion(observacionCierre);
        turno.setObservacionCierre(obsFinal);
        turno.setMontoFinal(efectivoContado);
        turno.setFechaCierre(LocalDateTime.now());
        turno.setEstado(EstadoTurno.CERRADO);

        TurnoCaja guardado = turnoCajaRepository.save(turno);
        decorarMetricasCierre(guardado);

        auditService.record(
                "TURNO_CERRADO", "TurnoCaja", guardado.getId(), usuario, guardado, obsFinal,
                auditService.change("estado",                    EstadoTurno.ABIERTO, EstadoTurno.CERRADO),
                auditService.change("efectivoContado",           null, efectivoContado),
                auditService.change("transferenciasVerificadas", null, transferenciasVerificadas),
                auditService.change("diferenciaEfectivo",        null, arqueo.diferenciaEfectivo()),
                auditService.change("diferenciaTransferencias",  null, arqueo.diferenciaTransferencias()),
                auditService.change("diferenciaTotal",           null, arqueo.diferenciaTotal()),
                auditService.change("faltante",                  null, guardado.getFaltante()),
                auditService.change("observacionCierre",         null, obsFinal)
        );
        return guardado;
    }

    // ─────────────────────────────────────────────────────────────────
    // CONSULTAS
    // ─────────────────────────────────────────────────────────────────

    public TurnoCaja obtenerTurnoActivo() {
        return turnoCajaRepository
                .findByEstadoIn(List.of(EstadoTurno.ABIERTO, EstadoTurno.SIMULADO))
                .map(this::decorarMetricasCierre)
                .orElse(null);
    }

    public List<TurnoCaja> listarPorRango(LocalDate fechaInicio, LocalDate fechaFin,
                                          String username) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("Usuario no existe"));

        if (!"ADMIN".equals(usuario.getRol().getNombre())) {
            throw new BadRequestException("Solo ADMIN puede consultar historial de turnos");
        }
        if (fechaInicio == null || fechaFin == null) {
            throw new BadRequestException("Las fechas son obligatorias");
        }
        if (fechaInicio.isAfter(fechaFin)) {
            throw new BadRequestException("La fecha inicio no puede ser mayor a la fecha fin");
        }

        return turnoCajaRepository
                .findByFechaAperturaBetweenOrderByFechaAperturaDesc(
                        fechaInicio.atStartOfDay(),
                        fechaFin.atTime(23, 59, 59))
                .stream()
                .map(this::decorarMetricasCierre)
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────
    // CÁLCULO DE ARQUEO (record inmutable, sin efectos secundarios)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Calcula todas las métricas de conciliación a partir del ledger y los valores
     * contados. No modifica el turno ni accede a la BD.
     */
    private ArqueoResult calcularArqueo(TurnoCaja turno,
                                        BigDecimal efectivoContado,
                                        BigDecimal transferenciasVerificadas) {
        BigDecimal esperado             = safe(turno.getEsperado());
        BigDecimal transferenciasNetas  = safe(turno.getTransferenciasNetas());
        BigDecimal totalOperativo       = safe(turno.getTotalOperativoTurno());

        BigDecimal efContado   = safe(efectivoContado);
        BigDecimal transVerif  = safe(transferenciasVerificadas);

        // Diferencias con signo: negativo = falta, positivo = sobra
        BigDecimal difEfectivo        = efContado.subtract(esperado);
        BigDecimal difTransferencias  = transVerif.subtract(transferenciasNetas);
        BigDecimal totalVerificado    = efContado.add(transVerif);
        BigDecimal difTotal           = totalVerificado.subtract(totalOperativo);

        // FIX 1: faltante = solo la parte negativa (monto que falta, siempre >= 0)
        BigDecimal faltante = difEfectivo.compareTo(BigDecimal.ZERO) < 0
                ? difEfectivo.negate()
                : BigDecimal.ZERO;

        return new ArqueoResult(
                difEfectivo, difTransferencias,
                totalVerificado, difTotal, faltante
        );
    }

    /** Vuelca los resultados del arqueo al objeto TurnoCaja (en memoria). */
    private void aplicarArqueoATurno(TurnoCaja turno,
                                     BigDecimal efectivoContado,
                                     BigDecimal transferenciasVerificadas,
                                     ArqueoResult arqueo) {
        turno.setEfectivoContado(efectivoContado);
        turno.setTransferenciasVerificadas(transferenciasVerificadas);
        turno.setDiferenciaEfectivo(arqueo.diferenciaEfectivo());
        turno.setDiferenciaTransferencias(arqueo.diferenciaTransferencias());
        turno.setTotalVerificado(arqueo.totalVerificado());
        turno.setDiferenciaTotal(arqueo.diferenciaTotal());
        turno.setFaltante(arqueo.faltante());
    }

    private record ArqueoResult(
            BigDecimal diferenciaEfectivo,
            BigDecimal diferenciaTransferencias,
            BigDecimal totalVerificado,
            BigDecimal diferenciaTotal,
            BigDecimal faltante
    ) {}

    // ─────────────────────────────────────────────────────────────────
    // DECORACIÓN DE MÉTRICAS (ledger → @Transient)
    // ─────────────────────────────────────────────────────────────────

    TurnoCaja decorarMetricasCierre(TurnoCaja turno) {
        if (turno == null || turno.getId() == null) return turno;

        BigDecimal recaudoEfe    = calculosFinancierosService.sumarRecaudoTurno(turno, MedioFinanciero.EFECTIVO);
        BigDecimal recaudoTransf = calculosFinancierosService.sumarRecaudoTurno(turno, MedioFinanciero.TRANSFERENCIA);
        BigDecimal gastosEfe     = calculosFinancierosService.sumarGastosPorTurno(turno, MedioFinanciero.EFECTIVO);
        BigDecimal gastosTransf  = calculosFinancierosService.sumarGastosPorTurno(turno, MedioFinanciero.TRANSFERENCIA);

        BigDecimal recaudoBruto       = recaudoEfe.add(recaudoTransf);
        BigDecimal gastosCaja         = gastosEfe.add(gastosTransf);

        LocalDate desdeFecha = turno.getFechaApertura().toLocalDate();
        LocalDate hastaFecha = turno.getFechaCierre() != null
                ? turno.getFechaCierre().toLocalDate()
                : LocalDate.now();
        BigDecimal gastosAdmin = gastoAdminRepository
                .findByFechaBetween(desdeFecha, hastaFecha)
                .stream()
                .map(g -> g.getMonto() != null ? g.getMonto() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal gananciaFinanciera    = recaudoBruto.subtract(gastosCaja).subtract(gastosAdmin);
        BigDecimal saldoCajaEsperado     = turno.getMontoInicial().add(recaudoEfe).subtract(gastosEfe);
        BigDecimal transferenciasNetas   = recaudoTransf.subtract(gastosTransf);
        BigDecimal totalOperativoTurno   = saldoCajaEsperado.add(transferenciasNetas);

        turno.setRecaudoBruto(recaudoBruto);
        turno.setTotalVentas(recaudoBruto);
        turno.setTotalGastos(gastosCaja);
        turno.setTotalGastosAdmin(gastosAdmin);
        turno.setGananciaNeta(gananciaFinanciera);
        turno.setEsperado(saldoCajaEsperado);
        turno.setTransferenciasNetas(transferenciasNetas);
        turno.setTotalOperativoTurno(totalOperativoTurno);
        turno.setUmbralDescuadre(UMBRAL_DESCUADRE);

        return turno;
    }

    // ─────────────────────────────────────────────────────────────────
    // UTILIDADES
    // ─────────────────────────────────────────────────────────────────

    private BigDecimal safe(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private String limpiarObservacion(String obs) {
        if (obs == null || obs.isBlank()) return null;
        String clean = obs.trim().replace("\r", " ").replace("\n", " ");
        return clean.length() > 500 ? clean.substring(0, 500) : clean;
    }

    private String formatMoney(BigDecimal value) {
        if (value == null) return "$0";
        return String.format("$%,.0f", value);
    }
}
