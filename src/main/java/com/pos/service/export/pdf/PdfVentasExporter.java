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
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class PdfVentasExporter {

    private static final DateTimeFormatter FECHA_FORMATO =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public byte[] exportar(ReporteVentaDTO reporte) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            document.add(new Paragraph("Reporte de Ventas")
                    .setBold()
                    .setFontSize(18)
                    .setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph(
                    "Desde: " + reporte.getFechaInicio() +
                    " Hasta: " + reporte.getFechaFin())
                    .setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph("\n"));

            document.add(new Paragraph("Total Ventas: " + reporte.getTotalVentas()));
            document.add(new Paragraph("Total Bruto: $" + reporte.getTotalBruto()));
            document.add(new Paragraph("Total Descuentos: $" + reporte.getTotalDescuentos()));
            document.add(new Paragraph("Total Neto: $" + reporte.getTotalNeto()));
            document.add(new Paragraph("Efectivo: $" + reporte.getTotalEfectivo()));
            document.add(new Paragraph("Transferencia: $" + reporte.getTotalTransferencia()));
            document.add(new Paragraph("\n"));

            Table tabla = new Table(UnitValue.createPercentArray(
                    new float[]{40, 80, 80, 60, 80, 60, 60}))
                    .useAllAvailableWidth();

            String[] columnas = {
                    "ID", "Fecha", "Tipo", "Estado",
                    "Cliente", "Total", "Forma Pago"
            };

            for (String c : columnas) {
                tabla.addHeaderCell(
                        new Cell()
                                .add(new Paragraph(c))
                                .setBold()
                                .setTextAlignment(TextAlignment.CENTER)
                );
            }
            int index = 1;
            for (VentaResponseDTO v : reporte.getVentas()) {
                tabla.addCell(String.valueOf(index++));
                tabla.addCell(v.fecha().format(FECHA_FORMATO));
                tabla.addCell(v.tipoVenta().name());
                tabla.addCell(v.estado().name());
                tabla.addCell(v.clienteNombre() != null ? v.clienteNombre() : "-");
                tabla.addCell("$" + v.total());
                tabla.addCell(v.formaPago().name());
            }

            document.add(tabla);
            document.close();

            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF de ventas", e);
        }
    }
}
