package com.pos.service.export.excel;
import com.pos.dto.report.ReporteCierreTurnoDTO;
import com.pos.dto.turno.GastoCajaResponseDTO;
import com.pos.dto.venta.VentaResponseDTO;
import com.pos.service.export.excel.ExcelTurnoExporter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;


@Service
public class ExcelTurnoExporter {

    private static final DateTimeFormatter FECHA_FORMATO =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public byte[] exportar(ReporteCierreTurnoDTO reporte) {

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Cierre Turno");
            int rowNum = 0;

            // ===== ESTILOS =====
            DataFormat format = workbook.createDataFormat();

            CellStyle moneda = workbook.createCellStyle();
            moneda.setDataFormat(format.getFormat("$#,##0.00"));

            CellStyle bold = workbook.createCellStyle();
            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            bold.setFont(boldFont);

            // ===== TOTALES =====
            rowNum = crearFila(sheet, rowNum, "Total Ventas", reporte.getTotalVentas(), moneda, bold);
            rowNum = crearFila(sheet, rowNum, "Total Efectivo", reporte.getTotalEfectivo(), moneda, bold);
            rowNum = crearFila(sheet, rowNum, "Total Transferencia", reporte.getTotalTransferencia(), moneda, bold);
            rowNum = crearFila(sheet, rowNum, "Total Gastos", reporte.getTotalGastos(), moneda, bold);
            rowNum = crearFila(sheet, rowNum, "Neto en Caja", reporte.getNetoEnCaja(), moneda, bold);

            rowNum += 2;

            // ===== TABLA VENTAS =====
            rowNum = crearTablaVentas(sheet, rowNum, reporte.getVentas(), moneda);

            rowNum += 2;

            // ===== TABLA GASTOS =====
            crearTablaGastos(sheet, rowNum, reporte.getGastos(), moneda);

            for (int i = 0; i < 7; i++) sheet.autoSizeColumn(i);

            workbook.write(baos);
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generando Excel de Turno", e);
        }
    }

    // ---------- helpers ----------
    private int crearFila(
            Sheet sheet,
            int rowNum,
            String label,
            BigDecimal valor,
            CellStyle moneda,
            CellStyle bold
    ) {
        Row row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue(label);
        row.getCell(0).setCellStyle(bold);

        Cell cell = row.createCell(1);
        cell.setCellValue(valor.doubleValue());
        cell.setCellStyle(moneda);

        return rowNum;
    }

    private int crearTablaVentas(
            Sheet sheet,
            int rowNum,
            List<VentaResponseDTO> ventas,
            CellStyle moneda
    ) {
        String[] headers = {"ID", "Fecha", "Tipo", "Estado", "Cliente", "Total", "Forma Pago"};
        Row header = sheet.createRow(rowNum++);
        for (int i = 0; i < headers.length; i++) {
            header.createCell(i).setCellValue(headers[i]);
        }

        for (VentaResponseDTO v : ventas) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(v.id());
            row.createCell(1).setCellValue(v.fecha().format(FECHA_FORMATO));
            row.createCell(2).setCellValue(v.tipoVenta().name());
            row.createCell(3).setCellValue(v.estado().name());
            row.createCell(4).setCellValue(v.clienteNombre() != null ? v.clienteNombre() : "-");

            Cell total = row.createCell(5);
            total.setCellValue(v.total().doubleValue());
            total.setCellStyle(moneda);

            row.createCell(6).setCellValue(v.formaPago().name());
        }
        return rowNum;
    }

    private void crearTablaGastos(
            Sheet sheet,
            int rowNum,
            List<GastoCajaResponseDTO> gastos,
            CellStyle moneda
    ) {
        String[] headers = {"ID", "Fecha", "Descripción", "Valor"};
        Row header = sheet.createRow(rowNum++);
        for (int i = 0; i < headers.length; i++) {
            header.createCell(i).setCellValue(headers[i]);
        }

        for (GastoCajaResponseDTO g : gastos) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(g.getId());
            row.createCell(1).setCellValue(g.getFecha().format(FECHA_FORMATO));
            row.createCell(2).setCellValue(g.getDescripcion());

            Cell valor = row.createCell(3);
            valor.setCellValue(g.getValor().doubleValue());
            valor.setCellStyle(moneda);
        }
    }
}

