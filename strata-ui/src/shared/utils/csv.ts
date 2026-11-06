// SPDX-License-Identifier: Apache-2.0
/**
 * Hand-rolled CSV export (no external deps).
 *
 * - Escapes `"` as `""`.
 * - Wraps fields containing comma / quote / newline in double quotes.
 * - Prefixes a UTF-8 BOM so Excel opens the file with correct encoding.
 */
function escapeCsvField(value: string | number | null | undefined): string {
  const s = String(value ?? "");
  const needsQuotes = /[",\n\r]/.test(s);
  const escaped = s.replace(/"/g, '""');
  return needsQuotes ? `"${escaped}"` : escaped;
}

/**
 * Build a CSV blob from headers + rows and trigger a browser download.
 *
 * @param filename  Download filename (e.g. `users.csv`).
 * @param headers   Column header labels.
 * @param rows      Data rows; each cell is a string, number, or null/undefined (normalized to "").
 */
export function downloadCsv(
  filename: string,
  headers: string[],
  rows: Array<Array<string | number | null | undefined>>,
): void {
  const lines = [headers, ...rows].map((row) => row.map(escapeCsvField).join(","));
  const body = "\uFEFF" + lines.join("\r\n");
  const blob = new Blob([body], { type: "text/csv;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = filename;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  URL.revokeObjectURL(url);
}

/**
 * A CSV column definition: a header label plus a cell extractor that pulls the
 * raw (stringifiable) value from a row — mirroring a `DataTable` column's
 * `title` / `render` pair, but returning plain data for export instead of JSX.
 */
export interface CsvColumn<T> {
  title: string;
  cell: (row: T) => string | number | null | undefined;
}

/**
 * Trigger a CSV download, deriving headers and rows from column definitions
 * instead of duplicating the header labels and cell mappings by hand.
 *
 * @param filename  Download filename (e.g. `users.csv`).
 * @param columns   Column definitions; `title` becomes the header row and
 *                  `cell` extracts each row's value in column order.
 * @param rows      Data rows to export.
 */
export function exportCsvFromColumns<T>(
  filename: string,
  columns: Array<CsvColumn<T>>,
  rows: T[],
): void {
  const headers = columns.map((c) => c.title);
  const data = rows.map((row) => columns.map((c) => c.cell(row)));
  downloadCsv(filename, headers, data);
}
