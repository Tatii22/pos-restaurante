package com.pos.service.report;

import com.pos.dto.gasto.GastoResponseDTO;
import com.pos.dto.report.DescuadreReporteDTO;
import com.pos.dto.report.ReporteRentabilidadDTO;
import com.pos.dto.venta.VentaResponseDTO;
import com.pos.entity.EstadoTurno;
import com.pos.entity.EstadoVenta;
import com.pos.entity.GastoAdmin;
import com.pos.entity.GastoCaja;
import com.pos.entity.CondicionPago;
import com.pos.entity.MedioFinanciero;
import com.pos.entity.TurnoCaja;
import com.pos.entity.Venta;
import com.pos.repository.GastoAdminRepository;
import com.pos.repository.GastoCajaRepository;
import com.pos.repository.TurnoCajaRepository;
import com.pos.repository.VentaRepository;
import com.pos.service.CalculosFinancierosService;
import com.pos.service.VentaService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ReporteRentabilidadService {

    private final VentaRepository ventaRepository;
    private final GastoCajaRepository gastoCajaRepository;
    private final GastoAdminRepository gastoAdminRepository;
    private final TurnoCajaRepository turnoCajaRepository;
    private final VentaService ventaService;
    private final CalculosFinancierosService calculosFinancierosService;

    public ReporteRentabilidadService(
            VentaRepository ventaRepository,
            GastoCajaRepository gastoCajaRepository,
            GastoAdminRepository gastoAdminRepository,
            TurnoCajaRepository turnoCajaRepository,
            VentaService ventaService,
            CalculosFinancierosService calculosFinancierosService
    ) {
        this.ventaRepository = ventaRepository;
        this.gastoCajaRepository = gastoCajaRepository;
        this.gastoAdminRepository = gastoAdminRepository;
        this.turnoCajaRepository = turnoCajaRepository;
        this.ventaService = ventaService;
        this.calculosFinancierosService = calculosFinancierosService;
    }

    public ReporteRentabilidadDTO generarReporte(LocalDate fechaInicio, LocalDate fechaFin) {
        validarFechas(fechaInicio, fechaFin);

        LocalDateTime inicio = fechaInicio.atStartOfDay();
        LocalDateTime fin = fechaFin.atTime(23, 59, 59);

        // Filtra por fecha de APERTURA, igual que listarPorRango en TurnoCajaService.
        // Criterio unificado: un turno pertenece al período en que inició, no en que cerró.
        List<TurnoCaja> turnosCerrados = turnoCajaRepository
                .findByEstadoAndFechaAperturaBetween(EstadoTurno.CERRADO, inicio, fin);

        Set<Long> cerradosIds = turnosCerrados.stream()
                .map(TurnoCaja::getId)
                .collect(java.util.stream.Collectors.toSet());

        List<Venta> ventas = ventaRepository.findByFechaBetweenAndEstadoIn(
                inicio,
                fin,
                List.of(EstadoVenta.DESPACHADA)
        ).stream()
                .filter(v -> v.getTurno() != null && cerradosIds.contains(v.getTurno().getId()))
                .toList();

        List<GastoCaja> gastosCaja = gastoCajaRepository.findByFechaBetween(inicio, fin)
                .stream()
                .filter(g -> g.getTurno() != null && cerradosIds.contains(g.getTurno().getId()))
                .toList();

        List<GastoAdmin> gastosAdmin = gastoAdminRepository.findByFechaBetween(fechaInicio, fechaFin);

        BigDecimal totalVentasEfectivo = calculosFinancierosService.sumarRecaudoPorPeriodo(inicio, fin, MedioFinanciero.EFECTIVO);
        BigDecimal totalVentasTransferencia = calculosFinancierosService.sumarRecaudoPorPeriodo(inicio, fin, MedioFinanciero.TRANSFERENCIA);
        BigDecimal totalVentas = totalVentasEfectivo.add(totalVentasTransferencia);
        BigDecimal totalVentasComerciales = BigDecimal.ZERO;
        BigDecimal ventasContado = BigDecimal.ZERO;
        BigDecimal ventasFiadas = BigDecimal.ZERO;
        for (Venta venta : ventas) {
            BigDecimal totalVenta = venta.getTotal() != null ? venta.getTotal() : BigDecimal.ZERO;
            totalVentasComerciales = totalVentasComerciales.add(totalVenta);
            if (venta.getCondicionPago() == CondicionPago.FIADO) {
                ventasFiadas = ventasFiadas.add(totalVenta);
            } else {
                ventasContado = ventasContado.add(totalVenta);
            }
        }

        BigDecimal totalGastosEfectivo = calculosFinancierosService.sumarGastosTotal(turnosCerrados, MedioFinanciero.EFECTIVO)
                .add(calculosFinancierosService.sumarGastosAdminPeriodo(inicio, fin, MedioFinanciero.EFECTIVO));
        BigDecimal totalGastosTransferencia = calculosFinancierosService.sumarGastosTotal(turnosCerrados, MedioFinanciero.TRANSFERENCIA)
                .add(calculosFinancierosService.sumarGastosAdminPeriodo(inicio, fin, MedioFinanciero.TRANSFERENCIA));
        BigDecimal totalGastos = totalGastosEfectivo.add(totalGastosTransferencia);

        ReporteRentabilidadDTO reporte = new ReporteRentabilidadDTO();
        reporte.setFechaInicio(fechaInicio);
        reporte.setFechaFin(fechaFin);
        reporte.setTotalVentas(totalVentas);
        reporte.setTotalVentasEfectivo(totalVentasEfectivo);
        reporte.setTotalVentasTransferencia(totalVentasTransferencia);
        reporte.setTotalGastos(totalGastos);
        reporte.setTotalGastosEfectivo(totalGastosEfectivo);
        reporte.setTotalGastosTransferencia(totalGastosTransferencia);
        reporte.setGananciaNeta(totalVentas.subtract(totalGastos));
        reporte.setTotalVentasComerciales(totalVentasComerciales);
        reporte.setVentasContado(ventasContado);
        reporte.setVentasFiadas(ventasFiadas);
        reporte.setCarteraGenerada(ventasFiadas);
        reporte.setRecaudoReal(totalVentas);

        // ── Descuadres de caja ──────────────────────────────────────
        BigDecimal diffAcum = BigDecimal.ZERO;
        List<DescuadreReporteDTO> descuadres = new ArrayList<>();

        // Ordenar turnos por fechaApertura para asignar numeroTurno secuencial por mes
        List<TurnoCaja> turnosOrdenados = turnosCerrados.stream()
                .sorted(Comparator.comparing(TurnoCaja::getFechaApertura))
                .toList();

        Map<YearMonth, Integer> contadorPorMes = new HashMap<>();
        for (TurnoCaja t : turnosOrdenados) {
            YearMonth ym = YearMonth.from(t.getFechaApertura());
            int nro = contadorPorMes.merge(ym, 1, (prev, one) -> prev + 1);
            Integer nt = t.getNumeroTurno();
            if (nt == null) nt = nro;

            BigDecimal dif = t.getDiferenciaTotal() != null ? t.getDiferenciaTotal() : BigDecimal.ZERO;
            if (dif.compareTo(BigDecimal.ZERO) != 0) {
                descuadres.add(new DescuadreReporteDTO(
                        t.getId(),
                        nt,
                        t.getFechaApertura(),
                        dif,
                        t.getObservacionCierre()
                ));
            }
            diffAcum = diffAcum.add(dif);
        }
        reporte.setDiferenciaAcumulada(diffAcum);
        reporte.setResultadoRealAjustado(reporte.getGananciaNeta().add(diffAcum));
        reporte.setDescuadres(descuadres);
        reporte.setVentas(mapVentas(ventas));
        reporte.setGastos(mapGastos(gastosCaja, gastosAdmin));

        return reporte;
    }

    private void validarFechas(LocalDate inicio, LocalDate fin) {
        if (inicio == null || fin == null) {
            throw new IllegalArgumentException("Las fechas no pueden ser nulas");
        }
        if (inicio.isAfter(fin)) {
            throw new IllegalArgumentException("La fecha inicio no puede ser mayor a la fecha fin");
        }
    }

    private List<VentaResponseDTO> mapVentas(List<Venta> ventas) {
        return ventas.stream().map(ventaService::construirRespuesta).toList();
    }

    private List<GastoResponseDTO> mapGastos(List<GastoCaja> caja, List<GastoAdmin> admin) {
        List<GastoResponseDTO> lista = new ArrayList<>();
        caja.forEach(g -> lista.add(new GastoResponseDTO(
                g.getId(),
                g.getFecha(),
                g.getDescripcion(),
                g.getMonto(),
                "CAJA"
        )));
        admin.forEach(g -> lista.add(new GastoResponseDTO(
                g.getId(),
                g.getFecha().atStartOfDay(),
                g.getDescripcion(),
                g.getMonto(),
                "ADMIN"
        )));
        return lista;
    }
}
