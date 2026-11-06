// SPDX-License-Identifier: Apache-2.0
import { useState } from "react";
import { Box, DialogActions, Stack, Typography } from "@mui/material";
import { AddCircleOutlineOutlined, EditOutlined } from "@mui/icons-material";
import { Button, DataTable, Dialog, PageHeader, TextField } from "@/components/ui/index";
import { PageStack } from "@/shared/pageKit/PageLayout";
import { useI18n } from "@/shared/i18n";
import {
  complianceApi,
  type ExchangeRateDto,
  type ExchangeRatePayload,
} from "@/shared/api/compliance";
import { useListQuery } from "@/shared/hooks/useListQuery";
import { useMutationWithToast } from "@/shared/hooks/useMutationWithToast";
import { formatDateTime } from "@/shared/utils/date";

type RateForm = {
  fromCurrency: string;
  toCurrency: string;
  rate: string;
};

const KEY = ["api", "compliance", "exchange-rates"];

/**
 * Exchange rate management: list, create, and edit rates.
 * No delete endpoint on the backend — rates are upserted / edited in place.
 */
export function ExchangeRates() {
  const { t } = useI18n();

  const rates = useListQuery<ExchangeRateDto>(KEY, () => complianceApi.exchangeRatesList());

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<ExchangeRateDto | null>(null);
  const [form, setForm] = useState<RateForm>({ fromCurrency: "", toCurrency: "", rate: "1" });

  const create = useMutationWithToast(
    (payload: ExchangeRatePayload) => complianceApi.exchangeRateCreate(payload),
    { success: "toast.saved", failure: "toast.saveFailed" },
    { invalidateKeys: [KEY] },
  );

  const update = useMutationWithToast(
    ({ id, payload }: { id: string; payload: ExchangeRatePayload }) =>
      complianceApi.exchangeRateUpdate(id, payload),
    { success: "toast.saved", failure: "toast.saveFailed" },
    { invalidateKeys: [KEY] },
  );

  function openCreate() {
    setEditing(null);
    setForm({ fromCurrency: "", toCurrency: "", rate: "1" });
    setDialogOpen(true);
  }

  function openEdit(row: ExchangeRateDto) {
    setEditing(row);
    setForm({
      fromCurrency: row.fromCurrency,
      toCurrency: row.toCurrency,
      rate: String(row.rate),
    });
    setDialogOpen(true);
  }

  async function handleSave() {
    const payload: ExchangeRatePayload = {
      fromCurrency: form.fromCurrency,
      toCurrency: form.toCurrency,
      rate: Number(form.rate) || 0,
    };
    try {
      if (editing) {
        await update.mutateAsync({ id: editing.id, payload });
      } else {
        await create.mutateAsync(payload);
      }
      setDialogOpen(false);
    } catch {
      // toast already shown
    }
  }

  const busy = create.isPending || update.isPending;

  const columns = [
    {
      key: "fromCurrency",
      title: t("compliance.rate.fromCurrency"),
      render: (r: ExchangeRateDto) => (
        <Typography variant="body2" sx={{ fontFamily: "monospace", fontWeight: 600 }}>
          {r.fromCurrency}
        </Typography>
      ),
    },
    {
      key: "toCurrency",
      title: t("compliance.rate.toCurrency"),
      render: (r: ExchangeRateDto) => (
        <Typography variant="body2" sx={{ fontFamily: "monospace", fontWeight: 600 }}>
          {r.toCurrency}
        </Typography>
      ),
    },
    {
      key: "rate",
      title: t("compliance.rate.rate"),
      render: (r: ExchangeRateDto) => (
        <Typography variant="body2" sx={{ fontFamily: "monospace" }}>
          {r.rate}
        </Typography>
      ),
    },
    {
      key: "updatedAt",
      title: t("compliance.rate.updatedAt"),
      render: (r: ExchangeRateDto) => (
        <Typography variant="caption" color="text.secondary" sx={{ fontFamily: "monospace" }}>
          {formatDateTime(r.updatedAt)}
        </Typography>
      ),
    },
    {
      key: "actions",
      title: t("common.actions"),
      render: (r: ExchangeRateDto) => (
        <Button variant="text" size="sm" onClick={() => openEdit(r)}>
          <EditOutlined fontSize="small" />
        </Button>
      ),
    },
  ];

  return (
    <PageStack>
      <PageHeader title={t("compliance.rate.title")} subtitle={t("compliance.rate.subtitle")} />

      <Stack direction="row" spacing={1.5} sx={{ alignItems: "center" }}>
        <Box component="div" sx={{ flex: 1 }} />
        <Button variant="contained" onClick={openCreate}>
          <AddCircleOutlineOutlined fontSize="small" sx={{ mr: 0.5 }} />
          {t("compliance.rate.create")}
        </Button>
      </Stack>

      <DataTable
        columns={columns}
        data={rates.data}
        loading={rates.loading}
        rowKey={(r) => r.id}
        tableId="compliance.exchange-rates"
        onRetry={rates.reload}
      />

      <Dialog
        open={dialogOpen}
        onClose={() => setDialogOpen(false)}
        title={editing ? t("compliance.rate.edit") : t("compliance.rate.create")}
      >
        <Stack spacing={2} sx={{ py: 1 }}>
          <TextField
            label={t("compliance.rate.fromCurrency")}
            placeholder="USD"
            value={form.fromCurrency}
            onChange={(e) => setForm((f) => ({ ...f, fromCurrency: e.target.value.toUpperCase() }))}
            disabled={!!editing}
          />
          <TextField
            label={t("compliance.rate.toCurrency")}
            placeholder="CNY"
            value={form.toCurrency}
            onChange={(e) => setForm((f) => ({ ...f, toCurrency: e.target.value.toUpperCase() }))}
            disabled={!!editing}
          />
          <TextField
            label={t("compliance.rate.rate")}
            type="number"
            slotProps={{ htmlInput: { step: "0.0001" } }}
            value={form.rate}
            onChange={(e) => setForm((f) => ({ ...f, rate: e.target.value }))}
          />
        </Stack>
        <DialogActions sx={{ px: 0, pt: 2 }}>
          <Button variant="outlined" color="default" onClick={() => setDialogOpen(false)}>
            {t("common.cancel")}
          </Button>
          <Button
            variant="contained"
            disabled={busy || !form.fromCurrency || !form.toCurrency || !form.rate}
            onClick={() => void handleSave()}
          >
            {busy ? t("common.saving") : t("common.save")}
          </Button>
        </DialogActions>
      </Dialog>
    </PageStack>
  );
}

export default ExchangeRates;
