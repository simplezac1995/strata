// SPDX-License-Identifier: Apache-2.0
import { useEffect, useState } from "react";
import { useI18n } from "@/shared/i18n";
import { Box, Grid, IconButton, Typography } from "@mui/material";
import RefreshIcon from "@mui/icons-material/Refresh";
import ForumIcon from "@mui/icons-material/Forum";
import DataUsageIcon from "@mui/icons-material/DataUsage";
import PeopleIcon from "@mui/icons-material/People";
import BoltIcon from "@mui/icons-material/Bolt";
import { KpiCard, Tabs } from "@/components/ui/index";
import { BarChart, ChartCard, LineChart, PieChart } from "@/components/ui/chart";
import { EmptyState } from "@/components/ui/index";
import { PageStack } from "@/shared/pageKit/PageLayout";
import { apiGet } from "@/shared/api/client";

interface Stats {
  trend: { d: string; p: number; c: number; t: number }[];
  byModel: { model: string; calls: number; tokens: number }[];
  byAction: { action: string; c: number }[];
  totals: { calls: number; tokens: number; sessions: number };
}

const RANGE_TABS = [
  { id: "0", label: "今日" },
  { id: "1", label: "近7天" },
  { id: "2", label: "近30天" },
  { id: "3", label: "近90天" },
];

export function StatsDashboard() {
  const { t } = useI18n();
  const [s, setS] = useState<Stats | null>(null);
  const [range, setRange] = useState("1");
  const [loading, setLoading] = useState(true);

  const load = () => {
    setLoading(true);
    apiGet<Stats>("/api/v1/ai/stats")
      .then((r) => setS(r))
      .catch(() => {})
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [range]);

  const totals = s?.totals ?? { calls: 0, tokens: 0, sessions: 0 };

  const kpis = [
    {
      title: "总对话次数",
      value: totals.calls.toLocaleString(),
      delta: "环比上期",
      icon: <ForumIcon />,
      color: "primary" as const,
    },
    {
      title: "总 Token 消耗",
      value: totals.tokens.toLocaleString(),
      delta: "环比上期",
      icon: <DataUsageIcon />,
      color: "success" as const,
    },
    {
      title: "活跃用户数",
      value: totals.sessions.toLocaleString(),
      delta: "环比上期",
      icon: <PeopleIcon />,
      color: "secondary" as const,
    },
    {
      title: "平均响应时间",
      value: "—",
      delta: "性能优秀",
      icon: <BoltIcon />,
      color: "warning" as const,
    },
  ];

  const trend = s?.trend ?? [];
  const trendSeries = [
    { name: "输入 Token", data: trend.map((r) => r.p), color: "#3b82f6" },
    { name: "输出 Token", data: trend.map((r) => r.c), color: "#f59e0b" },
    { name: "总 Token", data: trend.map((r) => r.t), color: "#10b981" },
  ];

  const modelPie = (s?.byModel ?? []).map((r) => ({ name: r.model, value: r.calls }));

  const actionCategories = (s?.byAction ?? []).map((r) => r.action);
  const actionSeries = [{ name: "调用次数", data: (s?.byAction ?? []).map((r) => r.c) }];

  return (
    <PageStack>
      <Box
        sx={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          gap: 2,
          flexWrap: "wrap",
        }}
      >
        <Typography variant="h6" sx={{ fontWeight: 600 }}>
          AI 使用情况看板
        </Typography>
        <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
          <Tabs tabs={RANGE_TABS} active={range} onChange={(id) => setRange(id)} />
          <IconButton size="small" onClick={load} aria-label="刷新">
            <RefreshIcon fontSize="small" />
          </IconButton>
        </Box>
      </Box>

      <Box sx={{ display: "flex", gap: 2.5, flexWrap: "wrap" }}>
        {kpis.map((k) => (
          <Box key={k.title} sx={{ flex: "1 1 200px", minWidth: 200 }}>
            <KpiCard
              title={k.title}
              value={k.value}
              delta={k.delta}
              icon={k.icon}
              color={k.color}
            />
          </Box>
        ))}
      </Box>

      <Grid container spacing={2.5}>
        <Grid size={{ xs: 12, lg: 8 }}>
          <ChartCard
            title={t("ai.stats.tokenTrendTitle")}
            subtitle={t("ai.stats.tokenTrendSubtitle")}
          >
            {trend.length > 0 ? (
              <LineChart categories={trend.map((r) => r.d)} series={trendSeries} height={300} />
            ) : (
              <EmptyState message={loading ? "加载中…" : "暂无数据"} />
            )}
          </ChartCard>
        </Grid>
        <Grid size={{ xs: 12, lg: 4 }}>
          <ChartCard title="LLM 调用状态" subtitle="成功 / 受限 / 失败占比">
            <EmptyState message="调用状态明细后端暂未开放" />
          </ChartCard>
        </Grid>
        <Grid size={{ xs: 12, lg: 4 }}>
          <ChartCard title="模型使用分布" subtitle="按调用次数">
            {modelPie.length > 0 ? (
              <PieChart data={modelPie} height={300} donut />
            ) : (
              <EmptyState message={loading ? "加载中…" : "暂无数据"} />
            )}
          </ChartCard>
        </Grid>
        <Grid size={{ xs: 12, lg: 8 }}>
          <ChartCard title="操作类型分布" subtitle="按工具 / 动作调用次数">
            {actionCategories.length > 0 ? (
              <BarChart
                categories={actionCategories}
                series={actionSeries}
                height={300}
                horizontal
              />
            ) : (
              <EmptyState message={loading ? "加载中…" : "暂无数据"} />
            )}
          </ChartCard>
        </Grid>
      </Grid>
    </PageStack>
  );
}

export default StatsDashboard;
