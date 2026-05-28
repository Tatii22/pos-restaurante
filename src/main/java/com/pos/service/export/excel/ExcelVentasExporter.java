package com.pos.service.export.excel;

import com.pos.dto.report.ReporteVentaDTO;
import com.pos.dto.venta.VentaResponseDTO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Service
public class ExcelVentasExporter {

    private static final DateTimeFormatter FECHA_FORMATO =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public byte[] exportar(ReporteVentaDTO reporte) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Reporte Ventas");
            int fila = 0;

            ExcelReportHelper.addTitle(sheet, fila++, "REPORTE DE VENTAS");
            ExcelReportHelper.addSubtitle(sheet, fila++,
                    "Período: " + reporte.getFechaInicio() + " → " + reporte.getFechaFin());

            fila++;

            CellStyle money = ExcelReportHelper.createMoneyStyle(workbook);
            CellStyle headerStyle = ExcelReportHelper.createHeaderStyle(workbook);

            // KPIs - lenguaje humano y premium
            ExcelReportHelper.addKpiRow(sheet, fila++, "Ventas realizadas",
                    BigDecimal.valueOf(reporte.getTotalVentas()), money);
            ExcelReportHelper.addKpiRow(sheet, fila++, "Ingresos recibidos", reporte.getRecaudoReal(), money);
            ExcelReportHelper.addKpiRow(sheet, fila++, "Gastos registrados", reporte.getTotalGastos() != null ? reporte.getTotalGastos() : BigDecimal.ZERO, money);
            BigDecimal balV = (reporte.getRecaudoReal() != null && reporte.getTotalGastos() != null) 
                    ? reporte.getRecaudoReal().subtract(reporte.getTotalGastos()) : BigDecimal.ZERO;
            ExcelReportHelper.addKpiRow(sheet, fila++, "Ganancia neta del mes", balV, money, true); // destacado

            fila++;

            // Tabla detalle
            String[] cols = {"#", "Fecha", "Tipo", "Estado", "Cliente", "Total", "Forma Pago"};
            ExcelReportHelper.addTableHeader(sheet, fila++, cols, headerStyle);

            int idx = 1;
            for (VentaResponseDTO v : reporte.getVentas()) {
                Row row = sheet.createRow(fila++);
                row.createCell(0).setCellValue(idx++);
                row.createCell(1).setCellValue(v.fecha().format(FECHA_FORMATO));
                row.createCell(2).setCellValue(v.tipoVenta().name());
                row.createCell(3).setCellValue(v.estado().name());
                row.createCell(4).setCellValue(v.clienteNombre() != null ? v.clienteNombre() : "-");

                Cell totalCell = row.createCell(5);
                ExcelReportHelper.applyMoneyToCell(totalCell, v.total(), money);

                String pago = (v.condicionPago() == com.pos.entity.CondicionPago.FIADO) ? "Abono" : v.formaPago().name();
                row.createCell(6).setCellValue(pago);
            }

            ExcelReportHelper.autoSizeAll(sheet, 7);
            ExcelReportHelper.addFooter(sheet, fila + 2, "MentaPOS");

            workbook.write(baos);
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generando Excel de ventas", e);
        }
    }
}

