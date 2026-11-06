// SPDX-License-Identifier: Apache-2.0
import { useEffect, useState, useRef } from "react";
import {
  Avatar,
  Box,
  Chip,
  Fab,
  IconButton,
  Paper,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import AutoAwesomeIcon from "@mui/icons-material/AutoAwesome";
import CloseIcon from "@mui/icons-material/Close";
import SendIcon from "@mui/icons-material/Send";
import SmartToyOutlinedIcon from "@mui/icons-material/SmartToyOutlined";
import { aiChatApi } from "@/shared/api/ai";
import { ApiError } from "@/shared/api/client";
import { useI18n, tr } from "@/shared/i18n";

function FloatingCopilot() {
  const { lang, t, tError } = useI18n();
  const [open, setOpen] = useState(false);
  const [input, setInput] = useState("");
  const [busy, setBusy] = useState(false);
  const sessionIdRef = useRef<string | null>(null);
  const [msgs, setMsgs] = useState<{ role: string; text: string }[]>([]);

  useEffect(() => {
    setMsgs([{ role: "bot", text: tr(lang, "shell.copilotGreeting") }]);
  }, [lang]);

  const send = async () => {
    const userMsg = input.trim();
    if (!userMsg || busy) return;
    setMsgs((m) => [...m, { role: "user", text: userMsg }, { role: "bot", text: "" }]);
    setInput("");
    setBusy(true);
    try {
      if (!sessionIdRef.current) {
        const session = await aiChatApi.createSession(t("shell.copilot.sessionName"));
        sessionIdRef.current = session.id;
      }
      await aiChatApi.sendMessageStream(sessionIdRef.current, userMsg, (chunk) => {
        setMsgs((m) => {
          const next = [...m];
          const last = next[next.length - 1];
          if (last && last.role === "bot") {
            next[next.length - 1] = { ...last, text: last.text + chunk };
          }
          return next;
        });
      });
    } catch (err) {
      const detail = err instanceof ApiError ? tError(err.message) : tError("Request failed");
      setMsgs((m) => [...m, { role: "bot", text: `${detail}. ${t("shell.copilot.setupHint")}` }]);
    } finally {
      setBusy(false);
    }
  };

  return (
    <>
      {open ? (
        <Paper
          elevation={8}
          sx={{
            position: "fixed",
            bottom: 80,
            right: { xs: 12, sm: 20 },
            width: { xs: "calc(100vw - 24px)", sm: 300 },
            zIndex: 40,
            display: "flex",
            flexDirection: "column",
            overflow: "hidden",
            border: 1,
            borderColor: "divider",
          }}
        >
          <Stack
            direction="row"
            sx={{
              alignItems: "center",
              justifyContent: "space-between",
              px: 2,
              py: 1.5,
              borderBottom: 1,
              borderColor: "divider",
              bgcolor: "background.default",
            }}
          >
            <Stack direction="row" spacing={1} sx={{ alignItems: "center" }}>
              <AutoAwesomeIcon sx={{ fontSize: 16, color: "primary.main" }} />
              <Typography variant="body2" sx={{ fontWeight: 600 }}>
                {t("shell.copilot.label")}
              </Typography>
              <Chip
                label={t("shell.copilot.beta")}
                size="small"
                color="primary"
                variant="outlined"
              />
            </Stack>
            <IconButton
              size="small"
              onClick={() => setOpen(false)}
              aria-label={t("shell.search.close")}
            >
              <CloseIcon fontSize="small" />
            </IconButton>
          </Stack>

          <Box
            sx={{
              flex: 1,
              overflowY: "auto",
              p: 1.5,
              maxHeight: 256,
              bgcolor: "background.default",
            }}
          >
            <Stack spacing={1.25}>
              {msgs.map((m, i) => (
                <Stack
                  key={i}
                  direction="row"
                  spacing={1}
                  sx={{ justifyContent: m.role === "user" ? "flex-end" : "flex-start" }}
                >
                  {m.role === "bot" ? (
                    <Avatar sx={{ width: 20, height: 20, bgcolor: "primary.main" }}>
                      <SmartToyOutlinedIcon sx={{ fontSize: 12 }} />
                    </Avatar>
                  ) : null}
                  <Box
                    sx={{
                      fontSize: 12,
                      lineHeight: 1.6,
                      borderRadius: 2,
                      px: 1.5,
                      py: 1,
                      maxWidth: 220,
                      wordBreak: "break-word",
                      overflowWrap: "anywhere",
                      whiteSpace: "pre-wrap",
                      ...(m.role === "user"
                        ? {
                            bgcolor: "primary.main",
                            color: "primary.contrastText",
                            borderTopRightRadius: 4,
                          }
                        : {
                            bgcolor: "background.paper",
                            color: "text.secondary",
                            borderTopLeftRadius: 4,
                            border: 1,
                            borderColor: "divider",
                          }),
                    }}
                  >
                    {m.text}
                  </Box>
                </Stack>
              ))}
            </Stack>
          </Box>

          <Box sx={{ p: 1.5, borderTop: 1, borderColor: "divider", bgcolor: "background.paper" }}>
            <Stack direction="row" spacing={1}>
              <TextField
                size="small"
                fullWidth
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter") void send();
                }}
                placeholder={busy ? t("common.loading") : t("shell.copilot.placeholder")}
                disabled={busy}
                slotProps={{ input: { sx: { fontSize: 12, borderRadius: 2 } } }}
              />
              <IconButton
                color="primary"
                aria-label={t("common.send")}
                onClick={() => void send()}
                disabled={busy}
                sx={{
                  bgcolor: "primary.main",
                  color: "primary.contrastText",
                  borderRadius: 2,
                  "&:hover": { bgcolor: "primary.dark" },
                  "&.Mui-disabled": { bgcolor: "action.disabledBackground" },
                }}
              >
                <SendIcon sx={{ fontSize: 16 }} />
              </IconButton>
            </Stack>
          </Box>
        </Paper>
      ) : null}

      <Fab
        color="primary"
        onClick={() => setOpen((o) => !o)}
        title={t("shell.copilot.fabTitle")}
        sx={{
          position: "fixed",
          bottom: 20,
          right: { xs: 12, sm: 20 },
          zIndex: 40,
          width: 44,
          height: 44,
          boxShadow: 4,
          bgcolor: open ? "primary.dark" : "primary.main",
        }}
      >
        {open ? <CloseIcon /> : <AutoAwesomeIcon />}
      </Fab>
    </>
  );
}

export { FloatingCopilot };
