package com.pos.service.report;

import com.pos.dto.report.ReporteVentaDTO;
import com.pos.dto.venta.VentaResponseDTO;
import com.pos.entity.AbonoFiado;
import com.pos.entity.CondicionPago;
import com.pos.entity.EstadoTurno;
import com.pos.entity.EstadoVenta;
import com.pos.entity.MedioFinanciero;
import com.pos.entity.TurnoCaja;
import com.pos.entity.Venta;
import com.pos.repository.AbonoFiadoRepository;
import com.pos.repository.TurnoCajaRepository;
import com.pos.repository.VentaRepository;
import com.pos.service.CalculosFinancierosService;
import com.pos.service.VentaService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class ReporteVentaService {

    private final VentaRepository ventaRepository;
    private final AbonoFiadoRepository abonoFiadoRepository;
    private final TurnoCajaRepository turnoCajaRepository;
    private final VentaService ventaService;
    private final CalculosFinancierosService calculosFinancierosService;

    public ReporteVentaService(
            VentaRepository ventaRepository,
            AbonoFiadoRepository abonoFiadoRepository,
            TurnoCajaRepository turnoCajaRepository,
            VentaService ventaService,
            CalculosFinancierosService calculosFinancierosService
    ) {
        this.ventaRepository = ventaRepository;
        this.abonoFiadoRepository = abonoFiadoRepository;
        this.turnoCajaRepository = turnoCajaRepository;
        this.ventaService = ventaService;
        this.calculosFinancierosService = calculosFinancierosService;
    }

    public ReporteVentaDTO generarReporteVentas(LocalDate fechaInicio, LocalDate fechaFin) {
        LocalDateTime inicio = fechaInicio.atStartOfDay();
        LocalDateTime fin = fechaFin.atTime(23, 59, 59);

        // Filtra por fecha de APERTURA. Criterio unificado con Dashboard y listarPorRango.
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

        BigDecimal totalBruto = BigDecimal.ZERO;
        BigDecimal totalDescuentos = BigDecimal.ZERO;
        BigDecimal totalNeto = BigDecimal.ZERO;
        BigDecimal totalMontoContado = BigDecimal.ZERO;
        BigDecimal totalMontoFiado = BigDecimal.ZERO;
        BigDecimal carteraPendiente = BigDecimal.ZERO;
        long totalVentasContado = 0;
        long totalVentasFiadas = 0;
        BigDecimal totalAbonos = BigDecimal.ZERO;
        BigDecimal totalAbonosEfectivo = BigDecimal.ZERO;
        BigDecimal totalAbonosTransferencia = BigDecimal.ZERO;

        for (Venta venta : ventas) {
            BigDecimal descuento = obtenerDescuento(venta);
            BigDecimal totalFinal = venta.getTotal() != null ? venta.getTotal() : BigDecimal.ZERO;
            totalBruto = totalBruto.add(totalFinal.add(descuento));
            totalDescuentos = totalDescuentos.add(descuento);
            totalNeto = totalNeto.add(totalFinal);
            if (venta.getCondicionPago() == CondicionPago.FIADO) {
                totalVentasFiadas++;
                totalMontoFiado = totalMontoFiado.add(totalFinal);
                carteraPendiente = carteraPendiente.add(venta.getSaldoPendiente() != null ? venta.getSaldoPendiente() : BigDecimal.ZERO);
            } else {
                totalVentasContado++;
                totalMontoContado = totalMontoContado.add(totalFinal);
            }
        }

        BigDecimal totalEfectivo = calculosFinancierosService.sumarRecaudoTotal(turnosCerrados, MedioFinanciero.EFECTIVO);
        BigDecimal totalTransferencia = calculosFinancierosService.sumarRecaudoTotal(turnosCerrados, MedioFinanciero.TRANSFERENCIA);

        List<VentaResponseDTO> ventasDTO = ventas.stream()
                .map(ventaService::construirRespuesta)
                .toList();

        List<AbonoFiado> abonosPeriodo = abonoFiadoRepository.findByFechaBetweenOrderByFechaAsc(inicio, fin)
                .stream()
                .filter(a -> a.getTurno() != null && cerradosIds.contains(a.getTurno().getId()))
                .toList();
        for (AbonoFiado a : abonosPeriodo) {
            BigDecimal monto = a.getMonto() != null ? a.getMonto() : BigDecimal.ZERO;
            BigDecimal montoE = a.getMontoEfectivo() != null ? a.getMontoEfectivo() : BigDecimal.ZERO;
            BigDecimal montoT = a.getMontoTransferencia() != null ? a.getMontoTransferencia() : BigDecimal.ZERO;
            totalAbonos = totalAbonos.add(monto);
            totalAbonosEfectivo = totalAbonosEfectivo.add(montoE);
            totalAbonosTransferencia = totalAbonosTransferencia.add(montoT);
        }

        ReporteVentaDTO reporte = new ReporteVentaDTO();
        reporte.setFechaInicio(fechaInicio);
        reporte.setFechaFin(fechaFin);
        reporte.setTotalVentas((long) ventas.size());
        reporte.setTotalBruto(totalBruto);
        reporte.setTotalDescuentos(totalDescuentos);
        reporte.setTotalNeto(totalNeto);
        reporte.setTotalEfectivo(totalEfectivo);
        reporte.setTotalTransferencia(totalTransferencia);
        reporte.setTotalAbonos(totalAbonos);
        reporte.setTotalAbonosEfectivo(totalAbonosEfectivo);
        reporte.setTotalAbonosTransferencia(totalAbonosTransferencia);
        reporte.setTotalVentasContado(totalVentasContado);
        reporte.setTotalVentasFiadas(totalVentasFiadas);
        reporte.setTotalMontoContado(totalMontoContado);
        reporte.setTotalMontoFiado(totalMontoFiado);
        reporte.setCarteraGenerada(totalMontoFiado);
        reporte.setCarteraPendiente(carteraPendiente);
        reporte.setRecaudoReal(totalEfectivo.add(totalTransferencia));
        reporte.setVentas(ventasDTO);

        return reporte;
    }

    private BigDecimal obtenerDescuento(Venta venta) {
        return venta.getDescuentoValor() != null ? venta.getDescuentoValor() : BigDecimal.ZERO;
    }
}
