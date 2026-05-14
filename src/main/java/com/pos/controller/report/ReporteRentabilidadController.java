package com.pos.controller.report;



@RestController
@RequestMapping("/reportes/rentabilidad")
@PreAuthorize("hasAnyRole('ADMIN','CAJA')")
public class ReporteRentabilidadController {

    private final ReporteRentabilidadService reporteRentabilidadService;

    public ReporteRentabilidadController(
            ReporteRentabilidadService reporteRentabilidadService
    ) {
        this.reporteRentabilidadService = reporteRentabilidadService;
    }

    @GetMapping
    public ResponseEntity<ReporteRentabilidadDTO> obtenerReporteRentabilidad(
            @RequestParam LocalDate fechaInicio,
            @RequestParam LocalDate fechaFin
    ) {
        ReporteRentabilidadDTO reporte =
                reporteRentabilidadService.generarReporte(fechaInicio, fechaFin);

        return ResponseEntity.ok(reporte);
    }
}

