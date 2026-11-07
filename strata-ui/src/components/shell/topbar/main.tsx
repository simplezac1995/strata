// SPDX-License-Identifier: Apache-2.0
// The top bar itself: search, language menu, notifications and the user menu.

import { useState, type MouseEvent } from "react";
import { useNavigate } from "react-router-dom";
import {
  AppBar,
  Avatar,
  Box,
  Button,
  Divider,
  IconButton,
  ListItemIcon,
  ListItemText,
  Menu,
  MenuItem,
  Toolbar,
  Typography,
} from "@mui/material";
import BusinessIcon from "@mui/icons-material/Business";
import CheckIcon from "@mui/icons-material/Check";
import DarkModeIcon from "@mui/icons-material/DarkMode";
import KeyboardArrowDownIcon from "@mui/icons-material/KeyboardArrowDown";
import LanguageIcon from "@mui/icons-material/Language";
import LightModeIcon from "@mui/icons-material/LightMode";
import LockOutlinedIcon from "@mui/icons-material/LockOutlined";
import LogoutIcon from "@mui/icons-material/Logout";
import MenuIcon from "@mui/icons-material/Menu";
import PaletteOutlinedIcon from "@mui/icons-material/PaletteOutlined";
import PersonOutlinedIcon from "@mui/icons-material/PersonOutlined";
import SearchIcon from "@mui/icons-material/Search";
import ViewQuiltOutlinedIcon from "@mui/icons-material/ViewQuiltOutlined";
import { useApp } from "@/context/AppContext";
import { useAppearance } from "@/context/AppearanceContext";
import { useAuthStore } from "@/shared/auth/store";
import { useI18n } from "@/shared/i18n";
import { HorizontalNav } from "@/components/shell/HorizontalNav";
import { NotificationCenter } from "@/components/NotificationCenter";
import {
  topbarAppBarSx,
  topbarChipButtonSx,
  topbarIconButtonSx,
  topbarSearchButtonSx,
  topbarUserButtonSx,
} from "@/theme/shellStyles";
import { LANGS, useHasMixSubNav, TabsBar } from "./tabs";
import { TenantBadge, WsStatusBadge } from "./badges";

export function TopBar() {
  const {
    toggleSidebar,
    setLocked,
    tenants,
    currentTenant,
    setCurrentTenant,
    setLayoutDrawerOpen,
    showTagsview,
    topbarGradient,
    lockScreenEnabled,
    layoutMode,
    setSearchOpen,
  } = useApp();
  const { dark, toggleDark, lang, setLang } = useAppearance();
  const [tenantAnchor, setTenantAnchor] = useState<HTMLElement | null>(null);
  const [langAnchor, setLangAnchor] = useState<HTMLElement | null>(null);
  const [userAnchor, setUserAnchor] = useState<HTMLElement | null>(null);
  const navigate = useNavigate();
  const { t } = useI18n();
  const authUser = useAuthStore((s) => s.user);
  const displayName = authUser?.nickname?.trim() || authUser?.username || t("shell.admin");
  const initials = displayName.slice(0, 2).toUpperCase();

  const currentLang = LANGS.find((l) => l.code === lang) ?? LANGS[0];
  const isHorizontalLayout = layoutMode === "top" || layoutMode === "mix";
  const showBrand = isHorizontalLayout;
  const showInlineNav = isHorizontalLayout;
  const hasMixSubNav = useHasMixSubNav();
  const showMenuToggle =
    layoutMode === "left" || layoutMode === "split" || (layoutMode === "mix" && hasMixSubNav);
  const showTabsInTopBar = showTagsview && !(layoutMode === "mix" && hasMixSubNav);

  const closeMenus = () => {
    setTenantAnchor(null);
    setLangAnchor(null);
    setUserAnchor(null);
  };

  const openMenu = (setter: (el: HTMLElement | null) => void, event: MouseEvent<HTMLElement>) => {
    closeMenus();
    setter(event.currentTarget);
  };

  const userMenuItems = [
    {
      icon: PersonOutlinedIcon,
      label: "nav.profile",
      action: () => {
        navigate("/settings/profile");
        closeMenus();
      },
    },
    {
      icon: PaletteOutlinedIcon,
      label: "nav.appearance",
      action: () => {
        navigate("/settings/appearance");
        closeMenus();
      },
    },
    {
      icon: LanguageIcon,
      label: "nav.language",
      action: () => {
        navigate("/settings/language");
        closeMenus();
      },
    },
    ...(lockScreenEnabled
      ? [
          {
            icon: LockOutlinedIcon,
            label: "shell.lock.screen",
            action: () => {
              setLocked(true);
              closeMenus();
            },
          },
        ]
      : []),
  ];

  return (
    <AppBar position="static" sx={topbarAppBarSx(topbarGradient, dark)}>
      <Toolbar disableGutters sx={{ minHeight: 64, px: { xs: 2, sm: 2.5 }, gap: 1 }}>
        {showMenuToggle ? (
          <IconButton
            onClick={toggleSidebar}
            sx={topbarIconButtonSx(topbarGradient)}
            aria-label={t("shell.layout.config")}
          >
            <MenuIcon fontSize="small" />
          </IconButton>
        ) : null}

        {showBrand ? (
          <Button
            onClick={() => navigate("/dashboard")}
            sx={{
              flexShrink: 0,
              mr: 0.5,
              color: "inherit",
              textTransform: "none",
              minWidth: 0,
              px: 0.5,
            }}
          >
            <Avatar
              sx={{
                width: 32,
                height: 32,
                borderRadius: 1.5,
                mr: 1,
                bgcolor: topbarGradient ? "rgba(255,255,255,0.15)" : "primary.main",
                fontSize: 14,
                fontWeight: 700,
              }}
            >
              S
            </Avatar>
            <Typography
              variant="body2"
              sx={{ display: { xs: "none", md: "block" }, fontWeight: 600 }}
            >
              {t("auth.brand.title")}
            </Typography>
          </Button>
        ) : null}

        {showInlineNav ? (
          <Box sx={{ flex: 1, minWidth: 0 }}>
            <HorizontalNav inline />
          </Box>
        ) : (
          <Box sx={{ flex: 1, minWidth: 0 }} />
        )}

        <Box
          sx={{ display: "flex", alignItems: "center", gap: { xs: 0.75, sm: 1 }, flexShrink: 0 }}
        >
          {currentTenant ? (
            <>
              <Button
                size="small"
                onClick={(e) => openMenu(setTenantAnchor, e)}
                sx={{
                  ...topbarChipButtonSx(topbarGradient),
                  display: { xs: "none", sm: "inline-flex" },
                  border: topbarGradient ? undefined : 1,
                  borderColor: "divider",
                }}
                endIcon={
                  <KeyboardArrowDownIcon
                    sx={{
                      fontSize: 16,
                      transform: tenantAnchor ? "rotate(180deg)" : "none",
                    }}
                  />
                }
              >
                <Avatar
                  sx={{
                    width: 16,
                    height: 16,
                    fontSize: 9,
                    mr: 0.75,
                    bgcolor: currentTenant.color,
                  }}
                >
                  {currentTenant.avatar[0]}
                </Avatar>
                <Typography variant="caption" noWrap sx={{ maxWidth: 90 }}>
                  {currentTenant.name}
                </Typography>
              </Button>
              <Menu
                anchorEl={tenantAnchor}
                open={Boolean(tenantAnchor)}
                onClose={closeMenus}
                slotProps={{ paper: { sx: { width: 240, mt: 1 } } }}
              >
                <Typography variant="overline" sx={{ px: 2, py: 0.5, display: "block" }}>
                  {t("shell.switch.tenant")}
                </Typography>
                {tenants.map((tenant) => (
                  <MenuItem
                    key={tenant.id}
                    onClick={() => {
                      setCurrentTenant(tenant);
                      closeMenus();
                    }}
                  >
                    <ListItemIcon>
                      <Avatar sx={{ width: 28, height: 28, fontSize: 11, bgcolor: tenant.color }}>
                        {tenant.avatar}
                      </Avatar>
                    </ListItemIcon>
                    <ListItemText
                      primary={tenant.name}
                      secondary={<TenantBadge plan={tenant.plan} />}
                    />
                    {currentTenant?.id === tenant.id ? (
                      <CheckIcon color="primary" fontSize="small" />
                    ) : null}
                  </MenuItem>
                ))}
                <Divider sx={{ my: 0.5 }} />
                <MenuItem>
                  <ListItemIcon>
                    <BusinessIcon fontSize="small" />
                  </ListItemIcon>
                  <ListItemText primary={t("shell.manage.tenants")} />
                </MenuItem>
              </Menu>
            </>
          ) : null}

          <Button
            size="small"
            onClick={() => setSearchOpen(true)}
            startIcon={<SearchIcon sx={{ fontSize: 18, opacity: 0.7 }} />}
            sx={{
              ...topbarSearchButtonSx(topbarGradient),
              display: { xs: "none", md: "inline-flex" },
              width: { md: 220, lg: 248 },
            }}
          >
            {t("shell.search")}
            <Typography
              component="span"
              variant="caption"
              sx={{
                ml: "auto",
                opacity: 0.55,
                fontFamily: "monospace",
                fontSize: 11,
                px: 0.75,
                py: 0.25,
                borderRadius: 1,
                bgcolor: topbarGradient ? "rgba(255,255,255,0.08)" : "action.hover",
              }}
            >
              ⌘K
            </Typography>
          </Button>

          <NotificationCenter />
          <WsStatusBadge />

          <Button
            size="small"
            onClick={(e) => openMenu(setLangAnchor, e)}
            sx={{
              ...topbarChipButtonSx(topbarGradient),
              display: { xs: "none", md: "inline-flex" },
              border: topbarGradient ? undefined : 1,
              borderColor: "divider",
            }}
            endIcon={
              <KeyboardArrowDownIcon
                sx={{ fontSize: 16, transform: langAnchor ? "rotate(180deg)" : "none" }}
              />
            }
          >
            <span>{t(currentLang.abbreviation)}</span>
            <span>{currentLang.code.toUpperCase()}</span>
          </Button>
          <Menu
            anchorEl={langAnchor}
            open={Boolean(langAnchor)}
            onClose={closeMenus}
            slotProps={{ paper: { sx: { width: 192, mt: 1 } } }}
          >
            {LANGS.map((l) => (
              <MenuItem
                key={l.code}
                selected={l.code === lang}
                onClick={() => {
                  setLang(l.code);
                  closeMenus();
                }}
              >
                <ListItemText
                  primary={
                    <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
                      <span>{t(l.abbreviation)}</span>
                      <span>{l.code === "en" ? l.native : t(l.native)}</span>
                    </Box>
                  }
                  secondary={t(l.label)}
                />
              </MenuItem>
            ))}
          </Menu>

          <IconButton
            sx={topbarIconButtonSx(topbarGradient)}
            onClick={() => setLayoutDrawerOpen(true)}
            title={t("shell.layout.config")}
            aria-label={t("shell.layout.config")}
          >
            <ViewQuiltOutlinedIcon fontSize="small" />
          </IconButton>

          <IconButton
            sx={topbarIconButtonSx(topbarGradient)}
            onClick={toggleDark}
            title={dark ? t("shell.light.mode") : t("shell.dark.mode")}
            aria-label={dark ? t("shell.light.mode") : t("shell.dark.mode")}
          >
            {dark ? <LightModeIcon fontSize="small" /> : <DarkModeIcon fontSize="small" />}
          </IconButton>

          <Button
            onClick={(e) => openMenu(setUserAnchor, e)}
            sx={{
              ...topbarUserButtonSx(topbarGradient),
              borderRadius: 1.5,
              minWidth: 0,
              px: 1,
            }}
            endIcon={
              <KeyboardArrowDownIcon
                sx={{
                  fontSize: 16,
                  display: { xs: "none", md: "block" },
                  transform: userAnchor ? "rotate(180deg)" : "none",
                }}
              />
            }
          >
            <Avatar
              sx={{
                width: 28,
                height: 28,
                fontSize: 11,
                bgcolor: topbarGradient ? "rgba(255,255,255,0.2)" : "primary.main",
              }}
            >
              {initials}
            </Avatar>
            <Typography
              variant="caption"
              sx={{ display: { xs: "none", md: "block" }, ml: 0.75, fontWeight: 600 }}
            >
              {displayName}
            </Typography>
          </Button>
          <Menu
            anchorEl={userAnchor}
            open={Boolean(userAnchor)}
            onClose={closeMenus}
            slotProps={{ paper: { sx: { width: 192, mt: 1 } } }}
          >
            <Box sx={{ px: 2, py: 1.25 }}>
              <Typography variant="body2" sx={{ fontWeight: 600 }}>
                {displayName}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                {authUser?.username}
              </Typography>
            </Box>
            <Divider />
            {userMenuItems.map((item) => (
              <MenuItem key={item.label} onClick={item.action}>
                <ListItemIcon>
                  <item.icon fontSize="small" />
                </ListItemIcon>
                <ListItemText primary={t(item.label)} />
              </MenuItem>
            ))}
            <Divider />
            <MenuItem
              onClick={() => {
                closeMenus();
                void useAuthStore.getState().logout();
                navigate("/login");
              }}
              sx={{ color: "error.main" }}
            >
              <ListItemIcon sx={{ color: "error.main" }}>
                <LogoutIcon fontSize="small" />
              </ListItemIcon>
              <ListItemText primary={t("shell.sign.out")} />
            </MenuItem>
          </Menu>
        </Box>
      </Toolbar>

      {showTabsInTopBar ? <TabsBar /> : null}
    </AppBar>
  );
}
