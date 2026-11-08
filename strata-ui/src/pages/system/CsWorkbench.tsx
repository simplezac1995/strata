// SPDX-License-Identifier: Apache-2.0
import { useEffect, useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  Avatar,
  Box,
  Divider,
  IconButton,
  List,
  ListItemButton,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import SendIcon from "@mui/icons-material/Send";
import CloseIcon from "@mui/icons-material/Close";
import { Button, Tabs } from "@/components/ui/index";
import { useI18n } from "@/shared/i18n";
import { csApi, type CsConversationStatus, type CsConversationDto } from "@/shared/api/cs";
import { PageFill } from "@/shared/pageKit/PageLayout";

const TAB_IDS: { id: CsConversationStatus | ""; label: string }[] = [
  { id: "open", label: "cs.tab.open" },
  { id: "assigned", label: "cs.tab.assigned" },
  { id: "closed", label: "cs.tab.closed" },
];

export function CsWorkbench() {
  const { t } = useI18n();
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<CsConversationStatus | "">("open");
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [draft, setDraft] = useState("");
  const bottomRef = useRef<HTMLDivElement>(null);

  const { data: convos } = useQuery({
    queryKey: ["api", "cs", "conversations", { tab }],
    queryFn: () => csApi.conversations(tab, 0, 50),
    refetchInterval: 5_000,
  });

  const { data: messages } = useQuery({
    queryKey: ["api", "cs", "messages", selectedId],
    queryFn: () => csApi.messages(selectedId!),
    enabled: !!selectedId,
    refetchInterval: selectedId ? 5_000 : false,
  });

  const sendMutation = useMutation({
    mutationFn: (content: string) => csApi.sendMessage(selectedId!, { content }),
    onSuccess: () => {
      setDraft("");
      void queryClient.invalidateQueries({ queryKey: ["api", "cs"] });
    },
  });

  const closeMutation = useMutation({
    mutationFn: () => csApi.closeConversation(selectedId!),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["api", "cs"] });
    },
  });

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  const list = convos?.content ?? [];
  const selected = list.find((c) => c.id === selectedId) ?? null;

  const handleSelect = (c: CsConversationDto) => {
    setSelectedId(c.id);
  };

  const handleSend = () => {
    const content = draft.trim();
    if (!content || !selectedId) return;
    sendMutation.mutate(content);
  };

  return (
    <PageFill>
      <Box sx={{ display: "flex", height: "100%", bgcolor: "background.paper" }}>
        {/* Left: conversation list */}
        <Box
          sx={{
            width: 320,
            flexShrink: 0,
            borderRight: 1,
            borderColor: "divider",
            display: "flex",
            flexDirection: "column",
          }}
        >
          <Box sx={{ p: 2, pb: 0 }}>
            <Typography variant="h6" sx={{ fontWeight: 650, mb: 1 }}>
              {t("cs.title")}
            </Typography>
            <Tabs
              active={tab || "open"}
              onChange={(id) => {
                setTab(id as CsConversationStatus | "");
                setSelectedId(null);
              }}
              tabs={TAB_IDS.map((x) => ({ id: x.id || "open", label: t(x.label) }))}
            />
          </Box>
          <List sx={{ flex: 1, overflowY: "auto", py: 0 }}>
            {list.length === 0 ? (
              <Typography variant="body2" color="text.secondary" sx={{ p: 3, textAlign: "center" }}>
                {t("common.noData")}
              </Typography>
            ) : (
              list.map((c) => (
                <ListItemButton
                  key={c.id}
                  selected={c.id === selectedId}
                  onClick={() => handleSelect(c)}
                  sx={{ flexDirection: "column", alignItems: "flex-start", px: 2, py: 1.5 }}
                >
                  <Stack
                    direction="row"
                    spacing={1}
                    sx={{ width: "100%", alignItems: "center", justifyContent: "space-between" }}
                  >
                    <Typography variant="body2" sx={{ fontWeight: 600 }}>
                      {c.playerName}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      {c.lastMessageAt ? new Date(c.lastMessageAt).toLocaleTimeString() : ""}
                    </Typography>
                  </Stack>
                  <Typography
                    variant="caption"
                    color="text.secondary"
                    sx={{
                      display: "-webkit-box",
                      WebkitLineClamp: 1,
                      WebkitBoxOrient: "vertical",
                      overflow: "hidden",
                      maxWidth: "100%",
                    }}
                  >
                    {c.lastMessagePreview ?? t("cs.noPreview")}
                  </Typography>
                </ListItemButton>
              ))
            )}
          </List>
        </Box>

        {/* Right: message pane */}
        <Box sx={{ flex: 1, display: "flex", flexDirection: "column", minWidth: 0 }}>
          {!selected ? (
            <Box sx={{ flex: 1, display: "flex", alignItems: "center", justifyContent: "center" }}>
              <Typography variant="body2" color="text.secondary">
                {t("cs.selectConversation")}
              </Typography>
            </Box>
          ) : (
            <>
              <Box
                sx={{
                  px: 2,
                  py: 1.5,
                  borderBottom: 1,
                  borderColor: "divider",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "space-between",
                }}
              >
                <Stack direction="row" spacing={1.5} sx={{ alignItems: "center" }}>
                  <Avatar sx={{ width: 32, height: 32, bgcolor: "primary.main" }}>
                    {selected.playerName.slice(0, 1).toUpperCase()}
                  </Avatar>
                  <Typography variant="subtitle2" sx={{ fontWeight: 600 }}>
                    {selected.playerName}
                  </Typography>
                </Stack>
                {selected.status !== "closed" ? (
                  <Button
                    size="sm"
                    variant="outlined"
                    color="default"
                    startIcon={<CloseIcon />}
                    onClick={() => closeMutation.mutate()}
                    disabled={closeMutation.isPending}
                  >
                    {t("cs.close")}
                  </Button>
                ) : (
                  <Typography variant="caption" color="text.secondary">
                    {t("cs.closed")}
                  </Typography>
                )}
              </Box>

              <Box sx={{ flex: 1, overflowY: "auto", p: 2 }}>
                <Stack spacing={1.5}>
                  {(messages ?? []).map((m) => {
                    const isStaff = m.senderType === "staff";
                    return (
                      <Box
                        key={m.id}
                        sx={{
                          display: "flex",
                          justifyContent: isStaff ? "flex-end" : "flex-start",
                        }}
                      >
                        <Box
                          sx={{
                            maxWidth: "70%",
                            bgcolor: isStaff ? "primary.main" : "action.hover",
                            color: isStaff ? "primary.contrastText" : "text.primary",
                            px: 1.5,
                            py: 1,
                            borderRadius: 2,
                          }}
                        >
                          <Typography variant="caption" sx={{ opacity: 0.8 }}>
                            {m.senderName}
                          </Typography>
                          <Typography variant="body2" sx={{ whiteSpace: "pre-wrap" }}>
                            {m.content}
                          </Typography>
                          <Typography
                            variant="caption"
                            sx={{ display: "block", opacity: 0.7, textAlign: "right" }}
                          >
                            {new Date(m.createdAt).toLocaleTimeString()}
                          </Typography>
                        </Box>
                      </Box>
                    );
                  })}
                  <div ref={bottomRef} />
                </Stack>
              </Box>

              <Divider />
              <Stack direction="row" spacing={1} sx={{ p: 1.5 }}>
                <TextField
                  fullWidth
                  size="small"
                  placeholder={t("cs.inputPlaceholder")}
                  value={draft}
                  onChange={(e) => setDraft(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === "Enter" && !e.shiftKey) {
                      e.preventDefault();
                      handleSend();
                    }
                  }}
                  disabled={selected.status === "closed" || sendMutation.isPending}
                />
                <IconButton
                  color="primary"
                  aria-label={t("common.send")}
                  onClick={handleSend}
                  disabled={!draft.trim() || selected.status === "closed" || sendMutation.isPending}
                >
                  <SendIcon />
                </IconButton>
              </Stack>
            </>
          )}
        </Box>
      </Box>
    </PageFill>
  );
}

export default CsWorkbench;
