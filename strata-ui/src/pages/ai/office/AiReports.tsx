// SPDX-License-Identifier: Apache-2.0
import {
  Add as AddIcon,
  Search as SearchIcon,
  Refresh as RefreshIcon,
  Download as DownloadIcon,
  DeleteOutlined as DeleteIcon,
  Edit as EditIcon,
  Close as CloseIcon,
  Preview as PreviewIcon,
  Forum as ChatIcon,
  Download as ExportIcon,
  SmartToy as ModelIcon,
  CheckCircle as DoneIcon,
  Send as RunIcon,
} from "@mui/icons-material";
import { Avatar, Box, Checkbox, Chip, IconButton, Paper, Stack, Typography } from "@mui/material";
import { Button, ConfirmDialog, Dialog, Select, TextArea, TextField } from "@/components/ui/index";
import { PageStack } from "@/shared/pageKit/PageLayout";
import { toast } from "@/shared/pageKit/Toast";
import { useAiReports } from "./aireports/useAiReports";

export function AiReports() {
  const {
    tpls,
    setTpls,
    titleKw,
    setTitleKw,
    promptKw,
    setPromptKw,
    editing,
    setEditing,
    formOpen,
    setFormOpen,
    formName,
    setFormName,
    formDesc,
    setFormDesc,
    deleting,
    setDeleting,
    selectedId,
    setSelectedId,
    workTpl,
    setWorkTpl,
    model,
    setModel,
    polish,
    setPolish,
    prompt,
    setPrompt,
    result,
    setResult,
    generating,
    setGenerating,
    models,
    filtered,
    openCreate,
    openEdit,
    saveForm,
    openWorkbench,
    generate,
  } = useAiReports();

  return (
    <PageStack>
      {/* Filter bar */}
      <Box sx={{ display: "flex", gap: 1.5, alignItems: "center", flexWrap: "wrap" }}>
        <Typography variant="body2">标题</Typography>
        <TextField
          placeholder="请输入标题"
          value={titleKw}
          onChange={(e) => setTitleKw(e.target.value)}
          sx={{ width: 200 }}
        />
        <Typography variant="body2">提示词</Typography>
        <TextField
          placeholder="请输入提示词"
          value={promptKw}
          onChange={(e) => setPromptKw(e.target.value)}
          sx={{ width: 200 }}
        />
        <Button variant="contained" size="small" startIcon={<SearchIcon />}>
          查询
        </Button>
        <Button
          variant="outlined"
          size="small"
          color="default"
          startIcon={<RefreshIcon />}
          onClick={() => {
            setTitleKw("");
            setPromptKw("");
          }}
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
            <Box sx={{ display: "flex", alignItems: "center", gap: 1.5 }}>
              <Avatar
                sx={{
                  width: 44,
                  height: 44,
                  bgcolor: "secondary.main",
                  borderRadius: 1.5,
                  fontSize: 18,
                }}
              >
                {t.name.slice(0, 1)}
              </Avatar>
              <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
                {t.name}
              </Typography>
            </Box>
            <Paper
              variant="outlined"
              sx={{
                mt: 1.5,
                p: 1.5,
                height: 90,
                overflow: "auto",
                bgcolor: "action.hover",
              }}
            >
              <Typography
                variant="body2"
                sx={{
                  color: "text.secondary",
                  display: "-webkit-box",
                  WebkitLineClamp: 4,
                  WebkitBoxOrient: "vertical",
                  overflow: "hidden",
                }}
              >
                {t.description || "暂无描述"}
              </Typography>
            </Paper>
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
                  title="预览"
                  onClick={(e) => {
                    e.stopPropagation();
                    openWorkbench(t);
                  }}
                >
                  <PreviewIcon fontSize="small" sx={{ color: "primary.main" }} />
                </IconButton>
                <IconButton size="small" title="对话" onClick={(e) => e.stopPropagation()}>
                  <ChatIcon fontSize="small" sx={{ color: "primary.main" }} />
                </IconButton>
                <IconButton
                  size="small"
                  title="编辑"
                  onClick={(e) => {
                    e.stopPropagation();
                    openEdit(t);
                  }}
                >
                  <EditIcon fontSize="small" sx={{ color: "primary.main" }} />
                </IconButton>
                <IconButton
                  size="small"
                  title="删除"
                  onClick={(e) => {
                    e.stopPropagation();
                    setDeleting(t);
                  }}
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
              暂无报告模板，点击「新增」创建
            </Paper>
          </Box>
        )}
      </Box>

      {/* Create / Edit dialog */}
      <Dialog
        open={formOpen}
        onClose={() => setFormOpen(false)}
        title={editing ? "编辑" : "新增"}
        maxWidth="sm"
      >
        <Stack spacing={2.5} sx={{ py: 1 }}>
          <TextField
            label="标题 *"
            value={formName}
            onChange={(e) => setFormName(e.target.value)}
            placeholder="会议助手"
          />
          <TextArea
            label="描述 *"
            value={formDesc}
            onChange={(e) => setFormDesc(e.target.value)}
            rows={5}
            placeholder="提示词模板"
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

      {/* Generation workbench overlay */}
      {workTpl && (
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
            <Stack direction="row" spacing={1.5} sx={{ alignItems: "center" }}>
              <Typography variant="h6">{workTpl.name}</Typography>
              <Typography variant="body2" sx={{ color: "text.secondary" }}>
                AI 报告生成
              </Typography>
            </Stack>
            <IconButton onClick={() => setWorkTpl(null)}>
              <CloseIcon />
            </IconButton>
          </Box>
          <Box sx={{ display: "flex", gap: 2, height: "calc(100vh - 130px)" }}>
            {/* Left: doc preview */}
            <Paper
              variant="outlined"
              sx={{ flex: 1, p: 2.5, display: "flex", flexDirection: "column", overflow: "hidden" }}
            >
              <Stack
                direction="row"
                sx={{ alignItems: "center", justifyContent: "space-between", mb: 1.5 }}
              >
                <Stack direction="row" spacing={1} sx={{ alignItems: "center" }}>
                  <Typography variant="subtitle2">文档预览</Typography>
                  <Chip
                    label={result ? "已生成" : "等待生成"}
                    size="small"
                    variant="outlined"
                    color={result ? "success" : "default"}
                  />
                </Stack>
                <Button variant="outlined" size="small" color="default" startIcon={<ExportIcon />}>
                  下载文档
                </Button>
              </Stack>
              <Box
                sx={{
                  flex: 1,
                  overflowY: "auto",
                  border: 1,
                  borderColor: "divider",
                  borderRadius: 2,
                  p: 3,
                }}
              >
                {result ? (
                  <Typography variant="body2" sx={{ whiteSpace: "pre-wrap", lineHeight: 1.8 }}>
                    {result}
                  </Typography>
                ) : (
                  <Box sx={{ textAlign: "center", py: 10 }}>
                    <PreviewIcon sx={{ fontSize: 48, color: "text.disabled", mb: 1 }} />
                    <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
                      {generating ? "正在生成报告..." : "生成后在此预览报告"}
                    </Typography>
                    <Typography variant="body2" sx={{ color: "text.secondary" }}>
                      完成右侧配置并点击生成报告
                    </Typography>
                    <Stack direction="row" spacing={2} sx={{ justifyContent: "center", mt: 3 }}>
                      {["1 选择模型", "2 输入要求", "3 生成并编辑"].map((s, i) => (
                        <Stack key={s} direction="row" spacing={0.5} sx={{ alignItems: "center" }}>
                          <DoneIcon sx={{ fontSize: 16, color: "primary.main" }} />
                          <Typography variant="caption" sx={{ color: "text.secondary" }}>
                            {s}
                          </Typography>
                          {i < 2 ? (
                            <Typography variant="caption" sx={{ color: "divider" }}>
                              ·
                            </Typography>
                          ) : null}
                        </Stack>
                      ))}
                    </Stack>
                  </Box>
                )}
              </Box>
            </Paper>

            {/* Right: config */}
            <Paper
              variant="outlined"
              sx={{
                width: 440,
                p: 2.5,
                display: "flex",
                flexDirection: "column",
                overflow: "hidden",
              }}
            >
              <Stack direction="row" spacing={1} sx={{ alignItems: "center", mb: 1.5 }}>
                <Typography variant="subtitle2">报告配置</Typography>
                <Typography variant="caption" sx={{ color: "text.secondary" }}>
                  设置生成方式与内容要求
                </Typography>
              </Stack>
              <Typography variant="body2" sx={{ mb: 0.5 }}>
                模型
              </Typography>
              <Select
                value={model}
                onChange={(e) => setModel(e.target.value)}
                startAdornment={<ModelIcon sx={{ fontSize: 18, mr: 1 }} />}
                displayEmpty
                sx={{ mb: 1.5 }}
                options={[
                  { value: "", label: "请选择模型" },
                  ...models.map((m) => ({ value: m.id, label: `${m.name} (${m.code})` })),
                ]}
              />
              <Box
                sx={{
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "space-between",
                  mb: 1.5,
                }}
              >
                <Typography variant="body2">AI 润色</Typography>
                <Stack direction="row" spacing={1}>
                  {[false, true].map((v) => (
                    <Box
                      key={String(v)}
                      onClick={() => setPolish(v)}
                      sx={{
                        px: 2,
                        py: 0.5,
                        border: 1,
                        borderRadius: 1,
                        cursor: "pointer",
                        borderColor: polish === v ? "primary.main" : "divider",
                      }}
                    >
                      <Typography variant="body2">{v ? "是" : "否"}</Typography>
                    </Box>
                  ))}
                </Stack>
              </Box>
              <Typography variant="body2" sx={{ mb: 0.5 }}>
                提示词
              </Typography>
              <TextArea
                value={prompt}
                onChange={(e) => setPrompt(e.target.value)}
                rows={10}
                placeholder="请输入报告主题或要点"
                sx={{ flex: 1 }}
              />
              <Box sx={{ pt: 2 }}>
                <Button
                  variant="contained"
                  sx={{ width: "100%" }}
                  disabled={!prompt.trim() || generating}
                  startIcon={<RunIcon />}
                  onClick={() => void generate()}
                >
                  {generating ? "生成中..." : "生成报告"}
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
          setTpls((prev) => prev.filter((t) => t.id !== deleting!.id));
          setDeleting(null);
          toast.success("已删除");
        }}
        title="删除报告模板"
        description={`确认删除 "${deleting?.name}"？`}
        confirmColor="danger"
      />
    </PageStack>
  );
}

export default AiReports;
