package com.pos.service.report;

import com.pos.dto.report.ReporteCierreTurnoDTO;
import com.pos.dto.turno.GastoCajaResponseDTO;
import com.pos.dto.venta.VentaResponseDTO;
import com.pos.entity.AbonoFiado;
import com.pos.entity.EstadoVenta;
import com.pos.entity.GastoCaja;
import com.pos.entity.MedioFinanciero;
import com.pos.entity.TurnoCaja;
import com.pos.entity.Venta;
import com.pos.repository.AbonoFiadoRepository;
import com.pos.repository.GastoCajaRepository;
import com.pos.repository.TurnoCajaRepository;
import com.pos.repository.VentaRepository;
import com.pos.service.CalculosFinancierosService;
import com.pos.service.VentaService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReporteTurnoService {

    private final TurnoCajaRepository turnoCajaRepository;
    private final VentaRepository ventaRepository;
    private final GastoCajaRepository gastoCajaRepository;
    private final AbonoFiadoRepository abonoFiadoRepository;
    private final VentaService ventaService;
    private final CalculosFinancierosService calculosFinancierosService;

    public ReporteTurnoService(
            TurnoCajaRepository turnoCajaRepository,
            VentaRepository ventaRepository,
            GastoCajaRepository gastoCajaRepository,
            AbonoFiadoRepository abonoFiadoRepository,
            VentaService ventaService,
            CalculosFinancierosService calculosFinancierosService
    ) {
        this.turnoCajaRepository = turnoCajaRepository;
        this.ventaRepository = ventaRepository;
        this.gastoCajaRepository = gastoCajaRepository;
        this.abonoFiadoRepository = abonoFiadoRepository;
        this.ventaService = ventaService;
        this.calculosFinancierosService = calculosFinancierosService;
    }

    public ReporteCierreTurnoDTO generarReporteTurno(Long turnoId) {
        if (turnoId == null) {
            throw new IllegalArgumentException("Turno ID no puede ser nulo");
        }

        TurnoCaja turno = turnoCajaRepository.findById(turnoId)
                .orElseThrow(() -> new RuntimeException("Turno no encontrado: " + turnoId));

        LocalDateTime inicio = turno.getFechaApertura();
        LocalDateTime fin = turno.getFechaCierre() != null ? turno.getFechaCierre() : LocalDateTime.now();

        List<Venta> ventas = ventaRepository.findByTurnoAndEstadoAndFechaBetween(
                turno,
                EstadoVenta.DESPACHADA,
                inicio,
                fin
        );

        List<GastoCaja> gastos = gastoCajaRepository.findByTurnoAndFechaBetween(turno, inicio, fin);

        BigDecimal totalVentas = ventas.stream()
                .map(v -> v.getTotal() != null ? v.getTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalEfectivo = calculosFinancierosService.sumarRecaudoTurno(turno, MedioFinanciero.EFECTIVO);
        BigDecimal totalTransferencia = calculosFinancierosService.sumarRecaudoTurno(turno, MedioFinanciero.TRANSFERENCIA);
        BigDecimal totalGastosEfectivo = calculosFinancierosService.sumarGastosPorTurno(turno, MedioFinanciero.EFECTIVO);
        BigDecimal totalGastosTransferencia = calculosFinancierosService.sumarGastosPorTurno(turno, MedioFinanciero.TRANSFERENCIA);
        BigDecimal totalGastos = totalGastosEfectivo.add(totalGastosTransferencia);

        BigDecimal totalAbonos = BigDecimal.ZERO;
        BigDecimal totalAbonosEfectivo = BigDecimal.ZERO;
        BigDecimal totalAbonosTransferencia = BigDecimal.ZERO;
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
        BigDecimal cajaFisicaEsperada = turno.getMontoInicial()
                .add(totalEfectivo)
                .add(totalAbonosEfectivo)
                .subtract(totalGastosEfectivo);
        BigDecimal transferenciasNetas = totalTransferencia
                .add(totalAbonosTransferencia)
                .subtract(totalGastosTransferencia);
        BigDecimal totalOperativoTurno = cajaFisicaEsperada.add(transferenciasNetas);
        BigDecimal cajaContada = turno.getMontoFinal();
        BigDecimal diferenciaCaja = cajaContada != null ? cajaFisicaEsperada.subtract(cajaContada) : turno.getFaltante();

        reporte.setGananciaEfectivo(cajaFisicaEsperada.subtract(turno.getMontoInicial()));
        reporte.setGananciaTransferencia(transferenciasNetas);
        reporte.setNetoEnCaja(cajaFisicaEsperada);
        reporte.setCajaFisicaEsperada(cajaFisicaEsperada);
        reporte.setTransferenciasNetas(transferenciasNetas);
        reporte.setTotalOperativoTurno(totalOperativoTurno);
        reporte.setCajaContada(cajaContada);
        reporte.setDiferenciaCaja(diferenciaCaja);

        // Conciliación dual (si ya se cerró el turno)
        reporte.setEfectivoContado(turno.getEfectivoContado());
        reporte.setTransferenciasVerificadas(turno.getTransferenciasVerificadas());
        reporte.setDiferenciaEfectivo(turno.getDiferenciaEfectivo());
        reporte.setDiferenciaTransferencias(turno.getDiferenciaTransferencias());
        reporte.setTotalVerificado(turno.getTotalVerificado());
        reporte.setDiferenciaTotal(turno.getDiferenciaTotal());

        reporte.setVentas(mapVentas(ventas));
        reporte.setGastos(mapGastos(gastos));

        return reporte;
    }

    private List<VentaResponseDTO> mapVentas(List<Venta> ventas) {
        return ventas.stream().map(ventaService::construirRespuesta).toList();
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
