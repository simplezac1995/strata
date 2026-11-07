// SPDX-License-Identifier: Apache-2.0
import { DialogActions, Stack, Typography } from "@mui/material";
import {
  Button,
  TextField,
  Select,
  Dialog,
  ConfirmDialog,
  ExportCsvButton,
  PageHeader,
  DataTable,
  Badge,
  Pagination,
} from "@/components/ui/index";
import { PageStack } from "@/shared/pageKit/PageLayout";
import { useI18n } from "@/shared/i18n";
import { useAuthStore } from "@/shared/auth/store";
import { tenantApi, type TenantCreatePayload, type TenantUpdatePayload } from "@/shared/api/system";
import { usePagedQuery } from "@/shared/hooks/usePagedQuery";
import { useResourceCrud } from "@/shared/hooks/useResourceCrud";
import { useMutationWithToast } from "@/shared/hooks/useMutationWithToast";
import { exportCsvFromColumns, type CsvColumn } from "@/shared/utils/csv";
import type { TenantDto } from "@/shared/types";
import { InfoBanner } from "@/shared/pageKit/InfoBanner";

function planLabel(plan: string): string {
  return plan === "free"
    ? "Free"
    : plan === "pro"
      ? "Pro"
      : plan === "enterprise"
        ? "Enterprise"
        : plan;
}

function planVariant(plan: string): "default" | "pro" | "info" {
  return plan === "pro" ? "pro" : plan === "enterprise" ? "info" : "default";
}

export function Tenants() {
  const { t } = useI18n();
  const canCreate = useAuthStore((s) => s.hasAuthority("system:tenant:create"));
  const canUpdate = useAuthStore((s) => s.hasAuthority("system:tenant:update"));
  const canDelete = useAuthStore((s) => s.hasAuthority("system:tenant:delete"));

  const {
    rows: tenants,
    total,
    page,
    setPage,
    pageSize,
    setPageSize,
    loading,
    error,
    reload,
  } = usePagedQuery<TenantDto>(["api", "system", "tenants"], tenantApi.page);

  const toggleStatusMutation = useMutationWithToast(
    ({ id, status }: { id: string; status: string }) =>
      tenantApi.update(id, { status } as TenantUpdatePayload),
    { success: "toast.saved", failure: "toast.saveFailed" },
    { onSuccess: () => reload() },
  );

  const crud = useResourceCrud<
    TenantDto,
    TenantCreatePayload,
    TenantUpdatePayload,
    { code: string; name: string; domain: string; status: string; plan: string }
  >({
    create: (payload) => tenantApi.create(payload),
    update: (id, payload) => tenantApi.update(id, payload),
    remove: (id) => tenantApi.remove(id),
    reload,
    initForm: () => ({ code: "", name: "", domain: "", status: "active", plan: "free" }),
    toEditForm: (r) => ({
      code: r.code,
      name: r.name,
      domain: r.domain ?? "",
      status: r.status,
      plan: r.plan,
    }),
  });

  const busy = crud.busy || toggleStatusMutation.isPending;

  async function handleToggleStatus(r: TenantDto) {
    const status = r.status === "active" ? "suspended" : "active";
    try {
      await toggleStatusMutation.mutateAsync({ id: r.id, status });
    } catch {
      // toast already shown
    }
  }

  const csvColumns: CsvColumn<TenantDto>[] = [
    { title: t("system.tenants.code"), cell: (tn) => tn.code },
    { title: t("system.tenants.name"), cell: (tn) => tn.name },
    { title: t("system.tenants.domain"), cell: (tn) => tn.domain ?? "" },
    { title: t("system.tenants.plan"), cell: (tn) => tn.plan },
    { title: t("common.status"), cell: (tn) => tn.status },
  ];

  const cols = [
    { key: "code", title: t("system.tenants.code") },
    { key: "name", title: t("system.tenants.name") },
    {
      key: "domain",
      title: t("system.tenants.domain"),
      render: (r: TenantDto) => (
        <Typography variant="caption" sx={{ fontFamily: "monospace" }} color="text.secondary">
          {r.domain ?? "—"}
        </Typography>
      ),
    },
    {
      key: "plan",
      title: t("system.tenants.plan"),
      render: (r: TenantDto) => <Badge variant={planVariant(r.plan)}>{planLabel(r.plan)}</Badge>,
    },
    {
      key: "status",
      title: t("common.status"),
      render: (r: TenantDto) => (
        <Badge variant={r.status === "active" ? "success" : "default"}>
          {r.status === "active" ? t("common.enabled") : t("common.disabled")}
        </Badge>
      ),
    },
    {
      key: "actions",
      title: t("common.actions"),
      render: (r: TenantDto) => (
        <Stack direction="row" spacing={0.5}>
          {canUpdate && (
            <Button variant="text" size="sm" onClick={() => void handleToggleStatus(r)}>
              {r.status === "active" ? t("system.tenants.suspend") : t("system.tenants.activate")}
            </Button>
          )}
          {canUpdate && (
            <Button variant="text" size="sm" onClick={() => crud.openEdit(r)}>
              {t("common.edit")}
            </Button>
          )}
          {canDelete && (
            <Button variant="text" size="sm" color="danger" onClick={() => crud.confirmDelete(r)}>
              {t("common.delete")}
            </Button>
          )}
        </Stack>
      ),
    },
  ];

  return (
    <PageStack>
      <PageHeader
        title={t("nav.tenants")}
        subtitle={t("system.tenants.subtitle")}
        actions={
          <Stack direction="row" spacing={1}>
            <ExportCsvButton
              onClick={() => exportCsvFromColumns("tenants.csv", csvColumns, tenants)}
            />
            {canCreate ? (
              <Button variant="contained" onClick={crud.openCreate}>
                {t("system.tenants.create")}
              </Button>
            ) : null}
          </Stack>
        }
      />
      <InfoBanner>{t("system.tenants.multiTenantNote")}</InfoBanner>
      <DataTable
        columns={cols}
        data={tenants}
        loading={loading}
        rowKey={(r) => r.id}
        tableId="tenants"
        error={error}
        onRetry={reload}
        emptyAction={
          canCreate ? (
            <Button variant="contained" onClick={crud.openCreate}>
              {t("system.tenants.create")}
            </Button>
          ) : undefined
        }
      />
      <Pagination
        page={page}
        pageSize={pageSize}
        total={total}
        onPage={setPage}
        onPageSize={(size: number) => {
          setPageSize(size);
          setPage(1);
        }}
      />

      <Dialog
        open={crud.open}
        onClose={crud.closeDialog}
        title={crud.edit ? t("system.tenants.edit") : t("system.tenants.create")}
      >
        <Stack spacing={2} sx={{ py: 1 }}>
          <TextField
            label={t("system.tenants.code")}
            value={crud.form.code}
            onChange={(e) => crud.setForm((f) => ({ ...f, code: e.target.value }))}
          />
          <TextField
            label={t("system.tenants.name")}
            value={crud.form.name}
            onChange={(e) => crud.setForm((f) => ({ ...f, name: e.target.value }))}
          />
          <TextField
            label={t("system.tenants.domain")}
            value={crud.form.domain}
            onChange={(e) => crud.setForm((f) => ({ ...f, domain: e.target.value }))}
            placeholder="tenant.example.com"
          />
          <Select
            label={t("system.tenants.plan")}
            value={crud.form.plan}
            onChange={(e) => crud.setForm((f) => ({ ...f, plan: e.target.value }))}
            options={[
              { value: "free", label: "Free" },
              { value: "pro", label: "Pro" },
              { value: "enterprise", label: "Enterprise" },
            ]}
          />
          <Select
            label={t("common.status")}
            value={crud.form.status}
            onChange={(e) => crud.setForm((f) => ({ ...f, status: e.target.value }))}
            options={[
              { value: "active", label: t("common.enabled") },
              { value: "suspended", label: t("common.disabled") },
            ]}
          />
        </Stack>
        <DialogActions sx={{ px: 0, pt: 2 }}>
          <Button variant="outlined" color="default" onClick={crud.closeDialog}>
            {t("common.cancel")}
          </Button>
          <Button variant="contained" disabled={busy} onClick={() => void crud.handleSave()}>
            {busy ? t("common.saving") : t("common.save")}
          </Button>
        </DialogActions>
      </Dialog>

      <ConfirmDialog
        open={crud.deleting !== null}
        onClose={crud.closeDelete}
        onConfirm={() => void crud.runDelete()}
        title={t("common.delete")}
        description={t("common.deleteNamed").replace("{name}", crud.deleting?.name ?? "")}
        confirmColor="danger"
      />
    </PageStack>
  );
}

export default Tenants;
