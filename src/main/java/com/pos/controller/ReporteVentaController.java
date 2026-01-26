package com.pos.controller;

import com.pos.dto.report.ReporteVentaDTO;
import com.pos.service.ReporteVentaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
@RestController
@RequestMapping("/reportes/ventas")
public class ReporteVentaController {

    private final ReporteVentaService reporteVentaService;

    public ReporteVentaController(ReporteVentaService reporteVentaService) {
        this.reporteVentaService = reporteVentaService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ReporteVentaDTO obtenerReporteVentas(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaInicio,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaFin
    ) {

        if (fechaFin.isBefore(fechaInicio)) {
            throw new IllegalArgumentException(
                    "La fecha fin no puede ser anterior a la fecha inicio"
            );
        }

        return reporteVentaService.generarReporteVentas(fechaInicio, fechaFin);
    }
}
