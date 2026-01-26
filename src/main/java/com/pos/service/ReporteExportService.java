package com.pos.service;

import com.pos.dto.venta.VentaResponseDTO;
import com.pos.dto.report.ReporteVentaDTO;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
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


@Service
public class ReporteExportService {

    private static final DateTimeFormatter FECHA_FORMATO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // ----------------- PDF -----------------
    public byte[] generarReporteVentasPDF(ReporteVentaDTO reporte) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // Título
            document.add(new Paragraph("Reporte de Ventas")
                    .setBold()
                    .setFontSize(18)
                    .setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("Desde: " + reporte.getFechaInicio() + " Hasta: " + reporte.getFechaFin())
                    .setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("\n"));

            // Totales
            document.add(new Paragraph(String.format("Total Ventas: %d", reporte.getTotalVentas())));
            document.add(new Paragraph(String.format("Total Bruto: $%,.2f", reporte.getTotalBruto())));
            document.add(new Paragraph(String.format("Total Descuentos: $%,.2f", reporte.getTotalDescuentos())));
            document.add(new Paragraph(String.format("Total Neto: $%,.2f", reporte.getTotalNeto())));
            document.add(new Paragraph(String.format("Efectivo: $%,.2f", reporte.getTotalEfectivo())));
            document.add(new Paragraph(String.format("Transferencia: $%,.2f", reporte.getTotalTransferencia())));
            document.add(new Paragraph("\n"));

            // Tabla de Ventas
            Table tabla = new Table(UnitValue.createPercentArray(new float[]{40, 80, 80, 60, 80, 60, 60}))
                    .useAllAvailableWidth();

            // Encabezados
            String[] columnas = {"ID", "Fecha", "Tipo", "Estado", "Cliente", "Total", "Forma Pago"};
            for (String c : columnas) {
                tabla.addHeaderCell(new Cell().add(new Paragraph(c)).setBold().setTextAlignment(TextAlignment.CENTER));
            }

            // Filas
            for (VentaResponseDTO v : reporte.getVentas()) {
                tabla.addCell(String.valueOf(v.id()));
                tabla.addCell(v.fecha().format(FECHA_FORMATO));
                tabla.addCell(v.tipoVenta().name());
                tabla.addCell(v.estado().name());
                tabla.addCell(v.clienteNombre() != null ? v.clienteNombre() : "-");
                tabla.addCell(String.format("$%,.2f", v.total()));
                tabla.addCell(v.formaPago().name());
            }

            document.add(tabla);
            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF", e);
        }
    }

    // ----------------- EXCEL -----------------
    public byte[] generarReporteVentasExcel(ReporteVentaDTO reporte) {
        try (Workbook workbook = new XSSFWorkbook();
            ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Reporte Ventas");
            int filaNum = 0;

            // Título y fechas
            Row rowTitulo = sheet.createRow(filaNum++);
            rowTitulo.createCell(0).setCellValue("Reporte de Ventas");
            Row rowFechas = sheet.createRow(filaNum++);
            rowFechas.createCell(0).setCellValue("Desde: " + reporte.getFechaInicio() + " Hasta: " + reporte.getFechaFin());
            filaNum++;

            // Estilo moneda
            CellStyle estiloMoneda = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            estiloMoneda.setDataFormat(format.getFormat("$#,##0.00"));

            // Totales
            filaNum = crearFilaTotales(sheet, filaNum, "Total Ventas", BigDecimal.valueOf(reporte.getTotalVentas()), estiloMoneda);
            filaNum = crearFilaTotales(sheet, filaNum, "Total Bruto", reporte.getTotalBruto(), estiloMoneda);
            filaNum = crearFilaTotales(sheet, filaNum, "Total Descuentos", reporte.getTotalDescuentos(), estiloMoneda);
            filaNum = crearFilaTotales(sheet, filaNum, "Total Neto", reporte.getTotalNeto(), estiloMoneda);
            filaNum = crearFilaTotales(sheet, filaNum, "Efectivo", reporte.getTotalEfectivo(), estiloMoneda);
            filaNum = crearFilaTotales(sheet, filaNum, "Transferencia", reporte.getTotalTransferencia(), estiloMoneda);
            filaNum++;

            // Encabezado tabla
            Row header = sheet.createRow(filaNum++);
            String[] columnas = {"ID", "Fecha", "Tipo", "Estado", "Cliente", "Total", "Forma Pago"};
            for (int i = 0; i < columnas.length; i++) {
                header.createCell(i).setCellValue(columnas[i]);
            }

            // Filas ventas
            for (VentaResponseDTO v : reporte.getVentas()) {
                Row row = sheet.createRow(filaNum++);
                row.createCell(0).setCellValue(v.id());
                row.createCell(1).setCellValue(v.fecha().format(FECHA_FORMATO));
                row.createCell(2).setCellValue(v.tipoVenta().name());
                row.createCell(3).setCellValue(v.estado().name());
                row.createCell(4).setCellValue(v.clienteNombre() != null ? v.clienteNombre() : "-");
                
                // Aquí renombramos Cell de Excel para no confundir con iText
                org.apache.poi.ss.usermodel.Cell celdaTotal = row.createCell(5);
                celdaTotal.setCellValue(v.total().doubleValue());
                celdaTotal.setCellStyle(estiloMoneda);
                
                row.createCell(6).setCellValue(v.formaPago().name());
            }

            // Auto-ajustar columnas
            for (int i = 0; i < columnas.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(baos);
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generando Excel", e);
        }
    }

    private int crearFilaTotales(Sheet sheet, int filaNum, String etiqueta, BigDecimal valor, CellStyle estiloMoneda) {
        Row row = sheet.createRow(filaNum++);
        row.createCell(0).setCellValue(etiqueta);
        org.apache.poi.ss.usermodel.Cell celda = row.createCell(1);
        celda.setCellValue(valor.doubleValue());
        celda.setCellStyle(estiloMoneda);
        return filaNum;
    }

}
