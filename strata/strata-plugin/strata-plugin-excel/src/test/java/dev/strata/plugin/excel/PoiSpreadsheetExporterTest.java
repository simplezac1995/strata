// SPDX-License-Identifier: Apache-2.0
package dev.strata.plugin.excel;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class PoiSpreadsheetExporterTest {

    @Test
    void writes_xlsx_bytes() throws Exception {
        byte[] bytes =
                new PoiSpreadsheetExporter().writeXlsx("users", List.of("id", "name"), List.of(List.of("1", "alice")));
        try (XSSFWorkbook workbook = new XSSFWorkbook(new java.io.ByteArrayInputStream(bytes))) {
            assertThat(workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue())
                    .isEqualTo("id");
            assertThat(workbook.getSheetAt(0).getRow(1).getCell(1).getStringCellValue())
                    .isEqualTo("alice");
        }
    }
}
