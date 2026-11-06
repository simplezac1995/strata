// SPDX-License-Identifier: Apache-2.0
import {
  Stack,
  Typography,
  Box,
  Drawer,
  Tabs,
  Tab,
  Menu,
  MenuItem,
  IconButton,
  Divider,
} from "@mui/material";
import {
  Button,
  TextField,
  Select,
  Dialog,
  ConfirmDialog,
  DataTable,
  Pagination,
  QueryForm,
  QueryField,
} from "@/components/ui/index";
import { PageStack } from "@/shared/pageKit/PageLayout";
import { AiPageHeader } from "@/components/ui/index";
import { fileApi } from "@/shared/api/system";
import { toast } from "@/shared/pageKit/Toast";
import { failureMessage } from "@/shared/hooks/useCrud";
import { CloudUpload, Refresh, RestartAlt, DeleteOutlined, Close } from "@mui/icons-material";
import { DOC_STATUS_LABEL, MetaRow } from "./documents/part-1";
import { useDocuments } from "./documents/useDocuments";

export function Documents() {
  const {
    canCreate,
    canDelete,
    kbs,
    kbFilter,
    setKbFilter,
    statusFilter,
    setStatusFilter,
    keyword,
    setKeyword,
    appliedKb,
    setAppliedKb,
    appliedStatus,
    setAppliedStatus,
    appliedKeyword,
    setAppliedKeyword,
    fileInputRef,
    uploading,
    setUploading,
    menuAnchor,
    setMenuAnchor,
    menuDoc,
    setMenuDoc,
    detail,
    setDetail,
    detailTab,
    setDetailTab,
    documents,
    total,
    page,
    setPage,
    pageSize,
    setPageSize,
    loading,
    reload,
    busy,
    open,
    deleting,
    form,
    setForm,
    openCreate,
    closeDialog,
    handleSave,
    confirmDelete,
    closeDelete,
    runDelete,
    kbNames,
    kbName,
    rows,
    openDetail,
    cols,
  } = useDocuments();

  return (
    <PageStack>
      <AiPageHeader
        title="文档管理"
        subtitle="知识库文档上传、切片与状态查看"
        actions={
          canCreate ? (
            <Button variant="contained" onClick={openCreate}>
              新增
            </Button>
          ) : undefined
        }
      />

      <QueryForm
        onSearch={() => {
          setAppliedKb(kbFilter);
          setAppliedStatus(statusFilter);
          setAppliedKeyword(keyword);
          setPage(1);
        }}
        onReset={() => {
          setKbFilter("");
          setStatusFilter("");
          setKeyword("");
          setAppliedKb("");
          setAppliedStatus("");
          setAppliedKeyword("");
          setPage(1);
        }}
      >
        <QueryField width={220}>
          <Select
            label="知识库名"
            value={kbFilter}
            onChange={(e) => setKbFilter(e.target.value)}
            options={[
              { value: "", label: "全部" },
              ...kbs.map((k) => ({ value: k.id, label: k.name })),
            ]}
          />
        </QueryField>
        <QueryField width={180}>
          <Select
            label="切片状态"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            options={[
              { value: "", label: "全部" },
              ...Object.entries(DOC_STATUS_LABEL).map(([value, label]) => ({ value, label })),
            ]}
          />
        </QueryField>
        <QueryField width={220}>
          <TextField
            label="文件名"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            placeholder="请输入文件名"
          />
        </QueryField>
        <QueryField width={60}>
          <IconButton size="small" onClick={reload} title="刷新">
            <Refresh fontSize="small" />
          </IconButton>
        </QueryField>
      </QueryForm>

      <DataTable columns={cols} data={rows} loading={loading} rowKey={(doc) => doc.id} />
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

      <input
        ref={fileInputRef}
        type="file"
        accept=".txt,.md,.pdf,.doc,.docx"
        style={{ display: "none" }}
        onChange={async (e) => {
          const file = e.target.files?.[0];
          if (!file) return;
          setUploading(true);
          try {
            await fileApi.upload(file, "knowledge");
            setForm((f) => ({
              ...f,
              name: file.name,
              sizeKb: Math.round(file.size / 1024),
              chunks: 0,
              status: "pending",
            }));
            toast.success("upload success");
          } catch (err) {
            toast.error(failureMessage(err));
          } finally {
            setUploading(false);
            if (fileInputRef.current) fileInputRef.current.value = "";
          }
        }}
      />

      {/* 新增文档（上传） */}
      <Dialog open={open} onClose={closeDialog} title="新增" maxWidth="md">
        <Stack spacing={2.5} sx={{ py: 1 }}>
          <Select
            label="知识库"
            required
            value={form.knowledgeId}
            onChange={(e) => setForm((f) => ({ ...f, knowledgeId: e.target.value }))}
            options={kbs.map((k) => ({ value: k.id, label: k.name }))}
          />

          <Box>
            <Typography variant="body2" sx={{ color: "text.secondary", mb: 1 }}>
              来源
            </Typography>
            <Stack direction="row" spacing={1}>
              {["上传", "网页", "录入"].map((s) => (
                <Button
                  key={s}
                  variant={s === "上传" ? "contained" : "outlined"}
                  color="default"
                  size="sm"
                >
                  {s}
                </Button>
              ))}
            </Stack>
          </Box>

          <Box
            onClick={() => fileInputRef.current?.click()}
            sx={{
              border: "1px dashed",
              borderColor: "divider",
              borderRadius: 1.5,
              py: 4,
              textAlign: "center",
              cursor: "pointer",
              "&:hover": { borderColor: "primary.main", bgcolor: "action.hover" },
            }}
          >
            <CloudUpload sx={{ fontSize: 32, color: "text.disabled", mb: 1 }} />
            <Typography variant="body2" sx={{ color: "text.secondary" }}>
              将文件拖到此处，或{" "}
              <Box component="span" sx={{ color: "primary.main" }}>
                点击上传
              </Box>
            </Typography>
            <Typography variant="caption" sx={{ color: "text.disabled" }}>
              大小不超过 10MB，格式 md / doc / pdf / txt 等
            </Typography>
          </Box>

          <TextField
            label="文件名"
            value={form.name}
            onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
            placeholder={uploading ? "上传中..." : "上传后自动填充"}
          />
        </Stack>
        <Box sx={{ display: "flex", gap: 1, justifyContent: "flex-end", pt: 2 }}>
          <Button variant="outlined" color="default" onClick={closeDialog}>
            取消
          </Button>
          <Button
            variant="contained"
            disabled={busy || !form.knowledgeId}
            onClick={() => void handleSave()}
          >
            确认
          </Button>
        </Box>
      </Dialog>

      {/* 行内“更多”菜单 */}
      <Menu anchorEl={menuAnchor} open={!!menuAnchor} onClose={() => setMenuAnchor(null)}>
        <MenuItem disabled onClick={() => setMenuAnchor(null)}>
          切片
        </MenuItem>
        <MenuItem
          onClick={() => {
            setMenuAnchor(null);
            reload();
          }}
        >
          <RestartAlt sx={{ fontSize: 16, mr: 1 }} />
          重试
        </MenuItem>
        {canDelete ? (
          <MenuItem
            sx={{ color: "error.main" }}
            onClick={() => {
              setMenuAnchor(null);
              if (menuDoc) confirmDelete(menuDoc);
            }}
          >
            <DeleteOutlined sx={{ fontSize: 16, mr: 1 }} />
            删除
          </MenuItem>
        ) : null}
      </Menu>

      {/* 文档详情抽屉 */}
      <Drawer
        anchor="right"
        open={detail !== null}
        onClose={() => setDetail(null)}
        sx={{ "& .MuiDrawer-paper": { width: { xs: "100%", sm: 640 }, maxWidth: "100%" } }}
      >
        {detail ? (
          <Box sx={{ display: "flex", flexDirection: "column", height: "100%" }}>
            <Stack
              direction="row"
              sx={{ alignItems: "center", justifyContent: "space-between", px: 3, py: 2 }}
            >
              <Typography variant="h6" sx={{ fontWeight: 600 }}>
                {detail.name}
              </Typography>
              <IconButton size="small" onClick={() => setDetail(null)}>
                <Close />
              </IconButton>
            </Stack>
            <Tabs
              value={detailTab}
              onChange={(_, v) => setDetailTab(v)}
              sx={{ px: 2, borderBottom: 1, borderColor: "divider" }}
            >
              <Tab label="文档展示" />
              <Tab label="文档关键词" />
            </Tabs>
            <Box sx={{ flex: 1, overflowY: "auto", p: 3 }}>
              {detailTab === 0 ? (
                <Stack spacing={1.5}>
                  <MetaRow label="所属知识库" value={kbName(detail.knowledgeId)} />
                  <MetaRow label="文件大小" value={`${detail.sizeKb} KB`} />
                  <MetaRow label="切片数量" value={String(detail.chunks ?? 0)} />
                  <MetaRow label="状态" value={DOC_STATUS_LABEL[detail.status] ?? detail.status} />
                  <MetaRow label="更新时间" value={String(detail.createdAt ?? "").slice(0, 10)} />
                  <Divider sx={{ my: 1 }} />
                  <Typography variant="body2" sx={{ color: "text.secondary" }}>
                    文档正文（Markdown）暂未接入后端接口，此处展示文档元信息。
                  </Typography>
                </Stack>
              ) : (
                <Box
                  sx={{
                    height: 320,
                    border: "1px dashed",
                    borderColor: "divider",
                    borderRadius: 1.5,
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                  }}
                >
                  <Typography variant="body2" sx={{ color: "text.disabled" }}>
                    关键词词云暂未接入后端
                  </Typography>
                </Box>
              )}
            </Box>
          </Box>
        ) : null}
      </Drawer>

      <ConfirmDialog
        open={deleting !== null}
        onClose={closeDelete}
        onConfirm={() => void runDelete()}
        title="删除文档"
        description={`确认删除 "${deleting?.name}"？`}
        confirmColor="danger"
      />
    </PageStack>
  );
}

export default Documents;
