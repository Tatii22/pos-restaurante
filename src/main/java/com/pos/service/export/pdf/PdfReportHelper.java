package com.pos.service.export.pdf;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Helper profesional para generar reportes PDF consistentes y elegantes.
 * Centraliza estilos, espaciado, jerarquía y formato monetario COP.
 */
public final class PdfReportHelper {

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DeviceRgb HEADER_BG = new DeviceRgb(38, 64, 115);      // Azul corporativo
    private static final com.itextpdf.kernel.colors.Color HEADER_TEXT = ColorConstants.WHITE;
    private static final DeviceRgb ALT_ROW = new DeviceRgb(245, 245, 250);
    private static final DeviceRgb POSITIVE = new DeviceRgb(26, 115, 64);
    private static final DeviceRgb NEGATIVE = new DeviceRgb(166, 38, 38);

    private PdfReportHelper() {}

    // ================== HEADER PROFESIONAL ==================
    public static void addProfessionalHeader(Document doc, String tituloReporte, String rangoFechas, String generadoPor) {
        // Título principal
        Paragraph titulo = new Paragraph(tituloReporte)
                .setFontSize(20)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(4);
        doc.add(titulo);

        // Subtítulo con rango
        if (rangoFechas != null && !rangoFechas.isBlank()) {
            Paragraph subtitulo = new Paragraph(rangoFechas)
                    .setFontSize(11)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontColor(ColorConstants.DARK_GRAY)
                    .setMarginBottom(2);
            doc.add(subtitulo);
        }

        // Info de generación
        String generado = "Generado: " + LocalDateTime.now().format(FECHA_HORA);
        if (generadoPor != null) generado += "  •  " + generadoPor;

        Paragraph info = new Paragraph(generado)
                .setFontSize(9)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(ColorConstants.GRAY)
                .setMarginBottom(12);
        doc.add(info);

        // Línea separadora
        Table linea = new Table(UnitValue.createPercentArray(1))
                .useAllAvailableWidth()
                .setMarginBottom(10);
        linea.addCell(new Cell()
                .setHeight(1.5f)
                .setBackgroundColor(HEADER_BG)
                .setBorder(null));
        doc.add(linea);
    }

    // ================== SECCIÓN DE RESUMEN (KPI style) ==================
    public static void addSummarySection(Document doc, String tituloSeccion, java.util.Map<String, BigDecimal> metricas) {
        Paragraph seccion = new Paragraph(tituloSeccion)
                .setFontSize(13)
                .setBold()
                .setMarginTop(8)
                .setMarginBottom(6)
                .setFontColor(HEADER_BG);
        doc.add(seccion);

        // Regular KPIs in 3 columns
        Table tabla = new Table(UnitValue.createPercentArray(new float[]{33.3f, 33.3f, 33.3f}))
                .useAllAvailableWidth()
                .setMarginBottom(4);

        int col = 0;
        BigDecimal balanceValue = null;
        String balanceLabel = null;

        for (java.util.Map.Entry<String, BigDecimal> entry : metricas.entrySet()) {
            if (entry.getKey().equalsIgnoreCase("Balance final del turno")) {
                balanceLabel = entry.getKey();
                balanceValue = entry.getValue();
                continue; // lo renderizamos después como destacado
            }
            Cell celda = createKpiCell(entry.getKey(), entry.getValue(), false);
            tabla.addCell(celda);
            col++;
        }
        while (col % 3 != 0) {
            tabla.addCell(new Cell().setBorder(null));
            col++;
        }
        doc.add(tabla);

        // Balance final del turno - KPI PRINCIPAL destacado
        if (balanceLabel != null) {
            Cell balanceCell = createKpiCell(balanceLabel, balanceValue, true);
            Table balanceTable = new Table(UnitValue.createPercentArray(1))
                    .useAllAvailableWidth()
                    .setMarginTop(4)
                    .setMarginBottom(12);
            balanceTable.addCell(balanceCell);
            doc.add(balanceTable);
        }
    }

    private static Cell createKpiCell(String label, BigDecimal valor, boolean destacado) {
        DeviceRgb bg = destacado ? new DeviceRgb(232, 245, 233) : ALT_ROW; // verde muy suave para destacado
        float borderWidth = destacado ? 1.5f : 0.5f;
        com.itextpdf.layout.borders.Border border = new com.itextpdf.layout.borders.SolidBorder(
                destacado ? new DeviceRgb(76, 175, 80) : ColorConstants.LIGHT_GRAY, borderWidth);

        Cell cell = new Cell()
                .setPadding(destacado ? 12 : 8)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBackgroundColor(bg)
                .setBorder(border);

        Paragraph lbl = new Paragraph(label)
                .setFontSize(destacado ? 10 : 8)
                .setBold(destacado)
                .setFontColor(ColorConstants.DARK_GRAY)
                .setMarginBottom(2);

        String numStr = String.format("$%,.0f", valor != null ? valor : BigDecimal.ZERO);
        Paragraph num = new Paragraph(numStr)
                .setFontSize(destacado ? 18 : 14)
                .setBold(true)
                .setFontColor(valor != null && valor.compareTo(BigDecimal.ZERO) < 0 ? NEGATIVE : POSITIVE);

        cell.add(lbl);
        cell.add(num);
        return cell;
    }

    // ================== TABLA ESTILIZADA ==================
    public static Table createStyledTable(float[] widths) {
        Table table = new Table(UnitValue.createPercentArray(widths))
                .useAllAvailableWidth()
                .setMarginTop(4)
                .setMarginBottom(8);
        return table;
    }

    public static void addTableHeader(Table table, String[] headers) {
        for (String h : headers) {
            Cell headerCell = new Cell()
                    .add(new Paragraph(h)
                            .setBold()
                            .setFontSize(9)
                            .setFontColor(HEADER_TEXT))
                    .setBackgroundColor(HEADER_BG)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE)
                    .setPadding(6);
            table.addHeaderCell(headerCell);
        }
    }

    public static Cell createDataCell(String text, boolean rightAlign, boolean zebra, boolean money) {
        Cell cell = new Cell()
                .add(new Paragraph(text)
                        .setFontSize(9)
                        .setTextAlignment(rightAlign ? TextAlignment.RIGHT : TextAlignment.LEFT))
                .setPadding(5)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);

        if (zebra) {
            cell.setBackgroundColor(ALT_ROW);
        }
        if (money) {
            cell.setTextAlignment(TextAlignment.RIGHT);
        }
        return cell;
    }

    // ================== FOOTER ==================
    public static void addFooter(Document doc, String sistema) {
        doc.add(new Paragraph("\n"));
        Paragraph footer = new Paragraph(sistema + " • Reporte generado automáticamente • " + LocalDateTime.now().format(FECHA_HORA))
                .setFontSize(8)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(ColorConstants.GRAY);
        doc.add(footer);
    }

    // Utilidad para formato monetario seguro
    public static String formatMoney(BigDecimal valor) {
        return String.format("$%,.2f", valor != null ? valor : BigDecimal.ZERO);
    }
}
