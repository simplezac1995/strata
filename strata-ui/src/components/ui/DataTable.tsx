// SPDX-License-Identifier: Apache-2.0
import {
  useEffect,
  useMemo,
  useRef,
  useState,
  type MouseEvent as ReactMouseEvent,
  type ReactNode,
} from "react";
import {
  Box,
  Card as MuiCard,
  Checkbox as MuiCheckbox,
  FormControlLabel,
  IconButton,
  Popover,
  Skeleton,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Toolbar,
  Typography,
  alpha,
} from "@mui/material";
import { ArrowDownward, ArrowUpward, Tune } from "@mui/icons-material";
import { useI18n } from "@/shared/i18n";
import { EmptyState } from "@/shared/pageKit/EmptyState";
import { Alert } from "./Alert";
import { Button } from "./Button";
import { Pagination } from "./Pagination";
import {
  Col,
  TableProps,
  COL_STORAGE_PREFIX,
  MIN_COL_WIDTH,
  TableColPrefs,
  readColPrefs,
} from "./datatable/part-1";

export function DataTable<T>({
  columns,
  data,
  loading,
  rowKey,
  selectable,
  selectedKeys: controlledKeys,
  onSelectedKeysChange,
  total,
  page,
  pageSize,
  onPageChange,
  onPageSizeChange,
  sort,
  onSortChange,
  batchActions,
  tableId,
  onRowClick,
  error,
  onRetry,
  emptyAction,
}: TableProps<T>) {
  const { t } = useI18n();
  const [internalSelected, setInternalSelected] = useState<Set<string>>(new Set());
  const selected = controlledKeys ?? internalSelected;
  const setSelected = (update: Set<string> | ((prev: Set<string>) => Set<string>)) => {
    if (onSelectedKeysChange) {
      onSelectedKeysChange(typeof update === "function" ? update(selected) : update);
    } else {
      setInternalSelected(update);
    }
  };
  const toggleAll = () =>
    setSelected((s) => (s.size === data.length ? new Set() : new Set(data.map(rowKey))));
  const toggle = (k: string) =>
    setSelected((s) => {
      const n = new Set(s);
      if (n.has(k)) n.delete(k);
      else n.add(k);
      return n;
    });

  // Column visibility + widths (only active when tableId is provided).
  const storageKey = tableId ? `${COL_STORAGE_PREFIX}${tableId}` : null;
  const [hiddenKeys, setHiddenKeys] = useState<Set<string>>(() => {
    if (!storageKey) return new Set<string>();
    return new Set(readColPrefs(storageKey).hidden);
  });
  const [widths, setWidths] = useState<Record<string, number>>(() => {
    if (!storageKey) return {};
    return readColPrefs(storageKey).widths;
  });
  useEffect(() => {
    if (!storageKey) return;
    try {
      localStorage.setItem(
        storageKey,
        JSON.stringify({ hidden: [...hiddenKeys], widths } satisfies TableColPrefs),
      );
    } catch {
      // storage unavailable (private mode) -> ignore
    }
  }, [hiddenKeys, widths, storageKey]);

  const visibleColumns = useMemo(
    () => columns.filter((c) => !hiddenKeys.has(c.key)),
    [columns, hiddenKeys],
  );
  // The "actions" column is never hideable.
  const hideableColumns = useMemo(
    () => columns.filter((c) => c.hideable !== false && c.key !== "actions"),
    [columns],
  );
  const toggleColumn = (key: string) =>
    setHiddenKeys((prev) => {
      const next = new Set(prev);
      if (next.has(key)) next.delete(key);
      else next.add(key);
      return next;
    });

  const [menuAnchor, setMenuAnchor] = useState<HTMLElement | null>(null);

  // ── Column resize (drag handle on the header right edge) ──
  const dragRef = useRef<{ colKey: string; startX: number; startWidth: number } | null>(null);

  const startResize = (
    e: ReactMouseEvent<HTMLSpanElement>,
    colKey: string,
    currentWidth: number,
  ) => {
    e.preventDefault();
    e.stopPropagation();
    const startX = e.clientX;
    const startWidth = currentWidth;
    dragRef.current = { colKey, startX, startWidth };
    const onMove = (ev: MouseEvent) => {
      const drag = dragRef.current;
      if (!drag) return;
      const nextWidth = Math.max(
        MIN_COL_WIDTH,
        Math.round(drag.startWidth + (ev.clientX - drag.startX)),
      );
      setWidths((w) => ({ ...w, [drag.colKey]: nextWidth }));
    };
    const onUp = () => {
      window.removeEventListener("mousemove", onMove);
      window.removeEventListener("mouseup", onUp);
      dragRef.current = null;
    };
    window.addEventListener("mousemove", onMove);
    window.addEventListener("mouseup", onUp);
  };

  const cycleSort = (sortField: string) => {
    if (!onSortChange) return;
    if (!sort || sort.field !== sortField) onSortChange({ field: sortField, dir: "asc" });
    else if (sort.dir === "asc") onSortChange({ field: sortField, dir: "desc" });
    else onSortChange(undefined);
  };

  const renderHeaderCell = (c: Col<T>) => {
    const sortField = c.sortKey ?? c.key;
    const isSortable = !!c.sortable && !!onSortChange;
    const active = sort?.field === sortField;
    const effectiveWidth = widths[c.key] ?? c.width;
    const currentWidth = widths[c.key] ?? c.width ?? 120;
    return (
      <TableCell
        key={c.key}
        sx={{
          width: effectiveWidth,
          fontWeight: 600,
          position: "relative",
          textAlign: c.align ?? "center",
        }}
      >
        {isSortable ? (
          <Box
            component="button"
            type="button"
            onClick={() => cycleSort(sortField)}
            sx={{
              display: "inline-flex",
              alignItems: "center",
              gap: 0.5,
              fontWeight: 600,
              fontFamily: "inherit",
              fontSize: "inherit",
              color: "inherit",
              background: "none",
              border: "none",
              padding: 0,
              cursor: "pointer",
            }}
          >
            {c.title}
            {active ? (
              sort?.dir === "asc" ? (
                <ArrowUpward sx={{ fontSize: 15 }} />
              ) : (
                <ArrowDownward sx={{ fontSize: 15 }} />
              )
            ) : null}
          </Box>
        ) : (
          c.title
        )}
        {storageKey ? (
          <span
            onMouseDown={(e) => startResize(e, c.key, currentWidth)}
            title={t("common.resizeColumn")}
            style={{
              position: "absolute",
              top: 0,
              right: 0,
              height: "100%",
              width: 5,
              cursor: "col-resize",
              touchAction: "none",
              userSelect: "none",
            }}
          />
        ) : null}
      </TableCell>
    );
  };

  const showError = !!error && !loading && data.length === 0;
  const showBatchStrip = !!batchActions && selected.size > 0;

  return (
    <MuiCard>
      {tableId ? (
        <Toolbar
          variant="dense"
          sx={{ minHeight: 44, px: 2, gap: 1, borderBottom: 1, borderColor: "divider" }}
        >
          <Box sx={{ flexGrow: 1 }} />
          <IconButton
            size="small"
            aria-label={t("common.columnSettings")}
            onClick={(e) => setMenuAnchor(e.currentTarget)}
          >
            <Tune fontSize="small" />
          </IconButton>
          <Popover
            open={!!menuAnchor}
            anchorEl={menuAnchor}
            onClose={() => setMenuAnchor(null)}
            anchorOrigin={{ vertical: "bottom", horizontal: "right" }}
            transformOrigin={{ vertical: "top", horizontal: "right" }}
          >
            <Box sx={{ py: 0.5, minWidth: 220 }}>
              {hideableColumns.map((c) => (
                <FormControlLabel
                  key={c.key}
                  sx={{ px: 1.5, py: 0.25, width: "100%", margin: 0 }}
                  control={
                    <MuiCheckbox
                      size="small"
                      checked={!hiddenKeys.has(c.key)}
                      onChange={() => toggleColumn(c.key)}
                    />
                  }
                  label={c.title}
                />
              ))}
            </Box>
          </Popover>
        </Toolbar>
      ) : null}
      <TableContainer sx={{ overflowX: "auto" }}>
        <Table size="small">
          <TableHead>
            <TableRow>
              {selectable ? (
                <TableCell padding="checkbox">
                  <MuiCheckbox
                    size="small"
                    checked={selected.size === data.length && data.length > 0}
                    indeterminate={selected.size > 0 && selected.size < data.length}
                    onChange={toggleAll}
                  />
                </TableCell>
              ) : null}
              {visibleColumns.map(renderHeaderCell)}
            </TableRow>
          </TableHead>
          <TableBody>
            {loading ? (
              Array.from({ length: 6 }).map((_, i) => (
                <TableRow key={i}>
                  {selectable ? (
                    <TableCell padding="checkbox">
                      <Skeleton variant="rounded" width={18} height={18} />
                    </TableCell>
                  ) : null}
                  {visibleColumns.map((c, colIdx) => (
                    <TableCell key={c.key} sx={{ textAlign: "center" }}>
                      <Skeleton
                        variant="text"
                        width={
                          colIdx === 0
                            ? "55%"
                            : c.key === "status"
                              ? "35%"
                              : c.key === "actions"
                                ? "75%"
                                : "65%"
                        }
                      />
                    </TableCell>
                  ))}
                </TableRow>
              ))
            ) : data.length === 0 ? (
              <TableRow>
                <TableCell colSpan={visibleColumns.length + (selectable ? 1 : 0)}>
                  {showError ? (
                    <Stack spacing={1.5} sx={{ alignItems: "center", py: 1 }}>
                      <Alert type="error" sx={{ width: "100%", maxWidth: 480 }}>
                        {t("common.loadFailed")}
                      </Alert>
                      {onRetry ? (
                        <Button variant="outlined" size="sm" onClick={onRetry}>
                          {t("common.retry")}
                        </Button>
                      ) : null}
                    </Stack>
                  ) : (
                    <EmptyState message={t("common.noData")} action={emptyAction} />
                  )}
                </TableCell>
              </TableRow>
            ) : (
              data.map((row) => {
                const k = rowKey(row);
                return (
                  <TableRow
                    key={k}
                    hover
                    onClick={onRowClick ? () => onRowClick(row) : undefined}
                    sx={onRowClick ? { cursor: "pointer" } : undefined}
                  >
                    {selectable ? (
                      <TableCell padding="checkbox" onClick={(e) => e.stopPropagation()}>
                        <MuiCheckbox
                          size="small"
                          checked={selected.has(k)}
                          onChange={() => toggle(k)}
                        />
                      </TableCell>
                    ) : null}
                    {visibleColumns.map((c) => (
                      <TableCell
                        key={c.key}
                        sx={{ textAlign: c.align ?? "center", whiteSpace: "nowrap" }}
                      >
                        <Box
                          sx={{
                            display: "flex",
                            justifyContent:
                              c.align === "left"
                                ? "flex-start"
                                : c.align === "right"
                                  ? "flex-end"
                                  : "center",
                            alignItems: "center",
                          }}
                        >
                          {c.render
                            ? c.render(row)
                            : ((row as Record<string, unknown>)[c.key] as ReactNode)}
                        </Box>
                      </TableCell>
                    ))}
                  </TableRow>
                );
              })
            )}
          </TableBody>
        </Table>
      </TableContainer>
      {showBatchStrip ? (
        <Box
          sx={{
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            gap: 2,
            px: 2,
            py: 1,
            bgcolor: (theme) =>
              alpha(theme.palette.primary.main, theme.palette.mode === "light" ? 0.08 : 0.18),
          }}
        >
          <Typography variant="body2" sx={{ fontWeight: 600 }}>
            {t("common.batchSelected").replace("{n}", String(selected.size))}
          </Typography>
          <Box sx={{ display: "flex", gap: 1, flexWrap: "wrap", justifyContent: "flex-end" }}>
            {batchActions}
          </Box>
        </Box>
      ) : null}
      {typeof total === "number" &&
      Number.isFinite(total) &&
      page !== undefined &&
      pageSize !== undefined &&
      onPageChange &&
      onPageSizeChange ? (
        <Pagination
          total={total}
          page={page}
          pageSize={pageSize}
          onPage={onPageChange}
          onPageSize={onPageSizeChange}
        />
      ) : null}
    </MuiCard>
  );
}
