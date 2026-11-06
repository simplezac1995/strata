// SPDX-License-Identifier: Apache-2.0
import AddIcon from "@mui/icons-material/Add";
import DeleteIcon from "@mui/icons-material/Delete";
import AttachFileIcon from "@mui/icons-material/AttachFile";
import SmartToyOutlinedIcon from "@mui/icons-material/SmartToyOutlined";
import SendIcon from "@mui/icons-material/Send";
import ImageIcon from "@mui/icons-material/Image";
import MicIcon from "@mui/icons-material/Mic";
import MoreHorizIcon from "@mui/icons-material/MoreHoriz";
import SearchIcon from "@mui/icons-material/Search";
import {
  Avatar,
  Box,
  Chip,
  IconButton,
  Menu,
  MenuItem,
  Paper,
  Stack,
  Tooltip,
  Typography,
} from "@mui/material";
import { Button, ConfirmDialog, Select, TextField } from "@/components/ui/index";
import { AssistantContent, formatTime } from "./workspacechat/part-1";
import { useWorkspaceChat } from "./workspacechat/useWorkspaceChat";

export function WorkspaceChat() {
  const {
    t,
    model,
    setModel,
    models,
    setModels,
    sessionSearch,
    setSessionSearch,
    menuAnchor,
    setMenuAnchor,
    attachments,
    setAttachments,
    uploading,
    setUploading,
    fileInputRef,
    deleteTargetId,
    setDeleteTargetId,
    clearing,
    setClearing,
    activeId,
    busy,
    canCreate,
    createSession,
    deleteSession,
    input,
    loading,
    messages,
    selectSession,
    send,
    sessions,
    setInput,
    visibleSessions,
    handlePickFiles,
    handleSend,
    clearAllSessions,
  } = useWorkspaceChat();

  return (
    <Box sx={{ display: "flex", height: "calc(100vh - 112px)", overflow: "hidden" }}>
      {/* Left: new chat + session list */}
      <Box
        sx={{
          width: 300,
          flexShrink: 0,
          borderRight: 1,
          borderColor: "divider",
          bgcolor: "background.paper",
          display: "flex",
          flexDirection: "column",
        }}
      >
        <Box sx={{ p: 1.5, borderBottom: 1, borderColor: "divider" }}>
          <Box sx={{ display: "flex", gap: 1 }}>
            <Button
              variant="contained"
              fullWidth
              disabled={!canCreate || busy}
              onClick={() => void createSession(model || null)}
              startIcon={<AddIcon sx={{ fontSize: 18 }} />}
              sx={{ justifyContent: "flex-start", pl: 2 }}
            >
              新建聊天
            </Button>
            <Tooltip title="更多">
              <IconButton
                size="small"
                onClick={(e) => setMenuAnchor(e.currentTarget)}
                disabled={sessions.length === 0}
              >
                <MoreHorizIcon fontSize="small" />
              </IconButton>
            </Tooltip>
          </Box>
          <TextField
            size="small"
            placeholder="搜索会话"
            value={sessionSearch}
            onChange={(e) => setSessionSearch(e.target.value)}
            sx={{ mt: 1.5, "& fieldset": { border: "none" } }}
            slotProps={{
              input: {
                startAdornment: <SearchIcon sx={{ fontSize: 16, color: "text.disabled", mr: 1 }} />,
              },
            }}
          />
        </Box>

        <Box sx={{ flex: 1, overflowY: "auto", p: 1 }}>
          {visibleSessions.length === 0 ? (
            <Typography variant="body2" sx={{ color: "text.disabled", textAlign: "center", pt: 4 }}>
              {loading ? "加载中…" : "暂无会话"}
            </Typography>
          ) : (
            visibleSessions.map((s) => {
              const active = s.id === activeId;
              return (
                <Paper
                  key={s.id}
                  variant="outlined"
                  onClick={() => void selectSession(s.id)}
                  sx={{
                    p: 1.5,
                    mb: 1,
                    cursor: "pointer",
                    borderRadius: 2,
                    borderColor: active ? "primary.main" : "divider",
                    bgcolor: active ? "action.selected" : "background.paper",
                    borderWidth: active ? 2 : 1,
                    "&:hover": { bgcolor: "action.hover" },
                  }}
                >
                  <Typography variant="body2" sx={{ fontWeight: active ? 600 : 500 }} noWrap>
                    {s.title}
                  </Typography>
                  <Typography variant="caption" sx={{ color: "text.disabled" }}>
                    {formatTime(s.createdAt)}
                  </Typography>
                </Paper>
              );
            })
          )}
        </Box>
      </Box>

      {/* Main chat area */}
      <Box
        sx={{
          flex: 1,
          display: "flex",
          flexDirection: "column",
          bgcolor: (theme) => (theme.palette.mode === "dark" ? "grey.900" : "grey.50"),
        }}
      >
        <Box
          sx={{
            display: "flex",
            alignItems: "center",
            justifyContent: "flex-end",
            gap: 1.5,
            px: 3,
            py: 1.25,
            borderBottom: 1,
            borderColor: "divider",
            bgcolor: "background.paper",
          }}
        >
          <Select
            value={model}
            onChange={(e) => setModel(e.target.value)}
            options={models
              .filter((m) => m.enabled && m.type === "chat")
              .map((m) => ({ value: m.id, label: m.name || m.code }))}
            sx={{ minWidth: 180 }}
          />
          <Tooltip title="删除当前会话">
            <span>
              <IconButton
                size="small"
                onClick={() => activeId && setDeleteTargetId(activeId)}
                disabled={!activeId}
              >
                <DeleteIcon fontSize="small" />
              </IconButton>
            </span>
          </Tooltip>
        </Box>

        <Box sx={{ flex: 1, overflowY: "auto", px: 4, py: 3 }}>
          <Box sx={{ maxWidth: 900, mx: "auto" }}>
            {messages.length === 0 ? (
              <Box sx={{ display: "flex", flexDirection: "column", alignItems: "center", py: 6 }}>
                <Avatar sx={{ width: 64, height: 64, bgcolor: "primary.main", mb: 2 }}>
                  <SmartToyOutlinedIcon sx={{ width: 36, height: 36 }} />
                </Avatar>
                <Typography variant="h6" sx={{ mb: 0.5, fontWeight: 600 }}>
                  AI 助手
                </Typography>
                <Typography variant="body2" sx={{ color: "text.secondary", mb: 2 }}>
                  你好，我可以帮你管理后台任务、查询数据、回答问题
                </Typography>
              </Box>
            ) : (
              <Stack spacing={2.5}>
                {messages.map((m) => (
                  <Box
                    key={m.id}
                    sx={{
                      display: "flex",
                      gap: 1.5,
                      justifyContent: m.role === "user" ? "flex-end" : "flex-start",
                    }}
                  >
                    {m.role === "assistant" ? (
                      <Avatar sx={{ width: 32, height: 32, bgcolor: "primary.main" }}>
                        <SmartToyOutlinedIcon sx={{ fontSize: 18 }} />
                      </Avatar>
                    ) : null}
                    <Paper
                      variant="outlined"
                      sx={{
                        maxWidth: 640,
                        px: 2.25,
                        py: 1.5,
                        borderRadius: 2.5,
                        ...(m.role === "user"
                          ? {
                              bgcolor: "primary.main",
                              color: "primary.contrastText",
                              border: "none",
                            }
                          : { bgcolor: "background.paper" }),
                      }}
                    >
                      <AssistantContent
                        content={m.content}
                        onConfirm={(tool, args) =>
                          void send([], `[CONFIRM_APPROVED]|${tool}|${args}`)
                        }
                      />
                    </Paper>
                  </Box>
                ))}
              </Stack>
            )}
          </Box>
        </Box>

        {/* Input toolbar */}
        <Box sx={{ px: 4, pb: 2, pt: 1 }}>
          <Paper variant="outlined" sx={{ p: 1.5, borderRadius: 3, maxWidth: 900, mx: "auto" }}>
            <input
              ref={fileInputRef}
              type="file"
              multiple
              hidden
              onChange={(e) => void handlePickFiles(e)}
            />
            {attachments.length > 0 && (
              <Box sx={{ display: "flex", gap: 1, flexWrap: "wrap", mb: 1 }}>
                {attachments.map((a, i) => (
                  <Chip
                    key={i}
                    label={a.fileName}
                    size="small"
                    onDelete={() => setAttachments((p) => p.filter((_, j) => j !== i))}
                  />
                ))}
              </Box>
            )}
            <TextField
              multiline
              minRows={1}
              maxRows={4}
              fullWidth
              placeholder="已激活 1 个技能，请输入您的任务"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              disabled={!activeId || busy}
              onKeyDown={(e) => {
                if (e.key === "Enter" && !e.shiftKey) {
                  e.preventDefault();
                  void handleSend();
                }
              }}
              sx={{ "& fieldset": { border: "none" }, flex: 1 }}
            />
            <Box sx={{ display: "flex", alignItems: "center", gap: 0.5, mt: 0.5, pl: 0.5 }}>
              <Tooltip title="上传文件">
                <IconButton
                  size="small"
                  onClick={() => fileInputRef.current?.click()}
                  disabled={!activeId || busy || uploading}
                >
                  <AttachFileIcon fontSize="small" />
                </IconButton>
              </Tooltip>
              <Tooltip title="上传图片">
                <span>
                  <IconButton size="small" disabled={!activeId || busy}>
                    <ImageIcon fontSize="small" />
                  </IconButton>
                </span>
              </Tooltip>
              <IconButton size="small" disabled={!activeId || busy}>
                <AddIcon fontSize="small" />
              </IconButton>
              <Box sx={{ flex: 1 }} />
              <Tooltip title="语音输入">
                <span>
                  <IconButton size="small" disabled={!activeId || busy}>
                    <MicIcon fontSize="small" />
                  </IconButton>
                </span>
              </Tooltip>
              <IconButton
                color="primary"
                onClick={() => void handleSend()}
                disabled={(!input.trim() && attachments.length === 0) || !activeId || busy}
                sx={{
                  bgcolor: "primary.main",
                  color: "primary.contrastText",
                  "&:hover": { bgcolor: "primary.dark" },
                  "&.Mui-disabled": { bgcolor: "action.disabledBackground" },
                }}
              >
                <SendIcon />
              </IconButton>
            </Box>
          </Paper>
          <Typography
            variant="caption"
            sx={{ color: "text.disabled", display: "block", textAlign: "center", mt: 1 }}
          >
            AI 可能犯错，请核实重要操作
          </Typography>
        </Box>
      </Box>

      {/* Session menu */}
      <Menu anchorEl={menuAnchor} open={!!menuAnchor} onClose={() => setMenuAnchor(null)}>
        <MenuItem onClick={() => void clearAllSessions()} disabled={clearing}>
          {clearing ? "清空中…" : "清空所有记录"}
        </MenuItem>
      </Menu>

      <ConfirmDialog
        open={deleteTargetId !== null}
        onClose={() => setDeleteTargetId(null)}
        onConfirm={async () => {
          if (deleteTargetId) {
            const hadMore = sessions.filter((s) => s.id !== deleteTargetId).length > 0;
            await deleteSession(deleteTargetId);
            if (!hadMore) void createSession(model || null);
          }
          setDeleteTargetId(null);
        }}
        title="ai.workspace.deleteSessionTitle"
        description="ai.workspace.deleteSessionConfirm"
        confirmText="common.delete"
      />
    </Box>
  );
}

export default WorkspaceChat;
