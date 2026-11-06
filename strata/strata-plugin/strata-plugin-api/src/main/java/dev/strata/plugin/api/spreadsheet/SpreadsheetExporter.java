// SPDX-License-Identifier: Apache-2.0
package dev.strata.plugin.api.spreadsheet;

import java.util.List;

/** SPI for tabular export (XLSX). Community ships OpenXML via {@code strata-plugin-excel}. */
public interface SpreadsheetExporter {

    byte[] writeXlsx(String sheetName, List<String> headers, List<List<String>> rows);
}
