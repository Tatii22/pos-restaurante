package com.pos.service.report;


@Service
public class ReporteTurnoService {

    private final TurnoCajaRepository turnoCajaRepository;
    private final VentaRepository ventaRepository;
    private final GastoCajaRepository gastoCajaRepository;
    private final VentaService ventaService;

    public ReporteTurnoService(
            TurnoCajaRepository turnoCajaRepository,
            VentaRepository ventaRepository,
            GastoCajaRepository gastoCajaRepository,
            VentaService ventaService
    ) {
        this.turnoCajaRepository = turnoCajaRepository;
        this.ventaRepository = ventaRepository;
        this.gastoCajaRepository = gastoCajaRepository;
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

        for (Venta venta : ventas) {
            BigDecimal totalFinal = venta.getTotal().subtract(obtenerDescuento(venta));
            VentaResponseDTO ventaDTO = ventaService.construirRespuesta(venta);
            totalVentas = totalVentas.add(totalFinal);
            totalEfectivo = totalEfectivo.add(
                    ventaDTO.pagoEfectivo() != null ? ventaDTO.pagoEfectivo() : BigDecimal.ZERO
            );
            totalTransferencia = totalTransferencia.add(
                    ventaDTO.pagoTransferencia() != null ? ventaDTO.pagoTransferencia() : BigDecimal.ZERO
            );
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
        reporte.setGananciaEfectivo(totalEfectivo.subtract(totalGastosEfectivo));
        reporte.setGananciaTransferencia(totalTransferencia.subtract(totalGastosTransferencia));
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


