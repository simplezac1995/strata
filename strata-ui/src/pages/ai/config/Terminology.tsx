// SPDX-License-Identifier: Apache-2.0
import { useMemo, useState } from "react";
import { Box, Drawer, IconButton, Stack, Typography } from "@mui/material";
import AddIcon from "@mui/icons-material/Add";
import CloudUploadIcon from "@mui/icons-material/CloudUpload";
import DeleteOutline from "@mui/icons-material/DeleteOutlined";
import RefreshIcon from "@mui/icons-material/Refresh";
import {
  Button,
  TextField,
  TextArea,
  ConfirmDialog,
  DataTable,
  Pagination,
} from "@/components/ui/index";
import { AiPageHeader } from "@/components/ui/index";
import { useI18n } from "@/shared/i18n";
import { useAuthStore } from "@/shared/auth/store";
import { termApi, type AiTermPayload, type AiTermUpdatePayload } from "@/shared/api/ai";
import { usePagedQuery } from "@/shared/hooks/usePagedQuery";
import { useResourceCrud } from "@/shared/hooks/useResourceCrud";
import type { AiTermDto } from "@/shared/types";
import { PageStack, ContentCard } from "@/shared/pageKit/PageLayout";

interface TermForm {
  term: string;
  translation: string;
  notes: string;
  enabled: boolean;
  synonyms: string[];
}

export function Terminology() {
  const { t } = useI18n();
  const canCreate = useAuthStore((s) => s.hasAuthority("ai:term:create"));
  const canUpdate = useAuthStore((s) => s.hasAuthority("ai:term:update"));
  const canDelete = useAuthStore((s) => s.hasAuthority("ai:term:delete"));

  const [termKw, setTermKw] = useState("");

  const { rows, total, page, setPage, pageSize, setPageSize, loading, reload } =
    usePagedQuery<AiTermDto>(["api", "ai", "terms"], termApi.page, {
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
  } = useResourceCrud<AiTermDto, AiTermPayload, AiTermUpdatePayload, TermForm>({
    create: (payload) => termApi.create(payload),
    update: (id, payload) => termApi.update(id, payload),
    remove: (id) => termApi.remove(id),
    reload,
    initForm: () => ({ term: "", translation: "", notes: "", enabled: true, synonyms: [] }),
    toEditForm: (r) => ({
      term: r.term,
      translation: r.translation,
      notes: r.notes ?? "",
      enabled: r.enabled,
      synonyms: r.translation
        ? r.translation
            .split(/[、,，]/)
            .map((s) => s.trim())
            .filter(Boolean)
        : [],
    }),
    toCreatePayload: (f) => ({
      term: f.term,
      translation: f.synonyms.join("、") || f.translation,
      notes: f.notes || null,
      enabled: f.enabled,
    }),
    toUpdatePayload: (f) => ({
      translation: f.synonyms.join("、") || f.translation,
      notes: f.notes || null,
      enabled: f.enabled,
    }),
  });

  const filtered = useMemo(() => {
    const kw = termKw.trim().toLowerCase();
    if (!kw) return rows;
    return rows.filter(
      (r) => r.term.toLowerCase().includes(kw) || (r.translation ?? "").toLowerCase().includes(kw),
    );
  }, [rows, termKw]);

  const cols = [
    {
      key: "term",
      title: "术语名称",
      render: (r: AiTermDto) => (
        <Box component="code" sx={{ fontSize: 13, fontFamily: "monospace", fontWeight: 600 }}>
          {r.term}
        </Box>
      ),
    },
    {
      key: "translation",
      title: "描述",
      render: (r: AiTermDto) => <Typography variant="body2">{r.translation}</Typography>,
    },
    {
      key: "notes",
      title: "备注",
      render: (r: AiTermDto) => (
        <Typography variant="caption" sx={{ color: "text.secondary" }}>
          {r.notes || "—"}
        </Typography>
      ),
    },
    {
      key: "actions",
      title: "操作",
      render: (r: AiTermDto) => (
        <Stack direction="row" spacing={0.5}>
          {canUpdate && (
            <Button variant="text" size="sm" onClick={() => openEdit(r)}>
              编辑
            </Button>
          )}
          {canDelete && (
            <Button variant="text" size="sm" color="danger" onClick={() => confirmDelete(r)}>
              删除
            </Button>
          )}
        </Stack>
      ),
    },
  ];

  const setSynonym = (i: number, value: string) =>
    setForm((f) => {
      const next = [...f.synonyms];
      next[i] = value;
      return { ...f, synonyms: next };
    });
  const addSynonym = () => setForm((f) => ({ ...f, synonyms: [...f.synonyms, ""] }));
  const removeSynonym = (i: number) =>
    setForm((f) => ({ ...f, synonyms: f.synonyms.filter((_, idx) => idx !== i) }));

  return (
    <PageStack>
      <AiPageHeader title="AI 术语" subtitle="维护领域术语及其同义词，提升问答翻译与一致性。" />

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
            label="术语名称"
            placeholder="请输入术语名称"
            value={termKw}
            onChange={(e) => setTermKw(e.target.value)}
            sx={{ width: 240 }}
          />
          <Button variant="contained" onClick={reload}>
            查询
          </Button>
          <Button variant="outlined" color="default" onClick={() => setTermKw("")}>
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
          <Button variant="outlined" color="default" disabled title="导入（后端未开放）">
            <CloudUploadIcon sx={{ fontSize: 18, mr: 0.5 }} />
            导入
          </Button>
          <Button variant="outlined" color="default" disabled title="批量删除（后端未开放）">
            <DeleteOutline sx={{ fontSize: 18, mr: 0.5 }} />
            删除
          </Button>
        </Stack>
        <IconButton size="small" onClick={reload} aria-label="刷新">
          <RefreshIcon fontSize="small" />
        </IconButton>
      </Stack>

      <DataTable columns={cols} data={filtered} loading={loading} rowKey={(r) => r.id} selectable />
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

      {/* ── Right drawer: 新建/编辑术语 ── */}
      <Drawer
        anchor="right"
        open={open}
        onClose={closeDialog}
        sx={{ "& .MuiDrawer-paper": { width: { xs: "100%", sm: 480 }, p: 3 } }}
      >
        <Typography variant="h6" sx={{ mb: 2, fontWeight: 600 }}>
          {edit ? "编辑术语" : "新建术语"}
        </Typography>
        <Stack spacing={2.5} sx={{ flex: 1 }}>
          <TextField
            label="术语名称"
            required
            value={form.term}
            onChange={(e) => setForm((f) => ({ ...f, term: e.target.value }))}
            placeholder="例如 账单"
          />
          <TextArea
            label="术语描述"
            required
            value={form.translation}
            onChange={(e) => setForm((f) => ({ ...f, translation: e.target.value }))}
            rows={3}
            placeholder="例如 token、令牌"
          />
          <Box>
            <Typography variant="caption" sx={{ color: "text.secondary", mb: 1, display: "block" }}>
              同义词
            </Typography>
            <Stack spacing={1.25}>
              {form.synonyms.map((syn, i) => (
                <Stack key={i} direction="row" spacing={1} sx={{ alignItems: "center" }}>
                  <TextField
                    value={syn}
                    onChange={(e) => setSynonym(i, e.target.value)}
                    placeholder="同义词"
                  />
                  <IconButton size="small" color="error" onClick={() => removeSynonym(i)}>
                    <DeleteOutline fontSize="small" />
                  </IconButton>
                </Stack>
              ))}
              <Button
                variant="outlined"
                color="primary"
                onClick={addSynonym}
                sx={{ borderStyle: "dashed" }}
              >
                <AddIcon sx={{ fontSize: 18, mr: 0.5 }} />
                添加同义词
              </Button>
            </Stack>
          </Box>
        </Stack>
        <Stack direction="row" spacing={1.5} sx={{ justifyContent: "flex-end", pt: 2 }}>
          <Button variant="outlined" color="default" onClick={closeDialog}>
            取消
          </Button>
          <Button variant="contained" disabled={busy} onClick={() => void handleSave()}>
            {edit ? "更新" : "保存"}
          </Button>
        </Stack>
      </Drawer>

      <ConfirmDialog
        open={deleting !== null}
        onClose={closeDelete}
        onConfirm={() => void runDelete()}
        title={t("common.delete")}
        description={t("common.deleteNamed").replace("{name}", deleting?.term ?? "")}
        confirmColor="danger"
      />
    </PageStack>
  );
}

export default Terminology;
