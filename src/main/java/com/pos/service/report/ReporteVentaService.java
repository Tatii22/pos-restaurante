package com.pos.service.report;
import com.pos.dto.report.ReporteVentaDTO;
import com.pos.dto.venta.VentaResponseDTO;
import com.pos.entity.EstadoVenta;
import com.pos.entity.CondicionPago;
import com.pos.service.VentaService;
import com.pos.repository.VentaRepository;
import com.pos.repository.AbonoFiadoRepository;
import com.pos.entity.AbonoFiado;
import org.springframework.stereotype.Service;
import com.pos.entity.Venta;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReporteVentaService {

    private final VentaRepository ventaRepository;
    private final AbonoFiadoRepository abonoFiadoRepository;
    private final VentaService ventaService;
    public ReporteVentaService(VentaRepository ventaRepository, AbonoFiadoRepository abonoFiadoRepository, VentaService ventaService) {
        this.ventaRepository = ventaRepository;
        this.abonoFiadoRepository = abonoFiadoRepository;
        this.ventaService = ventaService;
    }


    public ReporteVentaDTO generarReporteVentas(
            LocalDate fechaInicio,
            LocalDate fechaFin
    ) {

        LocalDateTime inicio = fechaInicio.atStartOfDay();
        LocalDateTime fin = fechaFin.atTime(23, 59, 59);

        List<Venta> ventas = ventaRepository.findByFechaBetweenAndEstadoIn(
                inicio,
                fin,
                List.of(EstadoVenta.DESPACHADA, EstadoVenta.ANULADA)
        );
        ventas = ventas.stream()
                .filter(v -> v.getEstado() == EstadoVenta.DESPACHADA)
                .toList();

        BigDecimal totalBruto = BigDecimal.ZERO;
        BigDecimal totalDescuentos = BigDecimal.ZERO;
        BigDecimal totalNeto = BigDecimal.ZERO;
        BigDecimal totalEfectivo = BigDecimal.ZERO;
        BigDecimal totalTransferencia = BigDecimal.ZERO;
        BigDecimal totalAbonos = BigDecimal.ZERO;
        BigDecimal totalAbonosEfectivo = BigDecimal.ZERO;
        BigDecimal totalAbonosTransferencia = BigDecimal.ZERO;

        for (Venta venta : ventas) {
            if (venta.getEstado() == EstadoVenta.ANULADA) {
                continue;
            }

            BigDecimal descuento = obtenerDescuento(venta);
            BigDecimal totalFinal = venta.getTotal(); // total ya es neto
            VentaResponseDTO ventaDTO = mapToVentaResponse(venta);

            totalBruto = totalBruto.add(venta.getTotal().add(descuento));
            totalDescuentos = totalDescuentos.add(descuento);
            totalNeto = totalNeto.add(totalFinal);
            // No sumar pagos de ventas FIADO (se reportan como abonos por separado)
            if (venta.getCondicionPago() != com.pos.entity.CondicionPago.FIADO) {
                totalEfectivo = totalEfectivo.add(
                        ventaDTO.pagoEfectivo() != null ? ventaDTO.pagoEfectivo() : BigDecimal.ZERO
                );
                totalTransferencia = totalTransferencia.add(
                        ventaDTO.pagoTransferencia() != null ? ventaDTO.pagoTransferencia() : BigDecimal.ZERO
                );
            }
        }


        List<VentaResponseDTO> ventasDTO = ventas.stream()
                .map(this::mapToVentaResponse)
                .toList();

        long totalVentas = ventas.stream()
                .filter(v -> v.getEstado() == EstadoVenta.DESPACHADA)
                .count();
        // Sumar abonos en el periodo indicado
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
        reporte.setTotalVentas(totalVentas);
        reporte.setTotalBruto(totalBruto);
        reporte.setTotalDescuentos(totalDescuentos);
        reporte.setTotalNeto(totalNeto);
        reporte.setTotalEfectivo(totalEfectivo);
        reporte.setTotalTransferencia(totalTransferencia);
        reporte.setTotalAbonos(totalAbonos);
        reporte.setTotalAbonosEfectivo(totalAbonosEfectivo);
        reporte.setTotalAbonosTransferencia(totalAbonosTransferencia);
        reporte.setVentas(ventasDTO);
        return reporte;
    }

    private BigDecimal obtenerDescuento(Venta venta) {
        return venta.getDescuentoValor() != null
                ? venta.getDescuentoValor()
                : BigDecimal.ZERO;
    }

    private VentaResponseDTO mapToVentaResponse(Venta venta) {
        return ventaService.construirRespuesta(venta);
    }
}
