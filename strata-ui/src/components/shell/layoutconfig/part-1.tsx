// SPDX-License-Identifier: Apache-2.0
// Split out of layoutConfig.tsx by scripts/auto-split.mjs.

import type { ReactNode } from "react";
import { Box, ButtonBase, Grid, Paper, Stack, Typography, useTheme } from "@mui/material";
import CheckIcon from "@mui/icons-material/Check";
import { Switch } from "@/components/ui/index";
import { SIDEBAR_THEME_OPTIONS } from "@/components/shell/Sidebar";
import type { LayoutMode } from "@/context/AppContext";
import type { Density, SidebarTheme } from "@/context/AppearanceContext";
import { useI18n } from "@/shared/i18n";
import { configRowSx, configSectionLabelSx, selectionTileSx } from "@/theme/shellStyles";
import { LayoutPreview } from "../layoutConfig";

export function ConfigSection({ label, children }: { label: string; children: ReactNode }) {
  return (
    <Box component="section" sx={{ pt: 2.5, "&:first-of-type": { pt: 0.5 } }}>
      <Typography component="h3" sx={configSectionLabelSx}>
        {label}
      </Typography>
      <Box sx={{ mt: 1.5 }}>{children}</Box>
    </Box>
  );
}

export function ConfigSwitchRow({
  label,
  checked,
  onChange,
  disabled,
}: {
  label: string;
  checked: boolean;
  onChange: (v: boolean) => void;
  disabled?: boolean;
}) {
  const theme = useTheme();
  return (
    <Box
      sx={{
        ...configRowSx(theme),
        ...(disabled ? { opacity: 0.4, pointerEvents: "none" } : {}),
      }}
    >
      <Typography variant="body2" color="text.secondary">
        {label}
      </Typography>
      <Switch checked={checked} onChange={onChange} />
    </Box>
  );
}

export const LAYOUT_MODE_IDS: LayoutMode[] = ["left", "mix", "top", "split"];

export const LAYOUT_LABEL_KEYS: Record<LayoutMode, string> = {
  left: "shell.layoutLeft",
  mix: "shell.layoutMix",
  top: "shell.layoutTop",
  split: "shell.layoutSplit",
};

export function LayoutModePicker({
  value,
  onChange,
  t,
}: {
  value: LayoutMode;
  onChange: (mode: LayoutMode) => void;
  t: (key: string) => string;
}) {
  const theme = useTheme();

  return (
    <Grid container spacing={1.25}>
      {LAYOUT_MODE_IDS.map((id) => {
        const active = value === id;
        return (
          <Grid key={id} size={6}>
            <ButtonBase
              onClick={() => onChange(id)}
              sx={{ ...selectionTileSx(active, theme), width: "100%", textAlign: "center" }}
            >
              <Box
                sx={{
                  width: "100%",
                  height: 52,
                  borderRadius: 1,
                  overflow: "hidden",
                  border: 1,
                  borderColor: "divider",
                }}
              >
                <LayoutPreview mode={id} theme={theme} />
              </Box>
              <Stack direction="row" spacing={0.5} sx={{ alignItems: "center", minHeight: 16 }}>
                {active ? <CheckIcon sx={{ fontSize: 14, color: "primary.main" }} /> : null}
                <Typography
                  variant="caption"
                  sx={{
                    fontWeight: 500,
                    color: active ? "primary.main" : "text.secondary",
                  }}
                >
                  {t(LAYOUT_LABEL_KEYS[id])}
                </Typography>
              </Stack>
            </ButtonBase>
          </Grid>
        );
      })}
    </Grid>
  );
}

export function SidebarThemePreview({
  sidebar,
  accent,
  compact,
}: {
  sidebar: string;
  accent: string;
  compact?: boolean;
}) {
  const theme = useTheme();
  return (
    <Box
      sx={{
        width: "100%",
        height: compact ? 44 : 64,
        borderRadius: 1,
        overflow: "hidden",
        display: "flex",
        border: 1,
        borderColor: theme.palette.mode === "light" ? "rgba(0,0,0,0.06)" : "rgba(255,255,255,0.06)",
      }}
    >
      <Box
        sx={{
          width: "30%",
          display: "flex",
          flexDirection: "column",
          gap: 0.5,
          p: 0.5,
          justifyContent: "center",
          bgcolor: sidebar,
        }}
      >
        <Box
          sx={{ width: "100%", height: 4, borderRadius: 999, bgcolor: "rgba(255,255,255,0.25)" }}
        />
        <Box
          sx={{ width: "75%", height: 4, borderRadius: 999, bgcolor: "rgba(255,255,255,0.15)" }}
        />
        <Box
          sx={{ width: "75%", height: 4, borderRadius: 999, bgcolor: "rgba(255,255,255,0.15)" }}
        />
        <Box sx={{ width: "100%", height: 4, borderRadius: 999, bgcolor: accent, opacity: 0.9 }} />
      </Box>
      <Box
        sx={{
          flex: 1,
          p: 0.5,
          display: "flex",
          flexDirection: "column",
          gap: 0.5,
          bgcolor: "background.paper",
        }}
      >
        <Box sx={{ width: "100%", height: 4, borderRadius: 0.5, bgcolor: "action.hover" }} />
        <Stack direction="row" spacing={0.5}>
          <Box sx={{ width: 24, height: 10, borderRadius: 0.5, bgcolor: accent, opacity: 0.65 }} />
          <Box sx={{ flex: 1, height: 10, borderRadius: 0.5, bgcolor: "action.hover" }} />
        </Stack>
        <Box sx={{ width: "66%", height: 4, borderRadius: 0.5, bgcolor: "action.hover" }} />
      </Box>
    </Box>
  );
}

export function SidebarThemePicker({
  value,
  onChange,
  customSidebarBg,
  customSidebarAccent,
  compact,
}: {
  value: SidebarTheme;
  onChange: (theme: SidebarTheme) => void;
  customSidebarBg?: string;
  customSidebarAccent?: string;
  compact?: boolean;
}) {
  const { t } = useI18n();
  const theme = useTheme();

  return (
    <Grid container spacing={1.25}>
      {SIDEBAR_THEME_OPTIONS.map((opt) => {
        const active = value === opt.id;
        const isCustom = opt.id === "custom";
        const sidebar = isCustom && customSidebarBg ? customSidebarBg : opt.sidebar;
        const accent = isCustom && customSidebarAccent ? customSidebarAccent : opt.accent;
        return (
          <Grid key={opt.id} size={6}>
            <ButtonBase
              onClick={() => onChange(opt.id)}
              title={t(opt.descKey)}
              sx={{ ...selectionTileSx(active, theme), width: "100%", textAlign: "left" }}
            >
              <SidebarThemePreview sidebar={sidebar} accent={accent} compact={compact} />
              <Stack
                direction="row"
                spacing={0.5}
                sx={{ alignItems: "center", justifyContent: "center", width: "100%" }}
              >
                {active ? <CheckIcon sx={{ fontSize: 14, color: "primary.main" }} /> : null}
                <Typography
                  variant="caption"
                  noWrap
                  sx={{ fontWeight: 500, color: active ? "primary.main" : "text.secondary" }}
                >
                  {t(opt.nameKey)}
                </Typography>
              </Stack>
            </ButtonBase>
          </Grid>
        );
      })}
    </Grid>
  );
}

export function CustomSidebarColorsPanel({
  customSidebarBg,
  setCustomSidebarBg,
  customSidebarAccent,
  setCustomSidebarAccent,
  t,
}: {
  customSidebarBg: string;
  setCustomSidebarBg: (c: string) => void;
  customSidebarAccent: string;
  setCustomSidebarAccent: (c: string) => void;
  t: (key: string) => string;
}) {
  return (
    <Paper variant="outlined" sx={{ mt: 1.5, p: 1.5 }}>
      <Typography sx={configSectionLabelSx}>{t("shell.layout.customColors")}</Typography>
      <Stack spacing={1.5} sx={{ mt: 1.5 }}>
        <Stack direction="row" spacing={1.5} sx={{ alignItems: "center" }}>
          <Typography variant="caption" color="text.secondary" sx={{ width: 80, flexShrink: 0 }}>
            {t("shell.layout.sidebarBg")}
          </Typography>
          <Stack direction="row" spacing={1} sx={{ alignItems: "center", minWidth: 0 }}>
            <Box
              component="input"
              type="color"
              value={customSidebarBg}
              onChange={(e) => setCustomSidebarBg(e.target.value)}
              sx={{
                width: 32,
                height: 32,
                borderRadius: 1,
                cursor: "pointer",
                border: 1,
                borderColor: "divider",
                p: 0.25,
                bgcolor: "transparent",
              }}
            />
            <Typography variant="caption" component="code" color="text.secondary" noWrap>
              {customSidebarBg}
            </Typography>
          </Stack>
        </Stack>
        <Stack direction="row" spacing={1.5} sx={{ alignItems: "center" }}>
          <Typography variant="caption" color="text.secondary" sx={{ width: 80, flexShrink: 0 }}>
            {t("shell.layout.accent")}
          </Typography>
          <Stack direction="row" spacing={1} sx={{ alignItems: "center", minWidth: 0 }}>
            <Box
              component="input"
              type="color"
              value={customSidebarAccent}
              onChange={(e) => setCustomSidebarAccent(e.target.value)}
              sx={{
                width: 32,
                height: 32,
                borderRadius: 1,
                cursor: "pointer",
                border: 1,
                borderColor: "divider",
                p: 0.25,
                bgcolor: "transparent",
              }}
            />
            <Typography variant="caption" component="code" color="text.secondary" noWrap>
              {customSidebarAccent}
            </Typography>
          </Stack>
        </Stack>
        <Typography variant="caption" color="text.secondary" sx={{ lineHeight: 1.6 }}>
          {t("shell.layout.darkBgTip")}
        </Typography>
      </Stack>
    </Paper>
  );
}

export const DENSITY_OPTIONS: { id: Density; labelKey: string; descKey: string; rows: number }[] = [
  {
    id: "default",
    labelKey: "settings.appearance.densityDefault",
    descKey: "settings.appearance.densityDefaultDesc",
    rows: 3,
  },
  {
    id: "compact",
    labelKey: "settings.appearance.densityCompact",
    descKey: "settings.appearance.densityCompactDesc",
    rows: 4,
  },
];

export function DensityPicker({
  value,
  onChange,
  t,
}: {
  value: Density;
  onChange: (d: Density) => void;
  t: (key: string) => string;
}) {
  const theme = useTheme();

  return (
    <Grid container spacing={1.25}>
      {DENSITY_OPTIONS.map((d) => {
        const active = value === d.id;
        return (
          <Grid key={d.id} size={6}>
            <ButtonBase
              onClick={() => onChange(d.id)}
              sx={{
                ...selectionTileSx(active, theme),
                width: "100%",
                textAlign: "left",
                p: 1.5,
                alignItems: "stretch",
              }}
            >
              <Stack spacing={0.5} sx={{ mb: 1 }}>
                {Array.from({ length: d.rows }, (_, i) => (
                  <Box
                    key={i}
                    sx={{
                      width: "100%",
                      borderRadius: 0.5,
                      bgcolor: "action.hover",
                      height: d.id === "default" ? 10 : 6,
                    }}
                  />
                ))}
              </Stack>
              <Typography
                variant="caption"
                sx={{ fontWeight: 500, color: active ? "primary.main" : "text.primary" }}
              >
                {t(d.labelKey)}
              </Typography>
              <Typography
                variant="caption"
                color="text.secondary"
                sx={{ mt: 0.25, lineHeight: 1.4 }}
              >
                {t(d.descKey)}
              </Typography>
            </ButtonBase>
          </Grid>
        );
      })}
    </Grid>
  );
}
