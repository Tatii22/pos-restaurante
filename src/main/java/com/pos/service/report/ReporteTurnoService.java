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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import com.pos.entity.EstadoVenta;
import com.pos.entity.FormaPago;


@Service
public class ReporteTurnoService {

    private final TurnoCajaRepository turnoCajaRepository;
    private final VentaRepository ventaRepository;
    private final GastoCajaRepository gastoCajaRepository;

    public ReporteTurnoService(
            TurnoCajaRepository turnoCajaRepository,
            VentaRepository ventaRepository,
            GastoCajaRepository gastoCajaRepository
    ) {
        this.turnoCajaRepository = turnoCajaRepository;
        this.ventaRepository = ventaRepository;
        this.gastoCajaRepository = gastoCajaRepository;
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

        for (Venta venta : ventas) {
            BigDecimal totalFinal = venta.getTotal().subtract(obtenerDescuento(venta));
            totalVentas = totalVentas.add(totalFinal);

            if (venta.getFormaPago() == FormaPago.EFECTIVO) {
                totalEfectivo = totalEfectivo.add(totalFinal);
            } else if (venta.getFormaPago() == FormaPago.TRANSFERENCIA) {
                totalTransferencia = totalTransferencia.add(totalFinal);
            }
        }

        for (GastoCaja gasto : gastos) {
            totalGastos = totalGastos.add(gasto.getMonto());
        }

        ReporteCierreTurnoDTO reporte = new ReporteCierreTurnoDTO();
        reporte.setTurnoId(turno.getId());
        reporte.setApertura(turno.getFechaApertura());
        reporte.setCierre(turno.getFechaCierre());
        reporte.setTotalVentas(totalVentas);
        reporte.setTotalEfectivo(totalEfectivo);
        reporte.setTotalTransferencia(totalTransferencia);
        reporte.setTotalGastos(totalGastos);
        reporte.setNetoEnCaja(totalEfectivo.subtract(totalGastos));
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

