package com.pos.controller;

import com.pos.dto.report.ReporteVentaDTO;
import com.pos.service.report.ReporteVentaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reportes/ventas")
public class ReporteVentaController {

    private final ReporteVentaService reporteVentaService;

    public ReporteVentaController(ReporteVentaService reporteVentaService) {
        this.reporteVentaService = reporteVentaService;
    }

    /**
     * Reporte de ventas por rango de fechas
     */
    @GetMapping
    public ResponseEntity<ReporteVentaDTO> generarReporteVentas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaInicio,

            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaFin
    ) {
        ReporteVentaDTO reporte =
                reporteVentaService.generarReporteVentas(fechaInicio, fechaFin);

        return ResponseEntity.ok(reporte);
    }
}
