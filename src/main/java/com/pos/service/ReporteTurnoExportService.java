package com.pos.service;

import com.pos.dto.turno.GastoCajaResponseDTO;
import com.pos.dto.venta.VentaResponseDTO;
import com.pos.dto.report.ReporteCierreTurnoDTO;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;


@Service
public class ReporteTurnoExportService {

    private static final DateTimeFormatter FECHA_FORMATO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // ----------------- PDF -----------------
    public byte[] generarReporteTurnoPDF(ReporteCierreTurnoDTO reporte) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // Título
            document.add(new Paragraph("Reporte de Cierre de Turno")
                    .setBold().setFontSize(18).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("Turno ID: " + reporte.getTurnoId() +
                    " | Apertura: " + reporte.getApertura() +
                    " | Cierre: " + reporte.getCierre())
                    .setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("\n"));

            // Totales
            Table tablaTotales = new Table(UnitValue.createPercentArray(new float[]{50, 50})).useAllAvailableWidth();
            tablaTotales.addCell(new Cell().add(new Paragraph("Total Ventas").setBold()).setTextAlignment(TextAlignment.CENTER));
            tablaTotales.addCell(new Cell().add(new Paragraph(String.format("$%,.2f", reporte.getTotalVentas())).setBold()).setTextAlignment(TextAlignment.CENTER));
            tablaTotales.addCell(new Cell().add(new Paragraph("Total Efectivo").setBold()).setTextAlignment(TextAlignment.CENTER));
            tablaTotales.addCell(new Cell().add(new Paragraph(String.format("$%,.2f", reporte.getTotalEfectivo())).setBold()).setTextAlignment(TextAlignment.CENTER));
            tablaTotales.addCell(new Cell().add(new Paragraph("Total Transferencia").setBold()).setTextAlignment(TextAlignment.CENTER));
            tablaTotales.addCell(new Cell().add(new Paragraph(String.format("$%,.2f", reporte.getTotalTransferencia())).setBold()).setTextAlignment(TextAlignment.CENTER));
            tablaTotales.addCell(new Cell().add(new Paragraph("Total Gastos").setBold()).setTextAlignment(TextAlignment.CENTER));
            tablaTotales.addCell(new Cell().add(new Paragraph(String.format("$%,.2f", reporte.getTotalGastos())).setBold()).setTextAlignment(TextAlignment.CENTER));
            tablaTotales.addCell(new Cell().add(new Paragraph("Neto en Caja").setBold()).setTextAlignment(TextAlignment.CENTER));
            tablaTotales.addCell(new Cell().add(new Paragraph(String.format("$%,.2f", reporte.getNetoEnCaja())).setBold()).setTextAlignment(TextAlignment.CENTER));

            document.add(tablaTotales);
            document.add(new Paragraph("\n"));

            // Tabla Ventas
            Table tablaVentas = new Table(UnitValue.createPercentArray(new float[]{40, 80, 80, 60, 80, 60, 60})).useAllAvailableWidth();
            String[] columnasVentas = {"ID", "Fecha", "Tipo", "Estado", "Cliente", "Total", "Forma Pago"};
            for (String c : columnasVentas) {
                tablaVentas.addHeaderCell(new Cell()
                        .add(new Paragraph(c))
                        .setBold()
                        .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                        .setTextAlignment(TextAlignment.CENTER));
            }
            boolean filaPar = true;
            for (VentaResponseDTO v : reporte.getVentas()) {
                tablaVentas.addCell(new Cell().add(new Paragraph(String.valueOf(v.id()))).setBackgroundColor(filaPar ? ColorConstants.WHITE : ColorConstants.LIGHT_GRAY));
                tablaVentas.addCell(new Cell().add(new Paragraph(v.fecha().format(FECHA_FORMATO))).setBackgroundColor(filaPar ? ColorConstants.WHITE : ColorConstants.LIGHT_GRAY));
                tablaVentas.addCell(new Cell().add(new Paragraph(v.tipoVenta().name())).setBackgroundColor(filaPar ? ColorConstants.WHITE : ColorConstants.LIGHT_GRAY));
                tablaVentas.addCell(new Cell().add(new Paragraph(v.estado().name())).setBackgroundColor(filaPar ? ColorConstants.WHITE : ColorConstants.LIGHT_GRAY));
                tablaVentas.addCell(new Cell().add(new Paragraph(v.clienteNombre() != null ? v.clienteNombre() : "-")).setBackgroundColor(filaPar ? ColorConstants.WHITE : ColorConstants.LIGHT_GRAY));
                tablaVentas.addCell(new Cell().add(new Paragraph(String.format("$%,.2f", v.total()))).setBackgroundColor(filaPar ? ColorConstants.WHITE : ColorConstants.LIGHT_GRAY));
                tablaVentas.addCell(new Cell().add(new Paragraph(v.formaPago().name())).setBackgroundColor(filaPar ? ColorConstants.WHITE : ColorConstants.LIGHT_GRAY));
                filaPar = !filaPar;
            }
            document.add(tablaVentas);
            document.add(new Paragraph("\n"));

            // Tabla Gastos
            Table tablaGastos = new Table(UnitValue.createPercentArray(new float[]{40, 80, 200, 60})).useAllAvailableWidth();
            String[] columnasGastos = {"ID", "Fecha", "Descripción", "Valor"};
            for (String c : columnasGastos) {
                tablaGastos.addHeaderCell(new Cell()
                        .add(new Paragraph(c))
                        .setBold()
                        .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                        .setTextAlignment(TextAlignment.CENTER));
            }
            filaPar = true;
            for (GastoCajaResponseDTO g : reporte.getGastos()) {
                tablaGastos.addCell(new Cell().add(new Paragraph(String.valueOf(g.getId()))).setBackgroundColor(filaPar ? ColorConstants.WHITE : ColorConstants.LIGHT_GRAY));
                tablaGastos.addCell(new Cell().add(new Paragraph(g.getFecha().format(FECHA_FORMATO))).setBackgroundColor(filaPar ? ColorConstants.WHITE : ColorConstants.LIGHT_GRAY));
                tablaGastos.addCell(new Cell().add(new Paragraph(g.getDescripcion())).setBackgroundColor(filaPar ? ColorConstants.WHITE : ColorConstants.LIGHT_GRAY));
                tablaGastos.addCell(new Cell().add(new Paragraph(String.format("$%,.2f", g.getValor()))).setBackgroundColor(filaPar ? ColorConstants.WHITE : ColorConstants.LIGHT_GRAY));
                filaPar = !filaPar;
            }
            document.add(tablaGastos);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF de Cierre de Turno", e);
        }
    }

    // ----------------- EXCEL -----------------
    public byte[] generarReporteTurnoExcel(ReporteCierreTurnoDTO reporte) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Cierre Turno");
            int filaNum = 0;

            // Estilo moneda
            CellStyle estiloMoneda = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            estiloMoneda.setDataFormat(format.getFormat("$#,##0.00"));

            // Totales
            filaNum = crearFilaTotales(sheet, filaNum, "Total Ventas", reporte.getTotalVentas(), estiloMoneda);
            filaNum = crearFilaTotales(sheet, filaNum, "Total Efectivo", reporte.getTotalEfectivo(), estiloMoneda);
            filaNum = crearFilaTotales(sheet, filaNum, "Total Transferencia", reporte.getTotalTransferencia(), estiloMoneda);
            filaNum = crearFilaTotales(sheet, filaNum, "Total Gastos", reporte.getTotalGastos(), estiloMoneda);
            filaNum = crearFilaTotales(sheet, filaNum, "Neto en Caja", reporte.getNetoEnCaja(), estiloMoneda);
            filaNum++;

            // Encabezado tabla ventas
            Row headerVentas = sheet.createRow(filaNum++);
            String[] columnasVentas = {"ID", "Fecha", "Tipo", "Estado", "Cliente", "Total", "Forma Pago"};
            for (int i = 0; i < columnasVentas.length; i++) headerVentas.createCell(i).setCellValue(columnasVentas[i]);

            // Filas ventas
            boolean filaPar = true;
            for (VentaResponseDTO v : reporte.getVentas()) {
                Row row = sheet.createRow(filaNum++);
                row.createCell(0).setCellValue(v.id());
                row.createCell(1).setCellValue(v.fecha().format(FECHA_FORMATO));
                row.createCell(2).setCellValue(v.tipoVenta().name());
                row.createCell(3).setCellValue(v.estado().name());
                row.createCell(4).setCellValue(v.clienteNombre() != null ? v.clienteNombre() : "-");

                org.apache.poi.ss.usermodel.Cell cellTotal = row.createCell(5);
                cellTotal.setCellValue(v.total().doubleValue());
                cellTotal.setCellStyle(estiloMoneda);

                row.createCell(6).setCellValue(v.formaPago().name());

                // Alternar color de fila
                if (!filaPar) {
                    for (int i = 0; i < 7; i++) {
                        row.getCell(i).getCellStyle().setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
                        row.getCell(i).getCellStyle().setFillPattern(FillPatternType.SOLID_FOREGROUND);
                    }
                }
                filaPar = !filaPar;
            }

            // Encabezado tabla gastos
            Row headerGastos = sheet.createRow(filaNum++);
            String[] columnasGastos = {"ID", "Fecha", "Descripción", "Valor"};
            for (int i = 0; i < columnasGastos.length; i++) headerGastos.createCell(i).setCellValue(columnasGastos[i]);

            // Filas gastos
            filaPar = true;
            for (GastoCajaResponseDTO g : reporte.getGastos()) {
                Row row = sheet.createRow(filaNum++);
                row.createCell(0).setCellValue(g.getId());
                row.createCell(1).setCellValue(g.getFecha().format(FECHA_FORMATO));
                row.createCell(2).setCellValue(g.getDescripcion());

                org.apache.poi.ss.usermodel.Cell cellValor = row.createCell(3);
                cellValor.setCellValue(g.getValor().doubleValue());
                cellValor.setCellStyle(estiloMoneda);

                if (!filaPar) {
                    for (int i = 0; i < 4; i++) {
                        row.getCell(i).getCellStyle().setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
                        row.getCell(i).getCellStyle().setFillPattern(FillPatternType.SOLID_FOREGROUND);
                    }
                }
                filaPar = !filaPar;
            }

            // Auto-ajustar columnas
            for (int i = 0; i < 7; i++) sheet.autoSizeColumn(i);
            for (int i = 0; i < 4; i++) sheet.autoSizeColumn(i);

            workbook.write(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generando Excel de Cierre de Turno", e);
        }
    }


    private int crearFilaTotales(Sheet sheet, int filaNum, String etiqueta, BigDecimal valor, CellStyle estiloMoneda) {
        Row row = sheet.createRow(filaNum++);
        row.createCell(0).setCellValue(etiqueta);
        org.apache.poi.ss.usermodel.Cell cell = row.createCell(1);
        cell.setCellValue(valor.doubleValue());
        cell.setCellStyle(estiloMoneda);
        return filaNum;
    }
}
