// SPDX-License-Identifier: Apache-2.0
import { useMemo, useState } from "react";
import SearchIcon from "@mui/icons-material/Search";
import GroupsOutlinedIcon from "@mui/icons-material/GroupsOutlined";
import ShieldOutlinedIcon from "@mui/icons-material/ShieldOutlined";
import { Avatar, Box, Grid, InputAdornment, Stack, TextField, Typography } from "@mui/material";
import { StatusChip } from "@/components/ui/index";
import { userApi } from "@/shared/api/system";
import { useListQuery } from "@/shared/hooks/useListQuery";
import { useI18n } from "@/shared/i18n";
import {
  PageHero as SettingsHero,
  PageMetric as SettingsMetric,
  PageMetricRow as SettingsMetricRow,
  PageSection as SettingsSection,
  PageWorkspace as SettingsWorkspace,
} from "@/shared/pageKit/PageWorkspace";
import type { UserDto } from "@/shared/types";

function initials(u: UserDto) {
  const base = (u.nickname || u.username || "?").trim();
  const parts = base.split(/\s+/);
  if (parts.length >= 2) return (parts[0][0] + parts[1][0]).toUpperCase();
  return base.slice(0, 2).toUpperCase();
}

export function Team() {
  const { t } = useI18n();
  const { data, loading } = useListQuery<UserDto>(
    ["api", "system", "users", "list"],
    () => userApi.list(),
    { staleTime: 5 * 60 * 1000 },
  );
  const [q, setQ] = useState("");

  const filtered = useMemo(() => {
    const needle = q.trim().toLowerCase();
    if (!needle) return data;
    return data.filter(
      (u) =>
        u.username.toLowerCase().includes(needle) ||
        (u.nickname ?? "").toLowerCase().includes(needle),
    );
  }, [data, q]);

  const enabledCount = useMemo(() => data.filter((u) => u.enabled).length, [data]);

  return (
    <SettingsWorkspace>
      <SettingsHero
        title={t("settings.team.title")}
        subtitle={t("settings.team.subtitle")}
        footer={
          <SettingsMetricRow>
            <SettingsMetric
              label={t("settings.team.metricMembers")}
              value={loading ? "—" : data.length}
            />
            <SettingsMetric
              label={t("settings.team.metricActive")}
              value={loading ? "—" : enabledCount}
            />
            <SettingsMetric
              label={t("settings.team.metricShowing")}
              value={loading ? "—" : filtered.length}
              hint={q.trim() ? t("settings.team.metricFiltered") : t("settings.team.metricAll")}
            />
          </SettingsMetricRow>
        }
      />

      <SettingsSection
        title={t("settings.team.sectionDirectory")}
        description={t("settings.team.sectionDirectoryDesc")}
      >
        <TextField
          fullWidth
          value={q}
          onChange={(e) => setQ(e.target.value)}
          placeholder={t("settings.team.searchPlaceholder")}
          slotProps={{
            input: {
              startAdornment: (
                <InputAdornment position="start">
                  <SearchIcon sx={{ color: "text.secondary" }} />
                </InputAdornment>
              ),
            },
          }}
          sx={{ mb: 3, maxWidth: 480 }}
        />

        <Grid container spacing={2.5}>
          {filtered.map((u) => (
            <Grid key={u.id} size={{ xs: 12, sm: 6, lg: 4 }}>
              <Box
                sx={{
                  p: 2.5,
                  height: "100%",
                  borderRadius: 3,
                  border: 1,
                  borderColor: "divider",
                  bgcolor: "background.default",
                  transition: "border-color 0.2s, box-shadow 0.2s",
                  "&:hover": {
                    borderColor: "primary.light",
                    boxShadow: (theme) =>
                      theme.palette.mode === "dark"
                        ? "0 8px 24px rgba(0,0,0,0.35)"
                        : "0 12px 32px rgba(15, 23, 42, 0.08)",
                  },
                }}
              >
                <Stack direction="row" spacing={2} sx={{ alignItems: "center" }}>
                  <Avatar
                    sx={{
                      width: 56,
                      height: 56,
                      bgcolor: "primary.main",
                      color: "primary.contrastText",
                      fontSize: 18,
                      fontWeight: 700,
                    }}
                  >
                    {initials(u)}
                  </Avatar>
                  <Box sx={{ flex: 1, minWidth: 0 }}>
                    <Typography variant="subtitle1" sx={{ fontWeight: 650 }} noWrap>
                      {u.nickname || u.username}
                    </Typography>
                    <Typography
                      variant="body2"
                      color="text.secondary"
                      noWrap
                      sx={{ fontFamily: "monospace" }}
                    >
                      {u.username}
                    </Typography>
                    <Stack direction="row" spacing={1} sx={{ alignItems: "center", mt: 1.25 }}>
                      <StatusChip enabled={u.enabled} />
                      <ShieldOutlinedIcon sx={{ fontSize: 18, color: "text.secondary" }} />
                    </Stack>
                  </Box>
                </Stack>
              </Box>
            </Grid>
          ))}
        </Grid>

        {!loading && filtered.length === 0 ? (
          <Stack spacing={1.5} sx={{ alignItems: "center", py: 8, color: "text.secondary" }}>
            <GroupsOutlinedIcon sx={{ fontSize: 40, opacity: 0.45 }} />
            <Typography variant="body1">{t("settings.team.noMatch")}</Typography>
          </Stack>
        ) : null}

        <Typography variant="body2" color="text.secondary" sx={{ mt: 3 }}>
          {t("settings.team.footer")}
        </Typography>
      </SettingsSection>
    </SettingsWorkspace>
  );
}

export default Team;
