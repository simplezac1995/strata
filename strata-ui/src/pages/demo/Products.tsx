// SPDX-License-Identifier: Apache-2.0
import { useState } from "react";
import { Box, DialogActions, Grid, Stack, Typography } from "@mui/material";
import {
  Button,
  TextField,
  Select,
  Dialog,
  ConfirmDialog,
  PageHeader,
  QueryForm,
  QueryField,
  DataTable,
  Pagination,
  StatusChip,
} from "@/components/ui/index";
import { PageStack } from "@/shared/pageKit/PageLayout";
import { useI18n } from "@/shared/i18n";
import { useAuthStore } from "@/shared/auth/store";
import { productApi, type ProductPayload } from "@/shared/api/demo";
import { usePagedQuery } from "@/shared/hooks/usePagedQuery";
import { useMutationWithToast } from "@/shared/hooks/useMutationWithToast";
import type { ProductDto } from "@/shared/types";

export function Products() {
  const { t } = useI18n();
  const canCreate = useAuthStore((s) => s.hasAuthority("demo:product:create"));
  const canUpdate = useAuthStore((s) => s.hasAuthority("demo:product:update"));
  const canDelete = useAuthStore((s) => s.hasAuthority("demo:product:delete"));

  const [filterName, setFilterName] = useState("");
  const [filterSku, setFilterSku] = useState("");
  const [filterEnabled, setFilterEnabled] = useState("all");

  const {
    rows: products,
    total,
    page,
    setPage,
    pageSize,
    setPageSize,
    loading,
    reload,
  } = usePagedQuery<ProductDto>(
    ["api", "demo", "products", { name: filterName, sku: filterSku, enabled: filterEnabled }],
    (pageIndex, size) =>
      productApi.page(pageIndex, size, {
        name: filterName || undefined,
        sku: filterSku || undefined,
        enabled: filterEnabled === "all" ? undefined : filterEnabled === "yes",
      }),
  );

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editProduct, setEditProduct] = useState<ProductDto | null>(null);
  const [deleteProduct, setDeleteProduct] = useState<ProductDto | null>(null);
  const [form, setForm] = useState({
    name: "",
    sku: "",
    price: 0,
    stock: 0,
    description: "",
    enabled: true,
  });

  const createMutation = useMutationWithToast(
    (payload: ProductPayload) => productApi.create(payload),
    { success: "toast.saved", failure: "toast.saveFailed" },
    { onSuccess: () => reload() },
  );

  const updateMutation = useMutationWithToast(
    ({ id, payload }: { id: string; payload: ProductPayload }) => productApi.update(id, payload),
    { success: "toast.saved", failure: "toast.saveFailed" },
    { onSuccess: () => reload() },
  );

  const deleteMutation = useMutationWithToast(
    (id: string) => productApi.remove(id),
    { success: "toast.deleted", failure: "toast.deleteFailed" },
    { onSuccess: () => reload() },
  );

  const busy = createMutation.isPending || updateMutation.isPending || deleteMutation.isPending;

  function openCreate() {
    setEditProduct(null);
    setForm({ name: "", sku: "", price: 0, stock: 0, description: "", enabled: true });
    setDialogOpen(true);
  }

  function openEdit(p: ProductDto) {
    setEditProduct(p);
    setForm({
      name: p.name,
      sku: p.sku,
      price: p.price,
      stock: p.stock,
      description: p.description ?? "",
      enabled: p.enabled,
    });
    setDialogOpen(true);
  }

  async function handleSave() {
    const payload: ProductPayload = {
      name: form.name,
      sku: form.sku,
      price: form.price,
      stock: form.stock,
      description: form.description || null,
      enabled: form.enabled,
    };
    try {
      if (editProduct) {
        await updateMutation.mutateAsync({ id: editProduct.id, payload });
      } else {
        await createMutation.mutateAsync(payload);
      }
      setDialogOpen(false);
    } catch {
      // toast already shown by useMutationWithToast
    }
  }

  async function handleDelete() {
    if (!deleteProduct) return;
    try {
      await deleteMutation.mutateAsync(deleteProduct.id);
      setDeleteProduct(null);
    } catch {
      // toast already shown by useMutationWithToast
    }
  }

  const columns = [
    {
      key: "name",
      title: t("demo.products.field.name"),
      render: (p: ProductDto) => (
        <Box>
          <Typography variant="body2" sx={{ fontWeight: 500 }}>
            {p.name}
          </Typography>
          {p.description ? (
            <Typography
              variant="caption"
              color="text.secondary"
              sx={{ display: "block", mt: 0.25 }}
            >
              {p.description}
            </Typography>
          ) : null}
        </Box>
      ),
    },
    {
      key: "sku",
      title: t("demo.products.field.sku"),
      render: (p: ProductDto) => (
        <Box
          component="code"
          sx={{ fontSize: 12, fontFamily: "monospace", color: "text.secondary" }}
        >
          {p.sku}
        </Box>
      ),
    },
    {
      key: "price",
      title: t("demo.products.field.price"),
      render: (p: ProductDto) => (
        <Typography variant="body2" sx={{ fontWeight: 600 }}>
          ${p.price.toFixed(2)}
        </Typography>
      ),
    },
    {
      key: "stock",
      title: t("demo.products.field.stock"),
      render: (p: ProductDto) => (
        <Typography
          variant="body2"
          sx={{
            fontFamily: "monospace",
            color: p.stock === 0 ? "error.main" : p.stock < 100 ? "warning.main" : "text.secondary",
          }}
        >
          {p.stock === 0 ? t("demo.products.outOfStock") : p.stock}
        </Typography>
      ),
    },
    {
      key: "enabled",
      title: t("common.status"),
      render: (p: ProductDto) => <StatusChip enabled={p.enabled} />,
    },
    {
      key: "actions",
      title: t("common.actions"),
      render: (p: ProductDto) => (
        <Stack direction="row" spacing={0.5}>
          {canUpdate && (
            <Button variant="text" size="sm" onClick={() => openEdit(p)}>
              {t("common.edit")}
            </Button>
          )}
          {canDelete && (
            <Button variant="text" size="sm" color="danger" onClick={() => setDeleteProduct(p)}>
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
        title={t("nav.products")}
        subtitle={t("demo.products.subtitle")}
        actions={
          canCreate ? (
            <Button variant="contained" onClick={openCreate}>
              {t("demo.products.createButton")}
            </Button>
          ) : undefined
        }
      />

      <QueryForm
        onSearch={() => setPage(1)}
        onReset={() => {
          setFilterName("");
          setFilterSku("");
          setFilterEnabled("all");
          setPage(1);
        }}
      >
        <TextField
          label={t("demo.products.field.name")}
          placeholder={t("demo.products.placeholder.nameFilter")}
          value={filterName}
          onChange={(e) => setFilterName(e.target.value)}
        />
        <TextField
          label={t("demo.products.field.sku")}
          placeholder="WKB-PRO-001"
          value={filterSku}
          onChange={(e) => setFilterSku(e.target.value)}
        />
        <QueryField width={144}>
          <Select
            label={t("common.status")}
            value={filterEnabled}
            onChange={(e) => setFilterEnabled(e.target.value)}
            options={[
              { value: "all", label: t("common.all") },
              { value: "yes", label: t("common.enabled") },
              { value: "no", label: t("common.disabled") },
            ]}
          />
        </QueryField>
      </QueryForm>

      <DataTable columns={columns} data={products} loading={loading} rowKey={(p) => p.id} />
      <Pagination
        total={total}
        page={page}
        pageSize={pageSize}
        onPage={setPage}
        onPageSize={setPageSize}
      />

      <Dialog
        open={dialogOpen}
        onClose={() => setDialogOpen(false)}
        title={t(editProduct ? "demo.products.editTitle" : "demo.products.createTitle")}
      >
        <Stack spacing={2} sx={{ py: 1 }}>
          <TextField
            label={t("common.nameRequired")}
            placeholder={t("demo.products.placeholder.name")}
            value={form.name}
            onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
          />
          <TextField
            label={t("demo.products.field.skuRequired")}
            placeholder="WKB-PRO-001"
            value={form.sku}
            onChange={(e) => setForm((f) => ({ ...f, sku: e.target.value }))}
          />
          <Grid container spacing={2}>
            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField
                label={t("demo.products.field.price")}
                type="number"
                value={String(form.price)}
                onChange={(e) => setForm((f) => ({ ...f, price: Number(e.target.value) }))}
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField
                label={t("demo.products.field.stock")}
                type="number"
                value={String(form.stock)}
                onChange={(e) => setForm((f) => ({ ...f, stock: Number(e.target.value) }))}
              />
            </Grid>
          </Grid>
          <TextField
            label={t("common.description")}
            placeholder={t("common.optional")}
            value={form.description}
            onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
          />
          <Select
            label={t("common.status")}
            value={form.enabled ? "true" : "false"}
            onChange={(e) => setForm((f) => ({ ...f, enabled: e.target.value === "true" }))}
            options={[
              { value: "true", label: t("common.enabled") },
              { value: "false", label: t("common.disabled") },
            ]}
          />
        </Stack>
        <DialogActions sx={{ px: 0, pt: 2 }}>
          <Button variant="outlined" color="default" onClick={() => setDialogOpen(false)}>
            {t("common.cancel")}
          </Button>
          <Button variant="contained" disabled={busy} onClick={() => void handleSave()}>
            {busy ? t("common.saving") : t("common.save")}
          </Button>
        </DialogActions>
      </Dialog>

      <ConfirmDialog
        open={!!deleteProduct}
        onClose={() => setDeleteProduct(null)}
        onConfirm={() => void handleDelete()}
        title={t("demo.products.deleteTitle")}
        description={t("demo.products.delete.withAsterisk").replace(
          "{name}",
          deleteProduct?.name ?? "",
        )}
        confirmColor="danger"
      />
    </PageStack>
  );
}

export default Products;
