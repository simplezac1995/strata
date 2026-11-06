// SPDX-License-Identifier: Apache-2.0
import { useEffect, useState } from "react";
import { Box, Avatar, CircularProgress, IconButton, Paper, Stack, Typography } from "@mui/material";
import { PageStack } from "@/shared/pageKit/PageLayout";
import AddIcon from "@mui/icons-material/Add";
import ArrowForwardIosIcon from "@mui/icons-material/ArrowForwardIos";
import ChevronRightIcon from "@mui/icons-material/ChevronRight";
import DeleteOutlineIcon from "@mui/icons-material/DeleteOutlined";
import HelpOutlineOutlinedIcon from "@mui/icons-material/HelpOutlineOutlined";
import MicIcon from "@mui/icons-material/Mic";
import PsychologyAltIcon from "@mui/icons-material/PsychologyAlt";
import SendIcon from "@mui/icons-material/Send";
import { aiChatApi } from "@/shared/api/ai";
import { Select, TextField } from "@/components/ui/index";

interface ChatMsg {
  role: "user" | "assistant";
  text: string;
}

const QUICK_QUESTIONS = ["如何设计一个高可用的微服务架构系统?", "如何解决分布式系统的事务问题?"];

export function AiMindMap() {
  const [sessionId, setSessionId] = useState<string | null>(null);
  const [msgs, setMsgs] = useState<ChatMsg[]>([]);
  const [input, setInput] = useState("");
  const [busy, setBusy] = useState(false);
  const [history, setHistory] = useState<{ id: string; title: string; time?: string }[]>([]);

  useEffect(() => {
    aiChatApi
      .listSessions()
      .then((s) => setHistory(s.map((x) => ({ id: x.id, title: x.title }))))
      .catch(() => {});
  }, []);

  async function send(text?: string) {
    const userText = (text ?? input).trim();
    if (!userText || busy) return;
    setInput("");
    setMsgs((m) => [...m, { role: "user", text: userText }, { role: "assistant", text: "" }]);
    setBusy(true);
    try {
      let sid = sessionId;
      if (!sid) {
        const s = await aiChatApi.createSession("脑图: " + userText.slice(0, 20));
        sid = s.id;
        setSessionId(sid);
      }
      await aiChatApi.sendMessageStream(
        sid,
        "请用 Markdown 列表格式生成思维导图大纲：" + userText,
        (chunk) => {
          setMsgs((m) => {
            const copy = [...m];
            const last = copy[copy.length - 1];
            if (last && last.role === "assistant") last.text += chunk;
            return copy;
          });
        },
      );
    } catch {
      setMsgs((m) => {
        const copy = [...m];
        const last = copy[copy.length - 1];
        if (last && last.role === "assistant" && !last.text) last.text = "生成失败，请重试";
        return copy;
      });
    } finally {
      setBusy(false);
    }
  }

  const welcome = msgs.length === 0;

  return (
    <PageStack>
      <Box sx={{ display: "flex", height: "calc(100vh - 160px)", gap: 2 }}>
        {/* Left: history sessions */}
        <Paper
          variant="outlined"
          sx={{ width: 280, p: 2, display: "flex", flexDirection: "column" }}
        >
          <Stack
            direction="row"
            sx={{ alignItems: "center", justifyContent: "space-between", mb: 2 }}
          >
            <IconButton size="small">
              <ChevronRightIcon />
            </IconButton>
          </Stack>
          <Box
            sx={{
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              bgcolor: "primary.main",
              color: "primary.contrastText",
              borderRadius: 2,
              py: 1,
              mb: 2,
              cursor: "pointer",
              fontWeight: 600,
            }}
            onClick={() => {
              setSessionId(null);
              setMsgs([]);
            }}
          >
            <AddIcon sx={{ mr: 0.5 }} /> 新建聊天
          </Box>
          <Box sx={{ overflow: "auto", flex: 1 }}>
            {history.map((h) => (
              <Paper
                key={h.id}
                variant="outlined"
                sx={{ p: 1.5, mb: 1, cursor: "pointer", "&:hover": { bgcolor: "action.hover" } }}
                onClick={() => {
                  setSessionId(h.id);
                  setMsgs([]);
                }}
              >
                <Typography variant="body2" noWrap sx={{ fontWeight: 500 }}>
                  {h.title}
                </Typography>
                <Typography variant="caption" sx={{ color: "text.secondary" }}>
                  {h.time ?? "最近会话"}
                </Typography>
              </Paper>
            ))}
          </Box>
        </Paper>

        {/* Main: chat / mindmap */}
        <Paper
          variant="outlined"
          sx={{ flex: 1, display: "flex", flexDirection: "column", borderRadius: 3 }}
        >
          {/* Top bar: model select + delete */}
          <Box
            sx={{
              display: "flex",
              justifyContent: "flex-end",
              alignItems: "center",
              gap: 1,
              p: 1.5,
            }}
          >
            <Box sx={{ width: 180 }}>
              <Select
                value="qwen3.7-flash"
                onChange={() => {}}
                options={[
                  { value: "qwen3.7-flash", label: "qwen3.7-flash" },
                  { value: "qwen3.7-plus", label: "Aliyun:qwen3.7-plus" },
                ]}
              />
            </Box>
            <IconButton size="small">
              <DeleteOutlineIcon fontSize="small" />
            </IconButton>
          </Box>

          {/* Body */}
          <Box sx={{ flex: 1, overflow: "auto", p: 3 }}>
            {welcome ? (
              <Box sx={{ maxWidth: 720, mx: "auto", pt: 6 }}>
                <Stack spacing={2} sx={{ alignItems: "center", mb: 4 }}>
                  <Avatar sx={{ width: 72, height: 72, bgcolor: "primary.light" }}>
                    <PsychologyAltIcon sx={{ width: 40, height: 40 }} />
                  </Avatar>
                  <Typography variant="h6" sx={{ fontWeight: 600 }}>
                    你好，我是AI 脑图专家
                  </Typography>
                  <Typography variant="body2" sx={{ color: "text.secondary" }}>
                    我可以帮您生成目标的脑图，例如：
                  </Typography>
                </Stack>
                <Typography
                  variant="caption"
                  sx={{ color: "text.disabled", display: "block", textAlign: "center", mb: 1.5 }}
                >
                  快速开始
                </Typography>
                <Stack direction="row" spacing={2} sx={{ justifyContent: "center" }}>
                  {QUICK_QUESTIONS.map((q) => (
                    <Paper
                      key={q}
                      variant="outlined"
                      sx={{
                        p: 2,
                        cursor: "pointer",
                        maxWidth: 300,
                        flex: 1,
                        display: "flex",
                        alignItems: "center",
                        gap: 1,
                        "&:hover": { borderColor: "primary.main" },
                      }}
                      onClick={() => void send(q)}
                    >
                      <HelpOutlineOutlinedIcon sx={{ color: "primary.main" }} />
                      <Typography variant="body2" sx={{ flex: 1 }}>
                        {q}
                      </Typography>
                      <ArrowForwardIosIcon sx={{ fontSize: 14, color: "text.disabled" }} />
                    </Paper>
                  ))}
                </Stack>
              </Box>
            ) : (
              <Stack spacing={2}>
                {msgs.map((m, i) => (
                  <Box
                    key={i}
                    sx={{
                      alignSelf: m.role === "user" ? "flex-end" : "flex-start",
                      maxWidth: "75%",
                    }}
                  >
                    {m.role === "assistant" && (
                      <Stack direction="row" spacing={1} sx={{ alignItems: "center", mb: 0.5 }}>
                        <Avatar sx={{ width: 22, height: 22, bgcolor: "primary.light" }}>
                          <PsychologyAltIcon sx={{ width: 14, height: 14 }} />
                        </Avatar>
                      </Stack>
                    )}
                    <Paper
                      variant="outlined"
                      sx={{
                        p: 2,
                        borderRadius: 3,
                        bgcolor: m.role === "user" ? "primary.main" : "action.hover",
                        color: m.role === "user" ? "primary.contrastText" : "text.primary",
                      }}
                    >
                      <Typography
                        component="pre"
                        variant="body2"
                        sx={{ whiteSpace: "pre-wrap", fontFamily: "inherit", m: 0 }}
                      >
                        {m.text || (busy ? "" : "")}
                      </Typography>
                    </Paper>
                  </Box>
                ))}
              </Stack>
            )}
          </Box>

          {/* Bottom input */}
          <Box sx={{ p: 2 }}>
            <Box
              sx={{
                display: "flex",
                alignItems: "center",
                gap: 1,
                p: 1,
                borderRadius: 2.5,
                border: 1,
                borderColor: "divider",
                bgcolor: "background.paper",
                maxWidth: 820,
                mx: "auto",
              }}
            >
              <Box sx={{ flex: 1 }}>
                <TextField
                  fullWidth
                  placeholder="请输入您想生成脑图的大纲"
                  value={input}
                  onChange={(e) => setInput(e.target.value)}
                  onKeyDown={(e) => e.key === "Enter" && void send()}
                  variant="standard"
                  slotProps={{ input: { disableUnderline: true } }}
                />
              </Box>
              <IconButton size="small">
                <MicIcon fontSize="small" sx={{ color: "text.disabled" }} />
              </IconButton>
              <Box
                sx={{
                  width: 40,
                  height: 40,
                  borderRadius: "50%",
                  bgcolor: busy ? "action.disabledBackground" : "primary.main",
                  color: "primary.contrastText",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  cursor: busy ? "default" : "pointer",
                }}
                onClick={() => void send()}
              >
                {busy ? (
                  <CircularProgress size={18} sx={{ color: "#fff" }} />
                ) : (
                  <SendIcon fontSize="small" />
                )}
              </Box>
            </Box>
          </Box>
        </Paper>
      </Box>
    </PageStack>
  );
}

export default AiMindMap;
