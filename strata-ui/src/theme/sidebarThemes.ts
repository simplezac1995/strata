// SPDX-License-Identifier: Apache-2.0
import type { SxProps, Theme } from "@mui/material/styles";
import { alpha } from "@mui/material/styles";
import type { SidebarTheme } from "@/context/AppearanceContext";
import { STRATA_PRIMARY } from "@/theme/muiTheme";

export interface SidebarTokens {
  shell: { bgcolor: string; borderColor: string };
  logo: {
    hoverBg: string;
    borderColor: string;
    text: string;
    subtext: string;
    accentBg: string;
  };
  nav: {
    idle: { color: string; hoverBg: string; hoverColor: string };
    active: { bgcolor: string; color: string };
    accentBar: string;
    icons: { active: string; idle: string; hover: string };
  };
  sectionLabel: string;
  separator: string;
  chevron: string;
  user: {
    footerBorder: string;
    name: string;
    role: string;
    hoverBg: string;
    settingsIcon: string;
  };
  scroll: { thumb: string; thumbHover: string };
}

const dark: SidebarTokens = {
  shell: { bgcolor: "#0F172A", borderColor: "rgba(255,255,255,0.06)" },
  logo: {
    hoverBg: "rgba(255,255,255,0.04)",
    borderColor: "rgba(255,255,255,0.06)",
    text: "#ffffff",
    subtext: "#64748b",
    accentBg: "#6366f1",
  },
  nav: {
    idle: { color: "#94a3b8", hoverBg: "rgba(255,255,255,0.06)", hoverColor: "#f1f5f9" },
    active: { bgcolor: "rgba(255,255,255,0.10)", color: "#ffffff" },
    accentBar: "#818cf8",
    icons: { active: "#a5b4fc", idle: "#64748b", hover: "#e2e8f0" },
  },
  sectionLabel: "#334155",
  separator: "rgba(255,255,255,0.06)",
  chevron: "#334155",
  user: {
    footerBorder: "rgba(255,255,255,0.06)",
    name: "#e2e8f0",
    role: "#64748b",
    hoverBg: "rgba(255,255,255,0.05)",
    settingsIcon: "#475569",
  },
  scroll: { thumb: "rgba(255,255,255,0.14)", thumbHover: "rgba(255,255,255,0.28)" },
};

const lightAppLight: SidebarTokens = {
  shell: { bgcolor: "#FFFFFF", borderColor: "#E2E8F0" },
  logo: {
    hoverBg: "#F8FAFC",
    borderColor: "#E2E8F0",
    text: "#0F172A",
    subtext: "#64748B",
    accentBg: STRATA_PRIMARY,
  },
  nav: {
    idle: { color: "#475569", hoverBg: "#F1F5F9", hoverColor: "#0F172A" },
    active: { bgcolor: "#EEF2FF", color: "#4338CA" },
    accentBar: STRATA_PRIMARY,
    icons: { active: STRATA_PRIMARY, idle: "#94A3B8", hover: "#475569" },
  },
  sectionLabel: "#94A3B8",
  separator: "#E2E8F0",
  chevron: "#CBD5E1",
  user: {
    footerBorder: "#E2E8F0",
    name: "#1E293B",
    role: "#64748B",
    hoverBg: "#F1F5F9",
    settingsIcon: "#94A3B8",
  },
  scroll: { thumb: "rgba(15,23,42,0.12)", thumbHover: "rgba(15,23,42,0.22)" },
};

const lightAppDark: SidebarTokens = {
  shell: { bgcolor: "#161B22", borderColor: "#21262D" },
  logo: {
    hoverBg: "rgba(255,255,255,0.04)",
    borderColor: "#21262D",
    text: "#f3f4f6",
    subtext: "#6b7280",
    accentBg: "#4f46e5",
  },
  nav: {
    idle: { color: "#9ca3af", hoverBg: "rgba(255,255,255,0.05)", hoverColor: "#f3f4f6" },
    active: { bgcolor: "rgba(99,102,241,0.10)", color: "#a5b4fc" },
    accentBar: "#818cf8",
    icons: { active: "#a5b4fc", idle: "#4b5563", hover: "#9ca3af" },
  },
  sectionLabel: "#4b5563",
  separator: "#21262D",
  chevron: "#4b5563",
  user: {
    footerBorder: "#21262D",
    name: "#e5e7eb",
    role: "#6b7280",
    hoverBg: "rgba(255,255,255,0.04)",
    settingsIcon: "#4b5563",
  },
  scroll: { thumb: "rgba(255,255,255,0.12)", thumbHover: "rgba(255,255,255,0.24)" },
};

const carbon: SidebarTokens = {
  shell: { bgcolor: "#161616", borderColor: "rgba(255,255,255,0.05)" },
  logo: {
    hoverBg: "rgba(255,255,255,0.04)",
    borderColor: "rgba(255,255,255,0.05)",
    text: "#ffffff",
    subtext: "#525252",
    accentBg: "#f59e0b",
  },
  nav: {
    idle: { color: "#737373", hoverBg: "rgba(255,255,255,0.05)", hoverColor: "#f5f5f5" },
    active: { bgcolor: "rgba(255,255,255,0.08)", color: "#ffffff" },
    accentBar: "#fbbf24",
    icons: { active: "#fbbf24", idle: "#404040", hover: "#a3a3a3" },
  },
  sectionLabel: "#262626",
  separator: "rgba(255,255,255,0.05)",
  chevron: "#262626",
  user: {
    footerBorder: "rgba(255,255,255,0.05)",
    name: "#e5e5e5",
    role: "#525252",
    hoverBg: "rgba(255,255,255,0.05)",
    settingsIcon: "#404040",
  },
  scroll: { thumb: "rgba(255,255,255,0.12)", thumbHover: "rgba(255,255,255,0.24)" },
};

const forest: SidebarTokens = {
  shell: { bgcolor: "#0B1A0E", borderColor: "rgba(20,83,45,0.30)" },
  logo: {
    hoverBg: "rgba(20,83,45,0.20)",
    borderColor: "rgba(20,83,45,0.30)",
    text: "#ffffff",
    subtext: "#15803d",
    accentBg: "#22c55e",
  },
  nav: {
    idle: { color: "rgba(74,222,128,0.90)", hoverBg: "rgba(20,83,45,0.30)", hoverColor: "#dcfce7" },
    active: { bgcolor: "rgba(22,101,52,0.40)", color: "#ffffff" },
    accentBar: "#4ade80",
    icons: { active: "#86efac", idle: "#166534", hover: "#86efac" },
  },
  sectionLabel: "rgba(20,83,45,0.70)",
  separator: "rgba(20,83,45,0.30)",
  chevron: "rgba(20,83,45,0.70)",
  user: {
    footerBorder: "rgba(20,83,45,0.30)",
    name: "#dcfce7",
    role: "#16a34a",
    hoverBg: "rgba(20,83,45,0.20)",
    settingsIcon: "#14532d",
  },
  scroll: { thumb: "rgba(74,222,128,0.22)", thumbHover: "rgba(74,222,128,0.38)" },
};

const crimson: SidebarTokens = {
  shell: { bgcolor: "#18080B", borderColor: "rgba(136,19,55,0.30)" },
  logo: {
    hoverBg: "rgba(136,19,55,0.20)",
    borderColor: "rgba(136,19,55,0.30)",
    text: "#ffffff",
    subtext: "#9f1239",
    accentBg: "#f43f5e",
  },
  nav: {
    idle: {
      color: "rgba(253,164,175,0.60)",
      hoverBg: "rgba(136,19,55,0.25)",
      hoverColor: "#ffe4e6",
    },
    active: { bgcolor: "rgba(159,18,57,0.40)", color: "#ffffff" },
    accentBar: "#fb7185",
    icons: { active: "#fda4af", idle: "rgba(136,19,55,0.80)", hover: "#fecdd3" },
  },
  sectionLabel: "rgba(136,19,55,0.60)",
  separator: "rgba(136,19,55,0.30)",
  chevron: "rgba(136,19,55,0.60)",
  user: {
    footerBorder: "rgba(136,19,55,0.30)",
    name: "#ffe4e6",
    role: "#be123c",
    hoverBg: "rgba(136,19,55,0.20)",
    settingsIcon: "#881337",
  },
  scroll: { thumb: "rgba(251,113,133,0.22)", thumbHover: "rgba(251,113,133,0.38)" },
};

function customTokens(bg: string, accent: string): SidebarTokens {
  return {
    shell: { bgcolor: bg, borderColor: "rgba(255,255,255,0.06)" },
    logo: {
      hoverBg: "rgba(255,255,255,0.05)",
      borderColor: "rgba(255,255,255,0.06)",
      text: "#ffffff",
      subtext: "rgba(255,255,255,0.40)",
      accentBg: accent,
    },
    nav: {
      idle: {
        color: "rgba(255,255,255,0.50)",
        hoverBg: "rgba(255,255,255,0.07)",
        hoverColor: "#ffffff",
      },
      active: { bgcolor: "rgba(255,255,255,0.10)", color: "#ffffff" },
      accentBar: accent,
      icons: { active: accent, idle: "rgba(255,255,255,0.55)", hover: "rgba(255,255,255,0.85)" },
    },
    sectionLabel: "rgba(255,255,255,0.20)",
    separator: "rgba(255,255,255,0.06)",
    chevron: "rgba(255,255,255,0.20)",
    user: {
      footerBorder: "rgba(255,255,255,0.06)",
      name: "rgba(255,255,255,0.90)",
      role: "rgba(255,255,255,0.40)",
      hoverBg: "rgba(255,255,255,0.05)",
      settingsIcon: "rgba(255,255,255,0.25)",
    },
    scroll: { thumb: "rgba(255,255,255,0.14)", thumbHover: "rgba(255,255,255,0.28)" },
  };
}

export function resolveSidebarTokens(
  theme: SidebarTheme,
  appDark: boolean,
  customBg?: string,
  customAccent?: string,
): SidebarTokens {
  if (theme === "custom") {
    return customTokens(customBg ?? "#1C1D20", customAccent ?? STRATA_PRIMARY);
  }
  if (theme === "light") return appDark ? lightAppDark : lightAppLight;
  if (theme === "carbon") return carbon;
  if (theme === "forest") return forest;
  if (theme === "crimson") return crimson;
  return dark;
}

export const sidebarShellBaseSx: SxProps<Theme> = {
  position: { xs: "fixed", md: "relative" },
  zIndex: { xs: 30, md: "auto" },
  flexShrink: 0,
  height: "100%",
  display: "flex",
  flexDirection: "column",
  transition: "all 0.2s",
  overflow: "hidden",
  borderRight: 1,
};

export function navItemButtonSx(
  tokens: SidebarTokens,
  active: boolean,
  depth: number,
  collapsed: boolean,
): SxProps<Theme> {
  const pl = collapsed ? 1 : depth === 0 ? 1.5 : depth === 1 ? 3.75 : depth === 2 ? 5.75 : 7.25;
  return {
    display: "flex",
    alignItems: "center",
    gap: collapsed ? 0 : 1.25,
    borderRadius: 1.5,
    mx: 1,
    my: 0.25,
    minHeight: "var(--nav-item-h, 40px)",
    py: 0.75,
    px: collapsed ? 1 : undefined,
    pl: collapsed ? 1 : pl,
    pr: collapsed ? 1 : 1.5,
    justifyContent: collapsed ? "center" : "flex-start",
    position: "relative",
    transition: "background-color 0.15s, color 0.15s",
    ...(active
      ? {
          bgcolor: tokens.nav.active.bgcolor,
          color: tokens.nav.active.color,
        }
      : {
          color: tokens.nav.idle.color,
          "&:hover": {
            bgcolor: tokens.nav.idle.hoverBg,
            color: tokens.nav.idle.hoverColor,
            "& [data-nav-icon]": { color: tokens.nav.icons.hover },
          },
        }),
  };
}

export function navIconSx(
  tokens: SidebarTokens,
  active: boolean,
  groupOpen: boolean,
): SxProps<Theme> {
  let color = tokens.nav.icons.idle;
  if (active) color = tokens.nav.icons.active;
  else if (groupOpen) color = tokens.nav.icons.hover;
  return { color, flexShrink: 0, transition: "color 0.15s, background-color 0.15s" };
}

/** Icon tile in sidebar rows — clearer hit area and active accent. */
export function navIconWrapSx(
  tokens: SidebarTokens,
  active: boolean,
  groupOpen: boolean,
): SxProps<Theme> {
  const accent = tokens.nav.accentBar;
  return {
    ...navIconSx(tokens, active, groupOpen),
    display: "inline-flex",
    alignItems: "center",
    justifyContent: "center",
    width: 32,
    height: 32,
    borderRadius: 1.5,
    flexShrink: 0,
    ...(active
      ? { bgcolor: alpha(accent, 0.16) }
      : groupOpen
        ? { bgcolor: alpha(accent, 0.08) }
        : {}),
  };
}

/** Scrollable nav column — thin, theme-aware scrollbar (avoids chunky OS default). */
export function sidebarNavScrollSx(tokens: SidebarTokens): SxProps<Theme> {
  const thumb = tokens.scroll.thumb;
  const thumbHover = tokens.scroll.thumbHover;
  return {
    flex: 1,
    overflowY: "auto",
    overflowX: "hidden",
    py: 1.5,
    scrollbarWidth: "thin",
    scrollbarColor: `${thumb} transparent`,
    "&::-webkit-scrollbar": { width: 4 },
    "&::-webkit-scrollbar-track": { bgcolor: "transparent" },
    "&::-webkit-scrollbar-thumb": {
      bgcolor: thumb,
      borderRadius: 8,
      "&:hover": { bgcolor: thumbHover },
    },
  };
}
