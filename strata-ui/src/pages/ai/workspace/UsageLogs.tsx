// SPDX-License-Identifier: Apache-2.0
import { useEffect, useState } from "react";
import { Paper, Table, TableBody, TableCell, TableHead, TableRow } from "@mui/material";
import { AiPageHeader, Pagination } from "@/components/ui/index";
import { useI18n } from "@/shared/i18n";
import { apiGet } from "@/shared/api/client";
import { PageStack } from "@/shared/pageKit/PageLayout";

interface UsageLogRow {
  id: number;
  provider: string;
  model: string;
  action: string;
  sessionId: string;
  promptTokens: number;
  completionTokens: number;
  totalTokens: number;
  createdAt: string;
}

interface Resp {
  records: UsageLogRow[];
  total: number;
}

export function UsageLogs() {
  const { t } = useI18n();
  const [rows, setRows] = useState<UsageLogRow[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    apiGet<Resp>(`/api/v1/ai/usage-logs?page=${page}&size=${size}`)
      .then((r: any) => {
        if (cancelled) return;
        setRows(r.records ?? []);
        setTotal(r.total ?? 0);
      })
      .catch(() => {})
      .finally(() => !cancelled && setLoading(false));
    return () => {
      cancelled = true;
    };
  }, [page, size]);

  return (
    <PageStack>
      <AiPageHeader title={t("ai.logs.title")} subtitle={t("ai.logs.subtitle")} />
      <Paper>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>{t("ai.logs.time")}</TableCell>
              <TableCell>{t("ai.logs.provider")}</TableCell>
              <TableCell>{t("ai.logs.model")}</TableCell>
              <TableCell>{t("ai.logs.action")}</TableCell>
              <TableCell align="right">{t("ai.logs.prompt")}</TableCell>
              <TableCell align="right">{t("ai.logs.completion")}</TableCell>
              <TableCell align="right">{t("ai.logs.total")}</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {loading ? (
              <TableRow>
                <TableCell colSpan={7}>…</TableCell>
              </TableRow>
            ) : rows.length === 0 ? (
              <TableRow>
                <TableCell colSpan={7} align="center">
                  —
                </TableCell>
              </TableRow>
            ) : (
              rows.map((r) => (
                <TableRow key={r.id}>
                  <TableCell>{new Date(r.createdAt).toLocaleString()}</TableCell>
                  <TableCell>{r.provider}</TableCell>
                  <TableCell>{r.model}</TableCell>
                  <TableCell>{r.action}</TableCell>
                  <TableCell align="right">{r.promptTokens}</TableCell>
                  <TableCell align="right">{r.completionTokens}</TableCell>
                  <TableCell align="right">{r.totalTokens}</TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
        <Pagination
          total={total}
          page={page + 1}
          pageSize={size}
          onPage={(p) => setPage(p - 1)}
          onPageSize={(s) => {
            setSize(s);
            setPage(0);
          }}
        />
      </Paper>
    </PageStack>
  );
}
