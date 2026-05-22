package com.pos.service.report;

import com.pos.dto.report.ReporteVentaDTO;
import com.pos.dto.venta.VentaResponseDTO;
import com.pos.entity.AbonoFiado;
import com.pos.entity.CondicionPago;
import com.pos.entity.EstadoVenta;
import com.pos.entity.MedioFinanciero;
import com.pos.entity.MovimientoFinancieroTipo;
import com.pos.entity.Venta;
import com.pos.repository.AbonoFiadoRepository;
import com.pos.repository.VentaRepository;
import com.pos.service.MovimientoFinancieroService;
import com.pos.service.VentaService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReporteVentaService {

    private final VentaRepository ventaRepository;
    private final AbonoFiadoRepository abonoFiadoRepository;
    private final VentaService ventaService;
    private final MovimientoFinancieroService movimientoFinancieroService;

    public ReporteVentaService(
            VentaRepository ventaRepository,
            AbonoFiadoRepository abonoFiadoRepository,
            VentaService ventaService,
            MovimientoFinancieroService movimientoFinancieroService
    ) {
        this.ventaRepository = ventaRepository;
        this.abonoFiadoRepository = abonoFiadoRepository;
        this.ventaService = ventaService;
        this.movimientoFinancieroService = movimientoFinancieroService;
    }

    public ReporteVentaDTO generarReporteVentas(LocalDate fechaInicio, LocalDate fechaFin) {
        LocalDateTime inicio = fechaInicio.atStartOfDay();
        LocalDateTime fin = fechaFin.atTime(23, 59, 59);

        List<Venta> ventas = ventaRepository.findByFechaBetweenAndEstadoIn(
                inicio,
                fin,
                List.of(EstadoVenta.DESPACHADA)
        );

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

        BigDecimal totalEfectivo = movimientoFinancieroService.sumarPeriodoMedioTipos(
                inicio,
                fin,
                MedioFinanciero.EFECTIVO,
                List.of(MovimientoFinancieroTipo.VENTA_CONTADO, MovimientoFinancieroTipo.ANULACION_VENTA)
        );
        BigDecimal totalTransferencia = movimientoFinancieroService.sumarPeriodoMedioTipos(
                inicio,
                fin,
                MedioFinanciero.TRANSFERENCIA,
                List.of(MovimientoFinancieroTipo.VENTA_CONTADO, MovimientoFinancieroTipo.ANULACION_VENTA)
        );

        List<VentaResponseDTO> ventasDTO = ventas.stream()
                .map(ventaService::construirRespuesta)
                .toList();

        List<AbonoFiado> abonosPeriodo = abonoFiadoRepository.findByFechaBetweenOrderByFechaAsc(inicio, fin);
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
        reporte.setRecaudoReal(totalEfectivo.add(totalTransferencia).add(totalAbonos));
        reporte.setVentas(ventasDTO);

        return reporte;
    }

    private BigDecimal obtenerDescuento(Venta venta) {
        return venta.getDescuentoValor() != null ? venta.getDescuentoValor() : BigDecimal.ZERO;
    }
}
