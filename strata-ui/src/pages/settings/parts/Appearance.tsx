// SPDX-License-Identifier: Apache-2.0
import { Box, ButtonBase, Grid, Stack, Typography } from "@mui/material";
import LightModeOutlinedIcon from "@mui/icons-material/LightModeOutlined";
import DarkModeOutlinedIcon from "@mui/icons-material/DarkModeOutlined";
import MonitorOutlinedIcon from "@mui/icons-material/MonitorOutlined";
import CheckIcon from "@mui/icons-material/Check";
import { useAppearance } from "@/context/AppearanceContext";
import { SIDEBAR_THEME_OPTIONS } from "@/components/shell/Sidebar";
import { useI18n } from "@/shared/i18n";
import { ContentCard } from "@/shared/pageKit/PageLayout";
import {
  PageHero as SettingsHero,
  PageWorkspace as SettingsWorkspace,
} from "@/shared/pageKit/PageWorkspace";
import type { SidebarTheme } from "@/context/AppContext";

const selectedTileSx = {
  border: 2,
  borderColor: "primary.main",
  bgcolor: (theme: { palette: { mode: string } }) =>
    theme.palette.mode === "dark" ? "rgba(79, 70, 229, 0.1)" : "rgba(238, 242, 255, 1)",
};

const tileSx = {
  border: 2,
  borderColor: "divider",
  "&:hover": { borderColor: "text.secondary" },
};

export function Appearance() {
  const { colorMode, setColorMode, density, setDensity, sidebarTheme, setSidebarTheme } =
    useAppearance();
  const { t } = useI18n();

  const colorModes = [
    {
      id: "light" as const,
      label: t("settings.appearance.light"),
      Icon: LightModeOutlinedIcon,
      active: colorMode === "light",
    },
    {
      id: "dark" as const,
      label: t("settings.appearance.dark"),
      Icon: DarkModeOutlinedIcon,
      active: colorMode === "dark",
    },
    {
      id: "system" as const,
      label: t("settings.appearance.system"),
      Icon: MonitorOutlinedIcon,
      active: colorMode === "system",
    },
  ];

  return (
    <SettingsWorkspace>
      <SettingsHero title={t("nav.appearance")} subtitle={t("settings.appearance.subtitle")} />

      <Stack spacing={3}>
        <ContentCard>
          <Typography variant="subtitle2" sx={{ fontWeight: 600, mb: 0.5 }}>
            {t("settings.appearance.sidebarTheme")}
          </Typography>
          <Typography variant="caption" color="text.secondary" sx={{ mb: 2, display: "block" }}>
            {t("settings.appearance.sidebarThemeDesc")}
          </Typography>
          <Grid container spacing={1.5}>
            {SIDEBAR_THEME_OPTIONS.map((opt) => {
              const isActive = sidebarTheme === opt.id;
              return (
                <Grid key={opt.id} size={{ xs: 6, sm: 4 }}>
                  <ButtonBase
                    data-testid={`sidebar-theme-${opt.id}`}
                    onClick={() => setSidebarTheme(opt.id as SidebarTheme)}
                    sx={{
                      width: "100%",
                      display: "flex",
                      flexDirection: "column",
                      alignItems: "center",
                      gap: 1,
                      p: 1.5,
                      borderRadius: 3,
                      ...(isActive ? selectedTileSx : tileSx),
                    }}
                  >
                    <Box
                      sx={{
                        width: "100%",
                        height: 64,
                        borderRadius: 2,
                        overflow: "hidden",
                        display: "flex",
                        boxShadow: 1,
                        border: 1,
                        borderColor: "divider",
                      }}
                    >
                      <Box
                        sx={{
                          width: "30%",
                          height: "100%",
                          display: "flex",
                          flexDirection: "column",
                          gap: 0.5,
                          p: 0.75,
                          background: opt.sidebar,
                        }}
                      >
                        <Box
                          sx={{
                            width: "100%",
                            height: 6,
                            borderRadius: 1,
                            bgcolor: "rgba(255,255,255,0.3)",
                          }}
                        />
                        <Box
                          sx={{
                            width: "75%",
                            height: 6,
                            borderRadius: 1,
                            bgcolor: "rgba(255,255,255,0.2)",
                          }}
                        />
                        <Box
                          sx={{
                            width: "75%",
                            height: 6,
                            borderRadius: 1,
                            bgcolor: "rgba(255,255,255,0.2)",
                          }}
                        />
                        <Box
                          sx={{
                            mt: 0.5,
                            width: "100%",
                            height: 6,
                            borderRadius: 1,
                            background: opt.accent,
                            opacity: 0.9,
                          }}
                        />
                        <Box
                          sx={{
                            width: "75%",
                            height: 6,
                            borderRadius: 1,
                            bgcolor: "rgba(255,255,255,0.2)",
                          }}
                        />
                      </Box>
                      <Box
                        sx={{
                          flex: 1,
                          height: "100%",
                          bgcolor: "background.paper",
                          p: 0.75,
                          display: "flex",
                          flexDirection: "column",
                          gap: 0.5,
                        }}
                      >
                        <Box
                          sx={{
                            width: "100%",
                            height: 6,
                            borderRadius: 1,
                            bgcolor: "action.hover",
                          }}
                        />
                        <Stack direction="row" spacing={0.5} sx={{ mt: 0.25 }}>
                          <Box
                            sx={{
                              width: 32,
                              height: 16,
                              borderRadius: 1,
                              background: opt.accent,
                              opacity: 0.7,
                            }}
                          />
                          <Box
                            sx={{ flex: 1, height: 16, borderRadius: 1, bgcolor: "action.hover" }}
                          />
                        </Stack>
                        <Box
                          sx={{
                            width: "100%",
                            height: 6,
                            borderRadius: 1,
                            bgcolor: "action.hover",
                          }}
                        />
                        <Box
                          sx={{
                            width: "66%",
                            height: 6,
                            borderRadius: 1,
                            bgcolor: "action.hover",
                          }}
                        />
                      </Box>
                    </Box>
                    <Stack direction="row" spacing={0.5} sx={{ alignItems: "center" }}>
                      {isActive ? <CheckIcon sx={{ fontSize: 14, color: "primary.main" }} /> : null}
                      <Typography
                        variant="caption"
                        sx={{ fontWeight: 500 }}
                        color={isActive ? "primary.main" : "text.secondary"}
                      >
                        {t(opt.nameKey)}
                      </Typography>
                    </Stack>
                  </ButtonBase>
                </Grid>
              );
            })}
          </Grid>
          <Typography variant="caption" color="text.secondary" sx={{ mt: 1.5, display: "block" }}>
            <Box component="span" sx={{ color: "text.primary", fontWeight: 500 }}>
              {t(SIDEBAR_THEME_OPTIONS.find((o) => o.id === sidebarTheme)?.nameKey ?? "")}:
            </Box>{" "}
            {t(SIDEBAR_THEME_OPTIONS.find((o) => o.id === sidebarTheme)?.descKey ?? "")}
          </Typography>
        </ContentCard>

        <ContentCard>
          <Typography variant="subtitle2" sx={{ fontWeight: 600, mb: 0.5 }}>
            {t("settings.appearance.contentMode")}
          </Typography>
          <Typography variant="caption" color="text.secondary" sx={{ mb: 2, display: "block" }}>
            {t("settings.appearance.contentModeDesc")}
          </Typography>
          <Grid container spacing={1.5}>
            {colorModes.map(({ id, label, Icon, active }) => (
              <Grid key={id} size={{ xs: 4 }}>
                <ButtonBase
                  data-testid={`color-mode-${id}`}
                  onClick={() => setColorMode(id)}
                  sx={{
                    width: "100%",
                    display: "flex",
                    flexDirection: "column",
                    alignItems: "center",
                    gap: 1.25,
                    p: 2,
                    borderRadius: 3,
                    ...(active ? selectedTileSx : tileSx),
                  }}
                >
                  <Icon sx={{ fontSize: 22, color: active ? "primary.main" : "text.secondary" }} />
                  <Typography
                    variant="body2"
                    sx={{ fontWeight: 500 }}
                    color={active ? "primary.main" : "text.secondary"}
                  >
                    {label}
                  </Typography>
                  {id === "system" ? (
                    <Typography variant="caption" color="text.disabled">
                      {t("settings.appearance.followsOs")}
                    </Typography>
                  ) : null}
                </ButtonBase>
              </Grid>
            ))}
          </Grid>
        </ContentCard>

        <ContentCard>
          <Typography variant="subtitle2" sx={{ fontWeight: 600, mb: 0.5 }}>
            {t("settings.appearance.layoutDensity")}
          </Typography>
          <Typography variant="caption" color="text.secondary" sx={{ mb: 2, display: "block" }}>
            {t("settings.appearance.layoutDensityDesc")}
          </Typography>
          <Grid container spacing={1.5}>
            {[
              {
                id: "default" as const,
                label: "settings.appearance.densityDefault",
                desc: "settings.appearance.densityDefaultDesc",
              },
              {
                id: "compact" as const,
                label: "settings.appearance.densityCompact",
                desc: "settings.appearance.densityCompactDesc",
              },
            ].map((d) => {
              const active = density === d.id;
              return (
                <Grid key={d.id} size={{ xs: 6 }}>
                  <ButtonBase
                    onClick={() => setDensity(d.id)}
                    sx={{
                      width: "100%",
                      display: "block",
                      textAlign: "left",
                      p: 2,
                      borderRadius: 3,
                      ...(active ? selectedTileSx : tileSx),
                    }}
                  >
                    <Stack spacing={0.5} sx={{ mb: 1.5 }}>
                      {(d.id === "default" ? [1, 2, 3] : [1, 2, 3, 4]).map((i) => (
                        <Box
                          key={i}
                          sx={{
                            width: "100%",
                            borderRadius: 1,
                            bgcolor: "action.hover",
                            height: d.id === "default" ? 12 : 8,
                          }}
                        />
                      ))}
                    </Stack>
                    <Typography
                      variant="body2"
                      sx={{ fontWeight: 600 }}
                      color={active ? "primary.main" : "text.primary"}
                    >
                      {t(d.label)}
                    </Typography>
                    <Typography
                      variant="caption"
                      color="text.secondary"
                      sx={{ mt: 0.5, display: "block" }}
                    >
                      {t(d.desc)}
                    </Typography>
                  </ButtonBase>
                </Grid>
              );
            })}
          </Grid>
        </ContentCard>
      </Stack>
    </SettingsWorkspace>
  );
}

export default Appearance;
