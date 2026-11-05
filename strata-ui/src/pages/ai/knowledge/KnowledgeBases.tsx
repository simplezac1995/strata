// SPDX-License-Identifier: Apache-2.0
import {
  Box,
  Card,
  CardContent,
  Stack,
  Typography,
  IconButton,
  Chip,
  Drawer,
  Divider,
  Radio,
  RadioGroup,
  FormControlLabel,
} from "@mui/material";
import {
  Add,
  Close,
  DeleteOutlined,
  Edit,
  Share as ShareIcon,
  Chat,
  Refresh,
  Search,
  Book,
  Article,
  Forum,
} from "@mui/icons-material";
import {
  Button,
  Select,
  TextField,
  TextArea,
  Switch,
  Dialog,
  ConfirmDialog,
  Pagination,
} from "@/components/ui/index";
import { PageStack } from "@/shared/pageKit/PageLayout";
import {
  KnowledgeForm,
  STEPS,
  EMBEDDING_MODELS,
  ModeCard,
  SectionLabel,
} from "./knowledgebases/part-1";
import { useKnowledgeBases } from "./knowledgebases/useKnowledgeBases";

export function KnowledgeBases() {
  const {
    canCreate,
    canUpdate,
    canDelete,
    modeOpen,
    setModeOpen,
    step,
    setStep,
    keyword,
    setKeyword,
    shareKb,
    setShareKb,
    kbs,
    total,
    page,
    setPage,
    pageSize,
    setPageSize,
    reload,
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
    visible,
    pickMode,
    startEdit,
  } = useKnowledgeBases();

  return (
    <PageStack>
      {/* 工具栏：左 新增 / 右 搜索 + 刷新 */}
      <Box sx={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 2 }}>
        {canCreate ? (
          <Button variant="contained" startIcon={<Add />} onClick={() => setModeOpen(true)}>
            新增
          </Button>
        ) : undefined}
        <Stack direction="row" spacing={1} sx={{ alignItems: "center" }}>
          <TextField
            placeholder="搜索知识库"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            slotProps={{
              input: {
                startAdornment: <Search sx={{ fontSize: 18, mr: 0.5, color: "text.disabled" }} />,
              },
            }}
            sx={{ width: 220 }}
          />
          <Button variant="icon" color="default" onClick={reload} title="刷新">
            <Refresh />
          </Button>
        </Stack>
      </Box>

      {/* 知识库卡片网格 */}
      <Box
        sx={{
          display: "grid",
          gridTemplateColumns: "repeat(auto-fill, minmax(300px, 1fr))",
          gap: 2,
        }}
      >
        {visible.map((kb) => (
          <Card key={kb.id} variant="outlined" sx={{ borderRadius: 2 }}>
            <CardContent>
              <Box sx={{ display: "flex", alignItems: "flex-start", gap: 1.5 }}>
                <Box
                  sx={{
                    width: 44,
                    height: 44,
                    borderRadius: 1.5,
                    bgcolor: "primary.light",
                    color: "primary.contrastText",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    flexShrink: 0,
                  }}
                >
                  <Book />
                </Box>
                <Box sx={{ flex: 1, minWidth: 0 }}>
                  <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
                    <Typography variant="subtitle1" noWrap sx={{ fontWeight: 600 }}>
                      {kb.name}
                    </Typography>
                    <Chip
                      label={kb.enabled ? "已发布" : "未发布"}
                      size="small"
                      color={kb.enabled ? "success" : "default"}
                      variant="outlined"
                    />
                  </Box>
                  <Typography variant="caption" sx={{ color: "text.secondary" }}>
                    文档数：-
                  </Typography>
                </Box>
              </Box>
              <Typography variant="body2" sx={{ color: "text.secondary", mt: 1.5, minHeight: 40 }}>
                {kb.description || "暂无描述"}
              </Typography>
              <Stack
                direction="row"
                spacing={0.5}
                sx={{ mt: 1.5, pt: 1.5, borderTop: 1, borderColor: "divider" }}
              >
                <IconButton size="small" title="分享" onClick={() => setShareKb(kb)}>
                  <ShareIcon fontSize="small" />
                </IconButton>
                {canUpdate ? (
                  <IconButton size="small" title="编辑" onClick={() => startEdit(kb)}>
                    <Edit fontSize="small" />
                  </IconButton>
                ) : undefined}
                {canDelete ? (
                  <IconButton
                    size="small"
                    color="error"
                    title="删除"
                    onClick={() => confirmDelete(kb)}
                  >
                    <DeleteOutlined fontSize="small" />
                  </IconButton>
                ) : undefined}
                <IconButton size="small" title="对话">
                  <Chat fontSize="small" />
                </IconButton>
              </Stack>
            </CardContent>
          </Card>
        ))}
      </Box>

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

      {/* 选择知识库模式 */}
      <Dialog
        open={modeOpen}
        onClose={() => setModeOpen(false)}
        title="选择知识库模式"
        maxWidth="md"
      >
        <Typography variant="body2" sx={{ color: "text.secondary", mb: 3 }}>
          请选择知识库的工作模式，创建后不可更改
        </Typography>
        <Box sx={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 2 }}>
          <ModeCard
            icon={<Article sx={{ fontSize: 32 }} />}
            title="通用模式"
            sub="Q2A 模式"
            desc="向量化文档，适用于文档检索、知识问答等场景"
            tags={["文档检索", "知识问答", "RAG"]}
            onClick={() => pickMode("q2a")}
          />
          <ModeCard
            icon={<Forum sx={{ fontSize: 32 }} />}
            title="客服模式"
            sub="Q2Q 模式"
            desc="仅向量化问题与答案，适用于 FAQ 问答、客服咨询等场景"
            tags={["FAQ问答", "客服咨询", "精准匹配"]}
            onClick={() => pickMode("q2q")}
          />
        </Box>
      </Dialog>

      {/* 右侧分步配置抽屉 */}
      <Drawer
        anchor="right"
        open={open}
        onClose={closeDialog}
        sx={{ "& .MuiDrawer-paper": { width: { xs: "100%", sm: 680 }, maxWidth: "100%" } }}
      >
        <Box sx={{ display: "flex", flexDirection: "column", height: "100%" }}>
          <Stack
            direction="row"
            sx={{ alignItems: "center", justifyContent: "space-between", px: 3, py: 2 }}
          >
            <Typography variant="h6">{edit ? "编辑知识库" : "新增知识库"}</Typography>
            <IconButton size="small" onClick={closeDialog}>
              <Close />
            </IconButton>
          </Stack>

          {/* 步骤条 */}
          <Box sx={{ display: "flex", gap: 1, px: 3, pb: 2 }}>
            {STEPS.map((label, i) => {
              const n = i + 1;
              const active = step === n;
              return (
                <Box
                  key={label}
                  onClick={() => setStep(n)}
                  sx={{
                    flex: 1,
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    gap: 0.75,
                    py: 1,
                    borderRadius: 1,
                    cursor: "pointer",
                    bgcolor: active ? "primary.main" : "action.hover",
                    color: active ? "primary.contrastText" : "text.secondary",
                  }}
                >
                  <Typography
                    variant="caption"
                    sx={{
                      width: 18,
                      height: 18,
                      borderRadius: "50%",
                      display: "inline-flex",
                      alignItems: "center",
                      justifyContent: "center",
                      bgcolor: active ? "rgba(255,255,255,0.25)" : "primary.light",
                      color: active ? "inherit" : "primary.main",
                      fontWeight: 700,
                    }}
                  >
                    {n}
                  </Typography>
                  <Typography variant="body2" sx={{ fontWeight: active ? 600 : 400 }}>
                    {label}
                  </Typography>
                </Box>
              );
            })}
          </Box>

          <Box sx={{ flex: 1, overflowY: "auto", px: 3, py: 2 }}>
            {step === 1 ? (
              <Stack spacing={2.5}>
                <TextField
                  label="名称"
                  required
                  value={form.name}
                  onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
                  placeholder="请输入名称"
                />
                <TextArea
                  label="知识库描述"
                  value={form.description}
                  onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
                  placeholder="请输入知识库描述，帮助用户了解该知识库的内容"
                  rows={3}
                />
                <Box sx={{ display: "flex", alignItems: "center", gap: 2 }}>
                  <Typography variant="body2" sx={{ color: "text.secondary" }}>
                    知识库模式
                  </Typography>
                  <RadioGroup
                    row
                    value={form.mode}
                    onChange={(e) =>
                      setForm((f) => ({ ...f, mode: e.target.value as KnowledgeForm["mode"] }))
                    }
                  >
                    <FormControlLabel
                      value="q2a"
                      control={<Radio size="small" />}
                      label="通用模式"
                    />
                    <FormControlLabel
                      value="q2q"
                      control={<Radio size="small" />}
                      label="客服模式"
                    />
                  </RadioGroup>
                </Box>
                <TextArea
                  label="欢迎语"
                  value={form.welcome}
                  onChange={(e) => setForm((f) => ({ ...f, welcome: e.target.value }))}
                  placeholder="描述知识库的内容，详尽的描述将帮助 AI 更深入理解该知识库"
                  rows={3}
                />
              </Stack>
            ) : null}

            {step === 2 ? (
              <Stack spacing={2.5}>
                <SectionLabel text="模型配置" />
                <Box sx={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 2 }}>
                  <Select
                    label="向量库"
                    value={form.vectorStore}
                    onChange={(e) => setForm((f) => ({ ...f, vectorStore: e.target.value }))}
                    options={[
                      { value: "pgvector", label: "pgvector" },
                      { value: "milvus", label: "milvus" },
                    ]}
                  />
                  <Select
                    label="向量模型"
                    value={form.embeddingModel}
                    onChange={(e) => setForm((f) => ({ ...f, embeddingModel: e.target.value }))}
                    options={EMBEDDING_MODELS.map((m) => ({ value: m, label: m }))}
                  />
                </Box>
                <SectionLabel text="检索配置" />
                <Box sx={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 2 }}>
                  <TextField
                    label="分片值"
                    type="number"
                    value={String(form.chunkSize)}
                    onChange={(e) => setForm((f) => ({ ...f, chunkSize: Number(e.target.value) }))}
                  />
                  <TextField
                    label="重叠值"
                    type="number"
                    value={String(form.chunkOverlap)}
                    onChange={(e) =>
                      setForm((f) => ({ ...f, chunkOverlap: Number(e.target.value) }))
                    }
                  />
                </Box>
              </Stack>
            ) : null}

            {step === 3 ? (
              <Stack spacing={2.5}>
                <SectionLabel text="访问控制" />
                <Stack
                  direction="row"
                  sx={{ alignItems: "center", justifyContent: "space-between" }}
                >
                  <Typography variant="body2">知识库启用</Typography>
                  <Switch
                    checked={form.enabled}
                    onChange={(v) => setForm((f) => ({ ...f, enabled: v }))}
                  />
                </Stack>
                <Divider />
                <Typography variant="caption" sx={{ color: "text.secondary" }}>
                  其余访问控制 / 内容过滤项暂未接入后端，保持默认。
                </Typography>
              </Stack>
            ) : null}

            {step === 4 ? (
              <Stack spacing={2.5}>
                <SectionLabel text="语义设置" />
                <Typography variant="body2" sx={{ color: "text.secondary" }}>
                  知识库标签与同义词术语暂未接入后端。
                </Typography>
              </Stack>
            ) : null}
          </Box>

          <Stack
            direction="row"
            spacing={1}
            sx={{ justifyContent: "flex-end", px: 3, py: 2, borderTop: 1, borderColor: "divider" }}
          >
            <Button variant="outlined" color="default" onClick={closeDialog}>
              取消
            </Button>
            <Button variant="contained" disabled={busy} onClick={() => void handleSave()}>
              确认
            </Button>
          </Stack>
        </Box>
      </Drawer>

      <ConfirmDialog
        open={deleting !== null}
        onClose={closeDelete}
        onConfirm={() => void runDelete()}
        title="删除知识库"
        description={`确认删除 "${deleting?.name}"？`}
        confirmColor="danger"
      />

      <Dialog
        open={shareKb !== null}
        onClose={() => setShareKb(null)}
        title={`分享：${shareKb?.name ?? ""}`}
        maxWidth="sm"
      >
        <Typography variant="body2" sx={{ color: "text.secondary", mb: 2 }}>
          复制链接即可在浏览器打开或分享。
        </Typography>
        <Box
          sx={{
            bgcolor: "grey.900",
            color: "grey.100",
            p: 2,
            borderRadius: 1,
            fontFamily: "monospace",
            fontSize: 13,
            wordBreak: "break-all",
          }}
        >
          {`/bot/#/kbs/${shareKb?.id}/chat`}
        </Box>
      </Dialog>
    </PageStack>
  );
}

export default KnowledgeBases;
