// SPDX-License-Identifier: Apache-2.0
// The shell wrapper and the favourites block of the sidebar.

import { type ReactNode } from "react";
import { useNavigate } from "react-router-dom";
import {
  Box,
  IconButton,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  ListSubheader,
} from "@mui/material";
import type { SxProps, Theme } from "@mui/material/styles";
import CloseIcon from "@mui/icons-material/Close";
import StarOutlinedIcon from "@mui/icons-material/StarOutlined";
import { useApp } from "@/context/AppContext";
import { useI18n } from "@/shared/i18n";
import { type SidebarTokens, sidebarShellBaseSx } from "@/theme/sidebarThemes";

export function shellSx(tokens: SidebarTokens, extra?: SxProps<Theme>): SxProps<Theme> {
  const core: SxProps<Theme> = {
    ...(sidebarShellBaseSx as object),
    bgcolor: tokens.shell.bgcolor,
    backgroundColor: tokens.shell.bgcolor,
    borderColor: tokens.shell.borderColor,
  };
  if (!extra) return core;
  return [core, extra] as SxProps<Theme>;
}

export function SidebarShell({ sx, children }: { sx?: SxProps<Theme>; children: ReactNode }) {
  return (
    <Box
      component="aside"
      sx={[
        {
          display: "flex",
          flexDirection: "column",
          height: "100%",
          overflow: "hidden",
        },
        ...(sx ? (Array.isArray(sx) ? sx : [sx]) : []),
      ]}
    >
      {children}
    </Box>
  );
}

export function SidebarFavorites({
  tokens,
  collapsed,
}: {
  tokens: SidebarTokens;
  collapsed: boolean;
}) {
  const { favoriteTabs, removeFavoriteTab, openTab } = useApp();
  const navigate = useNavigate();
  const { t } = useI18n();

  if (collapsed || favoriteTabs.length === 0) return null;

  return (
    <List
      dense
      disablePadding
      subheader={
        <ListSubheader
          disableSticky
          sx={{
            bgcolor: "transparent",
            lineHeight: 1.5,
            px: 2,
            pt: 1,
            pb: 0.5,
            color: tokens.sectionLabel,
            typography: "overline",
            fontWeight: 600,
          }}
        >
          {t("shell.sidebar.favorites")}
        </ListSubheader>
      }
      sx={{ px: 1, pt: 0.5, pb: 0.5 }}
    >
      {favoriteTabs.map((tab) => (
        <Box key={tab.path} sx={{ display: "flex", alignItems: "center", gap: 0.25 }}>
          <ListItemButton
            onClick={() => {
              openTab(tab);
              navigate(tab.path);
            }}
            sx={{
              flex: 1,
              borderRadius: 1.5,
              py: 0.75,
              minHeight: 36,
              gap: 1.5,
              alignItems: "center",
              color: tokens.nav.idle.color,
              "&:hover": {
                bgcolor: tokens.nav.idle.hoverBg,
                color: tokens.nav.idle.hoverColor,
              },
            }}
          >
            <ListItemIcon sx={{ minWidth: 20, mr: 0 }}>
              <StarOutlinedIcon sx={{ fontSize: 14, color: tokens.nav.icons.idle }} />
            </ListItemIcon>
            <ListItemText
              primary={t(tab.label)}
              slotProps={{ primary: { variant: "caption", noWrap: true } }}
            />
          </ListItemButton>
          <IconButton
            size="small"
            title={t("shell.favorite.remove")}
            aria-label={t("shell.favorite.remove")}
            onClick={() => removeFavoriteTab(tab.path)}
            sx={{
              opacity: 0.6,
              color: tokens.nav.idle.color,
              "&:hover": { opacity: 1, bgcolor: tokens.nav.idle.hoverBg },
            }}
          >
            <CloseIcon sx={{ fontSize: 14 }} />
          </IconButton>
        </Box>
      ))}
    </List>
  );
}

/* ─── Accordion context (menu hand-off) ─────────────────────── */
