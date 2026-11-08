// SPDX-License-Identifier: Apache-2.0
// Split out of DataTable.tsx by scripts/auto-split.mjs.

import { type ReactNode } from "react";
import type { SortState } from "@/shared/hooks/usePagedQuery";

export interface Col<T> {
  key: string;
  title: string;
  render?: (row: T) => ReactNode;
  /** Column width in px. */
  width?: number;
  /** Whether the column header is clickable to sort. */
  sortable?: boolean;
  /** Sort field sent to the backend; defaults to `key`. */
  sortKey?: string;
  /** Whether the column can be hidden via column settings (default true). */
  hideable?: boolean;
  /** Horizontal alignment of header and cell content. */
  align?: "left" | "center" | "right";
}

export interface TableProps<T> {
  columns: Col<T>[];
  data: T[];
  loading?: boolean;
  rowKey: (r: T) => string;
  selectable?: boolean;
  selectedKeys?: Set<string>;
  onSelectedKeysChange?: (keys: Set<string>) => void;
  /** Total record count across all pages; when a finite number is provided, a pagination bar is rendered inside the card. */
  total?: number;
  /** Current 1-based page (controlled). */
  page?: number;
  /** Current page size. */
  pageSize?: number;
  /** Fired when the user navigates to another page. */
  onPageChange?: (page: number) => void;
  /** Fired when the user changes the page size. */
  onPageSizeChange?: (size: number) => void;
  /** Current controlled sort state. */
  sort?: SortState;
  /** Fired when a sortable column header is clicked (cycles asc -> desc -> clear). */
  onSortChange?: (s: SortState | undefined) => void;
  /** Batch action strip content; rendered on the right when rows are selected. */
  batchActions?: ReactNode;
  /** Stable id enabling column visibility settings + resizable columns (persisted to localStorage). */
  tableId?: string;
  /** Fired when a data row is clicked. */
  onRowClick?: (row: T) => void;
  /** Error object from the query; renders an error empty-state when truthy. */
  error?: unknown;
  /** Retry callback shown in the error empty-state. */
  onRetry?: () => void;
  /** Optional action rendered inside the normal (non-error) empty state. */
  emptyAction?: ReactNode;
}

export const COL_STORAGE_PREFIX = "strata_table_cols:v1:";

export const MIN_COL_WIDTH = 48;

export interface TableColPrefs {
  hidden: string[];
  widths: Record<string, number>;
}

export function readColPrefs(storageKey: string): TableColPrefs {
  try {
    const raw = localStorage.getItem(storageKey);
    if (!raw) return { hidden: [], widths: {} };
    const parsed: unknown = JSON.parse(raw);
    if (Array.isArray(parsed)) {
      // legacy shape: bare array of hidden keys
      return { hidden: parsed as string[], widths: {} };
    }
    if (parsed && typeof parsed === "object") {
      const obj = parsed as Record<string, unknown>;
      return {
        hidden: Array.isArray(obj.hidden) ? (obj.hidden as string[]) : [],
        widths:
          obj.widths && typeof obj.widths === "object"
            ? (obj.widths as Record<string, number>)
            : {},
      };
    }
  } catch {
    // corrupted storage -> defaults
  }
  return { hidden: [], widths: {} };
}
