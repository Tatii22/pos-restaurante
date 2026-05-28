package com.pos.service.export.excel;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.util.CellRangeAddress;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Helper profesional para generar Excels financieros consistentes.
 * Centraliza estilos, formatos monetarios y estructura de reportes.
 */
public final class ExcelReportHelper {

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String MONEY_FORMAT = "$#,##0.00";

    private ExcelReportHelper() {}

    // ================== ESTILOS REUTILIZABLES ==================
    public static CellStyle createTitleStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    public static CellStyle createSubtitleStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontHeightInPoints((short) 11);
        font.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    public static CellStyle createSectionHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    public static CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 10);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    public static CellStyle createMoneyStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        DataFormat format = wb.createDataFormat();
        style.setDataFormat(format.getFormat(MONEY_FORMAT));
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }

    public static CellStyle createKpiValueStyle(Workbook wb, boolean positive) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }

    // ================== MÉTODOS DE ALTO NIVEL ==================
    public static void addTitle(Sheet sheet, int row, String text) {
        Row r = sheet.createRow(row);
        Cell c = r.createCell(0);
        c.setCellValue(text);
        c.setCellStyle(createTitleStyle(sheet.getWorkbook()));
        sheet.addMergedRegion(new CellRangeAddress(row, row, 0, 6));
    }

    public static void addSubtitle(Sheet sheet, int row, String text) {
        Row r = sheet.createRow(row);
        Cell c = r.createCell(0);
        c.setCellValue(text);
        c.setCellStyle(createSubtitleStyle(sheet.getWorkbook()));
        sheet.addMergedRegion(new CellRangeAddress(row, row, 0, 6));
    }

    public static void addSectionHeader(Sheet sheet, int row, String text) {
        Row r = sheet.createRow(row);
        Cell c = r.createCell(0);
        c.setCellValue(text.toUpperCase());
        c.setCellStyle(createSectionHeaderStyle(sheet.getWorkbook()));
        sheet.addMergedRegion(new CellRangeAddress(row, row, 0, 6));
    }

    public static void addKpiRow(Sheet sheet, int row, String label, BigDecimal value, CellStyle moneyStyle) {
        addKpiRow(sheet, row, label, value, moneyStyle, false);
    }

    public static void addKpiRow(Sheet sheet, int row, String label, BigDecimal value, CellStyle moneyStyle, boolean destacado) {
        Row r = sheet.createRow(row);
        Cell labelCell = r.createCell(0);
        labelCell.setCellValue(label);

        Cell valCell = r.createCell(1);
        valCell.setCellValue(value != null ? value.doubleValue() : 0.0);
        valCell.setCellStyle(moneyStyle);

        if (destacado) {
            // Estilo premium para "Ganancia neta del mes"
            CellStyle special = sheet.getWorkbook().createCellStyle();
            special.cloneStyleFrom(moneyStyle);
            Font f = sheet.getWorkbook().createFont();
            f.setBold(true);
            f.setFontHeightInPoints((short) 12);
            special.setFont(f);
            special.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            special.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            valCell.setCellStyle(special);

            CellStyle labelSpecial = sheet.getWorkbook().createCellStyle();
            Font fl = sheet.getWorkbook().createFont();
            fl.setBold(true);
            fl.setFontHeightInPoints((short) 11);
            labelSpecial.setFont(fl);
            labelSpecial.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            labelSpecial.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            labelCell.setCellStyle(labelSpecial);

            // Borde superior para destacar
            special.setBorderTop(BorderStyle.MEDIUM);
            special.setTopBorderColor(IndexedColors.DARK_GREEN.getIndex());
        }
    }

    public static void addTableHeader(Sheet sheet, int row, String[] headers, CellStyle headerStyle) {
        Row r = sheet.createRow(row);
        for (int i = 0; i < headers.length; i++) {
            Cell c = r.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(headerStyle);
        }
    }

    public static void applyMoneyToCell(Cell cell, BigDecimal value, CellStyle moneyStyle) {
        cell.setCellValue(value != null ? value.doubleValue() : 0.0);
        cell.setCellStyle(moneyStyle);
    }

    public static String formatMoney(BigDecimal v) {
        return String.format("$%,.2f", v != null ? v : BigDecimal.ZERO);
    }

    public static void autoSizeAll(Sheet sheet, int maxCols) {
        for (int i = 0; i < maxCols; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    public static void addFooter(Sheet sheet, int row, String sistema) {
        Row r = sheet.createRow(row);
        Cell c = r.createCell(0);
        c.setCellValue(sistema + " • Generado: " + LocalDateTime.now().format(FECHA_HORA));
        CellStyle style = sheet.getWorkbook().createCellStyle();
        Font f = sheet.getWorkbook().createFont();
        f.setFontHeightInPoints((short) 8);
        f.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setFont(f);
        c.setCellStyle(style);
    }
}
