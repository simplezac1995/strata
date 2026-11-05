// SPDX-License-Identifier: Apache-2.0
// Sidebar navigation entries for the "nav.overview" group, in display order.

import type { NavEntry } from "@/components/shell/navTypes";
import { LayoutDashboard, Map, Palette, Layers, Bell } from "@/components/shell/navIcons";

export const NAV_OVERVIEW: NavEntry[] = [
  { type: "separator", label: "nav.overview" },
  { id: "dashboard", label: "nav.dashboard", icon: LayoutDashboard, path: "/dashboard" },
  { id: "notifications", label: "nav.notifications", icon: Bell, path: "/notifications" },
  { id: "cover", label: "nav.cover", icon: Map, path: "/cover", devOnly: true },
  {
    id: "foundations",
    label: "nav.foundations",
    icon: Palette,
    path: "/foundations",
    devOnly: true,
  },
  { id: "components", label: "nav.components", icon: Layers, path: "/components", devOnly: true },
];
