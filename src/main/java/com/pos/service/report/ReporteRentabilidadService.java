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

        LocalDateTime inicio = fechaInicio.atStartOfDay();
        LocalDateTime fin = fechaFin.atTime(23, 59, 59);

        List<Venta> ventas = ventaRepository.findByFechaBetweenAndEstadoIn(
                inicio,
                fin,
                List.of(EstadoVenta.DESPACHADA)
        );

        List<GastoCaja> gastos = gastoCajaRepository.findByFechaBetween(inicio, fin);

        BigDecimal totalVentas = BigDecimal.ZERO;
        BigDecimal totalGastos = BigDecimal.ZERO;

        for (Venta venta : ventas) {
            BigDecimal totalFinal = venta.getTotal().subtract(obtenerDescuento(venta));
            totalVentas = totalVentas.add(totalFinal);
        }

        for (GastoCaja gasto : gastos) {
            totalGastos = totalGastos.add(gasto.getMonto());
        }

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

