// SPDX-License-Identifier: Apache-2.0
import { useMemo, useState } from "react";
import { Box, Chip, IconButton, Stack, Typography } from "@mui/material";
import AddIcon from "@mui/icons-material/Add";
import DeleteOutline from "@mui/icons-material/DeleteOutlined";
import RefreshIcon from "@mui/icons-material/Refresh";
import {
  Button,
  TextField,
  TextArea,
  Select,
  Dialog,
  ConfirmDialog,
  DataTable,
  StatusChip,
  Pagination,
} from "@/components/ui/index";
import { PageStack, ContentCard } from "@/shared/pageKit/PageLayout";
import { AiPageHeader } from "@/components/ui/index";
import { useI18n } from "@/shared/i18n";
import { useAuthStore } from "@/shared/auth/store";
import { promptApi, type AiPromptPayload } from "@/shared/api/ai";
import { usePagedQuery } from "@/shared/hooks/usePagedQuery";
import { useResourceCrud } from "@/shared/hooks/useResourceCrud";
import type { AiPromptDto } from "@/shared/types";

const TEMPLATE_TYPES = [
  { value: "StringTemplate", label: "StringTemplate" },
  { value: "FreeMarker", label: "FreeMarker" },
];

interface PromptForm {
  code: string;
  name: string;
  templateType: string;
  template: string;
  enabled: boolean;
}

export function Prompts() {
  const { t } = useI18n();
  const canCreate = useAuthStore((s) => s.hasAuthority("ai:prompt:create"));
  const canUpdate = useAuthStore((s) => s.hasAuthority("ai:prompt:update"));
  const canDelete = useAuthStore((s) => s.hasAuthority("ai:prompt:delete"));

  const [codeKw, setCodeKw] = useState("");
  const [nameKw, setNameKw] = useState("");

  const { rows, total, page, setPage, pageSize, setPageSize, loading, reload } =
    usePagedQuery<AiPromptDto>(["api", "ai", "prompts"], promptApi.page, {
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
  } = useResourceCrud<AiPromptDto, AiPromptPayload, AiPromptPayload, PromptForm>({
    create: (payload) => promptApi.create(payload),
    update: (id, payload) => promptApi.update(id, payload),
    remove: (id) => promptApi.remove(id),
    reload,
    initForm: () => ({
      code: "",
      name: "",
      templateType: "StringTemplate",
      template: "",
      enabled: true,
    }),
    toEditForm: (p) => ({
      code: p.code,
      name: p.name,
      templateType: "StringTemplate",
      template: p.template,
      enabled: p.enabled,
    }),
    // Backend payload only carries code/name/template/enabled; templateType is a visual-only control.
    toCreatePayload: (f) => ({
      code: f.code,
      name: f.name,
      template: f.template,
      enabled: f.enabled,
    }),
    toUpdatePayload: (f) => ({
      code: f.code,
      name: f.name,
      template: f.template,
      enabled: f.enabled,
    }),
  });

  const filtered = useMemo(() => {
    const c = codeKw.trim().toLowerCase();
    const n = nameKw.trim().toLowerCase();
    return rows.filter((p) => {
      if (c && !p.code.toLowerCase().includes(c)) return false;
      if (n && !p.name.toLowerCase().includes(n)) return false;
      return true;
    });
  }, [rows, codeKw, nameKw]);

  const cols = [
    {
      key: "code",
      title: "编码",
      render: (p: AiPromptDto) => (
        <Box component="code" sx={{ fontSize: 12, fontFamily: "monospace", color: "primary.main" }}>
          {p.code}
        </Box>
      ),
    },
    {
      key: "name",
      title: "名称",
      render: (p: AiPromptDto) => (
        <Typography variant="body2" sx={{ fontWeight: 600 }}>
          {p.name}
        </Typography>
      ),
    },
    {
      key: "templateType",
      title: "模板类型",
      render: () => (
        <Chip
          label={t("ai.prompts.templateString")}
          size="small"
          variant="outlined"
          color="default"
        />
      ),
    },
    {
      key: "template",
      title: "模板内容",
      render: (p: AiPromptDto) => (
        <Typography variant="caption" sx={{ color: "text.secondary" }} noWrap>
          {p.template}
        </Typography>
      ),
    },
    {
      key: "enabled",
      title: "启用",
      render: (p: AiPromptDto) => <StatusChip enabled={p.enabled} />,
    },
    {
      key: "actions",
      title: "操作",
      render: (p: AiPromptDto) => (
        <Stack direction="row" spacing={0.5}>
          {canUpdate && (
            <Button variant="text" size="sm" onClick={() => openEdit(p)}>
              编辑
            </Button>
          )}
          <Button variant="text" size="sm" disabled title="历史（后端未开放）">
            历史
          </Button>
          <Button variant="text" size="sm" disabled title="预览（后端未开放）">
            预览
          </Button>
          {canDelete && (
            <Button variant="text" size="sm" color="danger" onClick={() => confirmDelete(p)}>
              删除
            </Button>
          )}
        </Stack>
      ),
    },
  ];

  return (
    <PageStack>
      <AiPageHeader
        title="提示词管理"
        subtitle="管理系统提示词模板，支持 FreeMarker / StringTemplate。"
      />

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
            label="编码"
            placeholder="请输入编码"
            value={codeKw}
            onChange={(e) => setCodeKw(e.target.value)}
            sx={{ width: 200 }}
          />
          <TextField
            label="名称"
            placeholder="请输入名称"
            value={nameKw}
            onChange={(e) => setNameKw(e.target.value)}
            sx={{ width: 200 }}
          />
          <Button variant="contained" onClick={reload}>
            查询
          </Button>
          <Button
            variant="outlined"
            color="default"
            onClick={() => {
              setCodeKw("");
              setNameKw("");
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
          <Button variant="outlined" color="default" disabled title="刷新缓存（后端未开放）">
            <RefreshIcon sx={{ fontSize: 18, mr: 0.5 }} />
            刷新缓存
          </Button>
        </Stack>
        <IconButton size="small" onClick={reload} aria-label="刷新">
          <RefreshIcon fontSize="small" />
        </IconButton>
      </Stack>

      <DataTable columns={cols} data={filtered} loading={loading} rowKey={(p) => p.id} selectable />
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
        title={edit ? t("ai.prompt.edit") : t("ai.prompt.add")}
        maxWidth="md"
      >
        <Stack spacing={2} sx={{ py: 1 }}>
          <Stack direction="row" spacing={2}>
            <TextField
              label="编码"
              value={form.code}
              onChange={(e) => setForm((f) => ({ ...f, code: e.target.value }))}
              placeholder="system.greeting"
              sx={{ flex: 1 }}
            />
            <Select
              label="模板类型"
              value={form.templateType}
              onChange={(e) => setForm((f) => ({ ...f, templateType: e.target.value }))}
              options={TEMPLATE_TYPES}
              sx={{ width: 200 }}
            />
          </Stack>
          <TextField
            label="名称"
            value={form.name}
            onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
          />
          <TextArea
            label="模板内容"
            value={form.template}
            onChange={(e) => setForm((f) => ({ ...f, template: e.target.value }))}
            rows={8}
            placeholder={"You are a helpful assistant. User: {input}"}
            sx={{ "& .MuiInputBase-input": { fontFamily: "monospace", fontSize: 13 } }}
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

export default Prompts;
