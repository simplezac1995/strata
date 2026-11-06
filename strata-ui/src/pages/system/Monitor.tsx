// SPDX-License-Identifier: Apache-2.0
import { useQuery } from "@tanstack/react-query";
import {
  Box,
  Grid,
  LinearProgress,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Typography,
} from "@mui/material";
import MemoryOutlinedIcon from "@mui/icons-material/MemoryOutlined";
import StorageOutlinedIcon from "@mui/icons-material/StorageOutlined";
import AutorenewOutlinedIcon from "@mui/icons-material/AutorenewOutlined";
import HubOutlinedIcon from "@mui/icons-material/HubOutlined";
import { Card, PageHeader } from "@/components/ui/index";
import { useI18n } from "@/shared/i18n";
import { monitorApi } from "@/shared/api/monitor";
import type { MemoryUsageDto } from "@/shared/api/monitor";

function toMb(bytes: number): string {
  if (!Number.isFinite(bytes) || bytes < 0) return "—";
  if (bytes >= 1024 * 1024 * 1024) return `${(bytes / (1024 * 1024 * 1024)).toFixed(2)} GB`;
  return `${Math.round(bytes / (1024 * 1024))} MB`;
}

function MemoryCard({
  title,
  icon,
  usage,
}: {
  title: string;
  icon: React.ReactNode;
  usage: MemoryUsageDto | undefined;
}) {
  const { t } = useI18n();
  const used = usage?.used ?? 0;
  const max = usage?.max && usage.max > 0 ? usage.max : 1;
  const pct = Math.min(100, Math.round((used / max) * 100));
  return (
    <Card>
      <Stack spacing={1.5}>
        <Stack direction="row" spacing={1.5} sx={{ alignItems: "center" }}>
          <Box
            sx={{
              width: 40,
              height: 40,
              borderRadius: 2,
              bgcolor: "primary.lighter",
              color: "primary.main",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
            }}
          >
            {icon}
          </Box>
          <Box>
            <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
              {title}
            </Typography>
            <Typography variant="caption" color="text.secondary">
              {pct}%
            </Typography>
          </Box>
        </Stack>
        <LinearProgress variant="determinate" value={pct} sx={{ height: 8, borderRadius: 4 }} />
        <Stack direction="row" spacing={2}>
          <Typography variant="caption" color="text.secondary">
            {t("monitor.used")}: {toMb(used)}
          </Typography>
          <Typography variant="caption" color="text.secondary">
            {t("monitor.committed")}: {toMb(usage?.committed ?? 0)}
          </Typography>
          <Typography variant="caption" color="text.secondary">
            {t("monitor.max")}: {toMb(usage?.max ?? 0)}
          </Typography>
        </Stack>
      </Stack>
    </Card>
  );
}

export function Monitor() {
  const { t } = useI18n();
  const { data } = useQuery({
    queryKey: ["api", "monitor", "snapshot"],
    queryFn: () => monitorApi.snapshot(),
    refetchInterval: 5_000,
  });

  const threads = data?.threads;
  const pool = data?.pool;

  return (
    <Box sx={{ width: "100%", minWidth: 0 }}>
      <Stack spacing={3}>
        <PageHeader title={t("nav.monitor")} subtitle={t("monitor.subtitle")} />

        <Grid container spacing={2}>
          <Grid size={{ xs: 12, md: 6 }}>
            <MemoryCard
              title={t("monitor.heap")}
              icon={<MemoryOutlinedIcon />}
              usage={data?.heap}
            />
          </Grid>
          <Grid size={{ xs: 12, md: 6 }}>
            <MemoryCard
              title={t("monitor.nonHeap")}
              icon={<StorageOutlinedIcon />}
              usage={data?.nonHeap}
            />
          </Grid>
        </Grid>

        <Grid container spacing={2}>
          <Grid size={{ xs: 12, md: 6 }}>
            <Card>
              <Stack spacing={1.5}>
                <Stack direction="row" spacing={1.5} sx={{ alignItems: "center", mb: 1 }}>
                  <Box
                    sx={{
                      width: 40,
                      height: 40,
                      borderRadius: 2,
                      bgcolor: "warning.lighter",
                      color: "warning.main",
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "center",
                    }}
                  >
                    <AutorenewOutlinedIcon />
                  </Box>
                  <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
                    {t("monitor.threads")}
                  </Typography>
                </Stack>
                <Grid container spacing={1}>
                  <Grid size={{ xs: 6 }}>
                    <Typography variant="caption" color="text.secondary">
                      {t("monitor.threadsCurrent")}
                    </Typography>
                    <Typography variant="h5">{threads?.current ?? "—"}</Typography>
                  </Grid>
                  <Grid size={{ xs: 6 }}>
                    <Typography variant="caption" color="text.secondary">
                      {t("monitor.threadsPeak")}
                    </Typography>
                    <Typography variant="h5">{threads?.peak ?? "—"}</Typography>
                  </Grid>
                  <Grid size={{ xs: 6 }}>
                    <Typography variant="caption" color="text.secondary">
                      {t("monitor.threadsDaemon")}
                    </Typography>
                    <Typography variant="h5">{threads?.daemon ?? "—"}</Typography>
                  </Grid>
                  <Grid size={{ xs: 6 }}>
                    <Typography variant="caption" color="text.secondary">
                      {t("monitor.threadsTotalStarted")}
                    </Typography>
                    <Typography variant="h5">{threads?.totalStarted ?? "—"}</Typography>
                  </Grid>
                </Grid>
              </Stack>
            </Card>
          </Grid>

          <Grid size={{ xs: 12, md: 6 }}>
            <Card>
              <Stack spacing={1.5}>
                <Stack direction="row" spacing={1.5} sx={{ alignItems: "center", mb: 1 }}>
                  <Box
                    sx={{
                      width: 40,
                      height: 40,
                      borderRadius: 2,
                      bgcolor: "success.lighter",
                      color: "success.main",
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "center",
                    }}
                  >
                    <HubOutlinedIcon />
                  </Box>
                  <Box>
                    <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
                      {t("monitor.pool")}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      {pool?.poolName ?? "HikariCP"}
                    </Typography>
                  </Box>
                </Stack>
                {pool ? (
                  <Grid container spacing={1}>
                    <Grid size={{ xs: 6 }}>
                      <Typography variant="caption" color="text.secondary">
                        {t("monitor.poolActive")}
                      </Typography>
                      <Typography variant="h5">{pool.active}</Typography>
                    </Grid>
                    <Grid size={{ xs: 6 }}>
                      <Typography variant="caption" color="text.secondary">
                        {t("monitor.poolIdle")}
                      </Typography>
                      <Typography variant="h5">{pool.idle}</Typography>
                    </Grid>
                    <Grid size={{ xs: 6 }}>
                      <Typography variant="caption" color="text.secondary">
                        {t("monitor.poolPending")}
                      </Typography>
                      <Typography variant="h5">{pool.pending}</Typography>
                    </Grid>
                    <Grid size={{ xs: 6 }}>
                      <Typography variant="caption" color="text.secondary">
                        {t("monitor.poolMax")} / {t("monitor.poolMinIdle")}: {pool.max} /{" "}
                        {pool.minIdle}
                      </Typography>
                    </Grid>
                  </Grid>
                ) : (
                  <Typography variant="body2" color="text.secondary">
                    {t("common.noData")}
                  </Typography>
                )}
              </Stack>
            </Card>
          </Grid>
        </Grid>

        <Card>
          <Typography variant="subtitle1" sx={{ fontWeight: 600, mb: 1.5 }}>
            {t("monitor.gc")}
          </Typography>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>{t("monitor.gcName")}</TableCell>
                <TableCell align="right">{t("monitor.gcCount")}</TableCell>
                <TableCell align="right">{t("monitor.gcTime")}</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {(data?.gc ?? []).length === 0 ? (
                <TableRow>
                  <TableCell colSpan={3}>
                    <Typography
                      variant="body2"
                      color="text.secondary"
                      sx={{ py: 2, textAlign: "center" }}
                    >
                      {t("common.noData")}
                    </Typography>
                  </TableCell>
                </TableRow>
              ) : (
                (data?.gc ?? []).map((g) => (
                  <TableRow key={g.name}>
                    <TableCell>{g.name}</TableCell>
                    <TableCell align="right">{g.collectionCount}</TableCell>
                    <TableCell align="right">{g.collectionTimeMs} ms</TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </Card>
      </Stack>
    </Box>
  );
}

export default Monitor;
