package com.pos.controller;
import com.pos.dto.report.ReporteCierreTurnoDTO;
import com.pos.service.ReporteTurnoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reportes/turno")
public class ReporteTurnoController {

    private final ReporteTurnoService reporteTurnoService;

    public ReporteTurnoController(ReporteTurnoService reporteTurnoService) {
        this.reporteTurnoService = reporteTurnoService;
    }

    @GetMapping("/{turnoId}")
    @PreAuthorize("hasRole('ADMIN')") 
    public ResponseEntity<ReporteCierreTurnoDTO> obtenerReporteTurno(@PathVariable Long turnoId) {
        ReporteCierreTurnoDTO reporte = reporteTurnoService.generarReporteTurno(turnoId);
        return ResponseEntity.ok(reporte);
    }
}

