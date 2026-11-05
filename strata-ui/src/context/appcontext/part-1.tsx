// SPDX-License-Identifier: Apache-2.0
// Split out of AppContext.tsx by scripts/auto-split.mjs.

import { createContext, useContext } from "react";
import type { TenantDto } from "@/shared/types";
import { STRATA_PRIMARY } from "@/theme/muiTheme";

export type LayoutMode = "left" | "mix" | "top" | "split";

export interface Tenant {
  id: string;
  name: string;
  slug: string;
  plan: "free" | "pro" | "enterprise";
  avatar: string; /* initials */
  color: string; /* bg color */
}

export const TENANT_COLORS = [STRATA_PRIMARY, "#0EA5E9", "#10B981", "#F59E0B", "#EC4899"];

export function tenantFromDto(dto: TenantDto, index: number): Tenant {
  const initials =
    dto.name
      .split(/\s+/)
      .filter(Boolean)
      .map((part) => part[0])
      .join("")
      .slice(0, 2)
      .toUpperCase() || "?";
  const planRaw = dto.plan?.toLowerCase() ?? "free";
  const plan: Tenant["plan"] =
    planRaw === "enterprise" || planRaw === "pro" || planRaw === "free" ? planRaw : "free";
  return {
    id: dto.id,
    name: dto.name,
    slug: dto.code,
    plan,
    avatar: initials,
    color: TENANT_COLORS[index % TENANT_COLORS.length],
  };
}

export interface Tab {
  id: string;
  label: string;
  path: string;
}

export const FAVORITES_STORAGE_KEY = "strata_favorite_tabs:v1";

export const LAYOUT_PREFS_KEY = "strata_layout_prefs:v1";

export const TABS_STORAGE_KEY = "strata_tabs:v1";

export const DASHBOARD_TAB: Tab = { id: "dashboard", label: "nav.dashboard", path: "/dashboard" };

export interface LayoutPrefs {
  layoutMode: LayoutMode;
  menuAccordion: boolean;
  fixedHeader: boolean;
  showBreadcrumb: boolean;
  showTagsview: boolean;
  showSidebarLogo: boolean;
  grayMode: boolean;
  colorWeakMode: boolean;
  watermark: boolean;
  topbarGradient: boolean;
  menuGradient: boolean;
  lockScreenEnabled: boolean;
}

export function readLayoutPrefs(): Partial<LayoutPrefs> {
  try {
    const raw = localStorage.getItem(LAYOUT_PREFS_KEY);
    if (!raw) return {};
    const parsed = JSON.parse(raw) as Partial<LayoutPrefs>;
    return parsed && typeof parsed === "object" ? parsed : {};
  } catch {
    return {};
  }
}

export function writeLayoutPrefs(prefs: LayoutPrefs) {
  try {
    localStorage.setItem(LAYOUT_PREFS_KEY, JSON.stringify(prefs));
  } catch {
    /* quota / private mode */
  }
}

export function readFavoriteTabs(): Tab[] {
  try {
    const raw = localStorage.getItem(FAVORITES_STORAGE_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw) as Tab[];
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

export function writeFavoriteTabs(tabs: Tab[]) {
  try {
    localStorage.setItem(FAVORITES_STORAGE_KEY, JSON.stringify(tabs));
  } catch {
    /* quota / private mode */
  }
}

export interface PersistedTabs {
  tabs: Tab[];
  activeTab: string;
}

export function readTabState(): PersistedTabs {
  try {
    const raw = localStorage.getItem(TABS_STORAGE_KEY);
    if (!raw) return { tabs: [DASHBOARD_TAB], activeTab: "dashboard" };
    const parsed = JSON.parse(raw) as Partial<PersistedTabs>;
    if (!parsed || !Array.isArray(parsed.tabs) || parsed.tabs.length === 0) {
      return { tabs: [DASHBOARD_TAB], activeTab: "dashboard" };
    }
    const tabs = parsed.tabs.filter(
      (t): t is Tab => !!t && typeof t.id === "string" && typeof t.path === "string",
    );
    if (tabs.length === 0) return { tabs: [DASHBOARD_TAB], activeTab: "dashboard" };
    const hasDashboard = tabs.some((t) => t.id === DASHBOARD_TAB.id);
    const normalizedTabs = hasDashboard ? tabs : [DASHBOARD_TAB, ...tabs];
    const activeTab =
      parsed.activeTab && normalizedTabs.some((t) => t.id === parsed.activeTab)
        ? parsed.activeTab
        : normalizedTabs[0].id;
    return { tabs: normalizedTabs, activeTab };
  } catch {
    return { tabs: [DASHBOARD_TAB], activeTab: "dashboard" };
  }
}

export function writeTabState(tabs: Tab[], activeTab: string) {
  try {
    localStorage.setItem(TABS_STORAGE_KEY, JSON.stringify({ tabs, activeTab }));
  } catch {
    /* quota / private mode */
  }
}

export interface AppCtx {
  sidebarCollapsed: boolean;
  toggleSidebar: () => void;
  tabs: Tab[];
  activeTab: string;
  openTab: (t: Tab) => void;
  closeTab: (id: string) => void;
  closeOtherTabs: (id: string) => void;
  closeRightTabs: (id: string) => void;
  closeAllTabs: () => void;
  refreshPage: () => void;
  pageRefreshKey: number;
  favoriteTab: (tab: Tab) => boolean;
  favoriteTabs: Tab[];
  removeFavoriteTab: (path: string) => void;
  contentFullscreen: boolean;
  setContentFullscreen: (v: boolean) => void;
  aiPackEnabled: boolean;
  setAiPackEnabled: (v: boolean) => void;
  locked: boolean;
  setLocked: (v: boolean) => void;
  /* layout config */
  layoutMode: LayoutMode;
  setLayoutMode: (m: LayoutMode) => void;
  menuAccordion: boolean;
  setMenuAccordion: (v: boolean) => void;
  fixedHeader: boolean;
  setFixedHeader: (v: boolean) => void;
  showBreadcrumb: boolean;
  setShowBreadcrumb: (v: boolean) => void;
  showTagsview: boolean;
  setShowTagsview: (v: boolean) => void;
  showSidebarLogo: boolean;
  setShowSidebarLogo: (v: boolean) => void;
  grayMode: boolean;
  setGrayMode: (v: boolean) => void;
  colorWeakMode: boolean;
  setColorWeakMode: (v: boolean) => void;
  watermark: boolean;
  setWatermark: (v: boolean) => void;
  topbarGradient: boolean;
  setTopbarGradient: (v: boolean) => void;
  menuGradient: boolean;
  setMenuGradient: (v: boolean) => void;
  lockScreenEnabled: boolean;
  setLockScreenEnabled: (v: boolean) => void;
  /* tenant */
  tenants: Tenant[];
  currentTenant: Tenant | null;
  setCurrentTenant: (t: Tenant | null) => void;
  tenantsEnabled: boolean;
  /* custom nav icons: navItemId -> icon component name */
  customNavIcons: Record<string, string>;
  setCustomNavIcon: (id: string, icon: string) => void;
  /* drawer */
  layoutDrawerOpen: boolean;
  setLayoutDrawerOpen: (v: boolean) => void;
  /* search */
  searchOpen: boolean;
  setSearchOpen: (v: boolean) => void;
  /** Active top-level nav section (mix / split layouts). */
  activeNavSection: string;
  setActiveNavSection: (id: string) => void;
}

export const Ctx = createContext<AppCtx>(null!);

export const useApp = () => useContext(Ctx);
