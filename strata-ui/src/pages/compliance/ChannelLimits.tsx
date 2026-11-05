// SPDX-License-Identifier: Apache-2.0
import { Box, DialogActions, Stack, Typography } from "@mui/material";
import { AddCircleOutlineOutlined, DeleteOutlineOutlined, EditOutlined } from "@mui/icons-material";
import {
  Button,
  Chip,
  ConfirmDialog,
  DataTable,
  Dialog,
  PageHeader,
  Select,
  Switch,
  TextField,
} from "@/components/ui/index";
import { PageStack } from "@/shared/pageKit/PageLayout";
import { useI18n } from "@/shared/i18n";
import {
  complianceApi,
  type ChannelCode,
  type ChannelDirection,
  type ChannelLimitDto,
  type ChannelLimitPayload,
} from "@/shared/api/compliance";
import { useListQuery } from "@/shared/hooks/useListQuery";
import { useResourceCrud } from "@/shared/hooks/useResourceCrud";

type ChannelLimitForm = {
  channel: ChannelCode;
  direction: ChannelDirection;
  minAmount: string;
  maxAmount: string;
  dailyLimit: string;
  feeRate: string;
  enabled: boolean;
};

const CHANNEL_TONE: Record<ChannelCode, "primary" | "secondary" | "default"> = {
  TRON: "primary",
  BSC: "secondary",
};

const DIRECTION_TONE: Record<ChannelDirection, "success" | "warning" | "default"> = {
  DEPOSIT: "success",
  WITHDRAWAL: "warning",
};

const KEY = ["api", "compliance", "channel-limits"];

/**
 * Channel limits: per-channel (TRON / BSC) direction limits and fee rates.
 */
export function ChannelLimits() {
  const { t } = useI18n();

  const limits = useListQuery<ChannelLimitDto>(KEY, () => complianceApi.channelLimitsList());

  const crud = useResourceCrud<
    ChannelLimitDto,
    ChannelLimitPayload,
    ChannelLimitPayload,
    ChannelLimitForm
  >({
    create: (payload) => complianceApi.channelLimitCreate(payload),
    update: (id, payload) => complianceApi.channelLimitUpdate(id, payload),
    remove: (id) => complianceApi.channelLimitDelete(id),
    reload: limits.reload,
    initForm: () => ({
      channel: "TRON",
      direction: "DEPOSIT",
      minAmount: "0",
      maxAmount: "0",
      dailyLimit: "0",
      feeRate: "0",
      enabled: true,
    }),
    toEditForm: (r) => ({
      channel: r.channel,
      direction: r.direction,
      minAmount: String(r.minAmount),
      maxAmount: String(r.maxAmount),
      dailyLimit: String(r.dailyLimit),
      feeRate: String(r.feeRate),
      enabled: r.enabled,
    }),
    toCreatePayload: (form) => ({
      channel: form.channel,
      direction: form.direction,
      minAmount: Number(form.minAmount) || 0,
      maxAmount: Number(form.maxAmount) || 0,
      dailyLimit: Number(form.dailyLimit) || 0,
      feeRate: Number(form.feeRate) || 0,
      enabled: form.enabled,
    }),
    toUpdatePayload: (form) => ({
      channel: form.channel,
      direction: form.direction,
      minAmount: Number(form.minAmount) || 0,
      maxAmount: Number(form.maxAmount) || 0,
      dailyLimit: Number(form.dailyLimit) || 0,
      feeRate: Number(form.feeRate) || 0,
      enabled: form.enabled,
    }),
  });

  const columns = [
    {
      key: "channel",
      title: t("compliance.channel.channel"),
      render: (r: ChannelLimitDto) => (
        <Chip
          label={t(`compliance.channel.channel.${r.channel}`)}
          color={CHANNEL_TONE[r.channel] ?? "default"}
        />
      ),
    },
    {
      key: "direction",
      title: t("compliance.channel.direction"),
      render: (r: ChannelLimitDto) => (
        <Chip
          label={t(`compliance.channel.direction.${r.direction}`)}
          color={DIRECTION_TONE[r.direction] ?? "default"}
        />
      ),
    },
    {
      key: "minAmount",
      title: t("compliance.channel.minAmount"),
      render: (r: ChannelLimitDto) => (
        <Typography variant="body2" sx={{ fontFamily: "monospace" }}>
          {r.minAmount}
        </Typography>
      ),
    },
    {
      key: "maxAmount",
      title: t("compliance.channel.maxAmount"),
      render: (r: ChannelLimitDto) => (
        <Typography variant="body2" sx={{ fontFamily: "monospace" }}>
          {r.maxAmount}
        </Typography>
      ),
    },
    {
      key: "dailyLimit",
      title: t("compliance.channel.dailyLimit"),
      render: (r: ChannelLimitDto) => (
        <Typography variant="body2" sx={{ fontFamily: "monospace" }}>
          {r.dailyLimit}
        </Typography>
      ),
    },
    {
      key: "feeRate",
      title: t("compliance.channel.feeRate"),
      render: (r: ChannelLimitDto) => (
        <Typography variant="body2" sx={{ fontFamily: "monospace" }}>
          {r.feeRate}
        </Typography>
      ),
    },
    {
      key: "enabled",
      title: t("common.status"),
      render: (r: ChannelLimitDto) => (
        <Switch
          checked={r.enabled}
          onChange={(checked) =>
            void crud.updateMutation.mutateAsync({
              id: r.id,
              payload: {
                channel: r.channel,
                direction: r.direction,
                minAmount: r.minAmount,
                maxAmount: r.maxAmount,
                dailyLimit: r.dailyLimit,
                feeRate: r.feeRate,
                enabled: checked,
              },
            })
          }
          disabled={crud.busy}
        />
      ),
    },
    {
      key: "actions",
      title: t("common.actions"),
      render: (r: ChannelLimitDto) => (
        <Stack direction="row" spacing={0.5}>
          <Button variant="text" size="sm" onClick={() => crud.openEdit(r)}>
            <EditOutlined fontSize="small" />
          </Button>
          <Button variant="text" size="sm" color="danger" onClick={() => crud.confirmDelete(r)}>
            <DeleteOutlineOutlined fontSize="small" />
          </Button>
        </Stack>
      ),
    },
  ];

  return (
    <PageStack>
      <PageHeader
        title={t("compliance.channel.title")}
        subtitle={t("compliance.channel.subtitle")}
      />

      <Stack direction="row" spacing={1.5} sx={{ alignItems: "center" }}>
        <Box component="div" sx={{ flex: 1 }} />
        <Button variant="contained" onClick={crud.openCreate}>
          <AddCircleOutlineOutlined fontSize="small" sx={{ mr: 0.5 }} />
          {t("compliance.channel.create")}
        </Button>
      </Stack>

      <DataTable
        columns={columns}
        data={limits.data}
        loading={limits.loading}
        rowKey={(r) => r.id}
        tableId="compliance.channel-limits"
        onRetry={limits.reload}
      />

      <Dialog
        open={crud.open}
        onClose={crud.closeDialog}
        title={crud.edit ? t("compliance.channel.edit") : t("compliance.channel.create")}
      >
        <Stack spacing={2} sx={{ py: 1 }}>
          <Select
            label={t("compliance.channel.channel")}
            value={crud.form.channel}
            onChange={(e) =>
              crud.setForm((f) => ({ ...f, channel: e.target.value as ChannelCode }))
            }
            options={[
              { value: "TRON", label: t("compliance.channel.channel.TRON") },
              { value: "BSC", label: t("compliance.channel.channel.BSC") },
            ]}
          />
          <Select
            label={t("compliance.channel.direction")}
            value={crud.form.direction}
            onChange={(e) =>
              crud.setForm((f) => ({ ...f, direction: e.target.value as ChannelDirection }))
            }
            options={[
              { value: "DEPOSIT", label: t("compliance.channel.direction.DEPOSIT") },
              { value: "WITHDRAWAL", label: t("compliance.channel.direction.WITHDRAWAL") },
            ]}
          />
          <TextField
            label={t("compliance.channel.minAmount")}
            type="number"
            value={crud.form.minAmount}
            onChange={(e) => crud.setForm((f) => ({ ...f, minAmount: e.target.value }))}
          />
          <TextField
            label={t("compliance.channel.maxAmount")}
            type="number"
            value={crud.form.maxAmount}
            onChange={(e) => crud.setForm((f) => ({ ...f, maxAmount: e.target.value }))}
          />
          <TextField
            label={t("compliance.channel.dailyLimit")}
            type="number"
            value={crud.form.dailyLimit}
            onChange={(e) => crud.setForm((f) => ({ ...f, dailyLimit: e.target.value }))}
          />
          <TextField
            label={t("compliance.channel.feeRate")}
            type="number"
            slotProps={{ htmlInput: { step: "0.0001" } }}
            value={crud.form.feeRate}
            onChange={(e) => crud.setForm((f) => ({ ...f, feeRate: e.target.value }))}
          />
          <Switch
            checked={crud.form.enabled}
            onChange={(checked) => crud.setForm((f) => ({ ...f, enabled: checked }))}
            label={t("common.enabled")}
          />
        </Stack>
        <DialogActions sx={{ px: 0, pt: 2 }}>
          <Button variant="outlined" color="default" onClick={crud.closeDialog}>
            {t("common.cancel")}
          </Button>
          <Button variant="contained" disabled={crud.busy} onClick={() => void crud.handleSave()}>
            {crud.busy ? t("common.saving") : t("common.save")}
          </Button>
        </DialogActions>
      </Dialog>

      <ConfirmDialog
        open={crud.deleting !== null}
        onClose={crud.closeDelete}
        onConfirm={() => void crud.runDelete()}
        title={t("common.delete")}
        description={t("common.deleteNamed").replace(
          "{name}",
          `${crud.deleting?.channel ?? ""} ${crud.deleting?.direction ?? ""}`,
        )}
        confirmColor="danger"
      />
    </PageStack>
  );
}

export default ChannelLimits;
