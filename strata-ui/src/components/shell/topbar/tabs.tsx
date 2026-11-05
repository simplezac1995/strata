// SPDX-License-Identifier: Apache-2.0
// The secondary tab bar shown under the top bar, plus the language/route helpers it needs.

import { useState, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import { Box, IconButton, Tab, Tabs } from "@mui/material";
import CloseIcon from "@mui/icons-material/Close";
import { useApp } from "@/context/AppContext";
import { useAuthStore } from "@/shared/auth/store";
import { useI18n } from "@/shared/i18n";
import type { Lang } from "@/context/AppContext";
import { TabContextMenu, type TabMenuAction } from "@/components/shell/TabContextMenu";
import { NAV, deriveNavFromMenus, isSeparator } from "@/components/shell/navigation";
import { toast } from "@/shared/pageKit/Toast";
import type { AppTab } from "@/context/AppContext";

export const LANGS: { code: Lang; label: string; native: string; abbreviation: string }[] = [
  {
    code: "zh",
    label: "settings.language.zhLabel",
    native: "settings.language.zhNative",
    abbreviation: "settings.language.zhAbbr",
  },
  {
    code: "en",
    label: "settings.language.enLabel",
    native: "English",
    abbreviation: "EN",
  },
];

export function useHasMixSubNav() {
  const { layoutMode, activeNavSection } = useApp();
  const backendMenus = useAuthStore((s) => s.menus);
  return useMemo(() => {
    if (layoutMode !== "mix") return false;
    const base = backendMenus.length ? deriveNavFromMenus(backendMenus) : NAV;
    const entry = base.find((e) => !isSeparator(e) && e.id === activeNavSection);
    return (entry && "children" in entry && (entry.children?.length ?? 0) > 0) ?? false;
  }, [layoutMode, activeNavSection, backendMenus]);
}

export function TabsBar() {
  const {
    tabs,
    activeTab,
    openTab,
    closeTab,
    closeOtherTabs,
    closeRightTabs,
    closeAllTabs,
    refreshPage,
    favoriteTab,
    setContentFullscreen,
  } = useApp();
  const { t } = useI18n();
  const navigate = useNavigate();
  const [menu, setMenu] = useState<{ tab: AppTab; x: number; y: number } | null>(null);

  const activateTab = (tab: AppTab) => {
    openTab(tab);
    navigate(tab.path);
  };

  const closeTabAndNavigate = (tabId: string) => {
    const isActive = tabId === activeTab;
    const remaining = tabs.filter((item) => item.id !== tabId);
    closeTab(tabId);
    if (isActive) {
      const next = remaining.length > 0 ? remaining[remaining.length - 1] : { path: "/dashboard" };
      navigate(next.path);
    }
  };

  const handleMenuAction = (action: TabMenuAction, tab: AppTab) => {
    switch (action) {
      case "refresh":
        activateTab(tab);
        refreshPage();
        break;
      case "close":
        closeTabAndNavigate(tab.id);
        break;
      case "closeOthers":
        closeOtherTabs(tab.id);
        activateTab(tab);
        break;
      case "closeRight":
        closeRightTabs(tab.id);
        if (tab.id !== activeTab) activateTab(tab);
        break;
      case "closeAll":
        closeAllTabs();
        navigate("/dashboard");
        break;
      case "fullscreen":
        activateTab(tab);
        setContentFullscreen(true);
        break;
      case "favorite":
        if (favoriteTab(tab)) toast.success("toast.tab.favorited");
        else toast.info("toast.tab.alreadyFavorited");
        break;
    }
  };

  return (
    <>
      <Tabs
        value={activeTab}
        onChange={(_, tabId) => {
          const tab = tabs.find((item) => item.id === tabId);
          if (tab) activateTab(tab);
        }}
        variant="scrollable"
        scrollButtons="auto"
        sx={{
          minHeight: 40,
          bgcolor: "background.default",
          borderTop: 1,
          borderColor: "divider",
          px: 1,
          "& .MuiTab-root": {
            minHeight: 36,
            textTransform: "none",
            fontSize: "0.72rem",
            fontWeight: 600,
            px: 1.5,
            py: 0.75,
            borderRadius: "8px 8px 0 0",
            mr: 0.5,
            mt: 0.5,
          },
        }}
      >
        {tabs.map((tab) => (
          <Tab
            key={tab.id}
            value={tab.id}
            data-tab-id={tab.id}
            onContextMenu={(e) => {
              e.preventDefault();
              setMenu({ tab, x: e.clientX, y: e.clientY });
            }}
            label={
              <Box sx={{ display: "flex", alignItems: "center", gap: 0.5 }}>
                <span>{t(tab.label)}</span>
                {tab.id !== "dashboard" ? (
                  <IconButton
                    component="span"
                    size="small"
                    aria-label={t("common.dismiss")}
                    onClick={(e) => {
                      e.stopPropagation();
                      closeTabAndNavigate(tab.id);
                    }}
                    sx={{ p: 0.25, opacity: 0.6, "&:hover": { opacity: 1 } }}
                  >
                    <CloseIcon sx={{ fontSize: 12 }} />
                  </IconButton>
                ) : null}
              </Box>
            }
          />
        ))}
      </Tabs>
      {menu ? (
        <TabContextMenu
          tab={menu.tab}
          x={menu.x}
          y={menu.y}
          onAction={handleMenuAction}
          onClose={() => setMenu(null)}
          isLastTab={tabs.length > 0 && tabs[tabs.length - 1].id === menu.tab.id}
        />
      ) : null}
    </>
  );
}
