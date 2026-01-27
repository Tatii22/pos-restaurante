package com.pos.controller.export;

import com.pos.dto.report.ReporteCierreTurnoDTO;
import com.pos.service.report.ReporteTurnoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.pos.service.export.excel.ExcelExportService;
import com.pos.service.export.pdf.PdfExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api/export/turnos")
public class ExportTurnoController {

    private final ReporteTurnoService reporteTurnoService;
    private final PdfExportService pdfExportService;
    private final ExcelExportService excelExportService;

    public ExportTurnoController(
            ReporteTurnoService reporteTurnoService,
            PdfExportService pdfExportService,
            ExcelExportService excelExportService
    ) {
        this.reporteTurnoService = reporteTurnoService;
        this.pdfExportService = pdfExportService;
        this.excelExportService = excelExportService;
    }

    // ================== PDF ==================
    @GetMapping("/pdf")
    public ResponseEntity<byte[]> exportarTurnoPDF(@RequestParam Long turnoId) {

        ReporteCierreTurnoDTO reporte =
                reporteTurnoService.generarReporteTurno(turnoId);

        byte[] pdf = pdfExportService.exportarTurno(reporte);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte_turno.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // ================== EXCEL ==================
    @GetMapping("/excel")
    public ResponseEntity<byte[]> exportarTurnoExcel(@RequestParam Long turnoId) {

        ReporteCierreTurnoDTO reporte =
                reporteTurnoService.generarReporteTurno(turnoId);

        byte[] excel = excelExportService.exportarTurno(reporte);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte_turno.xlsx")
                .contentType(
                        MediaType.parseMediaType(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        )
                )
                .body(excel);
    }
}

