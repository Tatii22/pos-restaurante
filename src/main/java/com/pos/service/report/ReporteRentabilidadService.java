package com.pos.service.report;
import com.pos.dto.report.ReporteRentabilidadDTO;
import com.pos.dto.turno.GastoCajaResponseDTO;
import com.pos.dto.venta.VentaResponseDTO;
import com.pos.dto.gasto.GastoResponseDTO;
import com.pos.entity.EstadoVenta;
import com.pos.entity.GastoAdmin;
import com.pos.entity.GastoCaja;
import com.pos.entity.Venta;
import com.pos.repository.GastoAdminRepository;
import com.pos.repository.GastoCajaRepository;
import com.pos.repository.VentaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ReporteRentabilidadService {

    private final VentaRepository ventaRepository;
    private final GastoCajaRepository gastoCajaRepository;
    private final GastoAdminRepository gastoAdminRepository;

    public ReporteRentabilidadService(
            VentaRepository ventaRepository,
            GastoCajaRepository gastoCajaRepository,
            GastoAdminRepository gastoAdminRepository
    ) {
        this.ventaRepository = ventaRepository;
        this.gastoCajaRepository = gastoCajaRepository;
        this.gastoAdminRepository = gastoAdminRepository;
    }

    public ReporteRentabilidadDTO generarReporte(
            LocalDate fechaInicio,
            LocalDate fechaFin
    ) {

        validarFechas(fechaInicio, fechaFin);

        LocalDateTime inicio = fechaInicio.atStartOfDay();
        LocalDateTime fin = fechaFin.atTime(23, 59, 59);

        // ================== VENTAS ==================
        List<Venta> ventas = ventaRepository.findByFechaBetweenAndEstadoIn(
                inicio,
                fin,
                List.of(EstadoVenta.DESPACHADA)
        );

        // ================== GASTOS ==================
        List<GastoCaja> gastosCaja =
                gastoCajaRepository.findByFechaBetween(inicio, fin);

        List<GastoAdmin> gastosAdmin =
                gastoAdminRepository.findByFechaBetween(fechaInicio, fechaFin);

        BigDecimal totalVentas = calcularTotalVentas(ventas);
        BigDecimal totalGastosCaja = sumarGastosCaja(gastosCaja);
        BigDecimal totalGastosAdmin = sumarGastosAdmin(gastosAdmin);

        BigDecimal totalGastos = totalGastosCaja.add(totalGastosAdmin);

        // ================== DTO ==================
        ReporteRentabilidadDTO reporte = new ReporteRentabilidadDTO();
        reporte.setFechaInicio(fechaInicio);
        reporte.setFechaFin(fechaFin);
        reporte.setTotalVentas(totalVentas);
        reporte.setTotalGastos(totalGastos);
        reporte.setGananciaNeta(totalVentas.subtract(totalGastos));
        reporte.setVentas(mapVentas(ventas));
        reporte.setGastos(mapGastos(gastosCaja, gastosAdmin));

        return reporte;
    }

    // ================== HELPERS ==================

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
                .map(v -> v.getTotal().subtract(
                        v.getDescuentoValor() != null ? v.getDescuentoValor() : BigDecimal.ZERO
                ))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumarGastosCaja(List<GastoCaja> gastos) {
        return gastos.stream()
                .map(GastoCaja::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumarGastosAdmin(List<GastoAdmin> gastos) {
        return gastos.stream()
                .map(GastoAdmin::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<VentaResponseDTO> mapVentas(List<Venta> ventas) {
        return ventas.stream()
                .map(v -> new VentaResponseDTO(
                        v.getId(),
                        v.getFecha(),
                        v.getTipoVenta(),
                        v.getEstado(),
                        v.getClienteNombre(),
                        v.getTelefono(),
                        v.getDireccion(),
                        v.getValorDomicilio(),
                        v.getDescuentoPorcentaje(),
                        v.getDescuentoValor(),
                        v.getTotal(),
                        v.getFormaPago()
                ))
                .toList();
    }

    private List<GastoResponseDTO> mapGastos(
            List<GastoCaja> caja,
            List<GastoAdmin> admin
    ) {
        List<GastoResponseDTO> lista = new ArrayList<>();

        caja.forEach(g ->
                lista.add(new GastoResponseDTO(
                        g.getId(),
                        g.getFecha(),
                        g.getDescripcion(),
                        g.getMonto(),
                        "CAJA"
                ))
        );

        admin.forEach(g ->
                lista.add(new GastoResponseDTO(
                        g.getId(),
                        g.getFecha().atStartOfDay(),
                        g.getDescripcion(),
                        g.getMonto(),
                        "ADMIN"
                ))
        );

        return lista;
    }
}
