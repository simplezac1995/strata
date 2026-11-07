// SPDX-License-Identifier: Apache-2.0
import { Box, IconButton, Paper, Stack, Typography, Chip } from "@mui/material";
import AddIcon from "@mui/icons-material/Add";
import DeleteIcon from "@mui/icons-material/Delete";
import AttachFileIcon from "@mui/icons-material/AttachFile";
import SearchIcon from "@mui/icons-material/Search";
import RefreshIcon from "@mui/icons-material/Refresh";
import { Button, ConfirmDialog, Select, TextArea, TextField } from "@/components/ui/index";
import { AiBadge } from "@/components/ui/index";
import { SessionFilter } from "./aichat/part-1";
import { useAiChatState } from "./aichat/useAiChatState";

export function AiChat() {
  const {
    t,
    currentUserId,
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
    messagesEndRef,
    fileInputRef,
    uploadedFiles,
    setUploadedFiles,
    uploading,
    setUploading,
    deleteTargetId,
    setDeleteTargetId,
    search,
    setSearch,
    filter,
    setFilter,
    editingId,
    setEditingId,
    editTitle,
    setEditTitle,
    editInputRef,
    models,
    setModels,
    selectedModel,
    setSelectedModel,
    visibleSessions,
    commitRename,
    handleFileUpload,
    removeUploadedFile,
    handleSend,
    handleRetry,
  } = useAiChatState();

  return (
    <Box
      sx={{
        display: "flex",
        height: "calc(100vh - 112px)",
        overflow: "hidden",
        mx: { xs: -2.5, sm: -3, lg: -3.5 },
        my: { xs: -2.5, sm: -3, lg: -3.5 },
      }}
    >
      <Paper
        variant="outlined"
        sx={{
          width: 248,
          flexShrink: 0,
          display: "flex",
          flexDirection: "column",
          borderRadius: 0,
          borderTop: 0,
          borderBottom: 0,
          borderLeft: 0,
        }}
      >
        <Stack
          direction="row"
          spacing={1}
          sx={{ px: 1.5, py: 1.5, borderBottom: 1, borderColor: "divider", alignItems: "center" }}
        >
          <AiBadge />
          <Typography variant="body2" sx={{ flex: 1, fontWeight: 600 }}>
            {t("ai.workspace.sessions")}
          </Typography>
          <IconButton
            size="small"
            color="primary"
            aria-label={t("common.createSession")}
            disabled={!canCreate || busy}
            onClick={() => void createSession(selectedModel || null)}
          >
            <AddIcon fontSize="small" />
          </IconButton>
        </Stack>

        {/* Search + filter */}
        <Stack spacing={1} sx={{ px: 1.5, pt: 1.5, pb: 1 }}>
          <Box sx={{ position: "relative" }}>
            <SearchIcon
              fontSize="small"
              sx={{
                position: "absolute",
                left: 8,
                top: "50%",
                transform: "translateY(-50%)",
                color: "text.disabled",
                pointerEvents: "none",
              }}
            />
            <TextField
              size="small"
              fullWidth
              placeholder={t("ai.chat.searchPlaceholder")}
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              sx={{ "& .MuiOutlinedInput-root": { pl: 4 } }}
            />
          </Box>
          <Select
            size="small"
            value={filter}
            onChange={(e) => setFilter(e.target.value as SessionFilter)}
            options={[
              { value: "all", label: t("ai.chat.filterAll") },
              { value: "mine", label: t("ai.chat.filterMine") },
              { value: "shared", label: t("ai.chat.filterShared") },
            ]}
          />
        </Stack>

        <Box sx={{ flex: 1, overflowY: "auto", py: 1 }}>
          {loading && (
            <Typography variant="caption" color="text.secondary" sx={{ px: 1.5 }}>
              {t("common.loading")}
            </Typography>
          )}
          {!loading && visibleSessions.length === 0 && (
            <Typography variant="caption" color="text.secondary" sx={{ px: 1.5 }}>
              {sessions.length === 0 ? t("ai.common.noSessionsCreateOne") : t("ai.chat.noMatch")}
            </Typography>
          )}
          {visibleSessions.map((s) => (
            <Box
              key={s.id}
              onClick={() => void selectSession(s.id)}
              sx={{
                px: 1.5,
                py: 1.25,
                cursor: "pointer",
                bgcolor: s.id === activeId ? "primary.main" : "transparent",
                color: s.id === activeId ? "primary.contrastText" : "text.secondary",
                display: "flex",
                alignItems: "center",
                "&:hover": {
                  bgcolor: s.id === activeId ? "primary.main" : "action.hover",
                  "& .delete-btn": { opacity: 1 },
                },
              }}
            >
              <Box sx={{ flex: 1, minWidth: 0 }}>
                {editingId === s.id ? (
                  <TextField
                    inputRef={editInputRef}
                    size="small"
                    fullWidth
                    value={editTitle}
                    autoFocus
                    onChange={(e) => setEditTitle(e.target.value)}
                    onClick={(e) => e.stopPropagation()}
                    onBlur={() => void commitRename()}
                    onKeyDown={(e) => {
                      if (e.key === "Enter") {
                        e.preventDefault();
                        void commitRename();
                      } else if (e.key === "Escape") {
                        setEditingId(null);
                        setEditTitle("");
                      }
                    }}
                    sx={{ "& .MuiOutlinedInput-input": { py: 0.5, fontSize: 14 } }}
                  />
                ) : (
                  <Typography
                    variant="body2"
                    noWrap
                    sx={{ fontWeight: 500 }}
                    onDoubleClick={(e) => {
                      e.stopPropagation();
                      setEditingId(s.id);
                      setEditTitle(s.title);
                    }}
                  >
                    {s.title}
                  </Typography>
                )}
                <Typography variant="caption" sx={{ opacity: 0.7, mt: 0.25, display: "block" }}>
                  {s.createdAt}
                </Typography>
              </Box>
              <IconButton
                size="small"
                className="delete-btn"
                aria-label={t("common.delete")}
                sx={{
                  opacity: 0.5,
                  transition: "opacity 0.2s",
                  color: s.id === activeId ? "inherit" : "text.secondary",
                  "&:hover": { color: "error.main", opacity: 1 },
                }}
                onClick={(e) => {
                  e.stopPropagation();
                  setDeleteTargetId(s.id);
                }}
              >
                <DeleteIcon fontSize="small" />
              </IconButton>
            </Box>
          ))}
        </Box>
      </Paper>

      <Box
        sx={{ flex: 1, display: "flex", flexDirection: "column", bgcolor: "background.default" }}
      >
        <Box
          sx={{
            px: 2.5,
            py: 1.5,
            borderBottom: 1,
            borderColor: "divider",
            bgcolor: "background.paper",
            display: "flex",
            alignItems: "center",
            gap: 1.5,
          }}
        >
          <Typography variant="body2" color="text.secondary" sx={{ flex: 1 }}>
            {activeId
              ? t("ai.workspace.openaiCompatibleGatewayConfigureAiProvidersModels")
              : t("ai.common.selectOrCreateASession")}
          </Typography>
          <Select
            size="small"
            value={selectedModel}
            onChange={async (e) => {
              const mid = e.target.value;
              setSelectedModel(mid);
              if (canCreate) {
                await createSession(mid);
              }
            }}
            options={models.map((m) => ({ value: m.id, label: m.name || m.code }))}
            sx={{ minWidth: 180 }}
          />
        </Box>

        <Stack spacing={2.5} sx={{ flex: 1, overflowY: "auto", px: 2.5, py: 2.5 }}>
          {messages.map((m) => (
            <Stack
              key={m.id}
              direction="row"
              spacing={1}
              sx={{
                alignItems: "flex-start",
                justifyContent: m.role === "user" ? "flex-end" : "flex-start",
              }}
            >
              {m.role === "assistant" && (
                <Box
                  sx={{
                    width: 28,
                    height: 28,
                    borderRadius: "50%",
                    bgcolor: "primary.main",
                    color: "primary.contrastText",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    fontSize: 12,
                    flexShrink: 0,
                  }}
                >
                  AI
                </Box>
              )}
              <Paper
                variant="outlined"
                sx={{
                  maxWidth: 576,
                  px: 2,
                  py: 1.5,
                  borderRadius: 3,
                  bgcolor: m.role === "user" ? "primary.main" : "background.paper",
                  color: m.role === "user" ? "primary.contrastText" : "text.primary",
                  borderTopRightRadius: m.role === "user" ? 4 : undefined,
                  borderTopLeftRadius: m.role === "assistant" ? 4 : undefined,
                  border: m.role === "user" ? 0 : undefined,
                }}
              >
                <Typography variant="body2" sx={{ whiteSpace: "pre-wrap", lineHeight: 1.6 }}>
                  {m.content}
                </Typography>
              </Paper>
              {m.role === "assistant" && messages[messages.length - 1]?.id === m.id && !busy && (
                <IconButton
                  size="small"
                  onClick={() => void handleRetry()}
                  title={t("ai.assistant.retry")}
                  sx={{ mt: 0.5 }}
                >
                  <RefreshIcon fontSize="small" />
                </IconButton>
              )}
            </Stack>
          ))}
          <div ref={messagesEndRef} />
        </Stack>

        <Box
          sx={{
            px: 2.5,
            py: 2,
            borderTop: 1,
            borderColor: "divider",
            bgcolor: "background.paper",
          }}
        >
          {/* 已上传文件列表 */}
          {uploadedFiles.length > 0 && (
            <Stack direction="row" spacing={1} sx={{ mb: 1.5, flexWrap: "wrap", gap: 1 }}>
              {uploadedFiles.map((file, index) => (
                <Chip
                  key={`${file.bucket}-${file.key}`}
                  label={file.key.split("/").pop()}
                  size="small"
                  onDelete={() => removeUploadedFile(index)}
                  sx={{ maxWidth: 200 }}
                />
              ))}
            </Stack>
          )}

          <Stack direction="row" spacing={1} sx={{ alignItems: "flex-end" }}>
            {/* 上传按钮 */}
            <input
              ref={fileInputRef}
              type="file"
              multiple
              accept="image/*,.pdf,.doc,.docx,.txt,.md,.csv,.xlsx,.xls"
              style={{ display: "none" }}
              onChange={(e) => void handleFileUpload(e)}
            />
            <IconButton
              color="primary"
              aria-label={t("ai.assistant.attachFiles")}
              disabled={!activeId || busy || uploading}
              onClick={() => fileInputRef.current?.click()}
              sx={{ mb: 0.5 }}
            >
              <AttachFileIcon />
            </IconButton>

            <TextArea
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder={t("ai.workspace.askAiToHelpWithSystemAdministrationTasks")}
              rows={2}
              disabled={!activeId || busy}
              onKeyDown={(e) => {
                if (e.key === "Enter" && !e.shiftKey) {
                  e.preventDefault();
                  void handleSend();
                }
              }}
              sx={{ flex: 1 }}
            />
            <Button
              variant="contained"
              onClick={() => void handleSend()}
              disabled={
                (!input.trim() && uploadedFiles.length === 0) || !activeId || busy || !canCreate
              }
            >
              {uploading ? t("ai.assistant.uploadingFile") : t("ai.workspace.send")}
            </Button>
          </Stack>
        </Box>
      </Box>

      <ConfirmDialog
        open={deleteTargetId !== null}
        onClose={() => setDeleteTargetId(null)}
        onConfirm={() => {
          if (deleteTargetId) void deleteSession(deleteTargetId);
          setDeleteTargetId(null);
        }}
        title="ai.workspace.deleteSessionTitle"
        description="ai.workspace.deleteSessionConfirm"
        confirmText="common.delete"
      />
    </Box>
  );
}

export default AiChat;
