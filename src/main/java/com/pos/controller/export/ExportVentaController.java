package com.pos.controller.export;

import com.pos.dto.report.ReporteVentaDTO;
import com.pos.service.export.excel.ExcelExportService;
import com.pos.service.export.pdf.PdfExportService;
import com.pos.service.report.ReporteVentaService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/export/ventas")
@PreAuthorize("hasAnyRole('ADMIN','CAJA')")
public class ExportVentaController {

    private final ReporteVentaService reporteVentaService;
    private final PdfExportService pdfExportService;
    private final ExcelExportService excelExportService;

    public ExportVentaController(
            ReporteVentaService reporteVentaService,
            PdfExportService pdfExportService,
            ExcelExportService excelExportService
    ) {
        this.reporteVentaService = reporteVentaService;
        this.pdfExportService = pdfExportService;
        this.excelExportService = excelExportService;
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> exportarVentasPDF(
            @RequestParam LocalDate fechaInicio,
            @RequestParam LocalDate fechaFin
    ) {
        ReporteVentaDTO reporte =
                reporteVentaService.generarReporteVentas(fechaInicio, fechaFin);

        byte[] pdf = pdfExportService.exportarVentas(reporte);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte_ventas.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/excel")
    public ResponseEntity<byte[]> exportarVentasExcel(
            @RequestParam LocalDate fechaInicio,
            @RequestParam LocalDate fechaFin
    ) {
        ReporteVentaDTO reporte =
                reporteVentaService.generarReporteVentas(fechaInicio, fechaFin);

        byte[] excel = excelExportService.exportarVentas(reporte);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte_ventas.xlsx")
                .contentType(
                        MediaType.parseMediaType(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        )
                )
                .body(excel);
    }
}
