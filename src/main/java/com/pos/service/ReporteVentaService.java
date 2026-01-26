package com.pos.service;

import com.pos.dto.report.ReporteVentaDTO;
import com.pos.dto.venta.VentaResponseDTO;
import com.pos.entity.EstadoVenta;
import com.pos.entity.FormaPago;
import com.pos.entity.Venta;
import com.pos.repository.VentaRepository;
import org.springframework.stereotype.Service;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReporteVentaService {

    private final VentaRepository ventaRepository;

    public ReporteVentaService(VentaRepository ventaRepository) {
        this.ventaRepository = ventaRepository;
    }

    public ReporteVentaDTO generarReporteVentas(LocalDate fechaInicio,
                                                LocalDate fechaFin) {

        LocalDateTime inicio = fechaInicio.atStartOfDay();
        LocalDateTime fin = fechaFin.atTime(23, 59, 59);

        List<Venta> ventas = ventaRepository
                .findByFechaBetweenAndEstadoIn(
                        inicio,
                        fin,
                        List.of(EstadoVenta.DESPACHADA, EstadoVenta.ANULADA)
                );

        BigDecimal totalBruto = BigDecimal.ZERO;
        BigDecimal totalDescuentos = BigDecimal.ZERO;
        BigDecimal totalNeto = BigDecimal.ZERO;
        BigDecimal totalEfectivo = BigDecimal.ZERO;
        BigDecimal totalTransferencia = BigDecimal.ZERO;

        for (Venta venta : ventas) {
            
            if (venta.getEstado() == EstadoVenta.ANULADA) {
                continue; // solo se lista, no suma
            }

            BigDecimal descuento = venta.getDescuentoValor() != null
                    ? venta.getDescuentoValor()
                    : BigDecimal.ZERO;

            BigDecimal totalVenta = venta.getTotal();
            BigDecimal totalFinal = totalVenta.subtract(descuento);

            totalBruto = totalBruto.add(totalVenta);
            totalDescuentos = totalDescuentos.add(descuento);
            totalNeto = totalNeto.add(totalFinal);

            if (venta.getFormaPago() == FormaPago.EFECTIVO) {
                totalEfectivo = totalEfectivo.add(totalFinal);
            }

            if (venta.getFormaPago() == FormaPago.TRANSFERENCIA) {
                totalTransferencia = totalTransferencia.add(totalFinal);
            }
        }

        List<VentaResponseDTO> ventasDTO = ventas.stream()
                .map(this::mapToVentaResponse)
                .toList();
        //ventas incluye DESPACHADAS y ANULADAS (las anuladas no suman totales)
        ReporteVentaDTO reporte = new ReporteVentaDTO();
        reporte.setFechaInicio(fechaInicio);
        reporte.setFechaFin(fechaFin);
        long totalVentas = ventas.stream()
        .filter(v -> v.getEstado() == EstadoVenta.DESPACHADA)
        .count();
        reporte.setTotalVentas(totalVentas);
        reporte.setTotalBruto(totalBruto);
        reporte.setTotalDescuentos(totalDescuentos);
        reporte.setTotalNeto(totalNeto);
        reporte.setTotalEfectivo(totalEfectivo);
        reporte.setTotalTransferencia(totalTransferencia);
        reporte.setVentas(ventasDTO);

        return reporte;
    }

    private VentaResponseDTO mapToVentaResponse(Venta venta) {

        return new VentaResponseDTO(
                venta.getId(),
                venta.getFecha(),
                venta.getTipoVenta(),
                venta.getEstado(),
                venta.getClienteNombre(),
                venta.getTotal(),
                venta.getFormaPago()
        );
    }


}
