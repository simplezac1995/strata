// SPDX-License-Identifier: Apache-2.0
import { useMemo, useState } from "react";
import { Avatar, Box, Chip, IconButton, Stack, Tooltip, Typography } from "@mui/material";
import AddIcon from "@mui/icons-material/Add";
import DeleteOutline from "@mui/icons-material/DeleteOutlined";
import RefreshIcon from "@mui/icons-material/Refresh";
import StorageIcon from "@mui/icons-material/Storage";
import CheckCircleIcon from "@mui/icons-material/CheckCircle";
import ErrorIcon from "@mui/icons-material/Error";
import {
  Button,
  TextField,
  Select,
  Dialog,
  ConfirmDialog,
  DataTable,
  Pagination,
} from "@/components/ui/index";
import { AiPageHeader } from "@/components/ui/index";
import { useI18n } from "@/shared/i18n";
import { useAuthStore } from "@/shared/auth/store";
import { vectorStoreApi, type AiVectorStorePayload } from "@/shared/api/ai";
import { usePagedQuery } from "@/shared/hooks/usePagedQuery";
import { useResourceCrud } from "@/shared/hooks/useResourceCrud";
import type { AiVectorStoreDto } from "@/shared/types";
import { PageStack, ContentCard } from "@/shared/pageKit/PageLayout";

const VECTOR_STORE_TYPES = [
  { value: "pinecone", label: "Pinecone" },
  { value: "pgvector", label: "pgvector" },
  { value: "milvus", label: "Milvus" },
  { value: "chroma", label: "Chroma" },
  { value: "qdrant", label: "Qdrant" },
];

const TYPE_COLORS: Record<string, string> = {
  pinecone: "success",
  pgvector: "primary",
  milvus: "warning",
  chroma: "info",
  qdrant: "secondary",
};

interface VectorStoreForm {
  name: string;
  type: string;
  host: string;
  dimensions: number;
  enabled: boolean;
}

export function VectorStores() {
  const { t } = useI18n();
  const canCreate = useAuthStore((s) => s.hasAuthority("ai:vectorstore:create"));
  const canUpdate = useAuthStore((s) => s.hasAuthority("ai:vectorstore:update"));
  const canDelete = useAuthStore((s) => s.hasAuthority("ai:vectorstore:delete"));

  const [nameKw, setNameKw] = useState("");
  const [typeFilter, setTypeFilter] = useState("");

  const { rows, total, page, setPage, pageSize, setPageSize, loading, reload } =
    usePagedQuery<AiVectorStoreDto>(["api", "ai", "vector-stores"], vectorStoreApi.page, {
      initialPageSize: 6,
    });

  const {
    busy,
    open,
    edit,
    deleting,
    form,
    setForm,
    openCreate,
    openEdit,
    closeDialog,
    handleSave,
    confirmDelete,
    closeDelete,
    runDelete,
  } = useResourceCrud<
    AiVectorStoreDto,
    AiVectorStorePayload,
    AiVectorStorePayload,
    VectorStoreForm
  >({
    create: (payload) => vectorStoreApi.create(payload),
    update: (id, payload) => vectorStoreApi.update(id, payload),
    remove: (id) => vectorStoreApi.remove(id),
    reload,
    initForm: () => ({ name: "", type: "milvus", host: "", dimensions: 1536, enabled: true }),
    toEditForm: (v) => ({
      name: v.name,
      type: v.type ?? "milvus",
      host: v.host ?? "",
      dimensions: v.dimensions ?? 1536,
      enabled: v.enabled,
    }),
    toCreatePayload: (f) => f as unknown as AiVectorStorePayload,
    toUpdatePayload: (f) => f as unknown as AiVectorStorePayload,
  });

  const filtered = useMemo(() => {
    const kw = nameKw.trim().toLowerCase();
    return rows.filter((v) => {
      if (kw && !v.name.toLowerCase().includes(kw)) return false;
      if (typeFilter && v.type !== typeFilter) return false;
      return true;
    });
  }, [rows, nameKw, typeFilter]);

  const cols = [
    {
      key: "name",
      title: "名称",
      render: (v: AiVectorStoreDto) => (
        <Box sx={{ display: "flex", gap: 1.5, alignItems: "center" }}>
          <Avatar
            sx={{
              width: 32,
              height: 32,
              bgcolor: "primary.light",
              fontSize: 14,
            }}
          >
            <StorageIcon sx={{ fontSize: 18 }} />
          </Avatar>
          <Box>
            <Typography variant="body2" sx={{ fontWeight: 600 }}>
              {v.name}
            </Typography>
            <Typography variant="caption" sx={{ color: "text.secondary" }}>
              ID: {v.id}
            </Typography>
          </Box>
        </Box>
      ),
    },
    {
      key: "type",
      title: "类型",
      render: (v: AiVectorStoreDto) => (
        <Chip
          label={v.type}
          size="small"
          color={(TYPE_COLORS[v.type ?? ""] as any) ?? "default"}
          variant="outlined"
          sx={{ fontFamily: "monospace", fontSize: 12 }}
        />
      ),
    },
    {
      key: "host",
      title: "连接地址",
      render: (v: AiVectorStoreDto) => (
        <Typography variant="caption" sx={{ color: "text.secondary", fontFamily: "monospace" }}>
          {v.host ?? "—"}
        </Typography>
      ),
    },
    {
      key: "dimensions",
      title: "向量维度",
      render: (v: AiVectorStoreDto) => (
        <Chip
          label={v.dimensions ?? 1536}
          size="small"
          variant="outlined"
          sx={{ fontFamily: "monospace" }}
        />
      ),
    },
    {
      key: "enabled",
      title: "状态",
      render: (v: AiVectorStoreDto) => (
        <Box sx={{ display: "flex", gap: 0.5, alignItems: "center" }}>
          {v.enabled ? (
            <CheckCircleIcon sx={{ fontSize: 16, color: "success.main" }} />
          ) : (
            <ErrorIcon sx={{ fontSize: 16, color: "text.disabled" }} />
          )}
          <Typography
            variant="caption"
            sx={{ color: v.enabled ? "success.main" : "text.disabled" }}
          >
            {v.enabled ? "已启用" : "已停用"}
          </Typography>
        </Box>
      ),
    },
    {
      key: "actions",
      title: "操作",
      render: (v: AiVectorStoreDto) => (
        <Stack direction="row" spacing={0.5}>
          {canUpdate && (
            <Button variant="text" size="sm" onClick={() => openEdit(v)}>
              编辑
            </Button>
          )}
          {canDelete && (
            <Button variant="text" size="sm" color="danger" onClick={() => confirmDelete(v)}>
              删除
            </Button>
          )}
        </Stack>
      ),
    },
  ];

  return (
    <PageStack>
      <AiPageHeader title="向量配置" subtitle="管理知识库检索所用的向量库连接。" />

      <ContentCard sx={{ p: 2 }}>
        <Box
          sx={{
            display: "flex",
            gap: 1.5,
            flexWrap: "wrap",
            alignItems: "center",
            "& .MuiTextField-root, & .MuiFormControl-root": { width: "auto", minWidth: 160 },
          }}
        >
          <TextField
            label="名称"
            placeholder="请输入名称"
            value={nameKw}
            onChange={(e) => setNameKw(e.target.value)}
            sx={{ width: 220 }}
          />
          <Select
            label="类型"
            value={typeFilter}
            onChange={(e) => setTypeFilter(e.target.value)}
            options={[{ value: "", label: "全部类型" }, ...VECTOR_STORE_TYPES]}
            sx={{ width: 180 }}
          />
          <Button variant="contained" onClick={reload}>
            查询
          </Button>
          <Button
            variant="outlined"
            color="default"
            onClick={() => {
              setNameKw("");
              setTypeFilter("");
            }}
          >
            重置
          </Button>
        </Box>
      </ContentCard>

      <Stack direction="row" sx={{ alignItems: "center", justifyContent: "space-between" }}>
        <Stack direction="row" spacing={1}>
          {canCreate && (
            <Button variant="contained" onClick={openCreate}>
              <AddIcon sx={{ fontSize: 18, mr: 0.5 }} />
              新增
            </Button>
          )}
          <Button variant="outlined" color="default" disabled title="批量删除（后端未开放）">
            <DeleteOutline sx={{ fontSize: 18, mr: 0.5 }} />
            删除
          </Button>
        </Stack>
        <Tooltip title="刷新">
          <IconButton size="small" onClick={reload} aria-label="刷新">
            <RefreshIcon fontSize="small" />
          </IconButton>
        </Tooltip>
      </Stack>

      <DataTable columns={cols} data={filtered} loading={loading} rowKey={(v) => v.id} selectable />
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
        open={open}
        onClose={closeDialog}
        title={edit ? t("ai.vectorStore.edit") : t("ai.vectorStore.add")}
      >
        <Stack spacing={2} sx={{ py: 1 }}>
          <Select
            label="类型"
            value={form.type}
            onChange={(e) => setForm((f) => ({ ...f, type: e.target.value }))}
            options={VECTOR_STORE_TYPES}
          />
          <TextField
            label="名称"
            value={form.name}
            onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
            placeholder="milvus"
          />
          <TextField
            label="URI"
            value={form.host}
            onChange={(e) => setForm((f) => ({ ...f, host: e.target.value }))}
            placeholder="http://127.0.0.1:19530"
          />
          <TextField
            label="维度"
            type="number"
            value={String(form.dimensions)}
            onChange={(e) => setForm((f) => ({ ...f, dimensions: Number(e.target.value) }))}
          />
          <Select
            label="状态"
            value={form.enabled ? "true" : "false"}
            onChange={(e) => setForm((f) => ({ ...f, enabled: e.target.value === "true" }))}
            options={[
              { value: "true", label: t("common.enabled") },
              { value: "false", label: t("common.disabled") },
            ]}
          />
        </Stack>
        <Box sx={{ display: "flex", gap: 1, justifyContent: "flex-end", mt: 2 }}>
          <Button variant="outlined" color="default" onClick={closeDialog}>
            {t("common.cancel")}
          </Button>
          <Button variant="contained" disabled={busy} onClick={() => void handleSave()}>
            {busy ? t("common.saving") : t("common.save")}
          </Button>
        </Box>
      </Dialog>

      <ConfirmDialog
        open={deleting !== null}
        onClose={closeDelete}
        onConfirm={() => void runDelete()}
        title={t("common.delete")}
        description={t("common.deleteNamed").replace("{name}", deleting?.name ?? "")}
        confirmColor="danger"
      />
    </PageStack>
  );
}

export default VectorStores;
