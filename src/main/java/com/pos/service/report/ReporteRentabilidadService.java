package com.pos.service.report;
import com.pos.dto.report.ReporteRentabilidadDTO;
import com.pos.dto.turno.GastoCajaResponseDTO;
import com.pos.dto.venta.VentaResponseDTO;
import com.pos.entity.EstadoVenta;
import com.pos.entity.GastoCaja;
import com.pos.entity.Venta;
import com.pos.repository.GastoCajaRepository;
import com.pos.repository.VentaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ReporteRentabilidadService {

    private final VentaRepository ventaRepository;
    private final GastoCajaRepository gastoCajaRepository;

    public ReporteRentabilidadService(
            VentaRepository ventaRepository,
            GastoCajaRepository gastoCajaRepository
    ) {
        this.ventaRepository = ventaRepository;
        this.gastoCajaRepository = gastoCajaRepository;
    }

    public ReporteRentabilidadDTO generarReporte(
            LocalDate fechaInicio,
            LocalDate fechaFin
    ) {

        validarFechas(fechaInicio, fechaFin);

        LocalDateTime inicio = fechaInicio.atStartOfDay();
        LocalDateTime fin = fechaFin.atTime(23, 59, 59);

        List<Venta> ventas = ventaRepository.findByFechaBetweenAndEstadoIn(
                inicio,
                fin,
                List.of(EstadoVenta.DESPACHADA)
        );

        List<GastoCaja> gastos = gastoCajaRepository.findByFechaBetween(inicio, fin);

        BigDecimal totalVentas = calcularTotalVentas(ventas);
        BigDecimal totalGastos = calcularTotalGastos(gastos);

        ReporteRentabilidadDTO reporte = new ReporteRentabilidadDTO();
        reporte.setFechaInicio(fechaInicio);
        reporte.setFechaFin(fechaFin);
        reporte.setTotalVentas(totalVentas);
        reporte.setTotalGastos(totalGastos);
        reporte.setGananciaNeta(totalVentas.subtract(totalGastos));
        reporte.setVentas(mapVentas(ventas));
        reporte.setGastos(mapGastos(gastos));

        return reporte;
    }

    /* ----------------- helpers privados ----------------- */

    private void validarFechas(LocalDate inicio, LocalDate fin) {
        if (inicio == null || fin == null) {
            throw new IllegalArgumentException("Las fechas no pueden ser nulas");
        }
        if (inicio.isAfter(fin)) {
            throw new IllegalArgumentException("La fecha inicio no puede ser mayor a la fecha fin");
        }
    }

    private BigDecimal calcularTotalVentas(List<Venta> ventas) {
        return ventas.stream()
                .map(v -> v.getTotal().subtract(obtenerDescuento(v)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calcularTotalGastos(List<GastoCaja> gastos) {
        return gastos.stream()
                .map(GastoCaja::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal obtenerDescuento(Venta venta) {
        return venta.getDescuentoValor() != null
                ? venta.getDescuentoValor()
                : BigDecimal.ZERO;
    }

    private List<VentaResponseDTO> mapVentas(List<Venta> ventas) {
        return ventas.stream()
                .map(v -> new VentaResponseDTO(
                        v.getId(),
                        v.getFecha(),
                        v.getTipoVenta(),
                        v.getEstado(),
                        v.getClienteNombre(),
                        v.getTotal(),
                        v.getFormaPago()
                ))
                .toList();
    }

    private List<GastoCajaResponseDTO> mapGastos(List<GastoCaja> gastos) {
        return gastos.stream()
                .map(g -> new GastoCajaResponseDTO(
                        g.getId(),
                        g.getFecha(),
                        g.getDescripcion(),
                        g.getMonto()
                ))
                .toList();
    }
}

