// SPDX-License-Identifier: Apache-2.0
import { useNavigate, useLocation } from "react-router-dom";
import { useMemo, useState, useRef } from "react";
import { Box, Button, ListItemIcon, ListItemText, Menu, MenuItem, useTheme } from "@mui/material";
import ChevronRightIcon from "@mui/icons-material/ChevronRight";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import { useApp } from "@/context/AppContext";
import { useAppearance } from "@/context/AppearanceContext";
import { useAuthStore } from "@/shared/auth/store";
import { useI18n } from "@/shared/i18n";
import {
  NAV,
  deriveNavFromMenus,
  isSeparator,
  openNavItem,
  type NavEntry,
  type NavItem,
} from "@/components/shell/navigation";
import { ScrollableRow } from "@/components/shell/ScrollableRow";
import { NavIcon } from "@/components/shell/NavIcon";
import { ProBadge, AiBadge } from "@/components/ui/index";
import { horizontalNavBarSx, horizontalNavButtonSx, menuGradientSx } from "@/theme/shellStyles";

function filterItems(items: NavItem[], has: (perm?: string) => boolean): NavItem[] {
  const out: NavItem[] = [];
  for (const item of items) {
    if (item.perm && !has(item.perm)) continue;
    if (item.children) {
      const children = filterItems(item.children, has);
      if (children.length === 0) continue;
      out.push({ ...item, children });
    } else {
      out.push(item);
    }
  }
  return out;
}

function filterNav(entries: NavEntry[], has: (perm?: string) => boolean): NavEntry[] {
  const out: NavEntry[] = [];
  for (const e of entries) {
    if (isSeparator(e)) continue;
    if (e.perm && !has(e.perm)) continue;
    let children = e.children;
    if (children) {
      children = filterItems(children, has);
      if (children.length === 0) continue;
    }
    out.push(children ? { ...e, children } : e);
  }
  return out;
}

function firstPath(item: NavItem): string | undefined {
  if (item.path) return item.path;
  for (const child of item.children ?? []) {
    const p = firstPath(child);
    if (p) return p;
  }
  return undefined;
}

function isItemActive(item: NavItem, pathname: string): boolean {
  if (item.path && pathname === item.path) return true;
  return (item.children ?? []).some((c) => isItemActive(c, pathname));
}

const menuPaperSx = {
  minWidth: 176,
  mt: 0.5,
};

function DropdownMenuItem({
  item,
  pathname,
  onNavigate,
  onCloseRoot,
}: {
  item: NavItem;
  pathname: string;
  onNavigate: (path: string) => void;
  onCloseRoot: () => void;
}) {
  const { t } = useI18n();
  const [flyoutAnchor, setFlyoutAnchor] = useState<HTMLElement | null>(null);
  const hasChildren = (item.children?.length ?? 0) > 0;
  const active = isItemActive(item, pathname);
  const Icon = item.icon;

  const handleLeafClick = () => {
    const path = openNavItem(item);
    if (path) {
      onNavigate(path);
      onCloseRoot();
    }
  };

  if (!hasChildren) {
    return (
      <MenuItem selected={active} onClick={handleLeafClick} sx={{ py: 1, gap: 1 }}>
        {Icon ? (
          <ListItemIcon sx={{ minWidth: 28 }}>
            <NavIcon icon={Icon} size={15} />
          </ListItemIcon>
        ) : null}
        <ListItemText
          primary={t(item.label)}
          slotProps={{ primary: { variant: "body2", noWrap: true } }}
        />
        {item.badge === "pro" ? <ProBadge /> : null}
        {item.badge === "ai" ? <AiBadge /> : null}
      </MenuItem>
    );
  }

  return (
    <>
      <MenuItem
        selected={active}
        onMouseEnter={(e) => setFlyoutAnchor(e.currentTarget)}
        sx={{ py: 1, gap: 1 }}
      >
        {Icon ? (
          <ListItemIcon sx={{ minWidth: 28 }}>
            <NavIcon icon={Icon} size={15} />
          </ListItemIcon>
        ) : null}
        <ListItemText
          primary={t(item.label)}
          slotProps={{ primary: { variant: "body2", noWrap: true } }}
        />
        <ChevronRightIcon sx={{ fontSize: 16, color: "text.secondary", ml: "auto" }} />
      </MenuItem>
      <Menu
        open={Boolean(flyoutAnchor)}
        anchorEl={flyoutAnchor}
        onClose={() => setFlyoutAnchor(null)}
        anchorOrigin={{ vertical: "top", horizontal: "right" }}
        transformOrigin={{ vertical: "top", horizontal: "left" }}
        disableAutoFocus
        disableEnforceFocus
        disableRestoreFocus
        slotProps={{
          paper: { sx: menuPaperSx, onMouseLeave: () => setFlyoutAnchor(null) },
          list: { onMouseLeave: () => setFlyoutAnchor(null) },
        }}
      >
        {item.children!.map((child) => (
          <DropdownMenuItem
            key={child.id}
            item={child}
            pathname={pathname}
            onNavigate={onNavigate}
            onCloseRoot={onCloseRoot}
          />
        ))}
      </Menu>
    </>
  );
}

function TopNavDropdown({
  entry,
  open,
  anchorEl,
  onNavigate,
  onClose,
}: {
  entry: NavItem;
  open: boolean;
  anchorEl: HTMLElement | null;
  onNavigate: (path: string) => void;
  onClose: () => void;
}) {
  const location = useLocation();

  if (!entry.children?.length) return null;

  return (
    <Menu
      open={open}
      anchorEl={anchorEl}
      onClose={onClose}
      anchorOrigin={{ vertical: "bottom", horizontal: "left" }}
      transformOrigin={{ vertical: "top", horizontal: "left" }}
      disableAutoFocus
      disableEnforceFocus
      disableRestoreFocus
      disableScrollLock
      slotProps={{ paper: { sx: menuPaperSx } }}
    >
      {entry.children.map((child) => (
        <DropdownMenuItem
          key={child.id}
          item={child}
          pathname={location.pathname}
          onNavigate={onNavigate}
          onCloseRoot={onClose}
        />
      ))}
    </Menu>
  );
}

function HorizontalNavItem({
  entry,
  active,
  inline,
  onGradient,
  hoverMenus,
  onSelect,
  menuOpen,
  onMenuOpenChange,
}: {
  entry: NavItem;
  active: boolean;
  inline: boolean;
  onGradient: boolean;
  hoverMenus: boolean;
  onSelect: (entry: NavItem) => void;
  menuOpen?: boolean;
  onMenuOpenChange?: (open: boolean) => void;
}) {
  const theme = useTheme();
  const { t } = useI18n();
  const navigate = useNavigate();
  const [localOpen, setLocalOpen] = useState(false);
  const open = menuOpen ?? localOpen;
  const setOpen = onMenuOpenChange ?? setLocalOpen;
  const closeTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const btnRef = useRef<HTMLButtonElement>(null);
  const hasChildren = (entry.children?.length ?? 0) > 0;
  const Icon = entry.icon;

  const setHoverOpen = (hovering: boolean) => {
    if (closeTimer.current) {
      clearTimeout(closeTimer.current);
      closeTimer.current = null;
    }
    if (hovering) {
      setOpen(true);
      return;
    }
    closeTimer.current = setTimeout(() => setOpen(false), 120);
  };

  const buttonSx = horizontalNavButtonSx(theme, {
    onGradient,
    active,
    inline,
  });

  const navigateTo = (path: string) => {
    setOpen(false);
    navigate(path);
  };

  if (hoverMenus && hasChildren) {
    return (
      <Box
        sx={{ position: "relative", flexShrink: 0 }}
        onMouseEnter={() => setHoverOpen(true)}
        onMouseLeave={() => setHoverOpen(false)}
      >
        <Button
          ref={btnRef}
          data-nav-id={entry.id}
          sx={horizontalNavButtonSx(theme, {
            onGradient,
            active: active || open,
            inline,
          })}
          endIcon={
            <ExpandMoreIcon
              sx={{
                fontSize: 16,
                opacity: 0.6,
                transform: open ? "rotate(180deg)" : "none",
                transition: "transform 0.15s",
              }}
            />
          }
        >
          {Icon ? <NavIcon icon={Icon} size={inline ? 15 : 14} /> : null}
          {t(entry.label)}
        </Button>
        <Box onMouseEnter={() => setHoverOpen(true)} onMouseLeave={() => setHoverOpen(false)}>
          <TopNavDropdown
            entry={entry}
            open={open}
            anchorEl={btnRef.current}
            onNavigate={navigateTo}
            onClose={() => setOpen(false)}
          />
        </Box>
      </Box>
    );
  }

  return (
    <Button data-nav-id={entry.id} onClick={() => onSelect(entry)} sx={buttonSx}>
      {Icon ? <NavIcon icon={Icon} size={inline ? 15 : 14} /> : null}
      {t(entry.label)}
    </Button>
  );
}

export function HorizontalNav({ inline = false }: { inline?: boolean }) {
  const theme = useTheme();
  const navigate = useNavigate();
  const location = useLocation();
  const {
    layoutMode,
    topbarGradient,
    activeNavSection,
    setActiveNavSection,
    menuAccordion,
    menuGradient,
  } = useApp();
  const { sidebarTheme, dark, customSidebarAccent } = useAppearance();
  const authorities = useAuthStore((s) => s.authorities);
  const backendMenus = useAuthStore((s) => s.menus);
  const hoverMenus = layoutMode === "top";
  const [openMenuId, setOpenMenuId] = useState<string | null>(null);

  const nav = useMemo(() => {
    const base = backendMenus.length ? deriveNavFromMenus(backendMenus) : NAV;
    return filterNav(base, (perm) => !perm || authorities.includes(perm));
  }, [backendMenus, authorities]);

  const activeId = useMemo(() => {
    for (const entry of nav) {
      if (isSeparator(entry)) continue;
      if (
        layoutMode === "mix"
          ? activeNavSection === entry.id || isItemActive(entry, location.pathname)
          : isItemActive(entry, location.pathname)
      ) {
        return entry.id;
      }
    }
    return null;
  }, [nav, layoutMode, activeNavSection, location.pathname]);

  const scrollSelector = activeId ? `[data-nav-id="${activeId}"]` : null;
  const onGradient = inline && topbarGradient;
  const navMenuGradientSx = menuGradientSx(
    menuGradient,
    sidebarTheme,
    dark,
    "panel",
    customSidebarAccent,
  );

  const handleSelect = (entry: NavItem) => {
    const path = firstPath(entry);
    if (layoutMode === "mix") setActiveNavSection(entry.id);
    if (path) navigate(path);
  };

  const items = nav.map((entry) => {
    if (isSeparator(entry)) return null;
    const active =
      layoutMode === "mix"
        ? activeNavSection === entry.id || isItemActive(entry, location.pathname)
        : isItemActive(entry, location.pathname);

    return (
      <HorizontalNavItem
        key={entry.id}
        entry={entry}
        active={active}
        inline={inline}
        onGradient={onGradient}
        hoverMenus={hoverMenus}
        onSelect={handleSelect}
        menuOpen={hoverMenus && menuAccordion ? openMenuId === entry.id : undefined}
        onMenuOpenChange={
          hoverMenus && menuAccordion ? (open) => setOpenMenuId(open ? entry.id : null) : undefined
        }
      />
    );
  });

  if (inline) {
    return (
      <ScrollableRow
        sx={{ flex: 1, minWidth: 0, maxWidth: "100%", ...navMenuGradientSx }}
        gradient={onGradient}
        innerSx={{ py: 0.5 }}
        scrollIntoViewSelector={scrollSelector}
      >
        {items}
      </ScrollableRow>
    );
  }

  return (
    <Box sx={horizontalNavBarSx(theme)}>
      <ScrollableRow innerSx={{ py: 0.5 }} scrollIntoViewSelector={scrollSelector}>
        {items}
      </ScrollableRow>
    </Box>
  );
}
