package com.pos.service;
import com.pos.dto.report.ReporteCierreTurnoDTO;
import com.pos.dto.turno.GastoCajaResponseDTO;
import com.pos.dto.venta.VentaResponseDTO;
import com.pos.entity.EstadoVenta;
import com.pos.entity.FormaPago;
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


@Service
public class ReporteTurnoService {

    private final TurnoCajaRepository turnoCajaRepository;
    private final VentaRepository ventaRepository;
    private final GastoCajaRepository gastoCajaRepository;

    public ReporteTurnoService(TurnoCajaRepository turnoCajaRepository,
                               VentaRepository ventaRepository,
                               GastoCajaRepository gastoCajaRepository) {
        this.turnoCajaRepository = turnoCajaRepository;
        this.ventaRepository = ventaRepository;
        this.gastoCajaRepository = gastoCajaRepository;
    }

    public ReporteCierreTurnoDTO generarReporteTurno(Long turnoId) {
        
        if (turnoId == null) {
            throw new IllegalArgumentException("Turno ID no puede ser nulo");
        }

        // 1️⃣ Traer el turno
        TurnoCaja turno = turnoCajaRepository.findById(turnoId)
                .orElseThrow(() -> new RuntimeException("Turno no encontrado: " + turnoId));

        LocalDateTime inicio = turno.getFechaApertura();
        LocalDateTime fin = turno.getFechaCierre() != null ? turno.getFechaCierre() : LocalDateTime.now();

        // 2️⃣ Traer ventas despachadas dentro del turno
        List<Venta> ventas = ventaRepository.findByTurnoAndEstadoAndFechaBetween(
                turno,
                EstadoVenta.DESPACHADA,
                inicio,
                fin
        );

        // 3️⃣ Traer gastos del turno dentro del rango
        List<GastoCaja> gastos = gastoCajaRepository.findByTurnoAndFechaBetween(turno, inicio, fin);

        // 4️⃣ Calcular totales
        BigDecimal totalVentas = BigDecimal.ZERO;
        BigDecimal totalEfectivo = BigDecimal.ZERO;
        BigDecimal totalTransferencia = BigDecimal.ZERO;
        BigDecimal totalGastos = BigDecimal.ZERO;

        for (Venta venta : ventas) {
            BigDecimal descuento = venta.getDescuentoValor() != null ? venta.getDescuentoValor() : BigDecimal.ZERO;
            BigDecimal totalFinal = venta.getTotal().subtract(descuento);
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

        BigDecimal netoEnCaja = totalEfectivo.subtract(totalGastos);

        // 5️⃣ Mapear ventas a DTO
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

        // 6️⃣ Mapear gastos a DTO
        List<GastoCajaResponseDTO> gastosDTO = gastos.stream()
                .map(g -> new GastoCajaResponseDTO(
                        g.getId(),
                        g.getFecha(),
                        g.getDescripcion(),
                        g.getMonto()
                ))
                .toList();

        // 7️⃣ Crear reporte final
        ReporteCierreTurnoDTO reporte = new ReporteCierreTurnoDTO();
        reporte.setTurnoId(turno.getId());
        reporte.setApertura(turno.getFechaApertura());
        reporte.setCierre(turno.getFechaCierre());
        reporte.setTotalVentas(totalVentas);
        reporte.setTotalEfectivo(totalEfectivo);
        reporte.setTotalTransferencia(totalTransferencia);
        reporte.setTotalGastos(totalGastos);
        reporte.setNetoEnCaja(netoEnCaja);
        reporte.setVentas(ventasDTO);
        reporte.setGastos(gastosDTO);

        return reporte;
    }
}
