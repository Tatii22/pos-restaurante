package com.pos.service;
import com.pos.dto.report.ReporteRentabilidadDTO;
import com.pos.dto.turno.GastoCajaResponseDTO;
import com.pos.dto.venta.VentaResponseDTO;
import com.pos.entity.EstadoVenta;
import com.pos.entity.GastoCaja;
import com.pos.entity.Venta;
import com.pos.repository.GastoCajaRepository;
import com.pos.repository.VentaRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReporteRentabilidadService {

    private final VentaRepository ventaRepository;
    private final GastoCajaRepository gastoCajaRepository;

    public ReporteRentabilidadService(VentaRepository ventaRepository,
                                     GastoCajaRepository gastoCajaRepository) {
        this.ventaRepository = ventaRepository;
        this.gastoCajaRepository = gastoCajaRepository;
    }

    public ReporteRentabilidadDTO generarReporte(LocalDate fechaInicio, LocalDate fechaFin) {

        LocalDateTime inicio = fechaInicio.atStartOfDay();
        LocalDateTime fin = fechaFin.atTime(23, 59, 59);

        // 1️⃣ Traer ventas despachadas
        List<Venta> ventas = ventaRepository.findByFechaBetweenAndEstadoIn(
                inicio, fin, List.of(EstadoVenta.DESPACHADA)
        );

        // 2️⃣ Traer gastos
        List<GastoCaja> gastos = gastoCajaRepository.findByFechaBetween(inicio, fin);

        BigDecimal totalVentas = BigDecimal.ZERO;
        BigDecimal totalGastos = BigDecimal.ZERO;

        // 3️⃣ Calcular totales
        for (Venta v : ventas) {
            BigDecimal descuento = v.getDescuentoValor() != null ? v.getDescuentoValor() : BigDecimal.ZERO;
            BigDecimal totalFinal = v.getTotal().subtract(descuento);
            totalVentas = totalVentas.add(totalFinal);
        }

        for (GastoCaja g : gastos) {
            totalGastos = totalGastos.add(g.getMonto());
        }

        BigDecimal gananciaNeta = totalVentas.subtract(totalGastos);

        // 4️⃣ Mapear DTOs
        List<VentaResponseDTO> ventasDTO = ventas.stream()
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


        List<GastoCajaResponseDTO> gastosDTO = gastos.stream()
                .map(g -> new GastoCajaResponseDTO(
                        g.getId(),
                        g.getFecha(),
                        g.getDescripcion(),
                        g.getMonto()
                ))
                .toList();

        // 5️⃣ Armar reporte
        ReporteRentabilidadDTO reporte = new ReporteRentabilidadDTO();
        reporte.setFechaInicio(fechaInicio);
        reporte.setFechaFin(fechaFin);
        reporte.setTotalVentas(totalVentas);
        reporte.setTotalGastos(totalGastos);
        reporte.setGananciaNeta(gananciaNeta);
        reporte.setVentas(ventasDTO);
        reporte.setGastos(gastosDTO);

        return reporte;
    }
}
