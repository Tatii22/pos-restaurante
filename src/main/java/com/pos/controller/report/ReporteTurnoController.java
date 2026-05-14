package com.pos.controller.report;

import com.pos.dto.report.ReporteCierreTurnoDTO;
import com.pos.service.report.ReporteTurnoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
