package com.pos.service.report;

import com.pos.dto.gasto.GastoResponseDTO;
import com.pos.dto.report.ReporteRentabilidadDTO;
import com.pos.dto.venta.VentaResponseDTO;
import com.pos.entity.EstadoVenta;
import com.pos.entity.GastoAdmin;
import com.pos.entity.GastoCaja;
import com.pos.entity.CondicionPago;
import com.pos.entity.MedioFinanciero;
import com.pos.entity.MovimientoFinancieroTipo;
import com.pos.entity.Venta;
import com.pos.repository.GastoAdminRepository;
import com.pos.repository.GastoCajaRepository;
import com.pos.repository.VentaRepository;
import com.pos.service.MovimientoFinancieroService;
import com.pos.service.VentaService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReporteRentabilidadService {

    private final VentaRepository ventaRepository;
    private final GastoCajaRepository gastoCajaRepository;
    private final GastoAdminRepository gastoAdminRepository;
    private final VentaService ventaService;
    private final MovimientoFinancieroService movimientoFinancieroService;

    public ReporteRentabilidadService(
            VentaRepository ventaRepository,
            GastoCajaRepository gastoCajaRepository,
            GastoAdminRepository gastoAdminRepository,
            VentaService ventaService,
            MovimientoFinancieroService movimientoFinancieroService
    ) {
        this.ventaRepository = ventaRepository;
        this.gastoCajaRepository = gastoCajaRepository;
        this.gastoAdminRepository = gastoAdminRepository;
        this.ventaService = ventaService;
        this.movimientoFinancieroService = movimientoFinancieroService;
    }

    public ReporteRentabilidadDTO generarReporte(LocalDate fechaInicio, LocalDate fechaFin) {
        validarFechas(fechaInicio, fechaFin);

        LocalDateTime inicio = fechaInicio.atStartOfDay();
        LocalDateTime fin = fechaFin.atTime(23, 59, 59);

        List<Venta> ventas = ventaRepository.findByFechaBetweenAndEstadoIn(
                inicio,
                fin,
                List.of(EstadoVenta.DESPACHADA)
        );
        List<GastoCaja> gastosCaja = gastoCajaRepository.findByFechaBetween(inicio, fin);
        List<GastoAdmin> gastosAdmin = gastoAdminRepository.findByFechaBetween(fechaInicio, fechaFin);

        BigDecimal totalVentasEfectivo = movimientoFinancieroService.sumarPeriodoMedioTipos(
                inicio,
                fin,
                MedioFinanciero.EFECTIVO,
                List.of(MovimientoFinancieroTipo.VENTA_CONTADO, MovimientoFinancieroTipo.ABONO_FIADO, MovimientoFinancieroTipo.ANULACION_VENTA)
        );
        BigDecimal totalVentasTransferencia = movimientoFinancieroService.sumarPeriodoMedioTipos(
                inicio,
                fin,
                MedioFinanciero.TRANSFERENCIA,
                List.of(MovimientoFinancieroTipo.VENTA_CONTADO, MovimientoFinancieroTipo.ABONO_FIADO, MovimientoFinancieroTipo.ANULACION_VENTA)
        );
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

        BigDecimal totalGastosEfectivo = movimientoFinancieroService.sumarPeriodoMedioTipos(
                inicio,
                fin,
                MedioFinanciero.EFECTIVO,
                List.of(
                        MovimientoFinancieroTipo.GASTO_CAJA,
                        MovimientoFinancieroTipo.GASTO_ADMIN,
                        MovimientoFinancieroTipo.ELIMINACION_GASTO_CAJA,
                        MovimientoFinancieroTipo.ELIMINACION_GASTO_ADMIN
                )
        ).abs();
        BigDecimal totalGastosTransferencia = movimientoFinancieroService.sumarPeriodoMedioTipos(
                inicio,
                fin,
                MedioFinanciero.TRANSFERENCIA,
                List.of(
                        MovimientoFinancieroTipo.GASTO_CAJA,
                        MovimientoFinancieroTipo.GASTO_ADMIN,
                        MovimientoFinancieroTipo.ELIMINACION_GASTO_CAJA,
                        MovimientoFinancieroTipo.ELIMINACION_GASTO_ADMIN
                )
        ).abs();
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
