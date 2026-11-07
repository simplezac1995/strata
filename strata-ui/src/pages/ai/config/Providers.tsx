// SPDX-License-Identifier: Apache-2.0
import { useState } from "react";
import { Box, Chip, LinearProgress, Stack, Typography } from "@mui/material";
import { Button, DataTable, Tabs, TextField, Select, Switch } from "@/components/ui/index";
import { PageStack, ContentCard } from "@/shared/pageKit/PageLayout";
import { AiPageHeader } from "@/components/ui/index";

type GatewayTab = "policy" | "accounts" | "audit" | "billing" | "daily";

const TABS: { id: GatewayTab; label: string }[] = [
  { id: "policy", label: "默认策略" },
  { id: "accounts", label: "账户管理" },
  { id: "audit", label: "额度审计" },
  { id: "billing", label: "调用账目" },
  { id: "daily", label: "日统计" },
];

export function Providers() {
  const [tab, setTab] = useState<GatewayTab>("accounts");

  return (
    <PageStack>
      <AiPageHeader title="AI 网关" subtitle="管理模型调用账户、Token 额度、调用账目和日统计。" />

      <Tabs tabs={TABS} active={tab} onChange={(id) => setTab(id as GatewayTab)} />

      {tab === "policy" && <PolicyPanel />}
      {tab === "accounts" && <AccountsPanel />}
      {tab === "audit" && <AuditPanel />}
      {tab === "billing" && <BillingLedgerPanel />}
      {tab === "daily" && <DailyStatsPanel />}
    </PageStack>
  );
}

function PolicyPanel() {
  return (
    <ContentCard sx={{ p: 2.5 }}>
      <Stack
        direction="row"
        sx={{
          alignItems: "flex-start",
          justifyContent: "space-between",
          mb: 2,
          flexWrap: "wrap",
          gap: 1,
        }}
      >
        <Box>
          <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
            租户默认策略
          </Typography>
          <Typography variant="caption" sx={{ color: "text.secondary" }}>
            用户未单独配置时继承这里的额度与限流；来源 IP 白名单仅在租户策略生效。
          </Typography>
        </Box>
        <Stack direction="row" spacing={1} sx={{ alignItems: "center" }}>
          <Chip label="启用" size="small" color="success" variant="outlined" />
          <Button variant="outlined" color="default" disabled title="策略配置（后端未开放）">
            配置策略
          </Button>
          <Button variant="contained" disabled title="额度调整（后端未开放）">
            调整额度
          </Button>
        </Stack>
      </Stack>

      <Box sx={{ display: "flex", gap: 2, flexWrap: "wrap" }}>
        <QuotaCard title="日额度" used="—" limit="不限" reserved="预占 —" />
        <QuotaCard title="永久额度" used="—" limit="不限" reserved="预占 —" />
      </Box>

      <Box sx={{ display: "flex", gap: 2, flexWrap: "wrap", mt: 2 }}>
        <StatBlock label="RPM" value="不限" />
        <StatBlock label="TPM" value="不限" />
        <StatBlock label="并发" value="不限" />
        <StatBlock label="来源 IP" value="全部来源" />
      </Box>
    </ContentCard>
  );
}

function QuotaCard({
  title,
  used,
  limit,
  reserved,
}: {
  title: string;
  used: string;
  limit: string;
  reserved: string;
}) {
  return (
    <Box
      sx={{ flex: "1 1 280px", minWidth: 260, bgcolor: "action.hover", borderRadius: 1.5, p: 2 }}
    >
      <Typography variant="caption" sx={{ color: "text.secondary" }}>
        {title}
      </Typography>
      <Typography variant="h6" sx={{ fontWeight: 700, my: 0.5 }}>
        {used} / {limit}
      </Typography>
      <LinearProgress variant="determinate" value={0} sx={{ my: 1, height: 6, borderRadius: 3 }} />
      <Typography variant="caption" sx={{ color: "text.secondary" }}>
        {reserved}
      </Typography>
    </Box>
  );
}

function StatBlock({ label, value }: { label: string; value: string }) {
  return (
    <Box
      sx={{
        flex: "1 1 160px",
        minWidth: 150,
        bgcolor: "action.hover",
        borderRadius: 1.5,
        px: 2,
        py: 1.5,
        display: "flex",
        justifyContent: "space-between",
        alignItems: "center",
      }}
    >
      <Typography variant="caption" sx={{ color: "text.secondary" }}>
        {label}
      </Typography>
      <Typography variant="body2" sx={{ fontWeight: 600 }}>
        {value}
      </Typography>
    </Box>
  );
}

function FilterBar({ children }: { children: React.ReactNode }) {
  return (
    <ContentCard sx={{ p: 2 }}>
      <Box
        sx={{
          display: "flex",
          gap: 1.5,
          flexWrap: "wrap",
          alignItems: "center",
          "& .MuiTextField-root, & .MuiFormControl-root": { width: "auto", minWidth: 160 },
        }}
      >
        {children}
      </Box>
    </ContentCard>
  );
}

function AccountsPanel() {
  const cols = [
    { key: "username", title: "用户名", render: (r: { id: string }) => r.id },
    { key: "status", title: "状态", render: () => <Switch checked={false} disabled /> },
    {
      key: "quota",
      title: "Token 额度",
      render: () => (
        <Typography variant="caption" sx={{ color: "text.secondary" }}>
          日 — / 永久 —
        </Typography>
      ),
    },
    {
      key: "limit",
      title: "RPM / TPM / 并发",
      render: () => (
        <Typography variant="caption" sx={{ color: "text.secondary" }}>
          均继承租户：不限
        </Typography>
      ),
    },
    {
      key: "lastCall",
      title: "最近调用",
      render: () => (
        <Typography variant="caption" sx={{ color: "text.secondary" }}>
          —
        </Typography>
      ),
    },
    {
      key: "actions",
      title: "操作",
      render: () => (
        <Stack direction="row" spacing={0.5}>
          <Button variant="text" size="sm" disabled>
            配置
          </Button>
          <Button variant="text" size="sm" disabled>
            额度
          </Button>
          <Button variant="text" size="sm" color="danger" disabled>
            关闭
          </Button>
        </Stack>
      ),
    },
  ];
  return (
    <>
      <FilterBar>
        <TextField label="用户名" placeholder="请输入用户名" sx={{ width: 200 }} />
        <Select
          label="状态"
          value=""
          onChange={() => undefined}
          options={[
            { value: "", label: "全部" },
            { value: "on", label: "启用" },
            { value: "off", label: "停用" },
          ]}
          sx={{ width: 160 }}
        />
        <Button variant="contained">查询</Button>
        <Button variant="outlined" color="default">
          重置
        </Button>
      </FilterBar>
      <Button variant="contained" disabled title="批量开通（后端未开放）">
        ＋ 批量开通
      </Button>
      <DataTable columns={cols} data={[] as { id: string }[]} rowKey={(r) => r.id} />
    </>
  );
}

function AuditPanel() {
  const cols = [
    { key: "event", title: "事件" },
    { key: "username", title: "用户名" },
    { key: "period", title: "周期" },
    { key: "delta", title: "调整量 (M)" },
    { key: "after", title: "调整后 (M)" },
    { key: "reason", title: "原因" },
    { key: "operator", title: "操作人" },
    { key: "time", title: "时间" },
  ];
  return (
    <>
      <FilterBar>
        <TextField label="用户名" placeholder="用户名" sx={{ width: 180 }} />
        <TextField label="请求 ID" placeholder="请求 ID" sx={{ width: 200 }} />
        <Button variant="contained">查询</Button>
        <Button variant="outlined" color="default">
          重置
        </Button>
      </FilterBar>
      <DataTable columns={cols} data={[] as { id: string }[]} rowKey={(r) => r.id} />
    </>
  );
}

function BillingLedgerPanel() {
  const cols = [
    { key: "result", title: "结果" },
    { key: "username", title: "用户名" },
    { key: "requestId", title: "请求 ID" },
    { key: "model", title: "模型" },
    { key: "status", title: "状态" },
    { key: "tokens", title: "总 Token" },
    { key: "cost", title: "耗时" },
    { key: "code", title: "错误码" },
    { key: "time", title: "时间" },
  ];
  return (
    <>
      <FilterBar>
        <TextField label="用户名" placeholder="用户名" sx={{ width: 180 }} />
        <TextField label="请求 ID" placeholder="请求 ID" sx={{ width: 200 }} />
        <TextField label="模型" placeholder="模型名称" sx={{ width: 180 }} />
        <Button variant="contained">查询</Button>
        <Button variant="outlined" color="default">
          重置
        </Button>
      </FilterBar>
      <DataTable columns={cols} data={[] as { id: string }[]} rowKey={(r) => r.id} />
    </>
  );
}

function DailyStatsPanel() {
  const cols = [
    { key: "date", title: "日期" },
    { key: "username", title: "用户名" },
    { key: "model", title: "模型" },
    { key: "success", title: "成功" },
    { key: "rejected", title: "拒绝" },
    { key: "failed", title: "失败" },
    { key: "input", title: "输入 (M)" },
    { key: "output", title: "输出 (M)" },
    { key: "total", title: "总 (M)" },
  ];
  return (
    <>
      <FilterBar>
        <TextField label="用户名" placeholder="用户名" sx={{ width: 180 }} />
        <TextField label="模型" placeholder="模型名称" sx={{ width: 180 }} />
        <Button variant="contained">查询</Button>
        <Button variant="outlined" color="default">
          重置
        </Button>
      </FilterBar>
      <DataTable columns={cols} data={[] as { id: string }[]} rowKey={(r) => r.id} />
    </>
  );
}

export default Providers;
