// SPDX-License-Identifier: Apache-2.0
import { useEffect, useState } from "react";
import { Box, Table, TableBody, TableCell, TableHead, TableRow, Typography } from "@mui/material";
import ForumIcon from "@mui/icons-material/Forum";
import TextSnippetIcon from "@mui/icons-material/TextSnippet";
import HubIcon from "@mui/icons-material/Hub";
import PsychologyIcon from "@mui/icons-material/Psychology";
import ArticleIcon from "@mui/icons-material/Article";
import { AiPageHeader, KpiCard } from "@/components/ui/index";
import { useI18n } from "@/shared/i18n";
import { aiChatApi, aiToolApi, type AiUsageDto } from "@/shared/api/ai";
import { toast } from "@/shared/pageKit/Toast";
import type { AiChatSessionDto } from "@/shared/types";
import { ContentCard, PageStack } from "@/shared/pageKit/PageLayout";

export function AiUsage() {
  const { t } = useI18n();
  const [usage, setUsage] = useState<AiUsageDto | null>(null);
  const [sessions, setSessions] = useState<AiChatSessionDto[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    Promise.all([aiToolApi.usage(), aiChatApi.listSessions()])
      .then(([u, s]) => {
        if (cancelled) return;
        setUsage(u);
        setSessions(s);
      })
      .catch(() => toast.error("toast.loadFailed"))
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const kpis = [
    {
      title: t("ai.workspace.chatSessions"),
      value: loading ? "…" : (usage?.sessionCount ?? 0),
      delta: t("ai.workspace.aichatSessions"),
      icon: <ForumIcon />,
      color: "primary" as const,
    },
    {
      title: t("ai.workspace.messages"),
      value: loading ? "…" : (usage?.messageCount ?? 0),
      delta: t("ai.workspace.allChatMessages"),
      icon: <TextSnippetIcon />,
      color: "success" as const,
    },
    {
      title: t("nav.providers"),
      value: loading ? "…" : (usage?.providerCount ?? 0),
      delta: t("ai.workspace.configuredProviders"),
      icon: <HubIcon />,
      color: "secondary" as const,
    },
    {
      title: "模型数",
      value: loading ? "…" : (usage?.modelCount ?? 0),
      delta: "已接入模型",
      icon: <PsychologyIcon />,
      color: "warning" as const,
    },
    {
      title: "文档数",
      value: loading ? "…" : (usage?.documentCount ?? 0),
      delta: "知识库文档",
      icon: <ArticleIcon />,
      color: "primary" as const,
    },
  ];

  return (
    <PageStack>
      <AiPageHeader title={t("ai.workspace.usageStatistics")} />
      <Box sx={{ display: "flex", gap: 2.5, flexWrap: "wrap" }}>
        {kpis.map((s) => (
          <Box key={s.title} sx={{ flex: "1 1 180px", minWidth: 180 }}>
            <KpiCard
              title={s.title}
              value={s.value}
              delta={s.delta}
              icon={s.icon}
              color={s.color}
            />
          </Box>
        ))}
      </Box>
      <ContentCard>
        <Typography variant="subtitle1" sx={{ mb: 2, fontWeight: 600 }}>
          {t("ai.workspace.recentActivity")}
        </Typography>
        <Table size="small">
          <TableHead>
            <TableRow>
              {[t("ai.workspace.session"), t("ai.common.model"), t("common.created")].map((h) => (
                <TableCell key={h}>
                  <Typography
                    variant="caption"
                    sx={{ color: "text.secondary", textTransform: "uppercase", fontWeight: 600 }}
                  >
                    {h}
                  </Typography>
                </TableCell>
              ))}
            </TableRow>
          </TableHead>
          <TableBody>
            {!loading && sessions.length === 0 && (
              <TableRow>
                <TableCell colSpan={3} align="center">
                  <Typography variant="body2" sx={{ color: "text.secondary", py: 3 }}>
                    {t("ai.workspace.noChatSessionsYet")}
                  </Typography>
                </TableCell>
              </TableRow>
            )}
            {sessions.slice(0, 20).map((s) => (
              <TableRow key={s.id} hover>
                <TableCell>
                  <Typography sx={{ fontWeight: 500 }}>{s.title}</Typography>
                </TableCell>
                <TableCell>
                  <Typography
                    variant="caption"
                    sx={{ fontFamily: "monospace", color: "text.secondary" }}
                  >
                    {s.modelId ?? "—"}
                  </Typography>
                </TableCell>
                <TableCell>
                  <Typography
                    variant="caption"
                    sx={{ fontFamily: "monospace", color: "text.disabled" }}
                  >
                    {s.createdAt}
                  </Typography>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </ContentCard>
    </PageStack>
  );
}

export default AiUsage;
