// SPDX-License-Identifier: Apache-2.0
import { lazy, Suspense, useEffect, useState } from "react";
import { Link as RouterLink } from "react-router-dom";
import { CircularProgress, Grid, Link, Typography } from "@mui/material";
import PeopleOutlinedIcon from "@mui/icons-material/PeopleOutlined";
import ShieldOutlinedIcon from "@mui/icons-material/ShieldOutlined";
import Inventory2OutlinedIcon from "@mui/icons-material/Inventory2Outlined";
import HistoryOutlinedIcon from "@mui/icons-material/HistoryOutlined";
import DescriptionOutlinedIcon from "@mui/icons-material/DescriptionOutlined";
import { Card, KpiCard, PageHeader, StatusChip } from "@/components/ui/index";
import { CardSkeleton } from "@/components/ui/skeleton";
import { useI18n } from "@/shared/i18n";
import { userApi, roleApi, operationLogApi } from "@/shared/api/system";
import { productApi } from "@/shared/api/demo";
import { toast } from "@/shared/pageKit/Toast";
import { PageLink, PageStack, CardSectionHeader, ListRow } from "@/shared/pageKit/PageLayout";
import type { OperationLogDto, ProductDto, RoleDto, UserDto } from "@/shared/types";

// ECharts is heavy; load the charts section (and its ECharts dependency) after first paint.
const DashboardCharts = lazy(() => import("./DashboardCharts"));

export function Dashboard() {
  const { t, lang } = useI18n();
  const [users, setUsers] = useState<UserDto[]>([]);
  const [roles, setRoles] = useState<RoleDto[]>([]);
  const [products, setProducts] = useState<ProductDto[]>([]);
  const [ops, setOps] = useState<OperationLogDto[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    Promise.all([
      userApi.list().catch(() => [] as UserDto[]),
      roleApi.list().catch(() => [] as RoleDto[]),
      productApi.listAll().catch(() => [] as ProductDto[]),
      operationLogApi
        .page(0, 8)
        .then((r) => r.content)
        .catch(() => [] as OperationLogDto[]),
    ])
      .then(([u, r, p, o]) => {
        if (cancelled) return;
        setUsers(u);
        setRoles(r);
        setProducts(p);
        setOps(o);
      })
      .catch(() => toast.error("toast.loadFailed"))
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const today = new Date().toLocaleDateString(lang === "zh" ? "zh-CN" : "en-US", {
    weekday: "long",
    month: "long",
    day: "numeric",
  });

  const enabledUsers = users.filter((u) => u.enabled).length;
  const enabledRoles = roles.filter((r) => r.enabled).length;
  const enabledProducts = products.filter((p) => p.enabled).length;

  const kpis = [
    {
      title: t("nav.users"),
      value: String(users.length),
      hint: `${enabledUsers} ${t("common.enabled")}`,
      icon: <PeopleOutlinedIcon />,
      to: "/system/users",
    },
    {
      title: t("nav.roles"),
      value: String(roles.length),
      hint: `${enabledRoles} ${t("common.enabled")}`,
      icon: <ShieldOutlinedIcon />,
      to: "/system/roles",
    },
    {
      title: t("nav.products"),
      value: String(products.length),
      hint: `${enabledProducts} ${t("common.enabled")}`,
      icon: <Inventory2OutlinedIcon />,
      to: "/demo/products",
    },
    {
      title: t("nav.operation.logs"),
      value: String(ops.length),
      hint: t("shell.search.recent"),
      icon: <HistoryOutlinedIcon />,
      to: "/system/logs/operation",
    },
  ];

  return (
    <PageStack>
      <PageHeader title={t("nav.dashboard")} subtitle={today} />

      <Grid container spacing={2} sx={{ width: "100%", maxWidth: "100%" }}>
        {loading
          ? kpis.map((k) => (
              <Grid key={k.to} size={{ xs: 12, sm: 6, xl: 3 }}>
                <CardSkeleton />
              </Grid>
            ))
          : kpis.map((k) => (
              <Grid key={k.to} size={{ xs: 12, sm: 6, xl: 3 }}>
                <Link component={RouterLink} to={k.to} underline="none" color="inherit">
                  <KpiCard title={k.title} value={k.value} delta={k.hint} icon={k.icon} />
                </Link>
              </Grid>
            ))}
      </Grid>

      {/* Charts are code-split (ECharts) and load after the first paint */}
      <Suspense
        fallback={
          <Card
            sx={{ height: 300, display: "flex", alignItems: "center", justifyContent: "center" }}
          >
            <CircularProgress size={28} />
          </Card>
        }
      >
        <DashboardCharts users={users} products={products} ops={ops} />
      </Suspense>

      <Grid container spacing={2.5} sx={{ width: "100%", maxWidth: "100%" }}>
        <Grid size={{ xs: 12, lg: 6 }}>
          <Card>
            <CardSectionHeader
              title={t("nav.users")}
              action={<PageLink to="/system/users">{t("common.detail")}</PageLink>}
            />
            {users.slice(0, 6).map((u) => (
              <ListRow
                key={u.id}
                primary={u.nickname || u.username}
                secondary={u.username}
                end={<StatusChip enabled={u.enabled} />}
              />
            ))}
            {users.length === 0 && !loading ? (
              <Typography
                variant="body2"
                color="text.secondary"
                sx={{ py: 3, textAlign: "center" }}
              >
                {t("common.noData")}
              </Typography>
            ) : null}
          </Card>
        </Grid>

        <Grid size={{ xs: 12, lg: 6 }}>
          <Card>
            <CardSectionHeader
              title={t("nav.operation.logs")}
              icon={<DescriptionOutlinedIcon fontSize="small" color="primary" />}
              action={<PageLink to="/system/logs/operation">{t("common.detail")}</PageLink>}
            />
            {ops.map((o) => (
              <ListRow
                key={o.id}
                primary={`${o.moduleName} · ${o.actionName}`}
                secondary={o.operatorName || t("common.emDash")}
                end={
                  <Typography
                    variant="caption"
                    color="text.secondary"
                    sx={{ fontFamily: "monospace" }}
                  >
                    {o.createdAt?.replace("T", " ").slice(0, 16)}
                  </Typography>
                }
              />
            ))}
            {ops.length === 0 && !loading ? (
              <Typography
                variant="body2"
                color="text.secondary"
                sx={{ py: 3, textAlign: "center" }}
              >
                {t("common.noData")}
              </Typography>
            ) : null}
          </Card>
        </Grid>
      </Grid>
    </PageStack>
  );
}

export default Dashboard;
