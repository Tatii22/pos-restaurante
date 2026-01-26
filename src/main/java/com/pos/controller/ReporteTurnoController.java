package com.pos.controller;

import com.pos.dto.report.ReporteCierreTurnoDTO;
import com.pos.service.report.ReporteTurnoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reportes/turnos")
public class ReporteTurnoController {

    private final ReporteTurnoService reporteTurnoService;

    public ReporteTurnoController(ReporteTurnoService reporteTurnoService) {
        this.reporteTurnoService = reporteTurnoService;
    }

    /**
     * Genera el reporte de cierre de un turno
     */
    @GetMapping("/{turnoId}")
    public ResponseEntity<ReporteCierreTurnoDTO> obtenerReporteTurno(
            @PathVariable Long turnoId
    ) {
        ReporteCierreTurnoDTO reporte =
                reporteTurnoService.generarReporteTurno(turnoId);

        return ResponseEntity.ok(reporte);
    }
}
