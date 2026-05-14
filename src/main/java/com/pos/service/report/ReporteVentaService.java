package com.pos.service.report;
import com.pos.dto.report.ReporteVentaDTO;
import com.pos.dto.venta.VentaResponseDTO;
import com.pos.entity.EstadoVenta;
import com.pos.service.VentaService;
import com.pos.repository.VentaRepository;
import org.springframework.stereotype.Service;
import com.pos.entity.Venta;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReporteVentaService {

    private final VentaRepository ventaRepository;
    private final VentaService ventaService;

    public ReporteVentaService(VentaRepository ventaRepository, VentaService ventaService) {
        this.ventaRepository = ventaRepository;
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

        for (Venta venta : ventas) {
            if (venta.getEstado() == EstadoVenta.ANULADA) {
                continue;
            }

            BigDecimal descuento = obtenerDescuento(venta);
            BigDecimal totalFinal = venta.getTotal().subtract(descuento);
            VentaResponseDTO ventaDTO = mapToVentaResponse(venta);

            totalBruto = totalBruto.add(venta.getTotal());
            totalDescuentos = totalDescuentos.add(descuento);
            totalNeto = totalNeto.add(totalFinal);
            totalEfectivo = totalEfectivo.add(
                    ventaDTO.pagoEfectivo() != null ? ventaDTO.pagoEfectivo() : BigDecimal.ZERO
            );
            totalTransferencia = totalTransferencia.add(
                    ventaDTO.pagoTransferencia() != null ? ventaDTO.pagoTransferencia() : BigDecimal.ZERO
            );
        }

        List<VentaResponseDTO> ventasDTO = ventas.stream()
                .map(this::mapToVentaResponse)
                .toList();

        long totalVentas = ventas.stream()
                .filter(v -> v.getEstado() == EstadoVenta.DESPACHADA)
                .count();

        ReporteVentaDTO reporte = new ReporteVentaDTO();
        reporte.setFechaInicio(fechaInicio);
        reporte.setFechaFin(fechaFin);
        reporte.setTotalVentas(totalVentas);
        reporte.setTotalBruto(totalBruto);
        reporte.setTotalDescuentos(totalDescuentos);
        reporte.setTotalNeto(totalNeto);
        reporte.setTotalEfectivo(totalEfectivo);
        reporte.setTotalTransferencia(totalTransferencia);
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
