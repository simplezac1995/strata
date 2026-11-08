// SPDX-License-Identifier: Apache-2.0
import {
  Avatar,
  Box,
  Divider,
  IconButton,
  Menu,
  MenuItem,
  Paper,
  Stack,
  Tab,
  Tabs as MuiTabs,
  Tooltip,
  Typography,
} from "@mui/material";
import {
  Add,
  ContentCopy,
  Link as LinkIcon,
  MoreHoriz,
  Person,
  Psychology,
  Settings,
  Upload,
  AccountTree,
} from "@mui/icons-material";
import {
  Button,
  Chip,
  ConfirmDialog,
  Dialog,
  Pagination,
  TextArea,
  TextField,
} from "@/components/ui/index";
import { PageStack } from "@/shared/pageKit/PageLayout";
import { toast } from "@/shared/pageKit/Toast";
import { ShareTab, AVATAR_COLORS } from "./agents/part-1";
import { useAgents } from "./agents/useAgents";

export function Agents() {
  const {
    keyword,
    setKeyword,
    menuAnchor,
    setMenuAnchor,
    formOpen,
    setFormOpen,
    editing,
    setEditing,
    name,
    setName,
    desc,
    setDesc,
    secret,
    setSecret,
    saving,
    setSaving,
    shareOpen,
    setShareOpen,
    shareAgent,
    setShareAgent,
    shareTab,
    setShareTab,
    deleteTarget,
    setDeleteTarget,
    rows,
    total,
    page,
    setPage,
    pageSize,
    setPageSize,
    loading,
    reload,
    openCreate,
    openEdit,
    handleSave,
    handleDelete,
    openShare,
    shareUrl,
    iframeCode,
    scriptCode,
  } = useAgents();

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
        <Typography variant="body2" sx={{ color: "text.secondary" }}>
          业务名称
        </Typography>
        <TextField
          size="small"
          placeholder="请输入名称搜索"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          sx={{ width: 220 }}
        />
        <Button variant="contained" size="small" onClick={() => reload()}>
          查询
        </Button>
        <Button variant="outlined" size="small" color="default" onClick={() => setKeyword("")}>
          重置
        </Button>
      </Box>

      {/* Toolbar */}
      <Box sx={{ display: "flex", gap: 1 }}>
        <Button
          variant="contained"
          size="small"
          startIcon={<Add sx={{ fontSize: 14 }} />}
          onClick={openCreate}
        >
          新增
        </Button>
        <Button
          variant="outlined"
          size="small"
          color="default"
          startIcon={<Upload sx={{ fontSize: 14 }} />}
        >
          导入
        </Button>
      </Box>

      {/* Card grid */}
      {loading ? (
        <Typography variant="body2" sx={{ color: "text.secondary" }}>
          加载中…
        </Typography>
      ) : rows.length === 0 ? (
        <Paper sx={{ p: 6, textAlign: "center" }}>
          <Psychology sx={{ fontSize: 40, color: "text.disabled", mb: 1 }} />
          <Typography variant="body2" sx={{ color: "text.secondary" }}>
            暂无智能体，点击「新增」创建第一个编排
          </Typography>
        </Paper>
      ) : (
        <Box
          sx={{
            display: "grid",
            gridTemplateColumns: "repeat(auto-fill, minmax(280px, 1fr))",
            gap: 2,
          }}
        >
          {rows.map((a, idx) => (
            <Paper key={a.id} sx={{ p: 2, display: "flex", flexDirection: "column" }}>
              <Box sx={{ display: "flex", alignItems: "flex-start", gap: 1.5 }}>
                <Avatar
                  sx={{
                    width: 44,
                    height: 44,
                    bgcolor: AVATAR_COLORS[idx % AVATAR_COLORS.length],
                    fontSize: 20,
                    fontWeight: 700,
                  }}
                >
                  {a.name.slice(0, 1)}
                </Avatar>
                <Box sx={{ flex: 1, minWidth: 0 }}>
                  <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
                    <Typography variant="subtitle1" sx={{ fontWeight: 600, lineHeight: 1.3 }}>
                      {a.name}
                    </Typography>
                    <Chip label="高级编排" size="small" color="primary" variant="outlined" />
                  </Box>
                  <Box sx={{ display: "flex", alignItems: "center", gap: 0.5, mt: 0.5 }}>
                    <Person sx={{ fontSize: 14, color: "text.disabled" }} />
                    <Typography variant="caption" sx={{ color: "text.disabled" }}>
                      aitest
                    </Typography>
                  </Box>
                </Box>
              </Box>

              <Typography
                variant="body2"
                sx={{
                  color: "text.secondary",
                  mt: 1.5,
                  minHeight: 24,
                  display: "-webkit-box",
                  WebkitLineClamp: 2,
                  WebkitBoxOrient: "vertical",
                  overflow: "hidden",
                }}
              >
                {a.prompt || "暂无描述"}
              </Typography>

              <Box sx={{ flex: 1 }} />

              <Box
                sx={{
                  display: "flex",
                  alignItems: "center",
                  gap: 0.5,
                  mt: 2,
                  pt: 1.5,
                  borderTop: 1,
                  borderColor: "divider",
                }}
              >
                <Button
                  variant="contained"
                  size="small"
                  startIcon={<AccountTree sx={{ fontSize: 14 }} />}
                  onClick={() => openEdit(a)}
                >
                  编排
                </Button>
                <Tooltip title="分享">
                  <IconButton size="small" onClick={() => openShare(a)}>
                    <LinkIcon sx={{ fontSize: 16 }} />
                  </IconButton>
                </Tooltip>
                <Tooltip title="设置">
                  <IconButton size="small" onClick={() => openEdit(a)}>
                    <Settings sx={{ fontSize: 16 }} />
                  </IconButton>
                </Tooltip>
                <IconButton
                  size="small"
                  onClick={(e) => setMenuAnchor({ agent: a, el: e.currentTarget })}
                >
                  <MoreHoriz sx={{ fontSize: 16 }} />
                </IconButton>
              </Box>
            </Paper>
          ))}
        </Box>
      )}

      <Pagination
        total={total}
        page={page}
        pageSize={pageSize}
        onPage={setPage}
        onPageSize={(s: number) => {
          setPageSize(s);
          setPage(1);
        }}
      />

      {/* More menu */}
      <Menu anchorEl={menuAnchor?.el} open={!!menuAnchor} onClose={() => setMenuAnchor(null)}>
        <MenuItem
          onClick={() => {
            if (menuAnchor) openEdit(menuAnchor.agent);
            setMenuAnchor(null);
          }}
        >
          编辑
        </MenuItem>
        <MenuItem
          onClick={() => {
            if (menuAnchor) openShare(menuAnchor.agent);
            setMenuAnchor(null);
          }}
        >
          分享
        </MenuItem>
        <MenuItem
          sx={{ color: "error.main" }}
          onClick={() => {
            if (menuAnchor) setDeleteTarget(menuAnchor.agent);
            setMenuAnchor(null);
          }}
        >
          删除
        </MenuItem>
      </Menu>

      {/* Add / edit dialog */}
      <Dialog
        open={formOpen}
        onClose={() => setFormOpen(false)}
        title={editing ? "编辑" : "新增"}
        maxWidth="sm"
      >
        <Stack spacing={2} sx={{ py: 1 }}>
          <Box>
            <Typography variant="body2" sx={{ mb: 0.5 }}>
              <Box component="span" sx={{ color: "error.main" }}>
                *{" "}
              </Box>
              编排名称
            </Typography>
            <TextField
              size="small"
              fullWidth
              placeholder="请输入编排名称"
              value={name}
              onChange={(e) => setName(e.target.value)}
            />
          </Box>
          <Box>
            <Typography variant="body2" sx={{ mb: 0.5 }}>
              编排描述
            </Typography>
            <TextArea
              rows={3}
              placeholder="描述该编排的应用场景及用途"
              value={desc}
              onChange={(e) => setDesc(e.target.value)}
            />
          </Box>
          <Box
            sx={{
              border: 1,
              borderColor: "primary.light",
              borderRadius: 2,
              p: 1.5,
              bgcolor: "action.selected",
            }}
          >
            <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
              <input type="radio" checked readOnly />
              <Box>
                <Typography variant="body2" sx={{ fontWeight: 600 }}>
                  高级编排
                </Typography>
                <Typography variant="caption" sx={{ color: "text.secondary" }}>
                  适合高级用户自定义 AI 业务流
                </Typography>
              </Box>
            </Box>
          </Box>
          <Divider textAlign="center" sx={{ "&::before, &::after": { borderColor: "divider" } }}>
            <Typography variant="caption" sx={{ color: "text.disabled" }}>
              安全控制
            </Typography>
          </Divider>
          <Box sx={{ display: "flex", gap: 2 }}>
            <Box sx={{ flex: 1 }}>
              <Typography variant="body2" sx={{ mb: 0.5 }}>
                可见范围
              </Typography>
              <Button variant="outlined" color="default" size="small" sx={{ minWidth: 64 }}>
                +
              </Button>
            </Box>
            <Box sx={{ flex: 1 }}>
              <Typography variant="body2" sx={{ mb: 0.5 }}>
                安全密钥
              </Typography>
              <TextField
                size="small"
                fullWidth
                type="password"
                placeholder="请输入密码"
                value={secret}
                onChange={(e) => setSecret(e.target.value)}
              />
            </Box>
          </Box>
        </Stack>
        <Box sx={{ display: "flex", gap: 1, justifyContent: "flex-end", mt: 2 }}>
          <Button
            variant="outlined"
            color="default"
            size="small"
            onClick={() => setFormOpen(false)}
          >
            取消
          </Button>
          <Button
            variant="contained"
            size="small"
            disabled={saving}
            onClick={() => void handleSave()}
          >
            {saving ? "保存中…" : "确认"}
          </Button>
        </Box>
      </Dialog>

      {/* Share dialog */}
      <Dialog open={shareOpen} onClose={() => setShareOpen(false)} title="分享" maxWidth="md">
        <Box sx={{ display: "grid", gridTemplateColumns: "1fr 1fr 1fr", gap: 1.5, mb: 2 }}>
          {(
            [
              { id: "link", label: "链接访问", desc: "适合直接打开流程对话" },
              { id: "iframe", label: "Iframe 嵌入", desc: "适合嵌入到现有业务页" },
              { id: "script", label: "脚本挂载", desc: "适合提供悬浮式流程入口" },
            ] as { id: ShareTab; label: string; desc: string }[]
          ).map((opt) => (
            <Paper
              key={opt.id}
              variant="outlined"
              onClick={() => setShareTab(opt.id)}
              sx={{
                p: 1.5,
                cursor: "pointer",
                borderWidth: shareTab === opt.id ? 2 : 1,
                borderColor: shareTab === opt.id ? "primary.main" : "divider",
                bgcolor: shareTab === opt.id ? "action.selected" : "background.paper",
              }}
            >
              <Typography variant="body2" sx={{ fontWeight: 600 }}>
                {opt.label}
              </Typography>
              <Typography variant="caption" sx={{ color: "text.secondary" }}>
                {opt.desc}
              </Typography>
            </Paper>
          ))}
        </Box>

        <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center", mb: 1 }}>
          <MuiTabs value={shareTab} onChange={(_, v) => setShareTab(v)} sx={{ minHeight: 32 }}>
            <Tab value="link" label="链接访问" sx={{ minHeight: 32, fontSize: 13 }} />
            <Tab value="iframe" label="Iframe" sx={{ minHeight: 32, fontSize: 13 }} />
            <Tab value="script" label="脚本" sx={{ minHeight: 32, fontSize: 13 }} />
          </MuiTabs>
          <Box sx={{ display: "flex", gap: 1 }}>
            {shareTab === "link" && (
              <Button
                variant="outlined"
                color="default"
                size="small"
                onClick={() => window.open(shareUrl, "_blank")}
              >
                打开
              </Button>
            )}
            <Button
              variant="contained"
              size="small"
              startIcon={<ContentCopy sx={{ fontSize: 14 }} />}
              onClick={() => {
                const code =
                  shareTab === "link" ? shareUrl : shareTab === "iframe" ? iframeCode : scriptCode;
                void navigator.clipboard?.writeText(code).then(() => toast.success("已复制"));
              }}
            >
              {shareTab === "link" ? "复制链接" : "复制代码"}
            </Button>
          </Box>
        </Box>

        <Box
          component="pre"
          sx={{
            m: 0,
            p: 2,
            borderRadius: 1.5,
            bgcolor: "grey.900",
            color: "grey.100",
            fontFamily: "'JetBrains Mono', Consolas, monospace",
            fontSize: 12.5,
            whiteSpace: "pre-wrap",
            wordBreak: "break-all",
            minHeight: 64,
          }}
        >
          {shareTab === "link" ? shareUrl : shareTab === "iframe" ? iframeCode : scriptCode}
        </Box>
      </Dialog>

      {/* Delete confirm */}
      <ConfirmDialog
        open={deleteTarget !== null}
        onClose={() => setDeleteTarget(null)}
        onConfirm={() => void handleDelete()}
        title="common.delete"
        description="common.deleteNamed"
      />
    </PageStack>
  );
}

export default Agents;
