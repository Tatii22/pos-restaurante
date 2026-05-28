package com.pos.service.export.excel;

import com.pos.dto.report.ReporteRentabilidadDTO;
import com.pos.dto.venta.VentaResponseDTO;
import com.pos.dto.gasto.GastoResponseDTO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExcelRentabilidadExporter {

    private static final DateTimeFormatter FECHA_FORMATO =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final String[] MESES = {
        "Enero","Febrero","Marzo","Abril","Mayo","Junio",
        "Julio","Agosto","Septiembre","Octubre","Noviembre","Diciembre"
    };

    private String mesAnio(LocalDate d) {
        return MESES[d.getMonthValue() - 1] + " " + d.getYear();
    }

    public byte[] exportar(ReporteRentabilidadDTO reporte) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Rentabilidad");
            int fila = 0;

            String periodo = mesAnio(reporte.getFechaInicio());

            ExcelReportHelper.addTitle(sheet, fila++, "INFORME DE CIERRE DE MES — " + periodo.toUpperCase());
            ExcelReportHelper.addSubtitle(sheet, fila++,
                    "Período: " + reporte.getFechaInicio() + " → " + reporte.getFechaFin());

            fila++;

            CellStyle money = ExcelReportHelper.createMoneyStyle(workbook);
            CellStyle header = ExcelReportHelper.createHeaderStyle(workbook);

            // KPIs — Ingresos y Gastos 50/50 en la misma fila
            Row kpiRow = sheet.createRow(fila++);
            Cell iLabel = kpiRow.createCell(0);
            iLabel.setCellValue("Ingresos recibidos");
            Cell iVal = kpiRow.createCell(1);
            ExcelReportHelper.applyMoneyToCell(iVal, reporte.getRecaudoReal(), money);
            Cell gLabel = kpiRow.createCell(2);
            gLabel.setCellValue("Gastos registrados");
            Cell gVal = kpiRow.createCell(3);
            ExcelReportHelper.applyMoneyToCell(gVal, reporte.getTotalGastos(), money);

            // Ganancia neta — fila completa destacada
            ExcelReportHelper.addKpiRow(sheet, fila++, "Ganancia neta del mes", reporte.getGananciaNeta(), money, true);

            fila++;

            // Tabla Ventas
            String[] colsV = {"#", "Fecha", "Tipo", "Estado", "Cliente", "Total", "Forma Pago"};
            ExcelReportHelper.addTableHeader(sheet, fila++, colsV, header);
            int idx = 1;
            for (VentaResponseDTO v : reporte.getVentas()) {
                Row r = sheet.createRow(fila++);
                r.createCell(0).setCellValue(idx++);
                r.createCell(1).setCellValue(v.fecha().format(FECHA_FORMATO));
                r.createCell(2).setCellValue(v.tipoVenta().name());
                r.createCell(3).setCellValue(v.estado().name());
                r.createCell(4).setCellValue(v.clienteNombre() != null ? v.clienteNombre() : "-");
                Cell t = r.createCell(5);
                ExcelReportHelper.applyMoneyToCell(t, v.total(), money);
                String pago = v.formaPago() == com.pos.entity.FormaPago.FIADO ? "ABONO" : v.formaPago().name();
                r.createCell(6).setCellValue(pago);
            }

            fila++;

            // Tabla Gastos
            String[] colsG = {"#", "Fecha", "Descripción", "Valor"};
            ExcelReportHelper.addTableHeader(sheet, fila++, colsG, header);
            idx = 1;
            for (GastoResponseDTO g : reporte.getGastos()) {
                Row r = sheet.createRow(fila++);
                r.createCell(0).setCellValue(idx++);
                r.createCell(1).setCellValue(g.getFecha().format(FECHA_FORMATO));
                r.createCell(2).setCellValue(g.getDescripcion());
                Cell val = r.createCell(3);
                ExcelReportHelper.applyMoneyToCell(val, g.getMonto(), money);
            }

            ExcelReportHelper.autoSizeAll(sheet, 7);
            ExcelReportHelper.addFooter(sheet, fila + 2, "MentaPOS • Cierre de Mes — " + periodo);

            workbook.write(baos);
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generando Excel de Rentabilidad", e);
        }
    }
}
