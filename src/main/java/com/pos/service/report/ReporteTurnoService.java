package com.pos.service.report;
import com.pos.dto.report.ReporteCierreTurnoDTO;
import com.pos.dto.turno.GastoCajaResponseDTO;
import com.pos.dto.venta.VentaResponseDTO;
import com.pos.entity.GastoCaja;
import com.pos.entity.TurnoCaja;
import com.pos.entity.Venta;
import com.pos.repository.GastoCajaRepository;
import com.pos.repository.TurnoCajaRepository;
import com.pos.repository.VentaRepository;
import com.pos.repository.AbonoFiadoRepository;
import com.pos.entity.AbonoFiado;
import com.pos.service.VentaService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import com.pos.entity.EstadoVenta;


@Service
public class ReporteTurnoService {

    private final TurnoCajaRepository turnoCajaRepository;
    private final VentaRepository ventaRepository;
    private final GastoCajaRepository gastoCajaRepository;
    private final AbonoFiadoRepository abonoFiadoRepository;
    private final VentaService ventaService;

    public ReporteTurnoService(
            TurnoCajaRepository turnoCajaRepository,
            VentaRepository ventaRepository,
            GastoCajaRepository gastoCajaRepository,
            AbonoFiadoRepository abonoFiadoRepository,
            VentaService ventaService
    ) {
        this.turnoCajaRepository = turnoCajaRepository;
        this.ventaRepository = ventaRepository;
        this.gastoCajaRepository = gastoCajaRepository;
        this.abonoFiadoRepository = abonoFiadoRepository;
        this.ventaService = ventaService;
    }

    public ReporteCierreTurnoDTO generarReporteTurno(Long turnoId) {

        if (turnoId == null) {
            throw new IllegalArgumentException("Turno ID no puede ser nulo");
        }

        TurnoCaja turno = turnoCajaRepository.findById(turnoId)
                .orElseThrow(() -> new RuntimeException("Turno no encontrado: " + turnoId));

        LocalDateTime inicio = turno.getFechaApertura();
        LocalDateTime fin = turno.getFechaCierre() != null
                ? turno.getFechaCierre()
                : LocalDateTime.now();

        List<Venta> ventas = ventaRepository.findByTurnoAndEstadoAndFechaBetween(
                turno,
                EstadoVenta.DESPACHADA,
                inicio,
                fin
        );

        List<GastoCaja> gastos =
                gastoCajaRepository.findByTurnoAndFechaBetween(turno, inicio, fin);

        BigDecimal totalVentas = BigDecimal.ZERO;
        BigDecimal totalEfectivo = BigDecimal.ZERO;
        BigDecimal totalTransferencia = BigDecimal.ZERO;
        BigDecimal totalGastos = BigDecimal.ZERO;
        BigDecimal totalGastosEfectivo = BigDecimal.ZERO;
        BigDecimal totalGastosTransferencia = BigDecimal.ZERO;
        BigDecimal totalAbonos = BigDecimal.ZERO;
        BigDecimal totalAbonosEfectivo = BigDecimal.ZERO;
        BigDecimal totalAbonosTransferencia = BigDecimal.ZERO;

        for (Venta venta : ventas) {
            BigDecimal descuento = obtenerDescuento(venta);
            BigDecimal totalFinal = venta.getTotal(); // total ya es neto
            VentaResponseDTO ventaDTO = ventaService.construirRespuesta(venta);
            totalVentas = totalVentas.add(totalFinal);
            // No sumar pagos de ventas FIADO (se cuentan como abonos en el turno/periodo)
            if (venta.getCondicionPago() != com.pos.entity.CondicionPago.FIADO) {
                totalEfectivo = totalEfectivo.add(
                        ventaDTO.pagoEfectivo() != null ? ventaDTO.pagoEfectivo() : BigDecimal.ZERO
                );
                totalTransferencia = totalTransferencia.add(
                        ventaDTO.pagoTransferencia() != null ? ventaDTO.pagoTransferencia() : BigDecimal.ZERO
                );
            }
        }

        for (GastoCaja gasto : gastos) {
            totalGastos = totalGastos.add(gasto.getMonto());
            totalGastosEfectivo = totalGastosEfectivo.add(
                    gasto.getMontoEfectivo() != null ? gasto.getMontoEfectivo() : BigDecimal.ZERO
            );
            totalGastosTransferencia = totalGastosTransferencia.add(
                    gasto.getMontoTransferencia() != null ? gasto.getMontoTransferencia() : BigDecimal.ZERO
            );
        }

        // Sumar abonos registrados en el turno
        List<AbonoFiado> abonosTurno = abonoFiadoRepository.findByTurnoOrderByFechaAsc(turno);
        for (AbonoFiado a : abonosTurno) {
            BigDecimal monto = a.getMonto() != null ? a.getMonto() : BigDecimal.ZERO;
            BigDecimal montoE = a.getMontoEfectivo() != null ? a.getMontoEfectivo() : BigDecimal.ZERO;
            BigDecimal montoT = a.getMontoTransferencia() != null ? a.getMontoTransferencia() : BigDecimal.ZERO;
            totalAbonos = totalAbonos.add(monto);
            totalAbonosEfectivo = totalAbonosEfectivo.add(montoE);
            totalAbonosTransferencia = totalAbonosTransferencia.add(montoT);
        }

        ReporteCierreTurnoDTO reporte = new ReporteCierreTurnoDTO();
        reporte.setTurnoId(turno.getId());
        reporte.setApertura(turno.getFechaApertura());
        reporte.setCierre(turno.getFechaCierre());
        reporte.setTotalVentas(totalVentas);
        reporte.setTotalEfectivo(totalEfectivo);
        reporte.setTotalTransferencia(totalTransferencia);
        reporte.setTotalGastos(totalGastos);
        reporte.setTotalGastosEfectivo(totalGastosEfectivo);
        reporte.setTotalGastosTransferencia(totalGastosTransferencia);
        reporte.setTotalAbonos(totalAbonos);
        reporte.setTotalAbonosEfectivo(totalAbonosEfectivo);
        reporte.setTotalAbonosTransferencia(totalAbonosTransferencia);
        reporte.setGananciaEfectivo(totalEfectivo.subtract(totalGastosEfectivo));
        reporte.setGananciaTransferencia(totalTransferencia.subtract(totalGastosTransferencia));
        // Ajustar neto en caja: incluir abonos en efectivo y restar gastos en efectivo
        // Excluir saldos pendientes (ventas fiadas no cobradas) del total de ventas en este cálculo
        // totalEfectivo ya excluye ventas FIADO; totalAbonosEfectivo incluye abonos cobrados en el turno
        reporte.setNetoEnCaja(totalEfectivo.add(totalAbonosEfectivo).subtract(totalGastos));
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
                .map(ventaService::construirRespuesta)
                .toList();
    }

    private List<GastoCajaResponseDTO> mapGastos(List<GastoCaja> gastos) {
        return gastos.stream()
                .map(g -> new GastoCajaResponseDTO(
                        g.getId(),
                        g.getFecha(),
                        g.getDescripcion(),
                        g.getMonto(),
                        g.getMontoEfectivo() != null ? g.getMontoEfectivo() : BigDecimal.ZERO,
                        g.getMontoTransferencia() != null ? g.getMontoTransferencia() : BigDecimal.ZERO
                ))
                .toList();
    }
}

