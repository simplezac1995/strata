// SPDX-License-Identifier: Apache-2.0
import { useMemo, useState } from "react";
import {
  Box,
  CircularProgress,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Typography,
} from "@mui/material";
import { Button, TextArea } from "@/components/ui/index";
import { AiPageHeader } from "@/components/ui/index";
import { BarChart, LineChart, PieChart } from "@/components/ui/chart";
import { useI18n } from "@/shared/i18n";
import { aiSqlApi, type AiSqlChatResult, type AiSqlRow } from "@/shared/api/aiSql";
import { failureMessage } from "@/shared/hooks/useCrud";
import { toast } from "@/shared/pageKit/Toast";
import { PageStack, ContentCard } from "@/shared/pageKit/PageLayout";

function isNumericCell(v: string | number | boolean | null): v is number {
  return typeof v === "number" && Number.isFinite(v);
}

/**
 * Build chart series from columns/rows + the server's chartSuggestion.
 * categoryColumn overrides the auto-detected axis; valueColumns overrides the
 * series selection. Falls back to auto-detection when suggestion is absent.
 */
function deriveChart(result: AiSqlChatResult | null) {
  if (!result || result.rows.length === 0) return null;
  const { columns, rows, chartSuggestion } = result;
  if (columns.length < 2) return null;

  const numericCols = columns.filter((col) => rows.some((r) => isNumericCell(r[col])));
  if (numericCols.length === 0) return null;

  const nonNumericCols = columns.filter((c) => !numericCols.includes(c));
  const catCol =
    chartSuggestion?.categoryColumn && columns.includes(chartSuggestion.categoryColumn)
      ? chartSuggestion.categoryColumn
      : (nonNumericCols[0] ?? columns[0]);

  const seriesCols =
    chartSuggestion?.valueColumns && chartSuggestion.valueColumns.length > 0
      ? chartSuggestion.valueColumns.filter((c) => numericCols.includes(c))
      : numericCols;
  if (seriesCols.length === 0) return null;

  const categories = rows.map((r) => String(r[catCol] ?? ""));
  const series = seriesCols.map((col) => ({
    name: col,
    data: rows.map((r) => Number(r[col] ?? 0)),
  }));

  const suggested = chartSuggestion?.chartType;
  const looksTime = /date|time|day|month|hour/i.test(catCol);
  const chartType: "line" | "bar" | "pie" =
    suggested === "line" || suggested === "bar" || suggested === "pie"
      ? suggested
      : looksTime || rows.length > 12
        ? "line"
        : "bar";

  return { categories, series, chartType };
}

export function Chat2Sql() {
  const { t } = useI18n();
  const [question, setQuestion] = useState("");
  const [busy, setBusy] = useState(false);
  const [result, setResult] = useState<AiSqlChatResult | null>(null);

  const chart = useMemo(() => deriveChart(result), [result]);

  async function runQuery() {
    const q = question.trim();
    if (!q || busy) return;
    setBusy(true);
    setResult(null);
    try {
      const res = await aiSqlApi.chat(q);
      setResult(res);
    } catch (err) {
      toast.error(failureMessage(err, "toast.saveFailed"));
    } finally {
      setBusy(false);
    }
  }

  const rows: AiSqlRow[] = result?.rows ?? [];
  const columns: string[] = result?.columns ?? [];

  return (
    <PageStack>
      <AiPageHeader title={t("ai.sql.title")} subtitle={t("ai.sql.subtitle")} />

      <ContentCard>
        <Stack spacing={1.5}>
          <Typography variant="body2" sx={{ fontWeight: 600 }}>
            {t("ai.sql.askPlaceholder")}
          </Typography>
          <Stack direction="row" spacing={1} sx={{ alignItems: "flex-end" }}>
            <TextArea
              value={question}
              onChange={(e) => setQuestion(e.target.value)}
              placeholder={t("ai.sql.inputPlaceholder")}
              rows={2}
              disabled={busy}
              onKeyDown={(e) => {
                if (e.key === "Enter" && (e.ctrlKey || e.metaKey)) {
                  e.preventDefault();
                  void runQuery();
                }
              }}
              sx={{ flex: 1 }}
            />
            <Button
              variant="contained"
              onClick={() => void runQuery()}
              disabled={!question.trim() || busy}
            >
              {busy ? t("ai.common.runningEllipsis") : t("ai.sql.run")}
            </Button>
          </Stack>
        </Stack>
      </ContentCard>

      <ContentCard>
        <Typography variant="body2" sx={{ fontWeight: 600, mb: 1 }}>
          {t("ai.sql.sqlPreview")}
        </Typography>
        <Paper variant="outlined" sx={{ p: 1.5, bgcolor: "action.hover" }}>
          {busy ? (
            <Stack direction="row" spacing={1} sx={{ alignItems: "center" }}>
              <CircularProgress size={16} />
              <Typography variant="caption" color="text.secondary">
                {t("ai.common.generating")}
              </Typography>
            </Stack>
          ) : result?.sql ? (
            <Typography
              component="pre"
              variant="caption"
              sx={{ fontFamily: "monospace", whiteSpace: "pre-wrap", m: 0 }}
            >
              {result.sql}
            </Typography>
          ) : (
            <Typography variant="caption" color="text.secondary">
              {t("ai.sql.sqlEmpty")}
            </Typography>
          )}
        </Paper>
      </ContentCard>

      <ContentCard>
        <Typography variant="body2" sx={{ fontWeight: 600, mb: 1 }}>
          {t("ai.sql.resultTable")}
        </Typography>
        {rows.length === 0 ? (
          <Typography variant="caption" color="text.secondary">
            {busy ? t("ai.common.generating") : t("ai.sql.resultEmpty")}
          </Typography>
        ) : (
          <Box sx={{ overflowX: "auto" }}>
            <Table size="small">
              <TableHead>
                <TableRow>
                  {columns.map((c) => (
                    <TableCell key={c} sx={{ fontWeight: 600 }}>
                      {c}
                    </TableCell>
                  ))}
                </TableRow>
              </TableHead>
              <TableBody>
                {rows.map((row, i) => (
                  <TableRow key={i}>
                    {columns.map((c) => (
                      <TableCell key={c}>{String(row[c] ?? "")}</TableCell>
                    ))}
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </Box>
        )}
      </ContentCard>

      {chart && (
        <ContentCard>
          <Typography variant="body2" sx={{ fontWeight: 600, mb: 1 }}>
            {t("ai.sql.chart")}
          </Typography>
          {chart.chartType === "line" ? (
            <LineChart categories={chart.categories} series={chart.series} height={320} />
          ) : chart.chartType === "pie" ? (
            <PieChart
              data={chart.categories.map((name, i) => ({
                name,
                value: chart.series[0]?.data[i] ?? 0,
              }))}
              height={320}
              donut
            />
          ) : (
            <BarChart categories={chart.categories} series={chart.series} height={320} />
          )}
        </ContentCard>
      )}
    </PageStack>
  );
}

export default Chat2Sql;
