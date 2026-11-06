// SPDX-License-Identifier: Apache-2.0
import { useEffect, useRef, type ReactNode, lazy, Suspense } from "react";
import { Box, Backdrop, IconButton, Paper, Stack, Typography } from "@mui/material";
import MinimizeIcon from "@mui/icons-material/Minimize";
import { Sidebar } from "@/components/shell/Sidebar";
import { TopBar, TabsBar, useHasMixSubNav } from "@/components/shell/TopBar";
import { BreadcrumbBar } from "@/components/shell/BreadcrumbBar";
import { LayoutDrawer } from "@/components/shell/LayoutDrawer";
import { LayoutSync } from "@/components/shell/LayoutSync";
import { Watermark } from "@/components/shell/Watermark";
import { FloatingCopilot } from "@/components/shell/FloatingCopilot";
import { PageView } from "@/components/shell/PageView";
import { routeLabel } from "@/components/shell/navigation";
import { useApp } from "@/context/AppContext";
import { useAuthStore } from "@/shared/auth/store";
import { useLocation } from "react-router-dom";
import { SearchModal } from "@/components/shell/SearchModal";
import { useHotkeys } from "@/components/shell/useHotkeys";
import { useI18n } from "@/shared/i18n";

const LockScreen = lazy(() =>
  import("@/pages/auth/LockScreen").then((m) => ({ default: m.LockScreen })),
);

function MainArea({ scrollable = true }: { scrollable?: boolean }) {
  const { watermark } = useApp();
  const authUser = useAuthStore((s) => s.user);
  const { t } = useI18n();
  const watermarkText = authUser?.nickname?.trim() || authUser?.username || t("shell.admin");

  return (
    <Box
      component="main"
      sx={{
        position: "relative",
        flex: 1,
        minHeight: scrollable ? 0 : undefined,
        overflowY: scrollable ? "auto" : "visible",
        overflowX: "clip",
        p: { xs: 2.5, sm: 3, lg: 3.5 },
        bgcolor: "background.default",
        display: "flex",
        flexDirection: "column",
      }}
    >
      {watermark && <Watermark text={watermarkText} />}
      <Box
        sx={{
          position: "relative",
          zIndex: 2,
          minWidth: 0,
          maxWidth: "100%",
          flex: 1,
          minHeight: 0,
          display: "flex",
          flexDirection: "column",
        }}
      >
        <PageView />
      </Box>
    </Box>
  );
}

/** Header + body column; fixedHeader toggles whether chrome scrolls away with content. */
function AppShellColumn({ header, children }: { header: ReactNode; children: ReactNode }) {
  const { fixedHeader } = useApp();

  if (fixedHeader) {
    return (
      <Stack direction="column" sx={{ flex: 1, minWidth: 0, minHeight: 0, overflow: "hidden" }}>
        <Box sx={{ flexShrink: 0 }}>{header}</Box>
        {children}
      </Stack>
    );
  }

  return (
    <Box
      sx={{
        flex: 1,
        minWidth: 0,
        minHeight: 0,
        overflowY: "auto",
        overflowX: "clip",
        display: "flex",
        flexDirection: "column",
      }}
    >
      {header}
      {children}
    </Box>
  );
}

function MixLayout({ header }: { header: React.ReactNode }) {
  const hasSubNav = useHasMixSubNav();
  const { showTagsview, fixedHeader } = useApp();

  return (
    <AppShellColumn header={header}>
      {hasSubNav ? (
        <Stack
          direction="row"
          sx={{ flex: 1, minHeight: fixedHeader ? 0 : undefined, minWidth: 0 }}
        >
          <Sidebar variant="panel" />
          <Stack
            direction="column"
            sx={{ flex: 1, minWidth: 0, minHeight: fixedHeader ? 0 : undefined }}
          >
            {showTagsview ? <TabsBar /> : null}
            <MainArea scrollable={fixedHeader} />
          </Stack>
        </Stack>
      ) : (
        <MainArea scrollable={fixedHeader} />
      )}
    </AppShellColumn>
  );
}

export function AppShell() {
  const {
    locked,
    openTab,
    sidebarCollapsed,
    toggleSidebar,
    layoutMode,
    fixedHeader,
    lockScreenEnabled,
    contentFullscreen,
    setContentFullscreen,
    tabs,
    activeTab,
    showBreadcrumb,
    watermark,
  } = useApp();
  const { t } = useI18n();
  const location = useLocation();
  const activeTabMeta = tabs.find((tab) => tab.id === activeTab);
  const authUser = useAuthStore((s) => s.user);
  const watermarkText = authUser?.nickname?.trim() || authUser?.username || t("shell.admin");

  const autoCollapsedRef = useRef(false);

  // Global shortcuts (Cmd/Ctrl+K/B/W/]/[/1-9/S) live in one centralized hook.
  useHotkeys();

  useEffect(() => {
    // On mobile, auto-collapse the sidebar on first route change only.
    // Subsequent manual toggles must stick (no re-collapse loop).
    if (
      !autoCollapsedRef.current &&
      window.innerWidth < 768 &&
      !sidebarCollapsed &&
      layoutMode !== "top"
    ) {
      toggleSidebar();
      autoCollapsedRef.current = true;
    }
  }, [location.pathname, layoutMode, sidebarCollapsed, toggleSidebar]);

  useEffect(() => {
    const label = routeLabel(location.pathname);
    if (label) {
      openTab({
        id: location.pathname.replace(/\//g, "-").slice(1) || "dashboard",
        label,
        path: location.pathname,
      });
    }
  }, [location.pathname, openTab]);

  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if (e.key === "Escape" && contentFullscreen) {
        setContentFullscreen(false);
      }
    };
    window.addEventListener("keydown", handler);
    return () => window.removeEventListener("keydown", handler);
  }, [contentFullscreen, setContentFullscreen]);

  const showMobileOverlay =
    !sidebarCollapsed && (layoutMode === "left" || layoutMode === "mix" || layoutMode === "split");
  const header = (
    <>
      <TopBar />
      {showBreadcrumb ? (
        <Box
          sx={{
            flexShrink: 0,
            minWidth: 0,
            bgcolor: "background.paper",
            borderBottom: 1,
            borderColor: "divider",
            px: { xs: 2, sm: 2.5 },
            pt: 0.5,
            pb: 0.375,
            overflow: "hidden",
          }}
        >
          <BreadcrumbBar />
        </Box>
      ) : null}
    </>
  );

  return (
    <Box
      sx={{
        display: "flex",
        height: "100vh",
        overflow: "hidden",
        bgcolor: "background.default",
      }}
    >
      <LayoutSync />

      {contentFullscreen ? (
        <Box
          sx={{
            position: "fixed",
            inset: 0,
            zIndex: 90,
            display: "flex",
            flexDirection: "column",
            bgcolor: "background.default",
          }}
        >
          <Paper
            square
            elevation={0}
            sx={{
              display: "flex",
              alignItems: "center",
              justifyContent: "space-between",
              gap: 1.5,
              px: 2,
              py: 1.25,
              borderBottom: 1,
              borderColor: "divider",
              flexShrink: 0,
            }}
          >
            <Typography
              variant="body2"
              sx={{
                fontWeight: 600,
                overflow: "hidden",
                textOverflow: "ellipsis",
                whiteSpace: "nowrap",
                minWidth: 0,
              }}
            >
              {activeTabMeta ? t(activeTabMeta.label) : ""}
            </Typography>
            <IconButton
              size="small"
              onClick={() => setContentFullscreen(false)}
              aria-label={t("shell.tab.exitFullscreen")}
            >
              <MinimizeIcon fontSize="small" />
            </IconButton>
          </Paper>
          <Box
            component="main"
            sx={{
              position: "relative",
              flex: 1,
              overflowY: "auto",
              overflowX: "clip",
              p: { xs: 2.5, sm: 3, lg: 3.5 },
            }}
          >
            {watermark && <Watermark text={watermarkText} />}
            <Box sx={{ position: "relative", zIndex: 2 }}>
              <PageView />
            </Box>
          </Box>
        </Box>
      ) : (
        <>
          <Backdrop
            open={showMobileOverlay}
            sx={{ zIndex: 20, display: { xs: "block", md: "none" } }}
            onClick={toggleSidebar}
          />

          {layoutMode === "left" && (
            <>
              <Sidebar />
              <AppShellColumn header={header}>
                <MainArea scrollable={fixedHeader} />
              </AppShellColumn>
            </>
          )}

          {layoutMode === "mix" && <MixLayout header={header} />}

          {layoutMode === "top" && (
            <AppShellColumn header={header}>
              <MainArea scrollable={fixedHeader} />
            </AppShellColumn>
          )}

          {layoutMode === "split" && (
            <>
              <Sidebar variant="rail" />
              <Sidebar variant="panel" />
              <AppShellColumn header={header}>
                <MainArea scrollable={fixedHeader} />
              </AppShellColumn>
            </>
          )}
        </>
      )}

      <FloatingCopilot />
      {locked && lockScreenEnabled && (
        <Suspense fallback={null}>
          <LockScreen />
        </Suspense>
      )}
      <LayoutDrawer />
      <SearchModal />
    </Box>
  );
}
