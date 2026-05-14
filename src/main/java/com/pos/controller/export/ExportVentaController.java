package com.pos.controller.export;



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

