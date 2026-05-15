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
import com.pos.service.VentaService;
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
    private final VentaService ventaService;

    public ReporteRentabilidadService(
            VentaRepository ventaRepository,
            GastoCajaRepository gastoCajaRepository,
            GastoAdminRepository gastoAdminRepository,
            VentaService ventaService
    ) {
        this.ventaRepository = ventaRepository;
        this.gastoCajaRepository = gastoCajaRepository;
        this.gastoAdminRepository = gastoAdminRepository;
        this.ventaService = ventaService;
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
        // Separar por forma de pago (excluyendo saldo pendiente en FIADO)
        BigDecimal totalVentasEfectivo = calcularTotalVentasPorForma(ventas, com.pos.entity.FormaPago.EFECTIVO);
        BigDecimal totalVentasTransferencia = calcularTotalVentasPorForma(ventas, com.pos.entity.FormaPago.TRANSFERENCIA);

        BigDecimal totalGastosCaja = sumarGastosCaja(gastosCaja);
        BigDecimal totalGastosAdmin = sumarGastosAdmin(gastosAdmin);
        // Separar gastos por medio (si aplica)
        BigDecimal totalGastosEfectivo = sumarGastosCajaPorTipo(gastosCaja, true);
        BigDecimal totalGastosTransferencia = sumarGastosCajaPorTipo(gastosCaja, false);
        BigDecimal totalGastos = totalGastosCaja.add(totalGastosAdmin);
        // ================== DTO ==================
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
        // Contar solo lo efectivamente cobrado: total - saldoPendiente por venta.
        return ventas.stream()
                .map(v -> {
                    BigDecimal total = v.getTotal() != null ? v.getTotal() : BigDecimal.ZERO;
                    BigDecimal saldo = v.getSaldoPendiente() != null ? v.getSaldoPendiente() : BigDecimal.ZERO;
                    return total.subtract(saldo);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calcularTotalVentasPorForma(List<Venta> ventas, com.pos.entity.FormaPago forma) {
        return ventas.stream()
                .filter(v -> v.getCondicionPago() != com.pos.entity.CondicionPago.FIADO) // excluir fiado (saldo pendiente)
                .map(v -> {
                    // sumar solo si la forma calculada coincide con la forma solicitada
                    var dto = ventaService.construirRespuesta(v);
                    com.pos.entity.FormaPago f = dto.formaPago();
                    BigDecimal efectivo = dto.pagoEfectivo() != null ? dto.pagoEfectivo() : BigDecimal.ZERO;
                    BigDecimal transferencia = dto.pagoTransferencia() != null ? dto.pagoTransferencia() : BigDecimal.ZERO;
                    if (forma == com.pos.entity.FormaPago.EFECTIVO) {
                        return efectivo;
                    } else {
                        return transferencia;
                    }
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumarGastosCajaPorTipo(List<com.pos.entity.GastoCaja> gastos, boolean efectivo) {
        return gastos.stream()
                .map(g -> efectivo ? (g.getMontoEfectivo() != null ? g.getMontoEfectivo() : BigDecimal.ZERO)
                        : (g.getMontoTransferencia() != null ? g.getMontoTransferencia() : BigDecimal.ZERO))
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
                .map(ventaService::construirRespuesta)
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
