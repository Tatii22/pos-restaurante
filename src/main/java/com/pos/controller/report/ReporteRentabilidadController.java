package com.pos.controller.report;
import com.pos.dto.report.ReporteRentabilidadDTO;
import com.pos.service.report.ReporteRentabilidadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/reportes/rentabilidad")
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
