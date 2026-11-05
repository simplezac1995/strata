// SPDX-License-Identifier: Apache-2.0
// Theme presets offered in the sidebar settings popover.

import type { SidebarTheme } from "@/context/AppearanceContext";
import { STRATA_PRIMARY } from "@/theme/muiTheme";

export const SIDEBAR_THEME_OPTIONS: {
  id: SidebarTheme;
  nameKey: string;
  sidebar: string;
  accent: string;
  descKey: string;
}[] = [
  {
    id: "dark",
    nameKey: "shell.sidebar.theme.navy",
    sidebar: "#0F172A",
    accent: "#818CF8",
    descKey: "shell.sidebar.theme.navy.desc",
  },
  {
    id: "light",
    nameKey: "shell.sidebar.theme.light",
    sidebar: "#FAFAFA",
    accent: STRATA_PRIMARY,
    descKey: "shell.sidebar.theme.light.desc",
  },
  {
    id: "carbon",
    nameKey: "shell.sidebar.theme.carbon",
    sidebar: "#161616",
    accent: "#FBBF24",
    descKey: "shell.sidebar.theme.carbon.desc",
  },
  {
    id: "forest",
    nameKey: "shell.sidebar.theme.forest",
    sidebar: "#0B1A0E",
    accent: "#4ADE80",
    descKey: "shell.sidebar.theme.forest.desc",
  },
  {
    id: "crimson",
    nameKey: "shell.sidebar.theme.crimson",
    sidebar: "#18080B",
    accent: "#FB7185",
    descKey: "shell.sidebar.theme.crimson.desc",
  },
  {
    id: "custom",
    nameKey: "shell.sidebar.theme.custom",
    sidebar: "#1C1D20",
    accent: STRATA_PRIMARY,
    descKey: "shell.sidebar.theme.custom.desc",
  },
];
