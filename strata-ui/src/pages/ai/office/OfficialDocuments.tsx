// SPDX-License-Identifier: Apache-2.0
import {
  Add as AddIcon,
  Search as SearchIcon,
  Refresh as RefreshIcon,
  DeleteOutlined as DeleteIcon,
  Edit as EditIcon,
  DesignServices as DesignIcon,
  Close as CloseIcon,
  Visibility as PreviewIcon,
  Send as SendIcon,
  RestartAlt as ResetIcon,
} from "@mui/icons-material";
import { Box, Checkbox, IconButton, Paper, Stack, Switch, Typography } from "@mui/material";
import { Button, ConfirmDialog, Dialog, TextArea, TextField } from "@/components/ui/index";
import { PageStack } from "@/shared/pageKit/PageLayout";
import { toast } from "@/shared/pageKit/Toast";
import { DocPreviewSkeleton } from "./officialdocuments/part-1";
import { useOfficialDocuments } from "./officialdocuments/useOfficialDocuments";

export function OfficialDocuments() {
  const {
    templates,
    setTemplates,
    keyword,
    setKeyword,
    editing,
    setEditing,
    formOpen,
    setFormOpen,
    formName,
    setFormName,
    formPrompt,
    setFormPrompt,
    deleting,
    setDeleting,
    selectedId,
    setSelectedId,
    designerOpen,
    setDesignerOpen,
    designTitle,
    setDesignTitle,
    showHeader,
    setShowHeader,
    writingOpen,
    setWritingOpen,
    topic,
    setTopic,
    generated,
    setGenerated,
    writing,
    setWriting,
    filtered,
    openCreate,
    openEdit,
    saveForm,
    openDesigner,
    generateDoc,
  } = useOfficialDocuments();

  return (
    <PageStack>
      {/* Filter bar */}
      <Box sx={{ display: "flex", gap: 1.5, alignItems: "center", flexWrap: "wrap" }}>
        <Typography variant="body2">模板名称</Typography>
        <TextField
          placeholder="请输入模板名称"
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
          <Button variant="contained" startIcon={<AddIcon />} onClick={openCreate}>
            新增
          </Button>
          <Button variant="outlined" color="default" startIcon={<DeleteIcon />}>
            删除
          </Button>
        </Box>
        <Box sx={{ display: "flex", gap: 1 }}>
          <IconButton
            sx={{ border: 1, borderColor: "divider", borderRadius: 1.5, width: 38, height: 38 }}
          >
            <SearchIcon fontSize="small" />
          </IconButton>
          <IconButton
            sx={{ border: 1, borderColor: "divider", borderRadius: 1.5, width: 38, height: 38 }}
          >
            <RefreshIcon fontSize="small" />
          </IconButton>
        </Box>
      </Box>

      {/* Card grid (2 columns) */}
      <Box
        sx={{
          display: "grid",
          gridTemplateColumns: { xs: "1fr", sm: "repeat(2, 1fr)", lg: "repeat(3, 1fr)" },
          gap: 2,
        }}
      >
        {filtered.map((t) => (
          <Paper
            key={t.id}
            variant="outlined"
            sx={{
              borderRadius: 2,
              p: 2,
              cursor: "pointer",
              borderColor: selectedId === t.id ? "primary.main" : "divider",
              borderWidth: selectedId === t.id ? 2 : 1,
            }}
            onClick={() => setSelectedId(t.id)}
          >
            <Typography variant="subtitle1" sx={{ fontWeight: 600, mb: 1.5 }}>
              {t.name}
            </Typography>
            <DocPreviewSkeleton title={t.name} />
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
                <IconButton
                  size="small"
                  onClick={(e) => {
                    e.stopPropagation();
                    openEdit(t);
                  }}
                  title="编辑"
                >
                  <EditIcon fontSize="small" sx={{ color: "primary.main" }} />
                </IconButton>
                <IconButton
                  size="small"
                  onClick={(e) => {
                    e.stopPropagation();
                    openDesigner(t);
                  }}
                  title="设计"
                >
                  <DesignIcon fontSize="small" sx={{ color: "primary.main" }} />
                </IconButton>
                <IconButton
                  size="small"
                  onClick={(e) => {
                    e.stopPropagation();
                    setDeleting(t);
                  }}
                  title="删除"
                >
                  <DeleteIcon fontSize="small" sx={{ color: "error.main" }} />
                </IconButton>
              </Stack>
              <Stack direction="row" spacing={1} sx={{ alignItems: "center" }}>
                <Typography variant="caption" sx={{ color: "text.secondary" }}>
                  {t.updatedAt}
                </Typography>
                <Checkbox
                  size="small"
                  checked={selectedId === t.id}
                  onClick={(e) => e.stopPropagation()}
                  onChange={() => setSelectedId(selectedId === t.id ? null : t.id)}
                />
              </Stack>
            </Stack>
          </Paper>
        ))}
        {filtered.length === 0 && (
          <Box sx={{ gridColumn: "1 / -1" }}>
            <Paper variant="outlined" sx={{ p: 6, textAlign: "center", color: "text.secondary" }}>
              暂无公文模板，点击「新增」创建
            </Paper>
          </Box>
        )}
      </Box>

      {/* Create / Edit dialog */}
      <Dialog
        open={formOpen}
        onClose={() => setFormOpen(false)}
        title={editing ? "编辑公文模板" : "新增公文模板"}
        maxWidth="sm"
      >
        <Stack spacing={2.5} sx={{ py: 1 }}>
          <TextField
            label="模板名称"
            value={formName}
            onChange={(e) => setFormName(e.target.value)}
            placeholder="请输入模板名称，如：放假通知"
          />
          <TextArea
            label="提示词"
            value={formPrompt}
            onChange={(e) => setFormPrompt(e.target.value)}
            rows={5}
            placeholder="请输入公文写作提示词模板"
          />
        </Stack>
        <Box sx={{ display: "flex", justifyContent: "flex-end", gap: 1, pt: 2 }}>
          <Button variant="outlined" color="default" onClick={() => setFormOpen(false)}>
            取消
          </Button>
          <Button variant="contained" onClick={saveForm}>
            确认
          </Button>
        </Box>
      </Dialog>

      {/* Template designer overlay */}
      {designerOpen && (
        <Box
          sx={{
            position: "fixed",
            inset: 0,
            zIndex: 1300,
            bgcolor: "background.default",
            p: 3,
            overflow: "auto",
          }}
        >
          <Stack direction="row" spacing={1.5} sx={{ mb: 2, alignItems: "center" }}>
            <TextField
              value={designTitle}
              onChange={(e) => setDesignTitle(e.target.value)}
              sx={{ width: 280 }}
            />
            <Button variant="contained" size="small" onClick={() => toast.success("模板已保存")}>
              保存
            </Button>
            <Box sx={{ flex: 1 }} />
            <Button
              variant="outlined"
              size="small"
              color="default"
              onClick={() => {
                setDesignerOpen(false);
                setWritingOpen(true);
              }}
            >
              AI 写作
            </Button>
            <IconButton onClick={() => setDesignerOpen(false)}>
              <CloseIcon />
            </IconButton>
          </Stack>
          <Box sx={{ display: "flex", gap: 2, height: "calc(100vh - 130px)" }}>
            <Paper variant="outlined" sx={{ width: 320, p: 2.5, overflowY: "auto" }}>
              <Typography variant="subtitle2" sx={{ mb: 1.5 }}>
                版头要素
              </Typography>
              <Stack spacing={1.5}>
                <Box
                  sx={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}
                >
                  <Typography variant="body2">显示发文机关标志</Typography>
                  <Switch checked={showHeader} onChange={(_, v) => setShowHeader(v)} />
                </Box>
                <Box
                  sx={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}
                >
                  <Typography variant="body2">份号</Typography>
                  <Switch defaultChecked />
                </Box>
                <Box
                  sx={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}
                >
                  <Typography variant="body2">紧急程度</Typography>
                  <Switch />
                </Box>
                <Box
                  sx={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}
                >
                  <Typography variant="body2">签发人</Typography>
                  <Switch defaultChecked />
                </Box>
              </Stack>
            </Paper>
            <Paper
              variant="outlined"
              sx={{ flex: 1, p: 4, overflowY: "auto", display: "flex", justifyContent: "center" }}
            >
              <Box sx={{ width: 620, maxWidth: "100%" }}>
                <DocPreviewSkeleton title={designTitle} />
              </Box>
            </Paper>
          </Box>
        </Box>
      )}

      {/* AI writing overlay */}
      {writingOpen && (
        <Box
          sx={{
            position: "fixed",
            inset: 0,
            zIndex: 1300,
            bgcolor: "background.default",
            p: 3,
            overflow: "auto",
          }}
        >
          <Box
            sx={{ display: "flex", alignItems: "center", justifyContent: "space-between", mb: 2 }}
          >
            <Typography variant="h6">AI 公文写作</Typography>
            <IconButton onClick={() => setWritingOpen(false)}>
              <CloseIcon />
            </IconButton>
          </Box>
          <Box sx={{ display: "flex", gap: 2, height: "calc(100vh - 130px)" }}>
            <Paper variant="outlined" sx={{ flex: 1, p: 3, overflowY: "auto" }}>
              <Typography variant="subtitle2" sx={{ mb: 1.5 }}>
                只读预览
              </Typography>
              {generated ? (
                <Typography variant="body2" sx={{ whiteSpace: "pre-wrap", lineHeight: 1.8 }}>
                  {generated}
                </Typography>
              ) : (
                <Box sx={{ textAlign: "center", py: 10 }}>
                  <PreviewIcon sx={{ fontSize: 48, color: "text.disabled", mb: 1 }} />
                  <Typography variant="body2" sx={{ color: "text.secondary" }}>
                    {writing ? "正在生成..." : "输入右侧主题后生成公文"}
                  </Typography>
                </Box>
              )}
            </Paper>
            <Paper
              variant="outlined"
              sx={{
                width: 420,
                p: 2.5,
                display: "flex",
                flexDirection: "column",
                overflow: "hidden",
              }}
            >
              <Typography variant="subtitle2" sx={{ mb: 1.5 }}>
                写作配置
              </Typography>
              <TextArea
                label="主题 / 标题"
                value={topic}
                onChange={(e) => setTopic(e.target.value)}
                rows={3}
                placeholder="例如：关于开展2026年第三季度安全生产大检查的通知"
              />
              <Box sx={{ display: "flex", gap: 1, pt: 2, mt: "auto" }}>
                <Button
                  variant="outlined"
                  color="default"
                  startIcon={<ResetIcon />}
                  onClick={() => {
                    setTopic("");
                    setGenerated("");
                  }}
                >
                  重置
                </Button>
                <Button
                  variant="contained"
                  sx={{ flex: 1 }}
                  disabled={!topic.trim() || writing}
                  startIcon={<SendIcon />}
                  onClick={() => void generateDoc()}
                >
                  {writing ? "生成中..." : "生成文章"}
                </Button>
              </Box>
            </Paper>
          </Box>
        </Box>
      )}

      <ConfirmDialog
        open={deleting !== null}
        onClose={() => setDeleting(null)}
        onConfirm={() => {
          setTemplates((prev) => prev.filter((t) => t.id !== deleting!.id));
          setDeleting(null);
          toast.success("已删除");
        }}
        title="删除公文模板"
        description={`确认删除 "${deleting?.name}"？`}
        confirmColor="danger"
      />
    </PageStack>
  );
}

export default OfficialDocuments;
