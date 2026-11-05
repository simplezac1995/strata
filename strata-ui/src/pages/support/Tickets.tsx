// SPDX-License-Identifier: Apache-2.0
import { useState } from "react";
import { Box, Divider, Drawer, IconButton, Stack, Typography } from "@mui/material";
import { Close } from "@mui/icons-material";
import {
  Button,
  TextField,
  TextArea,
  Select,
  PageHeader,
  DataTable,
  Chip,
  Switch,
  Card,
} from "@/components/ui/index";
import { PageStack } from "@/shared/pageKit/PageLayout";
import { useI18n } from "@/shared/i18n";
import { useAuthStore } from "@/shared/auth/store";
import {
  ticketApi,
  type TicketDto,
  type TicketReplyDto,
  type TicketStatus,
} from "@/shared/api/ticket";
import { useListQuery } from "@/shared/hooks/useListQuery";
import { useMutationWithToast } from "@/shared/hooks/useMutationWithToast";
import { formatDateTime } from "@/shared/utils/date";

const TICKET_STATUS_TONE: Record<TicketStatus, "warning" | "info" | "success" | "default"> = {
  OPEN: "warning",
  PROCESSING: "info",
  RESOLVED: "success",
  CLOSED: "default",
};

function RepliesDrawer({ ticket, onClose }: { ticket: TicketDto | null; onClose: () => void }) {
  const { t } = useI18n();
  const [content, setContent] = useState("");
  const [internal, setInternal] = useState(false);

  const {
    data: replies,
    loading,
    reload,
  } = useListQuery<TicketReplyDto>(["api", "support", "ticketReplies", ticket?.id ?? ""], () =>
    ticket ? ticketApi.replies(ticket.id) : Promise.resolve([]),
  );

  const replyMutation = useMutationWithToast(
    (payload: { content: string; internal: boolean }) =>
      ticket ? ticketApi.reply(ticket.id, payload) : Promise.resolve(null),
    { success: "toast.saved", failure: "toast.saveFailed" },
    {
      onSuccess: () => {
        reload();
        setContent("");
      },
    },
  );

  return (
    <Drawer
      anchor="right"
      open={ticket !== null}
      onClose={onClose}
      slotProps={{
        paper: {
          sx: {
            width: { xs: "100%", sm: 480 },
            maxWidth: "100%",
            display: "flex",
            flexDirection: "column",
          },
        },
      }}
    >
      <Box sx={{ display: "flex", flexDirection: "column", height: "100%" }}>
        <Stack
          direction="row"
          sx={{
            alignItems: "center",
            justifyContent: "space-between",
            px: 2.5,
            py: 1.5,
            borderBottom: 1,
            borderColor: "divider",
          }}
        >
          <Box sx={{ minWidth: 0 }}>
            <Typography variant="subtitle1" sx={{ fontWeight: 600 }} noWrap>
              {ticket?.subject ?? ""}
            </Typography>
            <Typography variant="caption" color="text.secondary">
              {ticket ? `${ticket.customer} · ${formatDateTime(ticket.createdAt)}` : ""}
            </Typography>
          </Box>
          <IconButton size="small" onClick={onClose} aria-label={t("common.dismiss")}>
            <Close fontSize="small" />
          </IconButton>
        </Stack>

        <Box sx={{ flex: 1, overflowY: "auto", px: 2.5, py: 2 }}>
          {loading ? (
            <Typography variant="body2" color="text.secondary">
              {t("common.loading")}
            </Typography>
          ) : (
            <Stack spacing={2}>
              {ticket ? (
                <Box sx={{ p: 1.5, bgcolor: "action.hover", borderRadius: 1 }}>
                  <Typography variant="caption" color="text.secondary">
                    {ticket.customer}
                  </Typography>
                  <Typography variant="body2" sx={{ whiteSpace: "pre-wrap" }}>
                    {ticket.content}
                  </Typography>
                </Box>
              ) : null}
              {replies.map((r) => (
                <Box
                  key={r.id}
                  sx={{
                    p: 1.5,
                    bgcolor: r.internal ? "warning.light" : "background.paper",
                    border: 1,
                    borderColor: r.internal ? "warning.main" : "divider",
                    borderRadius: 1,
                  }}
                >
                  <Stack direction="row" spacing={1} sx={{ alignItems: "center" }}>
                    <Typography variant="caption" sx={{ fontWeight: 600 }}>
                      {r.author}
                    </Typography>
                    {r.internal ? (
                      <Chip label={t("tickets.reply.internal")} color="warning" size="small" />
                    ) : null}
                    <Typography variant="caption" color="text.secondary" sx={{ ml: "auto" }}>
                      {formatDateTime(r.createdAt)}
                    </Typography>
                  </Stack>
                  <Typography variant="body2" sx={{ whiteSpace: "pre-wrap", mt: 0.5 }}>
                    {r.content}
                  </Typography>
                </Box>
              ))}
            </Stack>
          )}
        </Box>

        <Box sx={{ borderTop: 1, borderColor: "divider", p: 2, bgcolor: "background.paper" }}>
          <TextArea
            label={t("tickets.reply.placeholder")}
            rows={3}
            value={content}
            onChange={(e) => setContent(e.target.value)}
          />
          <Stack
            direction="row"
            sx={{ alignItems: "center", justifyContent: "space-between", mt: 1 }}
          >
            <Switch
              label={t("tickets.reply.internalSwitch")}
              checked={internal}
              onChange={setInternal}
            />
            <Button
              variant="contained"
              disabled={!content.trim() || replyMutation.isPending}
              onClick={() => void replyMutation.mutateAsync({ content: content.trim(), internal })}
            >
              {replyMutation.isPending ? t("common.saving") : t("tickets.reply.send")}
            </Button>
          </Stack>
        </Box>
      </Box>
    </Drawer>
  );
}

export function Tickets() {
  const { t } = useI18n();
  const canReply = useAuthStore((s) => s.hasAuthority("support:ticket:reply"));

  const [statusFilter, setStatusFilter] = useState<"" | TicketStatus>("");
  const [keyword, setKeyword] = useState("");
  const [selected, setSelected] = useState<TicketDto | null>(null);

  const {
    data: tickets,
    loading,
    reload,
  } = useListQuery<TicketDto>(["api", "support", "tickets"], () => ticketApi.list());

  const kw = keyword.trim().toLowerCase();
  const filtered = tickets.filter(
    (tk) =>
      (statusFilter === "" || tk.status === statusFilter) &&
      (kw === "" ||
        tk.subject.toLowerCase().includes(kw) ||
        tk.customer.toLowerCase().includes(kw)),
  );

  const cols = [
    { key: "subject", title: t("tickets.subject") },
    { key: "customer", title: t("tickets.customer") },
    {
      key: "status",
      title: t("common.status"),
      render: (tk: TicketDto) => (
        <Chip
          label={t(`tickets.status.${tk.status.toLowerCase()}`)}
          color={TICKET_STATUS_TONE[tk.status]}
        />
      ),
    },
    {
      key: "createdAt",
      title: t("tickets.createdAt"),
      render: (tk: TicketDto) => (
        <Box component="span" sx={{ fontFamily: "monospace", fontSize: 12 }}>
          {formatDateTime(tk.createdAt)}
        </Box>
      ),
    },
  ];

  return (
    <PageStack>
      <PageHeader title={t("tickets.title")} subtitle={t("tickets.subtitle")} />

      <Box
        sx={{
          display: "flex",
          gap: 2,
          alignItems: "flex-start",
          flexWrap: { xs: "wrap", md: "nowrap" },
        }}
      >
        <Card sx={{ width: { xs: "100%", md: 260 }, flexShrink: 0 }} padding={2}>
          <Stack spacing={2}>
            <Select
              label={t("common.status")}
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value as "" | TicketStatus)}
              options={[
                { value: "", label: t("common.all") },
                { value: "OPEN", label: t("tickets.status.open") },
                { value: "PROCESSING", label: t("tickets.status.processing") },
                { value: "RESOLVED", label: t("tickets.status.resolved") },
                { value: "CLOSED", label: t("tickets.status.closed") },
              ]}
            />
            <TextField
              label={t("common.keyword")}
              placeholder={t("tickets.searchPlaceholder")}
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
            />
            <Button variant="outlined" color="default" onClick={() => setKeyword("")}>
              {t("common.reset")}
            </Button>
          </Stack>
        </Card>

        <Box sx={{ flex: 1, minWidth: 0 }}>
          <DataTable
            columns={cols}
            data={filtered}
            loading={loading}
            rowKey={(tk) => tk.id}
            tableId="support.tickets"
            onRetry={reload}
            onRowClick={canReply ? (tk) => setSelected(tk) : undefined}
          />
        </Box>
      </Box>

      <Divider />

      <RepliesDrawer ticket={selected} onClose={() => setSelected(null)} />
    </PageStack>
  );
}

export default Tickets;
