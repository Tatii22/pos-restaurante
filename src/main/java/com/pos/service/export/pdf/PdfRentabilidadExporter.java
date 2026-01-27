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
import com.pos.dto.turno.GastoCajaResponseDTO;
import com.pos.dto.venta.VentaResponseDTO;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;



@Service
public class PdfRentabilidadExporter {

    private static final DateTimeFormatter FECHA_FORMATO =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public byte[] exportar(ReporteRentabilidadDTO reporte) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            document.add(new Paragraph("Reporte de Rentabilidad (Ventas vs Gastos)")
                    .setBold()
                    .setFontSize(18)
                    .setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph(
                    "Desde: " + reporte.getFechaInicio() +
                    " Hasta: " + reporte.getFechaFin())
                    .setTextAlignment(TextAlignment.CENTER)
            );

            document.add(new Paragraph("\n"));

            crearTablaTotales(document, reporte);
            crearTablaVentas(document, reporte.getVentas());
            crearTablaGastos(document, reporte.getGastos());

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF de Rentabilidad", e);
        }
    }

    /* ---------- helpers privados ---------- */

    private void crearTablaTotales(Document document, ReporteRentabilidadDTO r) {
        Table tabla = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .useAllAvailableWidth();

        agregarFila(tabla, "Total Ventas", r.getTotalVentas());
        agregarFila(tabla, "Total Gastos", r.getTotalGastos());
        agregarFila(tabla, "Ganancia Neta", r.getGananciaNeta());

        document.add(tabla);
        document.add(new Paragraph("\n"));
    }

    private void agregarFila(Table tabla, String label, BigDecimal valor) {
        tabla.addCell(new Cell().add(new Paragraph(label).setBold())
                .setTextAlignment(TextAlignment.CENTER));
        tabla.addCell(new Cell().add(new Paragraph(String.format("$%,.2f", valor)))
                .setTextAlignment(TextAlignment.CENTER));
    }

    private void crearTablaVentas(Document document, List<VentaResponseDTO> ventas) {
        Table tabla = new Table(UnitValue.createPercentArray(
                new float[]{40, 80, 80, 60, 80, 60, 60}))
                .useAllAvailableWidth();

        String[] headers = {"ID", "Fecha", "Tipo", "Estado", "Cliente", "Total", "Forma Pago"};
        for (String h : headers) {
            tabla.addHeaderCell(new Cell().add(new Paragraph(h))
                    .setBold()
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                    .setTextAlignment(TextAlignment.CENTER));
        }

        boolean par = true;
        for (VentaResponseDTO v : ventas) {
            Color bg = par ? ColorConstants.WHITE : ColorConstants.LIGHT_GRAY;
            tabla.addCell(new Cell().add(new Paragraph(String.valueOf(v.id()))).setBackgroundColor(bg));
            tabla.addCell(new Cell().add(new Paragraph(v.fecha().format(FECHA_FORMATO))).setBackgroundColor(bg));
            tabla.addCell(new Cell().add(new Paragraph(v.tipoVenta().name())).setBackgroundColor(bg));
            tabla.addCell(new Cell().add(new Paragraph(v.estado().name())).setBackgroundColor(bg));
            tabla.addCell(new Cell().add(new Paragraph(
                    v.clienteNombre() != null ? v.clienteNombre() : "-")).setBackgroundColor(bg));
            tabla.addCell(new Cell().add(new Paragraph(String.format("$%,.2f", v.total()))).setBackgroundColor(bg));
            tabla.addCell(new Cell().add(new Paragraph(v.formaPago().name())).setBackgroundColor(bg));
            par = !par;
        }

        document.add(tabla);
        document.add(new Paragraph("\n"));
    }

    private void crearTablaGastos(Document document, List<GastoCajaResponseDTO> gastos) {
        Table tabla = new Table(UnitValue.createPercentArray(
                new float[]{40, 80, 200, 60}))
                .useAllAvailableWidth();

        String[] headers = {"ID", "Fecha", "Descripción", "Valor"};
        for (String h : headers) {
            tabla.addHeaderCell(new Cell().add(new Paragraph(h))
                    .setBold()
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                    .setTextAlignment(TextAlignment.CENTER));
        }

        boolean par = true;
        for (GastoCajaResponseDTO g : gastos) {
            Color bg = par ? ColorConstants.WHITE : ColorConstants.LIGHT_GRAY;
            tabla.addCell(new Cell().add(new Paragraph(String.valueOf(g.getId()))).setBackgroundColor(bg));
            tabla.addCell(new Cell().add(new Paragraph(g.getFecha().format(FECHA_FORMATO))).setBackgroundColor(bg));
            tabla.addCell(new Cell().add(new Paragraph(g.getDescripcion())).setBackgroundColor(bg));
            tabla.addCell(new Cell().add(new Paragraph(String.format("$%,.2f", g.getValor()))).setBackgroundColor(bg));
            par = !par;
        }

        document.add(tabla);
    }
}

