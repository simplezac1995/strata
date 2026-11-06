// SPDX-License-Identifier: Apache-2.0
import { useQuery } from "@tanstack/react-query";
import { Box, Grid, Stack, Typography, useTheme } from "@mui/material";
import { TrendingDownOutlined, TrendingUpOutlined, PeopleAltOutlined } from "@mui/icons-material";
import { LineChart } from "@/components/ui/chart";
import { PageFill } from "@/shared/pageKit/PageLayout";
import { useI18n } from "@/shared/i18n";
import { bigScreenApi } from "@/shared/api/bigScreen";

const REALTIME_KEY = ["api", "dashboard", "realtime"];
const GAME_FLOW_KEY = ["api", "dashboard", "game-flow"];

/** Polling interval for the real-time numbers (ms). */
const POLL_MS = 30_000;

/** minor units (cents) -> display amount string. */
function amount(minor: number): string {
  return (minor / 100).toFixed(2);
}

/**
 * Ops big-screen: headline realtime totals, a 7-day game-flow trend and a
 * bottom strip of live counters. All figures poll every 30s.
 *
 * Coloring is derived from the active MUI theme (light/dark) rather than a
 * fixed dark palette, so the screen matches the rest of the admin shell.
 */
export function BigScreen() {
  const { t } = useI18n();
  const theme = useTheme();

  const { data: realtime } = useQuery({
    queryKey: REALTIME_KEY,
    queryFn: () => bigScreenApi.realtime(),
    refetchInterval: POLL_MS,
  });

  const { data: flow } = useQuery({
    queryKey: GAME_FLOW_KEY,
    queryFn: () => bigScreenApi.gameFlow(7),
    refetchInterval: POLL_MS,
  });

  const categories = (flow ?? []).map((p) => p.date);
  const betSeries = (flow ?? []).map((p) => Number((p.gameBetAmountMinor / 100).toFixed(2)));
  const payoutSeries = (flow ?? []).map((p) => Number((p.gamePayoutAmountMinor / 100).toFixed(2)));

  const chartOption = {
    backgroundColor: "transparent",
    textStyle: { color: theme.palette.text.primary },
    legend: { textStyle: { color: theme.palette.text.secondary } },
    tooltip: {
      backgroundColor: theme.palette.background.paper,
      borderColor: theme.palette.divider,
      textStyle: { color: theme.palette.text.primary },
    },
    xAxis: {
      type: "category",
      boundaryGap: false,
      axisLine: { lineStyle: { color: theme.palette.divider } },
      axisLabel: { color: theme.palette.text.secondary },
    },
    yAxis: {
      type: "value",
      axisLabel: { color: theme.palette.text.secondary },
      splitLine: { lineStyle: { color: theme.palette.divider } },
    },
  };

  return (
    <PageFill>
      <Box
        sx={{
          flex: 1,
          minHeight: 0,
          bgcolor: "background.default",
          color: "text.primary",
          p: 3,
          borderRadius: 2,
          display: "flex",
          flexDirection: "column",
          gap: 3,
          overflow: "auto",
        }}
      >
        {/* Headline KPIs */}
        <Grid container spacing={3}>
          <Grid size={{ xs: 12, md: 4 }}>
            <StatBlock
              label={t("bigScreen.recharge")}
              value={amount(realtime?.todayRechargeAmountMinor ?? 0)}
              sub={t("bigScreen.count").replace("{n}", String(realtime?.todayRechargeCount ?? 0))}
              icon={<TrendingUpOutlined />}
              accent={theme.palette.success.main}
            />
          </Grid>
          <Grid size={{ xs: 12, md: 4 }}>
            <StatBlock
              label={t("bigScreen.withdraw")}
              value={amount(realtime?.todayWithdrawAmountMinor ?? 0)}
              sub={t("bigScreen.count").replace("{n}", String(realtime?.todayWithdrawCount ?? 0))}
              icon={<TrendingDownOutlined />}
              accent={theme.palette.primary.main}
            />
          </Grid>
          <Grid size={{ xs: 12, md: 4 }}>
            <StatBlock
              label={t("bigScreen.online")}
              value={String(realtime?.onlineCount ?? 0)}
              sub={t("bigScreen.activeToday").replace(
                "{n}",
                String(realtime?.activeUsersToday ?? 0),
              )}
              icon={<PeopleAltOutlined />}
              accent={theme.palette.warning.main}
            />
          </Grid>
        </Grid>

        {/* 7-day game flow trend */}
        <Box
          sx={{
            flex: 1,
            minHeight: 360,
            bgcolor: "background.paper",
            border: 1,
            borderColor: "divider",
            borderRadius: 2,
            p: 2,
          }}
        >
          <Typography variant="subtitle1" sx={{ mb: 1, color: "text.primary" }}>
            {t("bigScreen.gameFlowTitle")}
          </Typography>
          <LineChart
            height={320}
            categories={categories}
            series={[
              {
                name: t("bigScreen.series.bet"),
                data: betSeries,
                color: theme.palette.primary.main,
              },
              {
                name: t("bigScreen.series.payout"),
                data: payoutSeries,
                color: theme.palette.success.main,
              },
            ]}
            option={chartOption}
          />
        </Box>

        {/* Bottom live counters */}
        <Stack
          direction="row"
          spacing={4}
          sx={{
            justifyContent: "space-between",
            flexWrap: "wrap",
            bgcolor: "background.paper",
            border: 1,
            borderColor: "divider",
            borderRadius: 2,
            px: 3,
            py: 1.5,
          }}
        >
          <LiveCounter
            label={t("bigScreen.liveRecharge")}
            value={amount(realtime?.todayRechargeAmountMinor ?? 0)}
          />
          <LiveCounter
            label={t("bigScreen.liveWithdraw")}
            value={amount(realtime?.todayWithdrawAmountMinor ?? 0)}
          />
          <LiveCounter
            label={t("bigScreen.liveOnline")}
            value={String(realtime?.onlineCount ?? 0)}
          />
          <LiveCounter
            label={t("bigScreen.liveActive")}
            value={String(realtime?.activeUsersToday ?? 0)}
          />
        </Stack>
      </Box>
    </PageFill>
  );
}

function StatBlock({
  label,
  value,
  sub,
  icon,
  accent,
}: {
  label: string;
  value: string;
  sub: string;
  icon: React.ReactNode;
  accent: string;
}) {
  return (
    <Box
      sx={{
        bgcolor: "background.paper",
        border: 1,
        borderColor: "divider",
        borderRadius: 2,
        px: 3,
        py: 2.5,
      }}
    >
      <Stack direction="row" spacing={1} sx={{ alignItems: "center", color: "text.secondary" }}>
        <Box sx={{ color: accent }}>{icon}</Box>
        <Typography variant="overline">{label}</Typography>
      </Stack>
      <Typography
        variant="h3"
        sx={{ mt: 1, fontWeight: 700, letterSpacing: "-0.02em", color: "text.primary" }}
      >
        {value}
      </Typography>
      <Typography variant="caption" sx={{ color: "text.secondary" }}>
        {sub}
      </Typography>
    </Box>
  );
}

function LiveCounter({ label, value }: { label: string; value: string }) {
  return (
    <Stack spacing={0.25}>
      <Typography variant="caption" sx={{ color: "text.secondary" }}>
        {label}
      </Typography>
      <Typography
        variant="h6"
        sx={{ fontWeight: 700, color: "primary.main", fontFamily: "monospace" }}
      >
        {value}
      </Typography>
    </Stack>
  );
}

export default BigScreen;
