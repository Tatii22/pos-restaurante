package com.pos.controller;

import com.pos.dto.report.ReporteRentabilidadDTO;
import com.pos.service.ReporteRentabilidadService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/reportes/rentabilidad")
public class ReporteRentabilidadController {

    private final ReporteRentabilidadService reporteRentabilidadService;

    public ReporteRentabilidadController(ReporteRentabilidadService reporteRentabilidadService) {
        this.reporteRentabilidadService = reporteRentabilidadService;
    }


    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReporteRentabilidadDTO> obtenerReporte(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin
    ) {
        ReporteRentabilidadDTO reporte = reporteRentabilidadService.generarReporte(fechaInicio, fechaFin);
        return ResponseEntity.ok(reporte);
    }
}
