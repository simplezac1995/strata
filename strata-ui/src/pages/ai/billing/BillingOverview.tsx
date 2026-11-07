// SPDX-License-Identifier: Apache-2.0
import { useMemo, useState } from "react";
import { Stack, Typography } from "@mui/material";
import { useQuery } from "@tanstack/react-query";
import {
  AiPageHeader,
  Button,
  DataTable,
  Pagination,
  Tabs,
  TextField,
} from "@/components/ui/index";
import { LineChart } from "@/components/ui/chart";
import { useI18n } from "@/shared/i18n";
import { aiBillingApi } from "@/shared/api/aiBilling";
import { PageStack, ContentCard } from "@/shared/pageKit/PageLayout";

type RangeKey = "7" | "30" | "90";

const RANGE_TABS: { id: RangeKey; label: string }[] = [
  { id: "7", label: "近7天" },
  { id: "30", label: "近30天" },
  { id: "90", label: "近90天" },
];

function formatInt(n: number): string {
  return new Intl.NumberFormat().format(Math.round(n));
}

export function BillingOverview() {
  const { t } = useI18n();
  const [range, setRange] = useState<RangeKey>("30");
  const days = Number(range);
  const [userKw, setUserKw] = useState("");

  const summaryQuery = useQuery({
    queryKey: ["api", "ai", "billing", "summary", range],
    queryFn: () => aiBillingApi.summary(days),
  });

  const modelQuery = useQuery({
    queryKey: ["api", "ai", "billing", "aggregate", range, "model"],
    queryFn: () => aiBillingApi.aggregate(days, "model"),
  });

  const daily = useMemo(() => summaryQuery.data?.daily ?? [], [summaryQuery.data]);
  const aggregates = modelQuery.data ?? [];

  const trend = useMemo(() => {
    if (daily.length === 0) return null;
    return {
      categories: daily.map((d) => d.date.slice(5)),
      series: [{ name: t("ai.billing.totalTokens"), data: daily.map((d) => d.totalTokens) }],
    };
  }, [daily, t]);

  const cols = [
    {
      key: "groupKey",
      title: "模型名称",
      render: (r: (typeof aggregates)[number]) => (
        <Typography variant="body2" sx={{ fontWeight: 600, fontFamily: "monospace" }}>
          {r.groupKey || t("ai.billing.unknownModel")}
        </Typography>
      ),
    },
    {
      key: "callCount",
      title: "调用次数",
      render: (r: (typeof aggregates)[number]) => (
        <Typography variant="caption" sx={{ fontFamily: "monospace" }}>
          {formatInt(r.callCount)}
        </Typography>
      ),
    },
    {
      key: "promptTokens",
      title: "prompt tokens",
      render: (r: (typeof aggregates)[number]) => (
        <Typography variant="caption" sx={{ fontFamily: "monospace" }}>
          {formatInt(r.promptTokens)}
        </Typography>
      ),
    },
    {
      key: "completionTokens",
      title: "completion",
      render: (r: (typeof aggregates)[number]) => (
        <Typography variant="caption" sx={{ fontFamily: "monospace" }}>
          {formatInt(r.completionTokens)}
        </Typography>
      ),
    },
    {
      key: "totalTokens",
      title: "tokens",
      render: (r: (typeof aggregates)[number]) => (
        <Typography variant="caption" sx={{ fontFamily: "monospace", fontWeight: 600 }}>
          {formatInt(r.totalTokens)}
        </Typography>
      ),
    },
  ];

  return (
    <PageStack>
      <AiPageHeader title="账单管理" subtitle="按日查看模型调用 Token 消耗趋势与模型维度明细。" />

      <Tabs
        tabs={RANGE_TABS.map((tab) => ({ id: tab.id, label: tab.label }))}
        active={range}
        onChange={(id) => setRange(id as RangeKey)}
      />

      <ContentCard sx={{ p: 2.5 }}>
        <Typography variant="body2" sx={{ fontWeight: 600, mb: 1 }}>
          Token 消耗趋势
        </Typography>
        {trend ? (
          <LineChart categories={trend.categories} series={trend.series} height={300} areaStyle />
        ) : (
          <Typography variant="caption" sx={{ color: "text.secondary" }}>
            {summaryQuery.isLoading ? t("common.loading") : t("ai.billing.noData")}
          </Typography>
        )}
      </ContentCard>

      <ContentCard sx={{ p: 2 }}>
        <Stack
          direction="row"
          spacing={1.5}
          sx={{ flexWrap: "wrap", alignItems: "center", mb: 1.5 }}
        >
          <TextField
            label="用户名"
            placeholder="请输入用户名"
            value={userKw}
            onChange={(e) => setUserKw(e.target.value)}
            sx={{ width: 220 }}
          />
          <Button variant="contained" onClick={() => modelQuery.refetch()}>
            查询
          </Button>
          <Button variant="outlined" color="default" onClick={() => setUserKw("")}>
            重置
          </Button>
        </Stack>
        <DataTable
          columns={cols}
          data={aggregates}
          loading={modelQuery.isLoading}
          rowKey={(r) => r.groupKey}
          selectable
        />
        <Pagination
          page={1}
          pageSize={aggregates.length || 10}
          total={aggregates.length}
          onPage={() => undefined}
          onPageSize={() => undefined}
        />
      </ContentCard>
    </PageStack>
  );
}

export default BillingOverview;
