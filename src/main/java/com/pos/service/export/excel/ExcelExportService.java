package com.pos.service.export.excel;
import com.pos.dto.report.ReporteCierreTurnoDTO;
import com.pos.dto.report.ReporteRentabilidadDTO;
import com.pos.dto.report.ReporteVentaDTO;
import org.springframework.stereotype.Service;

@Service
public class ExcelExportService {

    private final ExcelVentasExporter ventasExporter;
    private final ExcelRentabilidadExporter rentabilidadExporter;
    private final ExcelTurnoExporter turnoExporter;

    public ExcelExportService(
            ExcelVentasExporter ventasExporter,
            ExcelRentabilidadExporter rentabilidadExporter,
            ExcelTurnoExporter turnoExporter
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

