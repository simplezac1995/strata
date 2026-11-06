// SPDX-License-Identifier: Apache-2.0
import { useState } from "react";
import { useI18n } from "@/shared/i18n";
import { useNavigate } from "react-router-dom";
import AddIcon from "@mui/icons-material/Add";
import DeleteIcon from "@mui/icons-material/DeleteOutlined";
import EditIcon from "@mui/icons-material/Edit";
import SearchIcon from "@mui/icons-material/Search";
import DownloadIcon from "@mui/icons-material/Download";
import RefreshIcon from "@mui/icons-material/Refresh";
import GridViewIcon from "@mui/icons-material/GridView";
import ChatIcon from "@mui/icons-material/Chat";
import DataObjectIcon from "@mui/icons-material/DataObject";
import RuleIcon from "@mui/icons-material/Rule";
import {
  Avatar,
  Box,
  Checkbox,
  Chip,
  IconButton,
  Paper,
  Radio,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Typography,
} from "@mui/material";
import {
  Button,
  TextField,
  Select,
  Dialog,
  ConfirmDialog,
  Pagination,
  EmptyState,
} from "@/components/ui/index";
import { PageStack } from "@/shared/pageKit/PageLayout";
import { useAuthStore } from "@/shared/auth/store";
import { datasetApi, type AiDatasetPayload } from "@/shared/api/ai";
import { usePagedQuery } from "@/shared/hooks/usePagedQuery";
import { useResourceCrud } from "@/shared/hooks/useResourceCrud";
import type { AiDatasetDto } from "@/shared/types";

interface DatasetForm {
  name: string;
  description: string;
  source: string;
  records: number;
  enabled: boolean;
}

export function Datasets() {
  const { t } = useI18n();
  const navigate = useNavigate();
  const canCreate = useAuthStore((s) => s.hasAuthority("ai:dataset:create"));
  const canUpdate = useAuthStore((s) => s.hasAuthority("ai:dataset:update"));
  const canDelete = useAuthStore((s) => s.hasAuthority("ai:dataset:delete"));

  const [keyword, setKeyword] = useState("");
  const [fieldDs, setFieldDs] = useState<AiDatasetDto | null>(null);

  const {
    rows: datasets,
    total,
    page,
    setPage,
    pageSize,
    setPageSize,
    loading,
    reload,
  } = usePagedQuery<AiDatasetDto>(["api", "ai", "datasets"], datasetApi.page, {
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
  } = useResourceCrud<AiDatasetDto, AiDatasetPayload, AiDatasetPayload, DatasetForm>({
    create: (payload) => datasetApi.create(payload),
    update: (id, payload) => datasetApi.update(id, payload),
    remove: (id) => datasetApi.remove(id),
    reload,
    initForm: () => ({ name: "", description: "", source: "manual", records: 0, enabled: false }),
    toEditForm: (ds) => ({
      name: ds.name,
      description: ds.description ?? "",
      source: ds.source ?? "manual",
      records: ds.records ?? 0,
      enabled: ds.enabled,
    }),
    toCreatePayload: (f) => f as unknown as AiDatasetPayload,
    toUpdatePayload: (f) => f as unknown as AiDatasetPayload,
  });

  const filtered = datasets.filter(
    (d) => !keyword || d.name.toLowerCase().includes(keyword.toLowerCase()),
  );

  return (
    <PageStack>
      {/* Filter bar */}
      <Box
        sx={{
          display: "flex",
          gap: 1.5,
          flexWrap: "wrap",
          alignItems: "center",
          "& .MuiTextField-root, & .MuiFormControl-root": { width: "auto", minWidth: 160 },
        }}
      >
        <Typography variant="body2">数据集名称</Typography>
        <TextField
          size="small"
          placeholder="请输入数据集名称"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          sx={{ width: 220 }}
        />
        <Button variant="contained" size="small" startIcon={<SearchIcon />}>
          查询
        </Button>
        <Button
          variant="outlined"
          size="small"
          color="default"
          startIcon={<RefreshIcon />}
          onClick={() => setKeyword("")}
        >
          重置
        </Button>
      </Box>

      {/* Toolbar */}
      <Box sx={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
        <Box sx={{ display: "flex", gap: 1 }}>
          {canCreate && (
            <Button variant="contained" startIcon={<AddIcon />} onClick={openCreate}>
              新增
            </Button>
          )}
          <Button variant="outlined" color="default" startIcon={<DeleteIcon />}>
            删除
          </Button>
        </Box>
        <Box sx={{ display: "flex", gap: 1 }}>
          {[SearchIcon, DownloadIcon, RefreshIcon].map((Icon, i) => (
            <IconButton
              key={i}
              sx={{ border: 1, borderColor: "divider", borderRadius: 1.5, width: 38, height: 38 }}
            >
              <Icon fontSize="small" />
            </IconButton>
          ))}
        </Box>
      </Box>

      {/* Card grid */}
      {filtered.length === 0 && !loading ? (
        <EmptyState message="暂无数据集，点击「新增」创建第一个数据集" />
      ) : (
        <Box
          sx={{
            display: "grid",
            gridTemplateColumns: { xs: "1fr", sm: "repeat(2, 1fr)", xl: "repeat(3, 1fr)" },
            gap: 2,
          }}
        >
          {filtered.map((ds) => (
            <Paper key={ds.id} variant="outlined" sx={{ p: 2.5, borderRadius: 2 }}>
              <Box sx={{ display: "flex", alignItems: "center", gap: 1.5 }}>
                <Avatar sx={{ width: 44, height: 44, bgcolor: "secondary.main", borderRadius: 2 }}>
                  {ds.name?.[0] ?? "D"}
                </Avatar>
                <Box sx={{ flex: 1, minWidth: 0 }}>
                  <Typography variant="subtitle1" sx={{ fontWeight: 600 }} noWrap>
                    {ds.name}
                  </Typography>
                  <Chip
                    label={t("ai.datasets.sourcePigxx")}
                    size="small"
                    variant="outlined"
                    sx={{ mt: 0.25 }}
                  />
                </Box>
              </Box>
              <Typography variant="body2" sx={{ color: "text.secondary", mt: 1.5, minHeight: 40 }}>
                {ds.description || "暂无描述"}
              </Typography>
              <Stack
                direction="row"
                sx={{
                  mt: 1.5,
                  pt: 1.5,
                  borderTop: 1,
                  borderColor: "divider",
                  alignItems: "center",
                  justifyContent: "space-between",
                }}
              >
                <Stack direction="row" spacing={0.5}>
                  {canUpdate && (
                    <IconButton size="small" onClick={() => openEdit(ds)} title="编辑">
                      <EditIcon fontSize="small" sx={{ color: "primary.main" }} />
                    </IconButton>
                  )}
                  {canDelete && (
                    <IconButton size="small" onClick={() => confirmDelete(ds)} title="删除">
                      <DeleteIcon fontSize="small" sx={{ color: "error.main" }} />
                    </IconButton>
                  )}
                  <IconButton size="small" onClick={() => setFieldDs(ds)} title="字段管理">
                    <GridViewIcon fontSize="small" sx={{ color: "primary.main" }} />
                  </IconButton>
                  <IconButton size="small" title="对话" onClick={() => navigate("/ai/workspace")}>
                    <ChatIcon fontSize="small" sx={{ color: "primary.main" }} />
                  </IconButton>
                </Stack>
                <Checkbox size="small" />
              </Stack>
              <Typography variant="caption" sx={{ color: "text.secondary" }}>
                记录数: {ds.records ?? 0}
              </Typography>
            </Paper>
          ))}
        </Box>
      )}

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

      {/* Add / Edit dialog */}
      <Dialog open={open} onClose={closeDialog} title={edit ? "编辑" : "新增"} maxWidth="sm">
        <Stack spacing={2} sx={{ py: 1 }}>
          <Box sx={{ display: "flex", alignItems: "center", gap: 2 }}>
            <Typography sx={{ width: 80 }}>* 名称</Typography>
            <TextField
              value={form.name}
              onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
              placeholder="请输入数据集名称"
            />
          </Box>
          <Box sx={{ display: "flex", gap: 2 }}>
            <Typography sx={{ width: 80, pt: 1 }}>* 描述</Typography>
            <TextField
              value={form.description}
              onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
              placeholder="请输入数据集描述"
              multiline
              minRows={3}
              sx={{ flex: 1 }}
            />
          </Box>
          <Box sx={{ display: "flex", alignItems: "center", gap: 2 }}>
            <Typography sx={{ width: 80 }}>* 数据源</Typography>
            <Select value="" onChange={() => {}} options={[{ value: "", label: "请选择数据源" }]} />
          </Box>
          <Box sx={{ display: "flex", alignItems: "center", gap: 2 }}>
            <Typography sx={{ width: 80 }}>* 数据表</Typography>
            <Select value="" onChange={() => {}} options={[{ value: "", label: "请选择数据表" }]} />
          </Box>
          <Box sx={{ display: "flex", alignItems: "center", gap: 2 }}>
            <Typography sx={{ width: 80 }}>专业术语</Typography>
            <Select
              value=""
              onChange={() => {}}
              options={[{ value: "", label: "请选择专业术语" }]}
            />
          </Box>
          <Box sx={{ display: "flex", alignItems: "center", gap: 2 }}>
            <Typography sx={{ width: 80 }}>* 学习状态</Typography>
            <Stack direction="row" spacing={2}>
              {[
                { value: false, label: "否" },
                { value: true, label: "是" },
              ].map((opt) => (
                <Box
                  key={opt.label}
                  onClick={() => setForm((f) => ({ ...f, enabled: opt.value }))}
                  sx={{
                    display: "flex",
                    alignItems: "center",
                    gap: 0.5,
                    px: 1.5,
                    py: 0.5,
                    border: 1,
                    borderRadius: 1,
                    cursor: "pointer",
                    borderColor: form.enabled === opt.value ? "primary.main" : "divider",
                  }}
                >
                  <Radio size="small" checked={form.enabled === opt.value} />
                  {opt.label}
                </Box>
              ))}
            </Stack>
          </Box>
        </Stack>
        <Box sx={{ display: "flex", gap: 1, justifyContent: "flex-end", pt: 2 }}>
          <Button variant="outlined" color="default" onClick={closeDialog}>
            取消
          </Button>
          <Button variant="contained" disabled={busy} onClick={() => void handleSave()}>
            确认
          </Button>
        </Box>
      </Dialog>

      {/* Field management big dialog */}
      <Dialog
        open={fieldDs !== null}
        onClose={() => setFieldDs(null)}
        title={`字段管理 - ${fieldDs?.name ?? ""}`}
        maxWidth="lg"
        sx={{ "& .MuiDialog-paper": { height: "80%" } }}
      >
        <Box sx={{ display: "flex", gap: 2, height: "100%" }}>
          {/* left: table list */}
          <Paper variant="outlined" sx={{ width: 220, p: 1.5, flexShrink: 0 }}>
            <Box sx={{ display: "flex", alignItems: "center", gap: 0.75, mb: 1 }}>
              <RuleIcon fontSize="small" />
              <Typography variant="subtitle2" sx={{ fontWeight: 600 }}>
                数据表列表
              </Typography>
            </Box>
            <Typography
              variant="body2"
              sx={{ color: "text.secondary", textAlign: "center", py: 3 }}
            >
              暂无数据表
            </Typography>
          </Paper>
          {/* right: field table */}
          <Box sx={{ flex: 1, display: "flex", flexDirection: "column" }}>
            <Box
              sx={{
                display: "flex",
                alignItems: "center",
                justifyContent: "space-between",
                mb: 1.5,
              }}
            >
              <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
                <DataObjectIcon fontSize="small" />
                <Typography variant="subtitle2" sx={{ fontWeight: 600 }}>
                  字段列表
                </Typography>
                <Chip label={0} size="small" color="primary" />
              </Box>
              <Typography variant="body2" sx={{ color: "text.secondary" }}>
                共 0 个字段
              </Typography>
            </Box>
            <Table size="small">
              <TableHead>
                <TableRow sx={{ bgcolor: "action.hover" }}>
                  <TableCell sx={{ fontWeight: 600 }}>主键</TableCell>
                  <TableCell sx={{ fontWeight: 600 }}>字段名称</TableCell>
                  <TableCell sx={{ fontWeight: 600 }}>字段类型</TableCell>
                  <TableCell sx={{ fontWeight: 600 }}>字段备注</TableCell>
                  <TableCell sx={{ fontWeight: 600 }}>虚拟备注</TableCell>
                  <TableCell sx={{ fontWeight: 600 }}>修正状态</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                <TableRow>
                  <TableCell
                    colSpan={6}
                    sx={{ textAlign: "center", color: "text.secondary", py: 4 }}
                  >
                    暂无数据
                  </TableCell>
                </TableRow>
              </TableBody>
            </Table>
          </Box>
        </Box>
        <Box sx={{ display: "flex", gap: 1, justifyContent: "flex-end" }}>
          <Button variant="outlined" color="default" onClick={() => setFieldDs(null)}>
            关闭
          </Button>
        </Box>
      </Dialog>

      <ConfirmDialog
        open={deleting !== null}
        onClose={closeDelete}
        onConfirm={() => void runDelete()}
        title="删除数据集"
        description={`确认删除 "${deleting?.name}"？`}
        confirmColor="danger"
      />
    </PageStack>
  );
}

export default Datasets;
