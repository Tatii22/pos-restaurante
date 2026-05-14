package com.pos.controller.export;

import com.pos.dto.report.ReporteRentabilidadDTO;
import com.pos.service.export.excel.ExcelExportService;
import com.pos.service.export.pdf.PdfExportService;
import com.pos.service.report.ReporteRentabilidadService;
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
@RequestMapping("/export/rentabilidad")
@PreAuthorize("hasAnyRole('ADMIN','CAJA')")
public class ExportRentabilidadController {

    private final ReporteRentabilidadService reporteRentabilidadService;
    private final PdfExportService pdfExportService;
    private final ExcelExportService excelExportService;

    public ExportRentabilidadController(
            ReporteRentabilidadService reporteRentabilidadService,
            PdfExportService pdfExportService,
            ExcelExportService excelExportService
    ) {
        this.reporteRentabilidadService = reporteRentabilidadService;
        this.pdfExportService = pdfExportService;
        this.excelExportService = excelExportService;
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> exportarRentabilidadPDF(
            @RequestParam LocalDate fechaInicio,
            @RequestParam LocalDate fechaFin
    ) {
        ReporteRentabilidadDTO reporte =
                reporteRentabilidadService.generarReporte(fechaInicio, fechaFin);

        byte[] pdf = pdfExportService.exportarRentabilidad(reporte);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte_rentabilidad.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/excel")
    public ResponseEntity<byte[]> exportarRentabilidadExcel(
            @RequestParam LocalDate fechaInicio,
            @RequestParam LocalDate fechaFin
    ) {
        ReporteRentabilidadDTO reporte =
                reporteRentabilidadService.generarReporte(fechaInicio, fechaFin);

        byte[] excel = excelExportService.exportarRentabilidad(reporte);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte_rentabilidad.xlsx")
                .contentType(
                        MediaType.parseMediaType(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        )
                )
                .body(excel);
    }
}
