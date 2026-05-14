package com.pos.service.export.pdf;

@Service
public class PdfExportService {

    private final PdfVentasExporter ventasExporter;
    private final PdfRentabilidadExporter rentabilidadExporter;
    private final PdfTurnoExporter turnoExporter;

    public PdfExportService(
            PdfVentasExporter ventasExporter,
            PdfRentabilidadExporter rentabilidadExporter,
            PdfTurnoExporter turnoExporter
    ) {
        this.ventasExporter = ventasExporter;
        this.rentabilidadExporter = rentabilidadExporter;
        this.turnoExporter = turnoExporter;
    }

    // ================== VENTAS ==================
    public byte[] exportarVentas(ReporteVentaDTO reporte) {
        return ventasExporter.exportar(reporte);
    }

    // ================== RENTABILIDAD ==================
    public byte[] exportarRentabilidad(ReporteRentabilidadDTO reporte) {
        return rentabilidadExporter.exportar(reporte);
    }

    // ================== TURNO ==================
    public byte[] exportarTurno(ReporteCierreTurnoDTO reporte) {
        return turnoExporter.exportar(reporte);
    }
}

