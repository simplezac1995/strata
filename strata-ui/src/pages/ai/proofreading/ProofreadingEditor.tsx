// SPDX-License-Identifier: Apache-2.0
import {
  Avatar,
  Box,
  Chip,
  CircularProgress,
  Divider,
  IconButton,
  Paper,
  Stack,
  Tab,
  Tabs as MuiTabs,
  Typography,
} from "@mui/material";
import {
  AutoAwesome,
  CheckCircleOutlined as CheckCircleOutline,
  CloudUpload,
  ContentCopy,
  FormatAlignCenter,
  FormatAlignLeft,
  FormatAlignRight,
  FormatBold,
  FormatColorText,
  FormatItalic,
  FormatStrikethrough,
  FormatUnderlined,
  Highlight,
  MoreHoriz,
  Redo,
  TableChart,
  Undo,
} from "@mui/icons-material";
import { Button, Checkbox, Dialog, TextField } from "@/components/ui/index";
import { toast } from "@/shared/pageKit/Toast";
import { RULE_GROUPS, TABS } from "./proofreadingeditor/part-1";
import { useProofreadingEditor } from "./proofreadingeditor/useProofreadingEditor";

export function ProofreadingEditor() {
  const {
    originalText,
    setOriginalText,
    busy,
    setBusy,
    checked,
    setChecked,
    resultText,
    setResultText,
    checkedAt,
    setCheckedAt,
    tab,
    setTab,
    rulesOpen,
    setRulesOpen,
    enabledRules,
    setEnabledRules,
    charCount,
    selectedCount,
    runCheck,
    toggleRule,
  } = useProofreadingEditor();

  return (
    <Box
      sx={{
        display: "flex",
        gap: 0,
        height: "100%",
        minHeight: 0,
        bgcolor: "grey.50",
        borderRadius: 2,
        overflow: "hidden",
        border: 1,
        borderColor: "divider",
      }}
    >
      {/* Left editor */}
      <Paper
        sx={{
          flex: 1,
          minWidth: 0,
          display: "flex",
          flexDirection: "column",
          borderRadius: 0,
          border: "none",
          bgcolor: "background.paper",
          height: "100%",
        }}
      >
        {/* Header */}
        <Box
          sx={{
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            px: 2.5,
            py: 1.5,
            borderBottom: 1,
            borderColor: "divider",
          }}
        >
          <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
            <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
              上传文件开始检查
            </Typography>
            {checked ? (
              <Chip
                icon={<CheckCircleOutline sx={{ fontSize: 14 }} />}
                label="已检查"
                size="small"
                color="success"
                variant="outlined"
              />
            ) : (
              <Typography variant="caption" sx={{ color: "text.secondary" }}>
                待上传
              </Typography>
            )}
          </Box>
          <Button variant="text" size="small" startIcon={<CloudUpload sx={{ fontSize: 16 }} />}>
            {checked ? "更换文档" : "上传文档"}
          </Button>
        </Box>

        {/* Toolbar */}
        <Box
          sx={{
            display: "flex",
            alignItems: "center",
            gap: 0.75,
            px: 2.5,
            py: 1,
            borderBottom: 1,
            borderColor: "divider",
            flexWrap: "wrap",
          }}
        >
          <IconButton size="small">
            <Undo fontSize="small" />
          </IconButton>
          <IconButton size="small">
            <Redo fontSize="small" />
          </IconButton>
          <Divider orientation="vertical" flexItem sx={{ mx: 0.5 }} />
          <IconButton size="small">
            <FormatBold fontSize="small" />
          </IconButton>
          <IconButton size="small">
            <FormatItalic fontSize="small" />
          </IconButton>
          <IconButton size="small">
            <FormatUnderlined fontSize="small" />
          </IconButton>
          <IconButton size="small">
            <FormatStrikethrough fontSize="small" />
          </IconButton>
          <Divider orientation="vertical" flexItem sx={{ mx: 0.5 }} />
          <Typography variant="caption" sx={{ color: "text.secondary" }}>
            仿宋体
          </Typography>
          <Typography variant="caption" sx={{ color: "text.secondary" }}>
            小四
          </Typography>
          <Typography variant="caption" sx={{ color: "text.secondary" }}>
            段落
          </Typography>
          <Divider orientation="vertical" flexItem sx={{ mx: 0.5 }} />
          <IconButton size="small">
            <FormatAlignLeft fontSize="small" />
          </IconButton>
          <IconButton size="small">
            <FormatAlignCenter fontSize="small" />
          </IconButton>
          <IconButton size="small">
            <FormatAlignRight fontSize="small" />
          </IconButton>
          <IconButton size="small">
            <FormatColorText fontSize="small" />
          </IconButton>
          <IconButton size="small">
            <Highlight fontSize="small" />
          </IconButton>
          <IconButton size="small">
            <TableChart fontSize="small" />
          </IconButton>
          <IconButton size="small">
            <MoreHoriz fontSize="small" />
          </IconButton>
        </Box>

        {/* Content (editable) */}
        <Box sx={{ flex: 1, p: 3, overflow: "auto", bgcolor: "grey.50" }}>
          <TextField
            fullWidth
            multiline
            minRows={12}
            placeholder="在此粘贴或输入需要检查的原文…"
            value={originalText}
            onChange={(e) => setOriginalText(e.target.value)}
            sx={{
              "& .MuiOutlinedInput-root": {
                bgcolor: "background.paper",
                fontFamily: "'FangSong', '仿宋', serif",
                fontSize: 16,
                lineHeight: 1.9,
              },
              "& fieldset": { border: "none" },
            }}
          />
        </Box>

        {/* Bottom bar */}
        <Box
          sx={{
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            px: 2.5,
            py: 1.25,
            borderTop: 1,
            borderColor: "divider",
            bgcolor: "background.paper",
          }}
        >
          <Box sx={{ display: "flex", gap: 2, alignItems: "center" }}>
            <Typography variant="caption" sx={{ color: "text.secondary" }}>
              {charCount} 字
            </Typography>
            <Typography
              variant="caption"
              sx={{ color: "primary.main", cursor: "pointer" }}
              onClick={() => setRulesOpen(true)}
            >
              检查规则
            </Typography>
            <Typography variant="caption" sx={{ color: "primary.main", cursor: "pointer" }}>
              检查进度
            </Typography>
          </Box>
          <Box sx={{ display: "flex", gap: 1 }}>
            <Button variant="outlined" size="small" color="default">
              下载原文
            </Button>
            <Button variant="outlined" size="small" color="default">
              导出修订稿
            </Button>
            <Button
              variant="contained"
              size="small"
              onClick={() => void runCheck()}
              disabled={busy || !originalText.trim()}
              startIcon={busy ? <CircularProgress size={14} color="inherit" /> : undefined}
            >
              {busy ? "检查中…" : checked ? "重新检查" : "开始检查"}
            </Button>
          </Box>
        </Box>
      </Paper>

      {/* Right annotations */}
      <Paper
        sx={{
          width: 420,
          flexShrink: 0,
          display: "flex",
          flexDirection: "column",
          borderRadius: 0,
          border: "none",
          borderLeft: 1,
          borderColor: "divider",
          bgcolor: "background.paper",
        }}
      >
        <Box
          sx={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "flex-start",
            px: 2.5,
            pt: 2,
            pb: 1,
          }}
        >
          <Box>
            <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
              检查批注
            </Typography>
            <Typography variant="caption" sx={{ color: "text.secondary" }}>
              选择正文标记或卡片逐处处理
            </Typography>
          </Box>
          <Typography variant="caption" sx={{ color: "text.secondary" }}>
            {checked ? `已检查 · ${checkedAt}` : "待上传"}
          </Typography>
        </Box>

        <MuiTabs
          value={tab}
          onChange={(_, v) => setTab(v)}
          sx={{ px: 2, minHeight: 36, "& .MuiTab-root": { minHeight: 36 } }}
        >
          {TABS.map((t) => (
            <Tab key={t} value={t} label={t} sx={{ fontSize: 13 }} />
          ))}
        </MuiTabs>

        <Box sx={{ flex: 1, p: 2, overflow: "auto", bgcolor: "grey.50" }}>
          {!checked || !resultText ? (
            <Stack spacing={1.5} sx={{ alignItems: "center", pt: 8 }}>
              <Avatar
                sx={{ width: 56, height: 56, bgcolor: "action.hover", color: "text.disabled" }}
              >
                <AutoAwesome />
              </Avatar>
              <Typography variant="body2" sx={{ color: "text.secondary" }}>
                当前分类暂无批注
              </Typography>
              <Typography variant="caption" sx={{ color: "text.disabled" }}>
                上传文档并开始检查后，建议会出现在这里。
              </Typography>
            </Stack>
          ) : (
            <Paper variant="outlined" sx={{ p: 2 }}>
              <Box sx={{ display: "flex", justifyContent: "space-between", mb: 1 }}>
                <Typography variant="subtitle2" sx={{ fontWeight: 600 }}>
                  AI 审校结果
                </Typography>
                <Chip label="通用" size="small" variant="outlined" />
              </Box>
              <Typography
                component="pre"
                variant="body2"
                sx={{ m: 0, whiteSpace: "pre-wrap", fontFamily: "inherit", lineHeight: 1.8 }}
              >
                {resultText}
              </Typography>
              <Box sx={{ display: "flex", justifyContent: "flex-end", gap: 0.5, mt: 1.5 }}>
                <Button
                  variant="text"
                  size="small"
                  startIcon={<ContentCopy sx={{ fontSize: 14 }} />}
                  onClick={() => {
                    void navigator.clipboard
                      ?.writeText(resultText)
                      .then(() => toast.success("已复制"));
                  }}
                >
                  复制结果
                </Button>
              </Box>
            </Paper>
          )}
        </Box>

        <Box sx={{ p: 1.5, borderTop: 1, borderColor: "divider" }}>
          <Typography variant="caption" sx={{ color: "text.disabled" }}>
            检查建议仅供参考，请结合原文确认。
          </Typography>
        </Box>
      </Paper>

      {/* Rules modal */}
      <Dialog
        open={rulesOpen}
        onClose={() => setRulesOpen(false)}
        title={`检查规则　已选择 ${selectedCount} 项`}
        maxWidth="md"
      >
        <Typography variant="body2" sx={{ mb: 2, color: "text.secondary" }}>
          为本次文档选择需要检查的规则
        </Typography>
        <Box sx={{ maxHeight: "50vh", overflow: "auto", pr: 1 }}>
          {RULE_GROUPS.map((g) => (
            <Box key={g.category} sx={{ mb: 2 }}>
              <Box sx={{ display: "flex", gap: 1, alignItems: "center" }}>
                <Checkbox
                  checked={g.items.every((it) => enabledRules[it.name])}
                  onChange={() => {
                    const allChecked = g.items.every((it) => enabledRules[it.name]);
                    setEnabledRules((p) => {
                      const next = { ...p };
                      for (const it of g.items) next[it.name] = !allChecked;
                      return next;
                    });
                  }}
                />
                <Typography variant="subtitle2" sx={{ fontWeight: 600, color: "primary.main" }}>
                  {g.category}
                </Typography>
              </Box>
              <Typography variant="body2" sx={{ color: "text.secondary", ml: 4.5, mb: 1 }}>
                {g.desc}
              </Typography>
              <Box sx={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 1, ml: 4.5 }}>
                {g.items.map((it) => (
                  <Box key={it.name} sx={{ borderLeft: 2, borderColor: "divider", pl: 1.5 }}>
                    <Box sx={{ display: "flex", gap: 0.5, alignItems: "center" }}>
                      <Checkbox
                        checked={!!enabledRules[it.name]}
                        onChange={() => toggleRule(it.name)}
                      />
                      <Typography variant="body2">{it.name}</Typography>
                    </Box>
                    <Typography variant="caption" sx={{ color: "text.disabled", pl: 4 }}>
                      {it.desc}
                    </Typography>
                  </Box>
                ))}
              </Box>
            </Box>
          ))}
        </Box>
        <Box sx={{ display: "flex", gap: 1, alignItems: "center", pt: 2 }}>
          <Typography variant="caption" sx={{ color: "text.disabled", mr: "auto" }}>
            规则只影响本次检查，不会修改词库配置。
          </Typography>
          <Button
            variant="outlined"
            size="small"
            color="default"
            onClick={() => {
              const all: Record<string, boolean> = {};
              for (const g of RULE_GROUPS) for (const it of g.items) all[it.name] = true;
              setEnabledRules(all);
            }}
          >
            恢复默认设置
          </Button>
          <Button
            variant="contained"
            size="small"
            onClick={() => {
              setRulesOpen(false);
              void runCheck();
            }}
          >
            应用并开始检查
          </Button>
        </Box>
      </Dialog>
    </Box>
  );
}

export default ProofreadingEditor;
