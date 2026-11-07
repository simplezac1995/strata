// SPDX-License-Identifier: Apache-2.0
import { useEffect, useState } from "react";
import { Link, Stack, Typography } from "@mui/material";
import { useI18n } from "@/shared/i18n";
import {
  Button,
  PageHeader,
  Badge,
  TextField,
  Select,
  Alert,
  DataTable,
} from "@/components/ui/index";
import { paymentApi, pluginApi } from "@/shared/api/demo";
import { useListQuery } from "@/shared/hooks/useListQuery";
import { useMutationWithToast } from "@/shared/hooks/useMutationWithToast";
import type { PaymentDto } from "@/shared/types";
import { ContentCard, PageStack } from "@/shared/pageKit/PageLayout";

export function PaymentsStripe() {
  const { t } = useI18n();
  const [stripeOn, setStripeOn] = useState<boolean | null>(null);
  const [amount, setAmount] = useState("9.99");
  const [currency, setCurrency] = useState("usd");
  const [checkoutUrl, setCheckoutUrl] = useState<string | null>(null);
  const { data, loading, reload } = useListQuery<PaymentDto>(["api", "plugins", "payments"], () =>
    paymentApi.list(),
  );

  const checkoutMutation = useMutationWithToast(
    ({ amountCents, currency }: { amountCents: number; currency: string }) =>
      paymentApi.create({ amountCents, currency }),
    { success: "toast.saved", failure: "toast.saveFailed" },
    {
      onSuccess: (created) => {
        setCheckoutUrl(created.checkoutUrl);
        void reload();
        if (created.checkoutUrl) {
          window.open(created.checkoutUrl, "_blank", "noopener,noreferrer");
        }
      },
    },
  );

  useEffect(() => {
    pluginApi
      .status()
      .then((s) => setStripeOn(s.paymentStripe))
      .catch(() => {
        // Non-fatal: treat an unreachable capability check as "Stripe off" rather than alarming the user.
        setStripeOn(false);
      });
  }, []);

  async function launchCheckout() {
    const cents = Math.round(Number(amount) * 100);
    try {
      await checkoutMutation.mutateAsync({ amountCents: cents, currency });
    } catch {
      // toast already shown
    }
  }

  const cols = [
    { key: "orderNo", title: t("plugins.common.order") },
    {
      key: "amount",
      title: t("plugins.common.amount"),
      render: (r: PaymentDto) => (
        <Typography variant="caption" sx={{ fontFamily: "monospace" }}>
          {(r.amountCents / 100).toFixed(2)} {r.currency.toUpperCase()}
        </Typography>
      ),
    },
    {
      key: "status",
      title: t("plugins.common.status"),
      render: (r: PaymentDto) => (
        <Badge
          variant={
            r.status === "succeeded" ? "success" : r.status === "failed" ? "error" : "warning"
          }
        >
          {r.status}
        </Badge>
      ),
    },
  ];

  if (stripeOn === false) {
    return (
      <PageStack>
        <PageHeader title={t("nav.payments")} subtitle={t("plugins.payments.subtitle")} />
        <Alert>{t("plugins.payments.stripeNotEnabled")}</Alert>
      </PageStack>
    );
  }

  return (
    <PageStack>
      <PageHeader title={t("nav.payments")} subtitle={t("plugins.payments.subtitle")} />

      <ContentCard>
        <Typography variant="h6" sx={{ mb: 2 }}>
          {t("plugins.payments.createCheckout")}
        </Typography>
        <Stack spacing={2}>
          <Stack direction={{ xs: "column", sm: "row" }} spacing={2}>
            <TextField
              label={t("plugins.payments.amount")}
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              type="number"
              sx={{ width: { xs: "100%", sm: 220 } }}
            />
            <Select
              label={t("plugins.payments.currency")}
              value={currency}
              onChange={(e) => setCurrency(e.target.value)}
              options={[
                { value: "usd", label: "USD" },
                { value: "eur", label: "EUR" },
                { value: "cny", label: "CNY" },
              ]}
              sx={{ flex: 1, minWidth: 160 }}
            />
          </Stack>
          <Button
            variant="contained"
            disabled={checkoutMutation.isPending || !amount}
            onClick={() => void launchCheckout()}
          >
            {checkoutMutation.isPending ? t("common.saving") : t("plugins.payments.launchCheckout")}
          </Button>
          {checkoutUrl && (
            <Alert>
              {t("plugins.payments.checkoutCreated")}{" "}
              <Link href={checkoutUrl} target="_blank" rel="noopener noreferrer">
                {t("plugins.payments.openCheckout")}
              </Link>
            </Alert>
          )}
        </Stack>
      </ContentCard>

      <ContentCard>
        <Typography variant="h6" sx={{ mb: 2 }}>
          {t("plugins.payments.paymentHistory")}
        </Typography>
        <DataTable columns={cols} data={data} loading={loading} rowKey={(r) => r.id} />
      </ContentCard>
    </PageStack>
  );
}

export default PaymentsStripe;
