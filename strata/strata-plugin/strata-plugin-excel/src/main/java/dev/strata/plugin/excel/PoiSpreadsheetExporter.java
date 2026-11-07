// SPDX-License-Identifier: Apache-2.0
package dev.strata.plugin.excel;

import dev.strata.plugin.api.spreadsheet.SpreadsheetExporter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class PoiSpreadsheetExporter implements SpreadsheetExporter {

    @Override
    public byte[] writeXlsx(String sheetName, List<String> headers, List<List<String>> rows) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(sheetName == null || sheetName.isBlank() ? "Sheet1" : sheetName);
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                headerRow.createCell(i).setCellValue(headers.get(i));
            }
            int rowIndex = 1;
            for (List<String> rowValues : rows) {
                Row row = sheet.createRow(rowIndex++);
                for (int i = 0; i < rowValues.size(); i++) {
                    row.createCell(i).setCellValue(rowValues.get(i));
                }
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to write XLSX", ex);
        }
    }
}
