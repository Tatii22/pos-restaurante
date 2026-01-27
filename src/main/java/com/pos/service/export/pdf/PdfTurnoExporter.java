package com.pos.service.export.pdf;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.pos.dto.report.ReporteCierreTurnoDTO;
import com.pos.dto.turno.GastoCajaResponseDTO;
import com.pos.dto.venta.VentaResponseDTO;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PdfTurnoExporter {

    private static final DateTimeFormatter FECHA_FORMATO =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public byte[] exportar(ReporteCierreTurnoDTO reporte) {

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // ===== TÍTULO =====
            document.add(new Paragraph("Reporte de Cierre de Turno")
                    .setBold()
                    .setFontSize(18)
                    .setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph(
                    "Turno ID: " + reporte.getTurnoId()
                            + " | Apertura: " + reporte.getApertura()
                            + " | Cierre: " + reporte.getCierre()
            ).setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph("\n"));

            // ===== TOTALES =====
            Table totales = new Table(UnitValue.createPercentArray(2))
                    .useAllAvailableWidth();

            agregarTotal(totales, "Total Ventas", reporte.getTotalVentas());
            agregarTotal(totales, "Total Efectivo", reporte.getTotalEfectivo());
            agregarTotal(totales, "Total Transferencia", reporte.getTotalTransferencia());
            agregarTotal(totales, "Total Gastos", reporte.getTotalGastos());
            agregarTotal(totales, "Neto en Caja", reporte.getNetoEnCaja());

            document.add(totales);
            document.add(new Paragraph("\n"));

            // ===== TABLA VENTAS =====
            document.add(crearTablaVentas(reporte.getVentas()));

            document.add(new Paragraph("\n"));

            // ===== TABLA GASTOS =====
            document.add(crearTablaGastos(reporte.getGastos()));

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF de Turno", e);
        }
    }

    // ---------- helpers ----------
    private void agregarTotal(Table table, String label, BigDecimal valor) {
        table.addCell(new Cell().add(new Paragraph(label).setBold()));
        table.addCell(new Cell().add(
                new Paragraph(String.format("$%,.2f", valor)).setBold()
        ));
    }

    private Table crearTablaVentas(List<VentaResponseDTO> ventas) {
        Table table = new Table(UnitValue.createPercentArray(
                new float[]{40, 80, 80, 60, 80, 60, 60}))
                .useAllAvailableWidth();

        String[] headers = {"ID", "Fecha", "Tipo", "Estado", "Cliente", "Total", "Forma Pago"};
        for (String h : headers) {
            table.addHeaderCell(new Cell()
                    .add(new Paragraph(h))
                    .setBold()
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY));
        }

        for (VentaResponseDTO v : ventas) {
            table.addCell(String.valueOf(v.id()));
            table.addCell(v.fecha().format(FECHA_FORMATO));
            table.addCell(v.tipoVenta().name());
            table.addCell(v.estado().name());
            table.addCell(v.clienteNombre() != null ? v.clienteNombre() : "-");
            table.addCell(String.format("$%,.2f", v.total()));
            table.addCell(v.formaPago().name());
        }

        return table;
    }

    private Table crearTablaGastos(List<GastoCajaResponseDTO> gastos) {
        Table table = new Table(UnitValue.createPercentArray(
                new float[]{40, 80, 200, 60}))
                .useAllAvailableWidth();

        String[] headers = {"ID", "Fecha", "Descripción", "Valor"};
        for (String h : headers) {
            table.addHeaderCell(new Cell()
                    .add(new Paragraph(h))
                    .setBold()
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY));
        }

        for (GastoCajaResponseDTO g : gastos) {
            table.addCell(String.valueOf(g.getId()));
            table.addCell(g.getFecha().format(FECHA_FORMATO));
            table.addCell(g.getDescripcion());
            table.addCell(String.format("$%,.2f", g.getValor()));
        }

        return table;
    }
}
