// SPDX-License-Identifier: Apache-2.0
import {
  Grid,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Typography,
} from "@mui/material";
import { AiPageHeader } from "@/components/ui/index";
import { useI18n } from "@/shared/i18n";
import { paymentApi } from "@/shared/api/demo";
import { useListQuery } from "@/shared/hooks/useListQuery";
import type { PaymentDto } from "@/shared/types";
import { ContentCard, PageStack } from "@/shared/pageKit/PageLayout";

export function AiBilling() {
  const { t } = useI18n();
  const { data: payments, loading } = useListQuery<PaymentDto>(
    ["api", "demo", "payments", "list"],
    () => paymentApi.list(),
  );
  const totalCents = payments.reduce((sum, p) => sum + (p.amountCents ?? 0), 0);
  const byCurrency = payments.reduce<Record<string, number>>((acc, p) => {
    const c = (p.currency || "usd").toUpperCase();
    acc[c] = (acc[c] ?? 0) + p.amountCents;
    return acc;
  }, {});

  return (
    <PageStack>
      <AiPageHeader title={t("ai.workspace.aiBilling")} />
      <Grid container spacing={2.5}>
        <Grid size={{ xs: 12, md: 4 }}>
          <ContentCard>
            <Typography variant="subtitle1" sx={{ mb: 0.5, fontWeight: 600 }}>
              {t("ai.workspace.demoPayments")}
            </Typography>
            <Typography variant="h4" sx={{ fontWeight: 700 }}>
              {loading
                ? "\u2026"
                : payments.length === 0
                  ? "$0.00"
                  : `$${(totalCents / 100).toFixed(2)}`}
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
              {loading
                ? t("common.loading")
                : `${payments.length} ${t("ai.workspace.paymentRecordSFromDemoApi")}`}
            </Typography>
          </ContentCard>
        </Grid>
        <Grid size={{ xs: 12, md: 8 }}>
          <ContentCard>
            <Typography variant="subtitle1" sx={{ mb: 2, fontWeight: 600 }}>
              {t("ai.workspace.costBreakdown")}
            </Typography>
            {loading && (
              <Typography variant="body2" color="text.secondary">
                {t("common.loading")}
              </Typography>
            )}
            {!loading && payments.length === 0 && (
              <Typography variant="body2" color="text.secondary">
                {t("ai.workspace.noPaymentRecordsYet")}
              </Typography>
            )}
            {!loading &&
              Object.entries(byCurrency).map(([currency, cents]) => (
                <Stack
                  key={currency}
                  direction="row"
                  sx={{ mb: 1.5, justifyContent: "space-between" }}
                >
                  <Typography variant="body2">{currency}</Typography>
                  <Typography variant="body2" sx={{ fontWeight: 600 }}>
                    ${(cents / 100).toFixed(2)}
                  </Typography>
                </Stack>
              ))}
            {!loading && payments.length > 0 && (
              <Table size="small" sx={{ mt: 2 }}>
                <TableHead>
                  <TableRow>
                    {[t("ai.workspace.order"), t("ai.workspace.amount"), t("common.status")].map(
                      (h) => (
                        <TableCell key={h}>
                          <Typography
                            variant="caption"
                            color="text.secondary"
                            sx={{ textTransform: "uppercase", fontWeight: 600 }}
                          >
                            {h}
                          </Typography>
                        </TableCell>
                      ),
                    )}
                  </TableRow>
                </TableHead>
                <TableBody>
                  {payments.slice(0, 10).map((p: PaymentDto) => (
                    <TableRow key={p.id}>
                      <TableCell>
                        <Typography
                          variant="caption"
                          sx={{ fontFamily: "monospace" }}
                          color="text.secondary"
                        >
                          {p.orderNo}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        {(p.amountCents / 100).toFixed(2)} {p.currency.toUpperCase()}
                      </TableCell>
                      <TableCell>
                        <Typography variant="caption">{p.status}</Typography>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </ContentCard>
        </Grid>
      </Grid>
    </PageStack>
  );
}

export default AiBilling;
