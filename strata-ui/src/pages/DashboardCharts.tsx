// SPDX-License-Identifier: Apache-2.0
/**
 * Dashboard charts section, code-split from the Dashboard route so ECharts only loads
 * after the KPI cards and lists render. Loaded via React.lazy from Dashboard.tsx.
 */
import { useMemo } from "react";
import { Grid } from "@mui/material";
import { ChartCard, LineChart, BarChart, PieChart } from "@/components/ui/chart";
import { useI18n } from "@/shared/i18n";
import type { OperationLogDto, ProductDto, UserDto } from "@/shared/types";

export interface DashboardChartsProps {
  users: UserDto[];
  products: ProductDto[];
  ops: OperationLogDto[];
}

function startOfLocalDay(d: Date): Date {
  return new Date(d.getFullYear(), d.getMonth(), d.getDate());
}

function localDayKey(d: Date): string {
  return `${d.getFullYear()}-${d.getMonth()}-${d.getDate()}`;
}

export default function DashboardCharts({ users, products, ops }: DashboardChartsProps) {
  const { t, lang } = useI18n();

  const enabledUsers = users.filter((u) => u.enabled).length;

  // Category day names come from Intl.DateTimeFormat under the active UI locale
  // (zh -> zh-CN, en -> en-US) rather than hardcoded "Mon".."Sun".
  const locale = lang === "zh" ? "zh-CN" : "en-US";
  const weekdayFmt = useMemo(() => new Intl.DateTimeFormat(locale, { weekday: "short" }), [locale]);

  // Chart data: user status distribution
  const userStatusData = useMemo(
    () => [
      { name: t("common.enabled"), value: enabledUsers },
      { name: t("common.disabled"), value: users.length - enabledUsers },
    ],
    [users, enabledUsers, t],
  );

  // Chart data: operation logs by module
  const opsByModule = useMemo(() => {
    const moduleMap = new Map<string, number>();
    ops.forEach((o) => {
      const module = o.moduleName || t("common.unknown");
      moduleMap.set(module, (moduleMap.get(module) || 0) + 1);
    });
    return Array.from(moduleMap.entries()).map(([name, value]) => ({ name, value }));
  }, [ops, t]);

  // Chart data: new users per calendar day over the trailing 7 days ending today.
  // Users without a parseable createdAt are skipped; empty days render as a flat zero.
  const newUsersTrend = useMemo(() => {
    const today = startOfLocalDay(new Date());
    const days: Date[] = [];
    for (let offset = 6; offset >= 0; offset--) {
      days.push(new Date(today.getFullYear(), today.getMonth(), today.getDate() - offset));
    }
    const counts = new Map<string, number>();
    for (const u of users) {
      if (!u.createdAt) continue;
      const parsed = new Date(u.createdAt);
      if (Number.isNaN(parsed.getTime())) continue;
      const key = localDayKey(startOfLocalDay(parsed));
      counts.set(key, (counts.get(key) ?? 0) + 1);
    }
    return {
      categories: days.map((d) => weekdayFmt.format(d)),
      data: days.map((d) => counts.get(localDayKey(d)) ?? 0),
    };
  }, [users, weekdayFmt]);

  // Chart data: product stock level distribution
  const stockDistribution = useMemo(() => {
    const bins = [
      { label: t("dashboard.stock.outOfStock"), test: (s: number) => s <= 0, count: 0 },
      { label: t("dashboard.stock.low"), test: (s: number) => s <= 50, count: 0 },
      { label: t("dashboard.stock.healthy"), test: (s: number) => s <= 200, count: 0 },
      { label: t("dashboard.stock.plentiful"), test: () => true, count: 0 },
    ];
    products.forEach((p) => {
      const stock = p.stock ?? 0;
      const bin = bins.find((b) => b.test(stock));
      if (bin) bin.count++;
    });
    return bins.map((b) => ({ name: b.label, value: b.count }));
  }, [products, t]);

  return (
    <Grid container spacing={2.5} sx={{ width: "100%", maxWidth: "100%" }}>
      <Grid size={{ xs: 12, lg: 8 }}>
        <ChartCard
          title={t("dashboard.weeklyActivity")}
          subtitle={t("dashboard.weeklyActivityDesc")}
        >
          <LineChart
            categories={newUsersTrend.categories}
            series={[{ name: t("dashboard.newUsers"), data: newUsersTrend.data }]}
            height={280}
            smooth
            areaStyle
          />
        </ChartCard>
      </Grid>
      <Grid size={{ xs: 12, lg: 4 }}>
        <ChartCard title={t("dashboard.userStatus")} subtitle={t("dashboard.userStatusDesc")}>
          <PieChart data={userStatusData} height={280} donut />
        </ChartCard>
      </Grid>
      <Grid size={{ xs: 12, lg: 6 }}>
        <ChartCard title={t("dashboard.opsByModule")} subtitle={t("dashboard.opsByModuleDesc")}>
          <BarChart
            categories={opsByModule.map((d) => d.name)}
            series={[{ name: t("nav.operation.logs"), data: opsByModule.map((d) => d.value) }]}
            height={280}
          />
        </ChartCard>
      </Grid>
      <Grid size={{ xs: 12, lg: 6 }}>
        <ChartCard title={t("dashboard.productPrice")} subtitle={t("dashboard.productPriceDesc")}>
          <BarChart
            categories={stockDistribution.map((d) => d.name)}
            series={[{ name: t("nav.products"), data: stockDistribution.map((d) => d.value) }]}
            height={280}
          />
        </ChartCard>
      </Grid>
    </Grid>
  );
}
