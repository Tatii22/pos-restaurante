package com.pos.service.export.pdf;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.pos.dto.venta.VentaResponseDTO;
import com.pos.dto.report.ReporteVentaDTO;
import com.pos.entity.CondicionPago;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class PdfVentasExporter {

    private static final DateTimeFormatter FECHA_FORMATO =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public byte[] exportar(ReporteVentaDTO reporte) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);
            document.setMargins(36, 36, 36, 36); // márgenes profesionales

            // === HEADER PROFESIONAL ===
            String rango = "Período: " + reporte.getFechaInicio() + " → " + reporte.getFechaFin();
            PdfReportHelper.addProfessionalHeader(document, "REPORTE DE VENTAS", rango, "Usuario: Sistema POS");

            // === RESUMEN (KPI dashboard style) ===
            Map<String, BigDecimal> resumen = new LinkedHashMap<>();
            resumen.put("Ventas realizadas", reporte.getTotalVentas() != null ? BigDecimal.valueOf(reporte.getTotalVentas()) : BigDecimal.ZERO);
            resumen.put("Ingresos recibidos", reporte.getRecaudoReal());
            resumen.put("Gastos registrados", reporte.getTotalGastos() != null ? reporte.getTotalGastos() : BigDecimal.ZERO);
            resumen.put("Balance final del turno", reporte.getRecaudoReal() != null && reporte.getTotalGastos() != null ? 
                    reporte.getRecaudoReal().subtract(reporte.getTotalGastos() != null ? reporte.getTotalGastos() : BigDecimal.ZERO) : BigDecimal.ZERO);
            PdfReportHelper.addSummarySection(document, "RESUMEN FINANCIERO", resumen);

            // === TABLA DETALLE DE VENTAS ===
            Paragraph seccion = new Paragraph("DETALLE DE VENTAS")
                    .setFontSize(12)
                    .setBold()
                    .setMarginTop(8)
                    .setMarginBottom(6);
            document.add(seccion);

            float[] anchos = {8, 18, 12, 12, 22, 14, 14};
            Table tabla = PdfReportHelper.createStyledTable(anchos);

            String[] headers = {"#", "Fecha", "Tipo", "Estado", "Cliente", "Total", "Forma Pago"};
            PdfReportHelper.addTableHeader(tabla, headers);

            int index = 1;
            boolean zebra = false;
            for (VentaResponseDTO v : reporte.getVentas()) {
                tabla.addCell(PdfReportHelper.createDataCell(String.valueOf(index++), false, zebra, false));
                tabla.addCell(PdfReportHelper.createDataCell(v.fecha().format(FECHA_FORMATO), false, zebra, false));
                tabla.addCell(PdfReportHelper.createDataCell(v.tipoVenta().name(), false, zebra, false));
                tabla.addCell(PdfReportHelper.createDataCell(v.estado().name(), false, zebra, false));
                tabla.addCell(PdfReportHelper.createDataCell(v.clienteNombre() != null ? v.clienteNombre() : "-", false, zebra, false));

                Cell totalCell = PdfReportHelper.createDataCell(
                        PdfReportHelper.formatMoney(v.total()), true, zebra, true);
                tabla.addCell(totalCell);

                String pago = (v.condicionPago() == CondicionPago.FIADO) ? "Abono" : v.formaPago().name();
                tabla.addCell(PdfReportHelper.createDataCell(pago, false, zebra, false));

                zebra = !zebra;
            }

            document.add(tabla);

            // === FOOTER ===
            PdfReportHelper.addFooter(document, "POS Restaurante • Reporte Financiero");

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF de ventas", e);
        }
    }
}
