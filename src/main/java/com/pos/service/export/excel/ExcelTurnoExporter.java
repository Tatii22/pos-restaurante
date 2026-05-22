package com.pos.service.export.excel;

import com.pos.dto.report.ReporteCierreTurnoDTO;
import com.pos.dto.turno.GastoCajaResponseDTO;
import com.pos.dto.venta.VentaResponseDTO;
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
            int fila = 0;

            ExcelReportHelper.addTitle(sheet, fila++, "REPORTE DE CIERRE DE TURNO #" + reporte.getTurnoId());
            ExcelReportHelper.addSubtitle(sheet, fila++,
                    "Apertura: " + reporte.getApertura() + "  |  Cierre: " + reporte.getCierre());

            fila++;

            CellStyle money = ExcelReportHelper.createMoneyStyle(workbook);
            CellStyle headerStyle = ExcelReportHelper.createHeaderStyle(workbook);

            // RESUMEN FINANCIERO - lenguaje humano y premium
            ExcelReportHelper.addKpiRow(sheet, fila++, "Ventas realizadas", reporte.getTotalVentas(), money);
            BigDecimal ingresosTurno = (reporte.getGananciaEfectivo() != null && reporte.getGananciaTransferencia() != null)
                    ? reporte.getGananciaEfectivo().add(reporte.getGananciaTransferencia()) : java.math.BigDecimal.ZERO;
            ExcelReportHelper.addKpiRow(sheet, fila++, "Ingresos recibidos", ingresosTurno, money);
            ExcelReportHelper.addKpiRow(sheet, fila++, "Gastos registrados", reporte.getTotalGastos(), money);

            BigDecimal balanceTurno = ingresosTurno.subtract(reporte.getTotalGastos() != null ? reporte.getTotalGastos() : java.math.BigDecimal.ZERO);
            ExcelReportHelper.addKpiRow(sheet, fila++, "Balance final del turno", balanceTurno, money, true); // destacado premium

            fila++;

            // Arqueo (se mantiene como detalle secundario)
            ExcelReportHelper.addKpiRow(sheet, fila++, "Base Caja (Inicial)", reporte.getCajaFisicaEsperada() != null && reporte.getGananciaEfectivo() != null ?
                    reporte.getCajaFisicaEsperada().subtract(reporte.getGananciaEfectivo()) : java.math.BigDecimal.ZERO, money);
            ExcelReportHelper.addKpiRow(sheet, fila++, "Efectivo Contado", reporte.getEfectivoContado(), money);
            ExcelReportHelper.addKpiRow(sheet, fila++, "Transferencias Verificadas", reporte.getTransferenciasVerificadas(), money);
            ExcelReportHelper.addKpiRow(sheet, fila++, "Total Verificado", reporte.getTotalVerificado(), money);
            ExcelReportHelper.addKpiRow(sheet, fila++, "Diferencia Total", reporte.getDiferenciaTotal(), money);

            fila++;

            // Tablas
            String[] colsV = {"#", "Fecha", "Tipo", "Estado", "Cliente", "Total", "Pago"};
            ExcelReportHelper.addTableHeader(sheet, fila++, colsV, headerStyle);
            int i = 1;
            for (VentaResponseDTO v : reporte.getVentas()) {
                Row r = sheet.createRow(fila++);
                r.createCell(0).setCellValue(i++);
                r.createCell(1).setCellValue(v.fecha().format(FECHA_FORMATO));
                r.createCell(2).setCellValue(v.tipoVenta().name());
                r.createCell(3).setCellValue(v.estado().name());
                r.createCell(4).setCellValue(v.clienteNombre() != null ? v.clienteNombre() : "-");
                Cell t = r.createCell(5);
                ExcelReportHelper.applyMoneyToCell(t, v.total(), money);
                r.createCell(6).setCellValue(v.formaPago().name());
            }

            fila++;

            String[] colsG = {"#", "Fecha", "Descripción", "Valor"};
            ExcelReportHelper.addTableHeader(sheet, fila++, colsG, headerStyle);
            i = 1;
            for (GastoCajaResponseDTO g : reporte.getGastos()) {
                Row r = sheet.createRow(fila++);
                r.createCell(0).setCellValue(i++);
                r.createCell(1).setCellValue(g.getFecha().format(FECHA_FORMATO));
                r.createCell(2).setCellValue(g.getDescripcion());
                Cell val = r.createCell(3);
                ExcelReportHelper.applyMoneyToCell(val, g.getValor(), money);
            }

            ExcelReportHelper.autoSizeAll(sheet, 7);
            ExcelReportHelper.addFooter(sheet, fila + 2, "POS Restaurante - Cierre Turno");

            workbook.write(baos);
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generando Excel de Turno", e);
        }
    }
}

