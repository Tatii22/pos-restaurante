package com.pos.service.export.pdf;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.pos.dto.report.ReporteCierreTurnoDTO;
import com.pos.dto.turno.GastoCajaResponseDTO;
import com.pos.dto.venta.VentaResponseDTO;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PdfTurnoExporter {

    private static final DateTimeFormatter FECHA_FORMATO =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public byte[] exportar(ReporteCierreTurnoDTO reporte) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);
            document.setMargins(36, 36, 36, 36);

            String titulo = "REPORTE DE CIERRE DE TURNO #" + reporte.getTurnoId();
            String rango = "Apertura: " + reporte.getApertura() + "  |  Cierre: " + reporte.getCierre();
            PdfReportHelper.addProfessionalHeader(document, titulo, rango, "Cajero: " + (reporte.getCierre() != null ? "Sistema" : ""));

            // === RESUMEN OPERATIVO (sin base) ===
            Map<String, BigDecimal> resumen = new LinkedHashMap<>();
            resumen.put("Ventas realizadas", reporte.getTotalVentas());
            resumen.put("Ingresos recibidos", reporte.getGananciaEfectivo() != null && reporte.getGananciaTransferencia() != null 
                    ? reporte.getGananciaEfectivo().add(reporte.getGananciaTransferencia()) : BigDecimal.ZERO);
            resumen.put("Gastos registrados", reporte.getTotalGastos());
            BigDecimal balanceTurno = (reporte.getGananciaEfectivo() != null && reporte.getGananciaTransferencia() != null)
                    ? reporte.getGananciaEfectivo().add(reporte.getGananciaTransferencia()).subtract(reporte.getTotalGastos() != null ? reporte.getTotalGastos() : BigDecimal.ZERO)
                    : BigDecimal.ZERO;
            resumen.put("Balance final del turno", balanceTurno);
            PdfReportHelper.addSummarySection(document, "RESUMEN FINANCIERO", resumen);

            // === ARQUEO (con base) - tabla simple de 2 columnas ===
            Paragraph arqueoTitle = new Paragraph("DETALLE DE ARQUEO Y CIERRE DUAL")
                    .setFontSize(12).setBold().setMarginTop(10).setMarginBottom(4);
            document.add(arqueoTitle);

            Table arqueo = new Table(new float[]{50, 50}).useAllAvailableWidth();
            arqueo.addCell(PdfReportHelper.createDataCell("Base de caja (inicial)", false, false, false));
            arqueo.addCell(PdfReportHelper.createDataCell(PdfReportHelper.formatMoney(reporte.getCajaFisicaEsperada() != null && reporte.getGananciaEfectivo() != null ?
                    reporte.getCajaFisicaEsperada().subtract(reporte.getGananciaEfectivo()) : BigDecimal.ZERO), true, false, true));

            arqueo.addCell(PdfReportHelper.createDataCell("Efectivo Contado", false, true, false));
            arqueo.addCell(PdfReportHelper.createDataCell(PdfReportHelper.formatMoney(reporte.getEfectivoContado()), true, true, true));

            arqueo.addCell(PdfReportHelper.createDataCell("Transferencias Verificadas", false, false, false));
            arqueo.addCell(PdfReportHelper.createDataCell(PdfReportHelper.formatMoney(reporte.getTransferenciasVerificadas()), true, false, true));

            arqueo.addCell(PdfReportHelper.createDataCell("TOTAL VERIFICADO", false, true, false));
            arqueo.addCell(PdfReportHelper.createDataCell(PdfReportHelper.formatMoney(reporte.getTotalVerificado()), true, true, true));

            arqueo.addCell(PdfReportHelper.createDataCell("DIFERENCIA TOTAL", false, false, false));
            arqueo.addCell(PdfReportHelper.createDataCell(PdfReportHelper.formatMoney(reporte.getDiferenciaTotal()), true, false, true));

            document.add(arqueo);

            // === TABLAS DETALLE ===
            document.add(new Paragraph("DETALLE DE VENTAS").setFontSize(11).setBold().setMarginTop(12));
            float[] wV = {8, 18, 12, 12, 22, 14, 14};
            Table tVentas = PdfReportHelper.createStyledTable(wV);
            PdfReportHelper.addTableHeader(tVentas, new String[]{"#", "Fecha", "Tipo", "Estado", "Cliente", "Total", "Pago"});
            int i = 1;
            boolean z = false;
            for (VentaResponseDTO v : reporte.getVentas()) {
                tVentas.addCell(PdfReportHelper.createDataCell(String.valueOf(i++), false, z, false));
                tVentas.addCell(PdfReportHelper.createDataCell(v.fecha().format(FECHA_FORMATO), false, z, false));
                tVentas.addCell(PdfReportHelper.createDataCell(v.tipoVenta().name(), false, z, false));
                tVentas.addCell(PdfReportHelper.createDataCell(v.estado().name(), false, z, false));
                tVentas.addCell(PdfReportHelper.createDataCell(v.clienteNombre() != null ? v.clienteNombre() : "-", false, z, false));
                tVentas.addCell(PdfReportHelper.createDataCell(PdfReportHelper.formatMoney(v.total()), true, z, true));
                String pago = v.formaPago() == com.pos.entity.FormaPago.FIADO ? "Abono" : v.formaPago().name();
                tVentas.addCell(PdfReportHelper.createDataCell(pago, false, z, false));
                z = !z;
            }
            document.add(tVentas);

            document.add(new Paragraph("DETALLE DE GASTOS").setFontSize(11).setBold().setMarginTop(10));
            float[] wG = {8, 18, 50, 14};
            Table tGastos = PdfReportHelper.createStyledTable(wG);
            PdfReportHelper.addTableHeader(tGastos, new String[]{"#", "Fecha", "Descripción", "Valor"});
            i = 1; z = false;
            for (GastoCajaResponseDTO g : reporte.getGastos()) {
                tGastos.addCell(PdfReportHelper.createDataCell(String.valueOf(i++), false, z, false));
                tGastos.addCell(PdfReportHelper.createDataCell(g.getFecha().format(FECHA_FORMATO), false, z, false));
                tGastos.addCell(PdfReportHelper.createDataCell(g.getDescripcion(), false, z, false));
                tGastos.addCell(PdfReportHelper.createDataCell(PdfReportHelper.formatMoney(g.getValor()), true, z, true));
                z = !z;
            }
            document.add(tGastos);

            PdfReportHelper.addFooter(document, "POS Restaurante • Cierre de Turno");

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF de Turno", e);
        }
    }
}
