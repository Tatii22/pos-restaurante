package com.pos.controller.report;


@RestController
@RequestMapping("/reportes/turnos")
@PreAuthorize("hasAnyRole('ADMIN','CAJA')")
public class ReporteTurnoController {

    private final ReporteTurnoService reporteTurnoService;

    public ReporteTurnoController(ReporteTurnoService reporteTurnoService) {
        this.reporteTurnoService = reporteTurnoService;
    }

    @GetMapping("/{turnoId}")
    public ResponseEntity<ReporteCierreTurnoDTO> obtenerReporteTurno(
            @PathVariable Long turnoId
    ) {
        ReporteCierreTurnoDTO reporte =
                reporteTurnoService.generarReporteTurno(turnoId);

        return ResponseEntity.ok(reporte);
    }
}

