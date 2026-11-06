// SPDX-License-Identifier: Apache-2.0
import { useState, useEffect, useMemo, useCallback, useDeferredValue, type ReactNode } from "react";
import { AppearanceProvider } from "@/context/AppearanceContext";
import { MuiProvider } from "@/theme/MuiProvider";
import type { Lang, Density, SidebarTheme } from "@/context/AppearanceContext";
import { useAuthStore } from "@/shared/auth/store";
import { tenantApi } from "@/shared/api/system";
import { SESSION_REFRESHED_EVENT } from "@/shared/api/client";
import {
  LayoutMode,
  Tenant,
  tenantFromDto,
  Tab,
  DASHBOARD_TAB,
  readLayoutPrefs,
  writeLayoutPrefs,
  readFavoriteTabs,
  writeFavoriteTabs,
  readTabState,
  writeTabState,
  AppCtx,
  Ctx,
  useApp,
} from "./appcontext/part-1";

export { tenantFromDto, useApp } from "./appcontext/part-1";
export type { LayoutMode, Tenant } from "./appcontext/part-1";

export function AppProvider({ children }: { children: ReactNode }) {
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [aiPackEnabled, setAiPackEnabled] = useState(true);
  const [locked, setLocked] = useState(false);
  const [initialTabState] = useState(() => readTabState());
  const [tabs, setTabs] = useState<Tab[]>(initialTabState.tabs);
  const [activeTab, setActiveTab] = useState(initialTabState.activeTab);
  const [pageRefreshKey, setPageRefreshKey] = useState(0);
  const [contentFullscreen, setContentFullscreen] = useState(false);
  const [favoriteTabs, setFavoriteTabs] = useState<Tab[]>(() => readFavoriteTabs());
  /* layout */
  const savedLayout = readLayoutPrefs();
  const [layoutMode, setLayoutMode] = useState<LayoutMode>(savedLayout.layoutMode ?? "left");
  const [menuAccordion, setMenuAccordion] = useState(savedLayout.menuAccordion ?? true);
  const [fixedHeader, setFixedHeader] = useState(savedLayout.fixedHeader ?? false);
  const [showBreadcrumb, setShowBreadcrumb] = useState(savedLayout.showBreadcrumb ?? true);
  const [showTagsview, setShowTagsview] = useState(savedLayout.showTagsview ?? true);
  const [showSidebarLogo, setShowSidebarLogo] = useState(savedLayout.showSidebarLogo ?? true);
  const [grayMode, setGrayMode] = useState(savedLayout.grayMode ?? false);
  const [colorWeakMode, setColorWeakMode] = useState(savedLayout.colorWeakMode ?? false);
  const [watermark, setWatermark] = useState(savedLayout.watermark ?? false);
  const [topbarGradient, setTopbarGradient] = useState(savedLayout.topbarGradient ?? false);
  const [menuGradient, setMenuGradient] = useState(savedLayout.menuGradient ?? false);
  const [lockScreenEnabled, setLockScreenEnabled] = useState(savedLayout.lockScreenEnabled ?? true);
  /* tenants */
  const [currentTenant, setCurrentTenant] = useState<Tenant | null>(null);
  const [tenants, setTenants] = useState<Tenant[]>([]);
  const user = useAuthStore((s) => s.user);

  const loadTenants = useCallback(async () => {
    if (!useAuthStore.getState().user) {
      setTenants([]);
      setCurrentTenant(null);
      return;
    }
    try {
      const dtos = await tenantApi.list();
      const mapped = dtos.map((dto, index) => tenantFromDto(dto, index));
      setTenants(mapped);
      setCurrentTenant((prev) => {
        if (prev && mapped.some((t) => t.id === prev.id)) return prev;
        return mapped[0] ?? null;
      });
    } catch {
      setTenants([]);
      setCurrentTenant(null);
    }
  }, []);

  useEffect(() => {
    void loadTenants();
  }, [user, loadTenants]);

  useEffect(() => {
    const onRefresh = () => void loadTenants();
    window.addEventListener(SESSION_REFRESHED_EVENT, onRefresh);
    return () => window.removeEventListener(SESSION_REFRESHED_EVENT, onRefresh);
  }, [loadTenants]);

  const tenantId = currentTenant?.id;
  useEffect(() => {
    if (tenantId) {
      localStorage.setItem("strata_tenant_id", tenantId);
    } else {
      localStorage.removeItem("strata_tenant_id");
    }
  }, [tenantId]);

  const tenantsEnabled = tenants.length > 1;
  /* custom icons */
  const [customNavIcons, setCustomNavIconsMap] = useState<Record<string, string>>({});
  const setCustomNavIcon = useCallback(
    (id: string, icon: string) => setCustomNavIconsMap((prev) => ({ ...prev, [id]: icon })),
    [],
  );
  /* drawer */
  const [layoutDrawerOpen, setLayoutDrawerOpen] = useState(false);
  /* search */
  const [searchOpen, setSearchOpen] = useState(false);
  const [activeNavSection, setActiveNavSection] = useState("dashboard");

  /* Accessibility modes are reflected as body classes (theme/density live in AppearanceProvider). */
  useEffect(() => {
    document.body.classList.toggle("grayscale", grayMode);
    document.body.classList.toggle("color-weak", colorWeakMode);
  }, [grayMode, colorWeakMode]);

  useEffect(() => {
    if (!lockScreenEnabled) setLocked(false);
  }, [lockScreenEnabled]);

  useEffect(() => {
    writeLayoutPrefs({
      layoutMode,
      menuAccordion,
      fixedHeader,
      showBreadcrumb,
      showTagsview,
      showSidebarLogo,
      grayMode,
      colorWeakMode,
      watermark,
      topbarGradient,
      menuGradient,
      lockScreenEnabled,
    });
  }, [
    layoutMode,
    menuAccordion,
    fixedHeader,
    showBreadcrumb,
    showTagsview,
    showSidebarLogo,
    grayMode,
    colorWeakMode,
    watermark,
    topbarGradient,
    menuGradient,
    lockScreenEnabled,
  ]);

  useEffect(() => {
    writeTabState(tabs, activeTab);
  }, [tabs, activeTab]);

  const toggleSidebar = useCallback(() => setSidebarCollapsed((c) => !c), []);

  const openTab = useCallback((tab: Tab) => {
    setTabs((prev) => {
      const idx = prev.findIndex((x) => x.id === tab.id);
      if (idx >= 0) {
        const next = [...prev];
        next[idx] = tab;
        return next;
      }
      return [...prev, tab];
    });
    setActiveTab(tab.id);
  }, []);

  const closeTab = useCallback(
    (id: string) => {
      if (id === "dashboard") return;
      setTabs((prev) => {
        const next = prev.filter((t) => t.id !== id);
        if (activeTab === id && next.length > 0) setActiveTab(next[next.length - 1].id);
        return next.length > 0 ? next : [DASHBOARD_TAB];
      });
    },
    [activeTab],
  );

  const closeOtherTabs = useCallback((keepId: string) => {
    setTabs((prev) => {
      const kept = prev.find((t) => t.id === keepId);
      return kept ? [kept] : [DASHBOARD_TAB];
    });
    setActiveTab(keepId);
  }, []);

  const closeRightTabs = useCallback((id: string) => {
    setTabs((prev) => {
      const idx = prev.findIndex((t) => t.id === id);
      if (idx < 0) return prev;
      const kept = prev.slice(0, idx + 1);
      return kept.length > 0 ? kept : [DASHBOARD_TAB];
    });
    setActiveTab(id);
  }, []);

  const closeAllTabs = useCallback(() => {
    setTabs([DASHBOARD_TAB]);
    setActiveTab("dashboard");
  }, []);

  const refreshPage = useCallback(() => {
    setPageRefreshKey((k) => k + 1);
  }, []);

  const favoriteTab = useCallback((tab: Tab) => {
    const favorites = readFavoriteTabs();
    if (favorites.some((f) => f.path === tab.path)) return false;
    const next = [...favorites, tab];
    writeFavoriteTabs(next);
    setFavoriteTabs(next);
    return true;
  }, []);

  const removeFavoriteTab = useCallback((path: string) => {
    const next = readFavoriteTabs().filter((f) => f.path !== path);
    writeFavoriteTabs(next);
    setFavoriteTabs(next);
  }, []);

  const value = useMemo<AppCtx>(
    () => ({
      sidebarCollapsed,
      toggleSidebar,
      tabs,
      activeTab,
      openTab,
      closeTab,
      closeOtherTabs,
      closeRightTabs,
      closeAllTabs,
      refreshPage,
      pageRefreshKey,
      favoriteTab,
      favoriteTabs,
      removeFavoriteTab,
      contentFullscreen,
      setContentFullscreen,
      aiPackEnabled,
      setAiPackEnabled,
      locked,
      setLocked,
      layoutMode,
      setLayoutMode,
      menuAccordion,
      setMenuAccordion,
      fixedHeader,
      setFixedHeader,
      showBreadcrumb,
      setShowBreadcrumb,
      showTagsview,
      setShowTagsview,
      showSidebarLogo,
      setShowSidebarLogo,
      grayMode,
      setGrayMode,
      colorWeakMode,
      setColorWeakMode,
      watermark,
      setWatermark,
      topbarGradient,
      setTopbarGradient,
      menuGradient,
      setMenuGradient,
      lockScreenEnabled,
      setLockScreenEnabled,
      tenants,
      currentTenant,
      setCurrentTenant,
      tenantsEnabled,
      customNavIcons,
      setCustomNavIcon,
      layoutDrawerOpen,
      setLayoutDrawerOpen,
      searchOpen,
      setSearchOpen,
      activeNavSection,
      setActiveNavSection,
    }),
    [
      tenantsEnabled,
      sidebarCollapsed,
      toggleSidebar,
      tabs,
      activeTab,
      openTab,
      closeTab,
      closeOtherTabs,
      closeRightTabs,
      closeAllTabs,
      refreshPage,
      pageRefreshKey,
      favoriteTab,
      favoriteTabs,
      removeFavoriteTab,
      contentFullscreen,
      aiPackEnabled,
      locked,
      layoutMode,
      menuAccordion,
      fixedHeader,
      showBreadcrumb,
      showTagsview,
      showSidebarLogo,
      grayMode,
      colorWeakMode,
      watermark,
      topbarGradient,
      menuGradient,
      lockScreenEnabled,
      tenants,
      currentTenant,
      customNavIcons,
      setCustomNavIcon,
      layoutDrawerOpen,
      searchOpen,
      activeNavSection,
    ],
  );

  // High-frequency layout changes (sidebar/tabs/drawer toggles) all land in the
  // same value object, so every useApp() consumer re-renders on each one. Rather
  // than splitting the context (which would touch every consumer and is too
  // invasive this round), defer the value: interactive layout updates render
  // synchronously while heavy content subtree reads the deferred value at a lower
  // priority and never blocks the input.
  const deferredValue = useDeferredValue(value);

  return (
    <AppearanceProvider>
      <MuiProvider>
        <Ctx.Provider value={deferredValue}>{children}</Ctx.Provider>
      </MuiProvider>
    </AppearanceProvider>
  );
}

export type { Lang, Density, SidebarTheme };

export type { Tab as AppTab };
