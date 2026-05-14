package com.pos.service.export.excel;
import com.pos.dto.report.ReporteVentaDTO;
import com.pos.dto.venta.VentaResponseDTO;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.io.ByteArrayOutputStream;

@Service
public class ExcelVentasExporter {

    private static final DateTimeFormatter FECHA_FORMATO =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public byte[] exportar(ReporteVentaDTO reporte) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Reporte Ventas");
            int fila = 0;

            Row titulo = sheet.createRow(fila++);
            titulo.createCell(0).setCellValue("Reporte de Ventas");

            Row fechas = sheet.createRow(fila++);
            fechas.createCell(0).setCellValue(
                    "Desde: " + reporte.getFechaInicio() +
                    " Hasta: " + reporte.getFechaFin()
            );

            fila++;

            CellStyle moneda = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            moneda.setDataFormat(format.getFormat("$#,##0.00"));

            fila = crearFila(sheet, fila, "Total Ventas",
                    BigDecimal.valueOf(reporte.getTotalVentas()), moneda);
            fila = crearFila(sheet, fila, "Total Bruto",
                    reporte.getTotalBruto(), moneda);
            fila = crearFila(sheet, fila, "Total Descuentos",
                    reporte.getTotalDescuentos(), moneda);
            fila = crearFila(sheet, fila, "Total Neto",
                    reporte.getTotalNeto(), moneda);
            fila = crearFila(sheet, fila, "Efectivo",
                    reporte.getTotalEfectivo(), moneda);
            fila = crearFila(sheet, fila, "Transferencia",
                    reporte.getTotalTransferencia(), moneda);

            fila++;

            String[] columnas = {
                    "ID", "Fecha", "Tipo", "Estado",
                    "Cliente", "Total", "Forma Pago"
            };

            Row header = sheet.createRow(fila++);
            for (int i = 0; i < columnas.length; i++) {
                header.createCell(i).setCellValue(columnas[i]);
            }
            int index = 1;
            for (VentaResponseDTO v : reporte.getVentas()) {
                Row row = sheet.createRow(fila++);
                row.createCell(0).setCellValue(index++);
                row.createCell(1).setCellValue(v.fecha().format(FECHA_FORMATO));
                row.createCell(2).setCellValue(v.tipoVenta().name());
                row.createCell(3).setCellValue(v.estado().name());
                row.createCell(4).setCellValue(
                        v.clienteNombre() != null ? v.clienteNombre() : "-"
                );

                org.apache.poi.ss.usermodel.Cell totalCell = row.createCell(5);
                totalCell.setCellValue(v.total().doubleValue());
                totalCell.setCellStyle(moneda);

                row.createCell(6).setCellValue(v.formaPago().name());
            }

            for (int i = 0; i < columnas.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(baos);
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generando Excel de ventas", e);
        }
    }

    private int crearFila(
            Sheet sheet,
            int fila,
            String etiqueta,
            BigDecimal valor,
            CellStyle estilo
    ) {
        Row row = sheet.createRow(fila++);
        row.createCell(0).setCellValue(etiqueta);

        org.apache.poi.ss.usermodel.Cell cell = row.createCell(1);
        cell.setCellValue(valor.doubleValue());
        cell.setCellStyle(estilo);

        return fila;
    }
}

