package com.pos.service.export.pdf;

import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.pos.dto.report.ReporteRentabilidadDTO;
import com.pos.dto.gasto.GastoResponseDTO;
import com.pos.dto.venta.VentaResponseDTO;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PdfRentabilidadExporter {

    private static final DateTimeFormatter FECHA_FORMATO =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final String[] MESES = {
        "Enero","Febrero","Marzo","Abril","Mayo","Junio",
        "Julio","Agosto","Septiembre","Octubre","Noviembre","Diciembre"
    };

    private String mesAnio(LocalDate d) {
        return MESES[d.getMonthValue() - 1] + " " + d.getYear();
    }

    public byte[] exportar(ReporteRentabilidadDTO reporte) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);
            document.setMargins(36, 36, 36, 36);

            String periodo = mesAnio(reporte.getFechaInicio());
            String rango = "Período: " + reporte.getFechaInicio() + " → " + reporte.getFechaFin();
            PdfReportHelper.addProfessionalHeader(document, "INFORME DE CIERRE DE MES — " + periodo.toUpperCase(), rango, "Usuario: MentaPOS");

            // === RESUMEN OPERATIVO (usando "Resultado Operativo" en lugar de Ganancia Neta) ===
            Map<String, BigDecimal> resumen = new LinkedHashMap<>();
            resumen.put("Ingresos recibidos", reporte.getRecaudoReal());
            resumen.put("Gastos registrados", reporte.getTotalGastos());
            resumen.put("Ganancia neta del mes", reporte.getGananciaNeta());
            PdfReportHelper.addSummarySection(document, "RESUMEN FINANCIERO", resumen);

            // === DETALLE VENTAS ===
            Paragraph sec1 = new Paragraph("DETALLE DE VENTAS")
                    .setFontSize(12).setBold().setMarginTop(10).setMarginBottom(6);
            document.add(sec1);

            float[] anchosV = {8, 18, 12, 12, 22, 14, 14};
            Table tablaV = PdfReportHelper.createStyledTable(anchosV);
            PdfReportHelper.addTableHeader(tablaV, new String[]{"#", "Fecha", "Tipo", "Estado", "Cliente", "Total", "Forma Pago"});

            int idx = 1;
            boolean zebra = false;
            for (VentaResponseDTO v : reporte.getVentas()) {
                tablaV.addCell(PdfReportHelper.createDataCell(String.valueOf(idx++), false, zebra, false));
                tablaV.addCell(PdfReportHelper.createDataCell(v.fecha().format(FECHA_FORMATO), false, zebra, false));
                tablaV.addCell(PdfReportHelper.createDataCell(v.tipoVenta().name(), false, zebra, false));
                tablaV.addCell(PdfReportHelper.createDataCell(v.estado().name(), false, zebra, false));
                tablaV.addCell(PdfReportHelper.createDataCell(v.clienteNombre() != null ? v.clienteNombre() : "-", false, zebra, false));
                tablaV.addCell(PdfReportHelper.createDataCell(PdfReportHelper.formatMoney(v.total()), true, zebra, true));
                String pago = v.formaPago() == com.pos.entity.FormaPago.FIADO ? "ABONO" : v.formaPago().name();
                tablaV.addCell(PdfReportHelper.createDataCell(pago, false, zebra, false));
                zebra = !zebra;
            }
            document.add(tablaV);

            // === DETALLE GASTOS ===
            Paragraph sec2 = new Paragraph("DETALLE DE GASTOS")
                    .setFontSize(12).setBold().setMarginTop(12).setMarginBottom(6);
            document.add(sec2);

            float[] anchosG = {8, 18, 50, 14};
            Table tablaG = PdfReportHelper.createStyledTable(anchosG);
            PdfReportHelper.addTableHeader(tablaG, new String[]{"#", "Fecha", "Descripción", "Valor"});

            idx = 1;
            zebra = false;
            for (GastoResponseDTO g : reporte.getGastos()) {
                tablaG.addCell(PdfReportHelper.createDataCell(String.valueOf(idx++), false, zebra, false));
                tablaG.addCell(PdfReportHelper.createDataCell(g.getFecha().format(FECHA_FORMATO), false, zebra, false));
                tablaG.addCell(PdfReportHelper.createDataCell(g.getDescripcion(), false, zebra, false));
                tablaG.addCell(PdfReportHelper.createDataCell(PdfReportHelper.formatMoney(g.getMonto()), true, zebra, true));
                zebra = !zebra;
            }
            document.add(tablaG);

            PdfReportHelper.addFooter(document, "MentaPOS • Cierre de Mes — " + periodo);

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF de Rentabilidad", e);
        }
    }
}

