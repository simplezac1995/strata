// SPDX-License-Identifier: Apache-2.0
// One sidebar row: link/branch behaviour, badges and the nested children.

import { useState, useRef, useContext } from "react";
import { NavLink, useLocation } from "react-router-dom";
import { Box, ListItemButton, ListItemText } from "@mui/material";
import ChevronRightIcon from "@mui/icons-material/ChevronRight";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import OpenInNewIcon from "@mui/icons-material/OpenInNew";
import ScheduleIcon from "@mui/icons-material/Schedule";
import { useApp } from "@/context/AppContext";
import { useI18n } from "@/shared/i18n";
import { ProBadge, AiBadge } from "@/components/ui/index";
import { ICON_MAP } from "@/components/shell/navIcons";
import { NavIcon } from "@/components/shell/NavIcon";
import { openNavItem } from "@/components/shell/navigation";
import type { NavItem } from "@/components/shell/navigation";
import { type SidebarTokens, navItemButtonSx, navIconWrapSx } from "@/theme/sidebarThemes";
import { SidebarNavContext, isItemActive } from "./context";
import { IconPicker } from "./IconPicker";

export function NavItemRow({
  item,
  depth = 0,
  collapsed,
  tokens,
  siblings = [],
}: {
  item: NavItem;
  depth?: number;
  collapsed: boolean;
  tokens: SidebarTokens;
  siblings?: NavItem[];
}) {
  const navCtx = useContext(SidebarNavContext);
  const [pickerOpen, setPickerOpen] = useState(false);
  const rowRef = useRef<HTMLDivElement>(null);
  const location = useLocation();
  const { customNavIcons, setCustomNavIcon } = useApp();
  const { t } = useI18n();
  const hasChildren = (item.children?.length ?? 0) > 0;
  const isExactActive = Boolean(item.path && location.pathname === item.path);
  const isGroupOpen = hasChildren && isItemActive(item, location.pathname);
  const customIconName = depth === 0 ? customNavIcons[item.id] : undefined;
  const IconComp = customIconName ? (ICON_MAP[customIconName] ?? item.icon) : item.icon;

  const open = hasChildren && navCtx ? navCtx.openIds.has(item.id) : false;
  const setOpen = () => {
    if (hasChildren && navCtx) {
      navCtx.toggleGroup(item.id, siblings);
    }
  };

  const handleClick = () => {
    if (openNavItem(item) === null && item.external) return;
    if (hasChildren) {
      setOpen();
    }
  };

  const rowTitle = collapsed
    ? t(item.label)
    : depth === 0 && !collapsed
      ? `${t(item.label)} (right-click to change icon)`
      : undefined;

  const buttonSx = navItemButtonSx(tokens, isExactActive, depth, collapsed);

  const rowInner = (
    <>
      {isExactActive && !collapsed && (
        <Box
          sx={{
            position: "absolute",
            left: 0,
            top: "22%",
            bottom: "22%",
            width: 3,
            borderRadius: "9999px",
            bgcolor: tokens.nav.accentBar,
          }}
        />
      )}

      <Box component="span" data-nav-icon sx={navIconWrapSx(tokens, isExactActive, isGroupOpen)}>
        <NavIcon icon={IconComp} size={18} />
      </Box>

      {!collapsed && (
        <>
          <ListItemText
            primary={t(item.label)}
            slotProps={{
              primary: {
                variant: "body2",
                noWrap: true,
                sx: {
                  fontWeight: isExactActive ? 600 : 500,
                  lineHeight: 1.4,
                },
              },
            }}
            sx={{ my: 0, flex: 1, minWidth: 0 }}
          />
          {(item.badge === "pro" ||
            item.badge === "ai" ||
            item.later ||
            item.external ||
            hasChildren) && (
            <Box
              component="span"
              sx={{ ml: "auto", display: "flex", flexShrink: 0, alignItems: "center", gap: 0.5 }}
            >
              {item.badge === "pro" && <ProBadge />}
              {item.badge === "ai" && <AiBadge />}
              {item.later && <ScheduleIcon sx={{ fontSize: 13, color: tokens.chevron }} />}
              {item.external && <OpenInNewIcon sx={{ fontSize: 13, color: tokens.chevron }} />}
              {hasChildren &&
                (open ? (
                  <ExpandMoreIcon sx={{ fontSize: 16, color: tokens.chevron }} />
                ) : (
                  <Box component="span" sx={{ display: "inline-flex", color: tokens.chevron }}>
                    <ChevronRightIcon sx={{ fontSize: 13 }} />
                  </Box>
                ))}
            </Box>
          )}
        </>
      )}
    </>
  );

  const rowContextMenu =
    depth === 0
      ? (e: React.MouseEvent) => {
          e.preventDefault();
          setPickerOpen((p) => !p);
        }
      : undefined;

  return (
    <Box ref={rowRef} sx={{ position: "relative" }}>
      {item.path && !item.external ? (
        <ListItemButton
          component={NavLink}
          to={item.path}
          end
          title={rowTitle}
          onContextMenu={rowContextMenu}
          sx={buttonSx}
        >
          {rowInner}
        </ListItemButton>
      ) : (
        <ListItemButton
          title={rowTitle}
          onContextMenu={rowContextMenu}
          onClick={handleClick}
          sx={buttonSx}
        >
          {rowInner}
        </ListItemButton>
      )}

      {hasChildren && open && !collapsed ? (
        <Box>
          {item.children!.map((child) => (
            <NavItemRow
              key={child.id}
              item={child}
              depth={depth + 1}
              collapsed={collapsed}
              tokens={tokens}
              siblings={item.children!}
            />
          ))}
        </Box>
      ) : null}
      {pickerOpen && depth === 0 ? (
        <IconPicker
          anchorEl={rowRef.current}
          current={customIconName ?? ""}
          onPick={(name) => setCustomNavIcon(item.id, name)}
          onClose={() => setPickerOpen(false)}
        />
      ) : null}
    </Box>
  );
}
