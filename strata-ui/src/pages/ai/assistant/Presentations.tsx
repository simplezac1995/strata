// SPDX-License-Identifier: Apache-2.0
import { useState } from "react";
import {
  Add as AddIcon,
  Search as SearchIcon,
  Refresh as RefreshIcon,
  Edit as EditIcon,
  ContentCopy as CopyIcon,
  Download as DownloadIcon,
  DeleteOutlined as DeleteIcon,
  CheckCircle as ReadyIcon,
  ArrowBack as BackIcon,
  Slideshow as DeckIcon,
} from "@mui/icons-material";
import { Box, Chip, IconButton, Paper, Stack, Typography } from "@mui/material";
import { Button, ConfirmDialog, Dialog, Tabs, TextField } from "@/components/ui/index";
import { PageStack } from "@/shared/pageKit/PageLayout";
import { aiToolApi } from "@/shared/api/ai";
import { toast } from "@/shared/pageKit/Toast";

interface Deck {
  id: string;
  title: string;
  updatedAt: string;
  status: "draft" | "editable";
  pages: number;
  cover: string;
}

const TABS = [
  { id: "all", label: "全部" },
  { id: "draft", label: "草稿" },
  { id: "editable", label: "可编辑" },
];

const COVER_STYLES: Record<string, { bg: string; title: string; color: string }> = {
  blue: { bg: "linear-gradient(135deg,#3b82f6,#1e40af)", title: "商务蓝", color: "#fff" },
  red: { bg: "linear-gradient(135deg,#ef4444,#7f1d1d)", title: "政务红", color: "#fff" },
  slate: { bg: "linear-gradient(135deg,#93c5fd,#bfdbfe)", title: "述职蓝", color: "#1e3a8a" },
};

export function Presentations() {
  const [decks, setDecks] = useState<Deck[]>([]);
  const [keyword, setKeyword] = useState("");
  const [tab, setTab] = useState("all");

  const [createOpen, setCreateOpen] = useState(false);
  const [topic, setTopic] = useState("");
  const [creating, setCreating] = useState(false);

  const [editing, setEditing] = useState<Deck | null>(null);
  const [deleting, setDeleting] = useState<Deck | null>(null);

  const filtered = decks.filter(
    (d) =>
      (!keyword || d.title.toLowerCase().includes(keyword.toLowerCase())) &&
      (tab === "all" || d.status === tab),
  );

  async function createDeck() {
    const t = topic.trim();
    if (!t) {
      toast.error("请输入演示主题");
      return;
    }
    setCreating(true);
    try {
      await aiToolApi.run("outline", t);
      const id = `deck-${Date.now()}`;
      setDecks((prev) => [
        ...prev,
        {
          id,
          title: t,
          updatedAt: new Date().toISOString().replace("T", " ").slice(0, 19),
          status: "editable",
          pages: 12,
          cover: ["blue", "red", "slate"][prev.length % 3],
        },
      ]);
      setCreateOpen(false);
      setTopic("");
      toast.success("演示文稿已生成");
    } catch (e) {
      toast.error(`生成失败: ${e instanceof Error ? e.message : String(e)}`);
    } finally {
      setCreating(false);
    }
  }

  return (
    <>
      <PageStack>
        <Stack direction="row" sx={{ alignItems: "center", justifyContent: "space-between" }}>
          <Stack direction="row" spacing={1.5} sx={{ alignItems: "center" }}>
            <Typography variant="h5" sx={{ fontWeight: 700 }}>
              AI 演示文稿
            </Typography>
            <Chip label="演示模式" size="small" color="warning" variant="outlined" />
            <Typography variant="body2" sx={{ color: "text.secondary" }}>
              共 {decks.length} 份
            </Typography>
          </Stack>
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setCreateOpen(true)}>
            新建演示文稿
          </Button>
        </Stack>
        <Typography variant="body2" sx={{ color: "text.secondary" }}>
          示例项目仅在当前浏览器会话保存，不会写入业务数据库。
        </Typography>

        {/* search + tabs */}
        <Box sx={{ display: "flex", gap: 1.5, alignItems: "center" }}>
          <TextField
            placeholder="搜索演示文稿标题"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            sx={{ flex: 1, maxWidth: 360 }}
            slotProps={{
              input: {
                startAdornment: (
                  <SearchIcon fontSize="small" sx={{ mr: 1, color: "text.secondary" }} />
                ),
              },
            }}
          />
          <Tabs tabs={TABS} active={tab} onChange={setTab} />
          <Box sx={{ flex: 1 }} />
          <Button variant="outlined" color="default" startIcon={<RefreshIcon />}>
            刷新
          </Button>
        </Box>

        {/* card grid */}
        {filtered.length === 0 ? (
          <Paper variant="outlined" sx={{ p: 8, textAlign: "center", color: "text.secondary" }}>
            <DeckIcon sx={{ fontSize: 48, color: "text.disabled", mb: 1 }} />
            <Typography variant="body2">暂无演示文稿，点击「新建演示文稿」开始</Typography>
          </Paper>
        ) : (
          <Box
            sx={{
              display: "grid",
              gridTemplateColumns: { xs: "1fr", sm: "repeat(2, 1fr)", lg: "repeat(3, 1fr)" },
              gap: 2,
            }}
          >
            {filtered.map((d) => {
              const cover = COVER_STYLES[d.cover] ?? COVER_STYLES.blue;
              return (
                <Paper key={d.id} variant="outlined" sx={{ borderRadius: 2, overflow: "hidden" }}>
                  <Box
                    sx={{
                      position: "relative",
                      height: 160,
                      background: cover.bg,
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "center",
                    }}
                  >
                    <Typography
                      sx={{ color: cover.color, fontWeight: 700, fontSize: 22, letterSpacing: 2 }}
                    >
                      {cover.title}
                    </Typography>
                    <Stack
                      direction="row"
                      spacing={0.5}
                      sx={{
                        position: "absolute",
                        inset: 0,
                        alignItems: "center",
                        justifyContent: "center",
                        gap: 1,
                        opacity: 0,
                        transition: "opacity 0.2s",
                        bgcolor: "rgba(0,0,0,0.25)",
                        ".MuiPaper-root:hover &": { opacity: 1 },
                      }}
                    >
                      <Button
                        size="small"
                        variant="contained"
                        startIcon={<EditIcon />}
                        onClick={() => setEditing(d)}
                      >
                        编辑
                      </Button>
                      <IconButton sx={{ bgcolor: "background.paper" }}>
                        <CopyIcon fontSize="small" />
                      </IconButton>
                      <IconButton sx={{ bgcolor: "background.paper" }}>
                        <DownloadIcon fontSize="small" />
                      </IconButton>
                      <IconButton
                        sx={{ bgcolor: "background.paper" }}
                        onClick={() => setDeleting(d)}
                      >
                        <DeleteIcon fontSize="small" sx={{ color: "error.main" }} />
                      </IconButton>
                    </Stack>
                  </Box>
                  <Box sx={{ p: 1.5 }}>
                    <Stack
                      direction="row"
                      sx={{ alignItems: "center", justifyContent: "space-between" }}
                    >
                      <Typography variant="body2" sx={{ fontWeight: 600 }} noWrap>
                        {d.title}
                      </Typography>
                      <Chip
                        label={d.status === "editable" ? "可编辑" : "草稿"}
                        size="small"
                        color={d.status === "editable" ? "success" : "warning"}
                        variant="outlined"
                      />
                    </Stack>
                    <Typography variant="caption" sx={{ color: "text.secondary" }}>
                      {d.updatedAt}
                    </Typography>
                    <Typography
                      variant="caption"
                      sx={{ color: "text.secondary", display: "block", mt: 0.5 }}
                    >
                      参考正文 {d.pages} 页 | 演示用户
                    </Typography>
                  </Box>
                </Paper>
              );
            })}
          </Box>
        )}
      </PageStack>

      {/* Create dialog */}
      <Dialog
        open={createOpen}
        onClose={() => setCreateOpen(false)}
        title="新建演示文稿"
        maxWidth="sm"
      >
        <TextField
          label="演示主题"
          value={topic}
          onChange={(e) => setTopic(e.target.value)}
          placeholder="例如：Q4 产品路线图"
          sx={{ mb: 2 }}
        />
        <Box sx={{ display: "flex", justifyContent: "flex-end", gap: 1 }}>
          <Button variant="outlined" color="default" onClick={() => setCreateOpen(false)}>
            取消
          </Button>
          <Button
            variant="contained"
            disabled={creating || !topic.trim()}
            onClick={() => void createDeck()}
          >
            {creating ? "生成中..." : "生成大纲"}
          </Button>
        </Box>
      </Dialog>

      {/* Ready / editing page */}
      {editing && (
        <Box
          sx={{ position: "fixed", inset: 0, zIndex: 1300, bgcolor: "background.default", p: 3 }}
        >
          <Stack
            direction="row"
            sx={{ alignItems: "center", justifyContent: "space-between", mb: 2 }}
          >
            <Stack direction="row" spacing={1.5} sx={{ alignItems: "center" }}>
              <IconButton onClick={() => setEditing(null)}>
                <BackIcon />
              </IconButton>
              <Typography variant="h6">继续创作演示文稿</Typography>
              <Typography variant="caption" sx={{ color: "text.secondary" }}>
                project id: {editing.id}
              </Typography>
            </Stack>
            <Chip label="演示模式" size="small" color="warning" variant="outlined" />
          </Stack>
          <Box
            sx={{
              display: "flex",
              flexDirection: "column",
              alignItems: "center",
              justifyContent: "center",
              height: "70%",
              textAlign: "center",
            }}
          >
            <ReadyIcon sx={{ fontSize: 64, color: "success.main", mb: 2 }} />
            <Typography variant="h5" sx={{ fontWeight: 700, mb: 1 }}>
              可视化页面已就绪
            </Typography>
            <Typography variant="body2" sx={{ color: "text.secondary", mb: 3 }}>
              「{editing.title}」共 {editing.pages} 页，可直接下载 PPTX 或继续编辑需求
            </Typography>
            <Button
              variant="contained"
              size="large"
              startIcon={<DownloadIcon />}
              onClick={() => toast.success("已开始下载 PPTX")}
            >
              生成并下载 PPTX
            </Button>
          </Box>
        </Box>
      )}

      <ConfirmDialog
        open={deleting !== null}
        onClose={() => setDeleting(null)}
        onConfirm={() => {
          setDecks((prev) => prev.filter((d) => d.id !== deleting!.id));
          setDeleting(null);
          toast.success("已删除");
        }}
        title="删除演示文稿"
        description={`确认删除 "${deleting?.title}" 吗？删除后无法恢复。`}
        confirmColor="danger"
      />
    </>
  );
}

export default Presentations;
