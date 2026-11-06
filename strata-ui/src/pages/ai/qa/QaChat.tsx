// SPDX-License-Identifier: Apache-2.0
import { useEffect, useRef, useState } from "react";
import { Box, Collapse, IconButton, Paper, Stack, Typography } from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import FolderIcon from "@mui/icons-material/Folder";
import { Button, TextArea } from "@/components/ui/index";
import { AiBadge } from "@/components/ui/index";
import { useI18n } from "@/shared/i18n";
import { useListQuery } from "@/shared/hooks/useListQuery";
import { knowledgeApi } from "@/shared/api/ai";
import { aiQaApi, type AiQaMessage } from "@/shared/api/aiQa";
import { failureMessage } from "@/shared/hooks/useCrud";
import { toast } from "@/shared/pageKit/Toast";
import type { AiKnowledgeDto } from "@/shared/types";

let nextId = 1;

export function QaChat() {
  const { t } = useI18n();
  const [kbId, setKbId] = useState<string>("");
  const [messages, setMessages] = useState<AiQaMessage[]>([]);
  const [input, setInput] = useState("");
  const [busy, setBusy] = useState(false);
  const [citationOpen, setCitationOpen] = useState<Record<string, boolean>>({});

  const { data: kbs, loading: kbsLoading } = useListQuery<AiKnowledgeDto>(
    ["api", "ai", "knowledge-bases", "list"],
    () => knowledgeApi.list(),
    { staleTime: 5 * 60 * 1000 },
  );

  const messagesEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, busy]);

  const selectedKb = kbs.find((k) => k.id === kbId) ?? null;

  async function handleSend() {
    const question = input.trim();
    if (!question || busy) return;
    const userMsg: AiQaMessage = { id: `m${nextId++}`, role: "user", content: question };
    setMessages((prev) => [...prev, userMsg]);
    setInput("");
    setBusy(true);
    try {
      const res = await aiQaApi.retrieve(question, kbId || null);
      setMessages((prev) => [
        ...prev,
        {
          id: `m${nextId++}`,
          role: "assistant",
          content: res.answer,
          citations: res.citations,
        },
      ]);
    } catch (err) {
      toast.error(failureMessage(err, "toast.saveFailed"));
      setMessages((prev) => [
        ...prev,
        {
          id: `m${nextId++}`,
          role: "assistant",
          content: t("ai.qa.emptyAnswer"),
          citations: [],
        },
      ]);
    } finally {
      setBusy(false);
    }
  }

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
      {/* Left: knowledge base list */}
      <Paper
        variant="outlined"
        sx={{
          width: 240,
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
            {t("ai.qa.knowledgeBases")}
          </Typography>
        </Stack>
        <Box sx={{ flex: 1, overflowY: "auto", py: 1 }}>
          <Box
            onClick={() => setKbId("")}
            sx={{
              px: 1.5,
              py: 1.25,
              cursor: "pointer",
              bgcolor: kbId === "" ? "primary.main" : "transparent",
              color: kbId === "" ? "primary.contrastText" : "text.secondary",
              display: "flex",
              alignItems: "center",
              gap: 1,
              "&:hover": { bgcolor: kbId === "" ? "primary.main" : "action.hover" },
            }}
          >
            <FolderIcon fontSize="small" />
            <Typography variant="body2" noWrap>
              {t("ai.qa.allKnowledgeBases")}
            </Typography>
          </Box>
          {kbsLoading && (
            <Typography
              variant="caption"
              color="text.secondary"
              sx={{ px: 1.5, display: "block", pt: 1 }}
            >
              {t("common.loading")}
            </Typography>
          )}
          {!kbsLoading && kbs.length === 0 && (
            <Typography
              variant="caption"
              color="text.secondary"
              sx={{ px: 1.5, display: "block", pt: 1 }}
            >
              {t("ai.qa.noKnowledgeBase")}
            </Typography>
          )}
          {kbs.map((kb) => (
            <Box
              key={kb.id}
              onClick={() => setKbId(kb.id)}
              sx={{
                px: 1.5,
                py: 1.25,
                cursor: "pointer",
                bgcolor: kbId === kb.id ? "primary.main" : "transparent",
                color: kbId === kb.id ? "primary.contrastText" : "text.secondary",
                display: "flex",
                alignItems: "center",
                gap: 1,
                "&:hover": { bgcolor: kbId === kb.id ? "primary.main" : "action.hover" },
              }}
            >
              <FolderIcon fontSize="small" />
              <Box sx={{ minWidth: 0 }}>
                <Typography variant="body2" noWrap sx={{ fontWeight: 500 }}>
                  {kb.name}
                </Typography>
              </Box>
            </Box>
          ))}
        </Box>
      </Paper>

      {/* Right: conversation */}
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
          }}
        >
          <Typography variant="body2" color="text.secondary">
            {selectedKb
              ? `${t("ai.qa.answeringWith")} ${selectedKb.name}`
              : t("ai.qa.answeringAll")}
          </Typography>
        </Box>

        <Stack spacing={2.5} sx={{ flex: 1, overflowY: "auto", px: 2.5, py: 2.5 }}>
          {messages.length === 0 && !busy && (
            <Box sx={{ textAlign: "center", pt: 8 }}>
              <Typography variant="body2" color="text.secondary">
                {t("ai.qa.emptyHint")}
              </Typography>
            </Box>
          )}
          {messages.map((m) => (
            <Stack
              key={m.id}
              spacing={0.5}
              sx={{
                alignItems: m.role === "user" ? "flex-end" : "flex-start",
              }}
            >
              <Paper
                variant="outlined"
                sx={{
                  maxWidth: 640,
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

              {m.role === "assistant" && m.citations && m.citations.length > 0 && (
                <Box sx={{ maxWidth: 640, width: "100%" }}>
                  <Stack
                    direction="row"
                    spacing={0.5}
                    sx={{ alignItems: "center", cursor: "pointer", width: "fit-content" }}
                    onClick={() => setCitationOpen((prev) => ({ ...prev, [m.id]: !prev[m.id] }))}
                  >
                    <Typography variant="caption" color="primary" sx={{ fontWeight: 600 }}>
                      {t("ai.qa.citations")} ({m.citations.length})
                    </Typography>
                    <IconButton
                      size="small"
                      aria-label={t("common.toggleCitations")}
                      sx={{ p: 0.25 }}
                    >
                      <ExpandMoreIcon
                        fontSize="small"
                        sx={{
                          transform: citationOpen[m.id] ? "rotate(180deg)" : "none",
                          transition: "transform 0.2s",
                        }}
                      />
                    </IconButton>
                  </Stack>
                  <Collapse in={citationOpen[m.id] ?? false}>
                    <Paper variant="outlined" sx={{ p: 1.5, mt: 0.5 }}>
                      <Stack spacing={1.25}>
                        {m.citations.map((c, idx) => {
                          const snippet =
                            c.snippet.length > 150 ? `${c.snippet.slice(0, 150)}…` : c.snippet;
                          return (
                            <Box key={`${c.documentId}-${c.chunkIndex}-${idx}`}>
                              <Typography variant="caption" sx={{ fontWeight: 600 }}>
                                {c.documentName}
                                <Typography
                                  component="span"
                                  variant="caption"
                                  color="text.secondary"
                                  sx={{ ml: 1 }}
                                >
                                  #chunk {c.chunkIndex}
                                </Typography>
                              </Typography>
                              <Typography
                                variant="caption"
                                color="text.secondary"
                                sx={{ display: "block", whiteSpace: "pre-wrap" }}
                              >
                                {snippet}
                              </Typography>
                            </Box>
                          );
                        })}
                      </Stack>
                    </Paper>
                  </Collapse>
                </Box>
              )}
            </Stack>
          ))}
          {busy && (
            <Paper variant="outlined" sx={{ maxWidth: 640, px: 2, py: 1.5, borderRadius: 3 }}>
              <Typography variant="body2" color="text.secondary">
                {t("ai.common.generating")}
              </Typography>
            </Paper>
          )}
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
          <Stack direction="row" spacing={1} sx={{ alignItems: "flex-end" }}>
            <TextArea
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder={t("ai.qa.placeholder")}
              rows={2}
              disabled={busy}
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
              disabled={!input.trim() || busy}
            >
              {busy ? t("ai.common.generating") : t("ai.qa.send")}
            </Button>
          </Stack>
        </Box>
      </Box>
    </Box>
  );
}

export default QaChat;
