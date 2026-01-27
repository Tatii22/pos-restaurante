package com.pos.service.export.excel;

import com.pos.dto.report.ReporteRentabilidadDTO;
import com.pos.dto.venta.VentaResponseDTO;
import com.pos.dto.gasto.GastoResponseDTO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExcelRentabilidadExporter {

    private static final DateTimeFormatter FECHA_FORMATO =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public byte[] exportar(ReporteRentabilidadDTO reporte) {

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Rentabilidad");
            int fila = 0;

            // ----------------- ESTILO MONEDA -----------------
            CellStyle estiloMoneda = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            estiloMoneda.setDataFormat(format.getFormat("$#,##0.00"));

            // ----------------- TOTALES -----------------
            fila = crearFilaTotal(sheet, fila, "Total Ventas", reporte.getTotalVentas(), estiloMoneda);
            fila = crearFilaTotal(sheet, fila, "Total Gastos", reporte.getTotalGastos(), estiloMoneda);
            fila = crearFilaTotal(sheet, fila, "Ganancia Neta", reporte.getGananciaNeta(), estiloMoneda);
            fila++;

            // ----------------- TABLA VENTAS -----------------
            fila = crearTablaVentas(sheet, fila, reporte.getVentas(), estiloMoneda);
            fila++;

            // ----------------- TABLA GASTOS -----------------
            crearTablaGastos(sheet, fila, reporte.getGastos(), estiloMoneda);

            // Auto-ajustar columnas
            for (int i = 0; i < 7; i++) sheet.autoSizeColumn(i);

            workbook.write(baos);
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generando Excel de Rentabilidad", e);
        }
    }

    // =====================================================
    // MÉTODOS PRIVADOS
    // =====================================================

    private int crearFilaTotal(
            Sheet sheet,
            int fila,
            String etiqueta,
            BigDecimal valor,
            CellStyle estiloMoneda
    ) {
        Row row = sheet.createRow(fila++);
        row.createCell(0).setCellValue(etiqueta);

        Cell cell = row.createCell(1);
        cell.setCellValue(valor != null ? valor.doubleValue() : 0.0);
        cell.setCellStyle(estiloMoneda);

        return fila;
    }

    private int crearTablaVentas(
            Sheet sheet,
            int fila,
            List<VentaResponseDTO> ventas,
            CellStyle estiloMoneda
    ) {
        String[] columnas = {
                "ID", "Fecha", "Tipo", "Estado", "Cliente", "Total", "Forma Pago"
        };

        Row header = sheet.createRow(fila++);
        for (int i = 0; i < columnas.length; i++) {
            header.createCell(i).setCellValue(columnas[i]);
        }
        int index = 1;
        for (VentaResponseDTO v : ventas) {
            Row row = sheet.createRow(fila++);
            row.createCell(0).setCellValue(index++);
            row.createCell(1).setCellValue(v.fecha().format(FECHA_FORMATO));
            row.createCell(2).setCellValue(v.tipoVenta().name());
            row.createCell(3).setCellValue(v.estado().name());
            row.createCell(4).setCellValue(
                    v.clienteNombre() != null ? v.clienteNombre() : "-"
            );

            Cell total = row.createCell(5);
            total.setCellValue(v.total().doubleValue());
            total.setCellStyle(estiloMoneda);

            row.createCell(6).setCellValue(v.formaPago().name());
        }

        return fila;
    }

    private int crearTablaGastos(
            Sheet sheet,
            int fila,
            List<GastoResponseDTO> gastos,
            CellStyle estiloMoneda
    ) {
        String[] columnas = {"ID", "Fecha", "Descripción", "Valor"};

        Row header = sheet.createRow(fila++);
        for (int i = 0; i < columnas.length; i++) {
            header.createCell(i).setCellValue(columnas[i]);
        }
        int index = 1;
        for (GastoResponseDTO g : gastos) {
            Row row = sheet.createRow(fila++);
            row.createCell(0).setCellValue(index++);
            row.createCell(1).setCellValue(g.getFecha().format(FECHA_FORMATO));
            row.createCell(2).setCellValue(g.getDescripcion());

            Cell valor = row.createCell(3);
            valor.setCellValue(g.getMonto().doubleValue());
            valor.setCellStyle(estiloMoneda);
        }

        return fila;
    }
}
