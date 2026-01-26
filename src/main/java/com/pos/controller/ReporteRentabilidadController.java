package com.pos.controller;

import com.pos.dto.report.ReporteRentabilidadDTO;
import com.pos.service.report.ReporteRentabilidadService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reportes/rentabilidad")
public class ReporteRentabilidadController {

    private final ReporteRentabilidadService reporteRentabilidadService;

    public ReporteRentabilidadController(ReporteRentabilidadService reporteRentabilidadService) {
        this.reporteRentabilidadService = reporteRentabilidadService;
    }

    /**
     * Reporte de rentabilidad (Ventas vs Gastos)
     */
    @GetMapping
    public ResponseEntity<ReporteRentabilidadDTO> generarReporteRentabilidad(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaInicio,

            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaFin
    ) {
        ReporteRentabilidadDTO reporte =
                reporteRentabilidadService.generarReporte(fechaInicio, fechaFin);

        return ResponseEntity.ok(reporte);
    }
}
