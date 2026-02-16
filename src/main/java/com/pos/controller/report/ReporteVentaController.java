package com.pos.controller.report;

import com.pos.dto.report.ReporteVentaDTO;
import com.pos.service.report.ReporteVentaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

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
