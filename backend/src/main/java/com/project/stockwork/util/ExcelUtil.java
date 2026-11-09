package com.project.stockwork.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;

public final class ExcelUtil {

    private ExcelUtil() {
    }

    public static XSSFWorkbook create(String sheetName, String[] headers) {
        XSSFWorkbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(sheetName);
        CellStyle headerStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        headerStyle.setFont(font);

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 22 * 256);
        }
        return workbook;
    }

    public static void write(HttpServletResponse response, XSSFWorkbook workbook, String fileName) {
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("UTF-8");
            String encoded = URLEncoder.encode(fileName, "UTF-8").replace("+", "%20");
            response.setHeader("Content-Disposition", "attachment;filename=" + encoded + ";filename*=UTF-8''" + encoded);
            try (OutputStream out = response.getOutputStream()) {
                workbook.write(out);
                out.flush();
            }
        } catch (IOException e) {
            throw new RuntimeException("导出文件失败", e);
        } finally {
            try {
                workbook.close();
            } catch (IOException ignored) {
                // ignore
            }
        }
    }

    public static void fillRow(Sheet sheet, int rowIndex, Object... values) {
        Row row = sheet.createRow(rowIndex);
        for (int i = 0; i < values.length; i++) {
            Cell cell = row.createCell(i);
            Object v = values[i];
            if (v == null) {
                cell.setCellValue("");
            } else if (v instanceof Number) {
                cell.setCellValue(((Number) v).doubleValue());
            } else {
                cell.setCellValue(String.valueOf(v));
            }
        }
    }
}
