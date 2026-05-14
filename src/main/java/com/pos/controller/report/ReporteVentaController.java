package com.pos.controller.report;



@RestController
@RequestMapping("/reportes/ventas")
@PreAuthorize("hasAnyRole('ADMIN','CAJA')")
public class ReporteVentaController {

    private final ReporteVentaService reporteVentaService;

    public ReporteVentaController(ReporteVentaService reporteVentaService) {
        this.reporteVentaService = reporteVentaService;
    }

    @GetMapping
    public ResponseEntity<ReporteVentaDTO> obtenerReporteVentas(
            @RequestParam LocalDate fechaInicio,
            @RequestParam LocalDate fechaFin
    ) {
        ReporteVentaDTO reporte =
                reporteVentaService.generarReporteVentas(fechaInicio, fechaFin);

        return ResponseEntity.ok(reporte);
    }
}

