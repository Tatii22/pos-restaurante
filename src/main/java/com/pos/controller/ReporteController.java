package com.pos.controller;

import com.pos.dto.report.ReporteVentaDTO;
import com.pos.dto.report.ReporteRentabilidadDTO;
import com.pos.dto.report.ReporteCierreTurnoDTO;
import com.pos.service.ReporteExportService;
import com.pos.service.ReporteRentabilidadExportService;
import com.pos.service.ReporteTurnoExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private final ReporteExportService reporteExportService;
    private final ReporteRentabilidadExportService reporteRentabilidadExportService;
    private final ReporteTurnoExportService reporteTurnoExportService;

    public ReporteController(ReporteExportService reporteExportService,
                             ReporteRentabilidadExportService reporteRentabilidadExportService,
                             ReporteTurnoExportService reporteTurnoExportService) {
        this.reporteExportService = reporteExportService;
        this.reporteRentabilidadExportService = reporteRentabilidadExportService;
        this.reporteTurnoExportService = reporteTurnoExportService;
    }

    // ----------------- VENTAS -----------------
    @PostMapping("/ventas/pdf")
    public ResponseEntity<byte[]> exportarVentasPDF(@RequestBody ReporteVentaDTO reporte) {
        byte[] data = reporteExportService.generarReporteVentasPDF(reporte);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte_ventas.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }

    @PostMapping("/ventas/excel")
    public ResponseEntity<byte[]> exportarVentasExcel(@RequestBody ReporteVentaDTO reporte) {
        byte[] data = reporteExportService.generarReporteVentasExcel(reporte);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte_ventas.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    // ----------------- RENTABILIDAD -----------------
    @PostMapping("/rentabilidad/pdf")
    public ResponseEntity<byte[]> exportarRentabilidadPDF(@RequestBody ReporteRentabilidadDTO reporte) {
        byte[] data = reporteRentabilidadExportService.generarReporteRentabilidadPDF(reporte);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte_rentabilidad.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }

    @PostMapping("/rentabilidad/excel")
    public ResponseEntity<byte[]> exportarRentabilidadExcel(@RequestBody ReporteRentabilidadDTO reporte) {
        byte[] data = reporteRentabilidadExportService.generarReporteRentabilidadExcel(reporte);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte_rentabilidad.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    // ----------------- TURNO -----------------
    @PostMapping("/turno/pdf")
    public ResponseEntity<byte[]> exportarTurnoPDF(@RequestBody ReporteCierreTurnoDTO reporte) {
        byte[] data = reporteTurnoExportService.generarReporteTurnoPDF(reporte);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte_turno.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }

    @PostMapping("/turno/excel")
    public ResponseEntity<byte[]> exportarTurnoExcel(@RequestBody ReporteCierreTurnoDTO reporte) {
        byte[] data = reporteTurnoExportService.generarReporteTurnoExcel(reporte);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte_turno.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }
}
