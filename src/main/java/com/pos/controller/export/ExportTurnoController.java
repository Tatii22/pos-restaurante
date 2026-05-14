package com.pos.controller.export;

import com.pos.dto.report.ReporteCierreTurnoDTO;
import com.pos.service.export.excel.ExcelExportService;
import com.pos.service.export.pdf.PdfExportService;
import com.pos.service.report.ReporteTurnoService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/export/turnos")
@PreAuthorize("hasAnyRole('ADMIN','CAJA')")
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
