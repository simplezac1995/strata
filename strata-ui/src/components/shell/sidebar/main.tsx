// SPDX-License-Identifier: Apache-2.0
// The sidebar itself.

import { useState, useMemo, useEffect, useCallback } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import {
  Avatar,
  Box,
  Divider,
  IconButton,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  ListSubheader,
  Typography,
} from "@mui/material";
import type { SxProps, Theme } from "@mui/material/styles";
import SettingsOutlinedIcon from "@mui/icons-material/SettingsOutlined";
import { useApp } from "@/context/AppContext";
import { useAppearance } from "@/context/AppearanceContext";
import { useI18n } from "@/shared/i18n";
import { useAuthStore } from "@/shared/auth/store";
import { NavIcon } from "@/components/shell/NavIcon";
import { NAV, deriveNavFromMenus, isSeparator } from "@/components/shell/navigation";
import type { NavItem } from "@/components/shell/navigation";
import { resolveSidebarTokens, sidebarNavScrollSx } from "@/theme/sidebarThemes";
import { menuGradientSx } from "@/theme/shellStyles";
import { filterNav } from "./navFilter";
import { shellSx, SidebarShell, SidebarFavorites } from "./shell";
import {
  SidebarNavCtx,
  collectDescendantIds,
  SidebarNavContext,
  collectOpenParents,
  isItemActive,
  firstPath,
} from "./context";
import { NavItemRow } from "./NavItemRow";

export type SidebarVariant = "default" | "rail" | "panel";

export function Sidebar({ variant = "default" }: { variant?: SidebarVariant }) {
  const {
    sidebarCollapsed,
    menuAccordion,
    menuGradient,
    showSidebarLogo,
    layoutMode,
    activeNavSection,
    setActiveNavSection,
  } = useApp();
  const { sidebarTheme, customSidebarBg, customSidebarAccent, dark } = useAppearance();
  const authorities = useAuthStore((s) => s.authorities);
  const user = useAuthStore((s) => s.user);
  const location = useLocation();
  const { t } = useI18n();
  const navigate = useNavigate();
  const tokens = resolveSidebarTokens(sidebarTheme, dark, customSidebarBg, customSidebarAccent);
  const displayName = user?.nickname?.trim() || user?.username || "—";
  const initials = displayName
    .split(/\s+/)
    .map((part) => part[0])
    .join("")
    .slice(0, 2)
    .toUpperCase();
  // sys_menu (`/auth/me` -> menus) is the source of truth for what exists / hierarchy / order; the static
  // NAV only supplies icons and real routes. Fall back to the static tree when the backend tree has not
  // hydrated yet (e.g. during the first paint before /auth/me resolves).
  const backendMenus = useAuthStore((s) => s.menus);
  const nav = useMemo(() => {
    const base = backendMenus.length ? deriveNavFromMenus(backendMenus) : NAV;
    return filterNav(base, (perm) => !perm || authorities.includes(perm));
  }, [backendMenus, authorities]);
  const topLevelNav = useMemo(() => nav.filter((entry) => !isSeparator(entry)) as NavItem[], [nav]);

  const navGradientSx = menuGradientSx(
    menuGradient,
    sidebarTheme,
    dark,
    variant,
    customSidebarAccent,
  );

  const [openIds, setOpenIds] = useState<Set<string>>(() => new Set());

  useEffect(() => {
    const topLevel = nav.filter((e) => !isSeparator(e)) as NavItem[];
    const parentIds = collectOpenParents(topLevel, location.pathname);
    const ids = new Set(parentIds);
    if (ids.size === 0) return;
    if (menuAccordion) {
      setOpenIds(ids);
      return;
    }
    setOpenIds((prev) => {
      const next = new Set(prev);
      for (const id of ids) next.add(id);
      return next;
    });
  }, [location.pathname, nav, menuAccordion]);

  const toggleGroup = useCallback(
    (id: string, siblings: NavItem[]) => {
      setOpenIds((prev) => {
        const next = new Set(prev);
        if (next.has(id)) {
          next.delete(id);
          const current = siblings.find((item) => item.id === id);
          if (current) {
            for (const desc of collectDescendantIds(current)) next.delete(desc);
          }
          return next;
        }
        if (menuAccordion) {
          for (const sibling of siblings) {
            if (sibling.id === id) continue;
            next.delete(sibling.id);
            for (const desc of collectDescendantIds(sibling)) next.delete(desc);
          }
        }
        next.add(id);
        return next;
      });
    },
    [menuAccordion],
  );

  const navCtx = useMemo<SidebarNavCtx>(
    () => ({ openIds, toggleGroup, menuAccordion }),
    [openIds, toggleGroup, menuAccordion],
  );

  const panelEntry = useMemo(() => {
    if (!activeNavSection) return nav.find((e) => !isSeparator(e)) as NavItem | undefined;
    return nav.find((e) => !isSeparator(e) && e.id === activeNavSection) as NavItem | undefined;
  }, [nav, activeNavSection]);

  if (variant === "rail") {
    return (
      <SidebarNavContext.Provider value={navCtx}>
        <SidebarShell
          sx={shellSx(tokens, {
            width: { xs: 64, md: 64 },
            transform: "translateX(0)",
          })}
        >
          {showSidebarLogo && (
            <Box
              onClick={() => navigate("/dashboard")}
              sx={{
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                height: 64,
                flexShrink: 0,
                cursor: "pointer",
                borderBottom: 1,
                borderColor: tokens.logo.borderColor,
                "&:hover": { bgcolor: tokens.logo.hoverBg },
              }}
            >
              <Box
                sx={{
                  width: 32,
                  height: 32,
                  borderRadius: 1,
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  color: "#fff",
                  fontWeight: 700,
                  fontSize: "0.875rem",
                  bgcolor: tokens.logo.accentBg,
                }}
              >
                S
              </Box>
            </Box>
          )}
          <Box
            component="nav"
            sx={
              [
                sidebarNavScrollSx(tokens),
                {
                  display: "flex",
                  flexDirection: "column",
                  alignItems: "center",
                  gap: 0.5,
                },
                navGradientSx,
              ].filter(Boolean) as SxProps<Theme>
            }
          >
            {nav.map((entry) => {
              if (isSeparator(entry)) return null;
              const active =
                activeNavSection === entry.id || isItemActive(entry, location.pathname);
              const Icon = entry.icon;
              return (
                <IconButton
                  key={entry.id}
                  title={t(entry.label)}
                  aria-label={t(entry.label)}
                  onClick={() => {
                    setActiveNavSection(entry.id);
                    const path = firstPath(entry);
                    if (path) navigate(path);
                  }}
                  sx={{
                    width: 40,
                    height: 40,
                    borderRadius: 1.5,
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
                          },
                        }),
                  }}
                >
                  {Icon ? (
                    <Box
                      component="span"
                      sx={{
                        display: "inline-flex",
                        color: active ? tokens.nav.icons.active : tokens.nav.icons.idle,
                      }}
                    >
                      <NavIcon icon={Icon} size={18} />
                    </Box>
                  ) : null}
                </IconButton>
              );
            })}
          </Box>
        </SidebarShell>
      </SidebarNavContext.Provider>
    );
  }

  if (variant === "panel") {
    const panelChildren = panelEntry?.children ?? [];
    if (panelChildren.length === 0) return null;

    const isMix = layoutMode === "mix";
    const panelTokens = resolveSidebarTokens(
      sidebarTheme,
      dark,
      customSidebarBg,
      customSidebarAccent,
    );
    const panelGradientSx = menuGradientSx(
      menuGradient,
      sidebarTheme,
      dark,
      variant,
      customSidebarAccent,
    );

    return (
      <SidebarNavContext.Provider value={navCtx}>
        <SidebarShell
          sx={shellSx(panelTokens, {
            display: isMix ? undefined : { xs: "none", sm: "flex" },
            width: { xs: 256, md: 208 },
            transform: isMix
              ? {
                  xs: sidebarCollapsed ? "translateX(-100%)" : "translateX(0)",
                  md: "translateX(0)",
                }
              : undefined,
          })}
        >
          {showSidebarLogo && (
            <Box
              sx={{
                px: 2,
                height: 64,
                display: "flex",
                alignItems: "center",
                flexShrink: 0,
                borderBottom: 1,
                borderColor: panelTokens.logo.borderColor,
              }}
            >
              <Typography
                variant="subtitle2"
                sx={{
                  color: panelTokens.logo.text,
                  overflow: "hidden",
                  textOverflow: "ellipsis",
                  whiteSpace: "nowrap",
                }}
              >
                {panelEntry ? t(panelEntry.label) : t("nav.dashboard")}
              </Typography>
            </Box>
          )}
          <Box
            component="nav"
            sx={
              [sidebarNavScrollSx(panelTokens), panelGradientSx].filter(Boolean) as SxProps<Theme>
            }
          >
            {panelChildren.map((child) => (
              <NavItemRow
                key={child.id}
                item={child}
                depth={1}
                collapsed={false}
                tokens={panelTokens}
                siblings={panelChildren}
              />
            ))}
          </Box>
        </SidebarShell>
      </SidebarNavContext.Provider>
    );
  }

  /* default vertical sidebar (left rail) */
  const hideOnTopLayout = layoutMode === "top" || layoutMode === "mix";
  if (hideOnTopLayout) return null;

  return (
    <SidebarNavContext.Provider value={navCtx}>
      <SidebarShell
        sx={shellSx(tokens, {
          width: { xs: 256, md: sidebarCollapsed ? 72 : 256 },
          transform: {
            xs: sidebarCollapsed ? "translateX(-100%)" : "translateX(0)",
            md: "translateX(0)",
          },
        })}
      >
        {showSidebarLogo && (
          <Box
            onClick={() => navigate("/dashboard")}
            sx={{
              display: "flex",
              alignItems: "center",
              gap: 1.5,
              height: 64,
              flexShrink: 0,
              px: 2,
              cursor: "pointer",
              transition: "background-color 0.15s",
              borderBottom: 1,
              borderColor: tokens.logo.borderColor,
              justifyContent: sidebarCollapsed ? "center" : "flex-start",
              "&:hover": { bgcolor: tokens.logo.hoverBg },
            }}
          >
            <Box
              sx={{
                width: 32,
                height: 32,
                borderRadius: 1,
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                color: "#fff",
                fontWeight: 700,
                fontSize: "0.875rem",
                flexShrink: 0,
                bgcolor: tokens.logo.accentBg,
              }}
            >
              S
            </Box>
            {!sidebarCollapsed && (
              <Box sx={{ minWidth: 0 }}>
                <Typography
                  data-testid="sidebar-brand-title"
                  variant="subtitle2"
                  noWrap
                  sx={{ color: tokens.logo.text, fontWeight: 700, letterSpacing: "0.01em" }}
                >
                  {t("auth.brand.title")}
                </Typography>
                <Typography
                  variant="caption"
                  sx={{ color: tokens.logo.subtext, display: "block", mt: 0.25 }}
                >
                  {t("auth.brand.tagline")}
                </Typography>
              </Box>
            )}
          </Box>
        )}

        <SidebarFavorites tokens={tokens} collapsed={sidebarCollapsed} />

        <Box
          component="nav"
          sx={[sidebarNavScrollSx(tokens), navGradientSx].filter(Boolean) as SxProps<Theme>}
        >
          {nav.map((entry, idx) => {
            if (isSeparator(entry)) {
              if (sidebarCollapsed) {
                return <Divider key={idx} sx={{ mx: 2, my: 1.5, borderColor: tokens.separator }} />;
              }
              return (
                <ListSubheader
                  key={entry.label}
                  disableSticky
                  sx={{
                    bgcolor: "transparent",
                    lineHeight: 1.5,
                    px: 2.5,
                    pt: 2,
                    pb: 0.5,
                    color: tokens.sectionLabel,
                    typography: "overline",
                    fontWeight: 600,
                  }}
                >
                  {t(entry.label)}
                </ListSubheader>
              );
            }
            return (
              <NavItemRow
                key={entry.id}
                item={entry}
                collapsed={sidebarCollapsed}
                tokens={tokens}
                siblings={topLevelNav}
              />
            );
          })}
          <Box sx={{ height: 16 }} />
        </Box>

        {!sidebarCollapsed ? (
          <Box
            sx={{
              px: 1.5,
              py: 1.5,
              borderTop: 1,
              borderColor: tokens.user.footerBorder,
              flexShrink: 0,
            }}
          >
            <ListItemButton
              sx={{
                borderRadius: 1.5,
                py: 1.25,
                transition: "background-color 0.15s",
                "&:hover": { bgcolor: tokens.user.hoverBg },
              }}
              onClick={() => navigate("/settings/profile")}
            >
              <ListItemIcon sx={{ minWidth: 40 }}>
                <Avatar
                  sx={{
                    width: 32,
                    height: 32,
                    fontSize: 12,
                    bgcolor: "primary.main",
                  }}
                >
                  {initials}
                </Avatar>
              </ListItemIcon>
              <ListItemText
                primary={displayName}
                secondary={user?.email ?? user?.username ?? ""}
                slotProps={{
                  primary: { variant: "body2", noWrap: true, sx: { color: tokens.user.name } },
                  secondary: { variant: "caption", noWrap: true, sx: { color: tokens.user.role } },
                }}
              />
              <SettingsOutlinedIcon sx={{ fontSize: 16, color: tokens.user.settingsIcon }} />
            </ListItemButton>
          </Box>
        ) : null}
      </SidebarShell>
    </SidebarNavContext.Provider>
  );
}

/* ─── Theme preview info (used by Settings + Drawer) ───────────── */
