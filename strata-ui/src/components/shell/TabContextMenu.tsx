// SPDX-License-Identifier: Apache-2.0
import { ListItemIcon, ListItemText, Menu, MenuItem } from "@mui/material";
import ChevronRightIcon from "@mui/icons-material/ChevronRight";
import CloseIcon from "@mui/icons-material/Close";
import FullscreenIcon from "@mui/icons-material/Fullscreen";
import RefreshIcon from "@mui/icons-material/Refresh";
import StarIcon from "@mui/icons-material/Star";
import TabIcon from "@mui/icons-material/Tab";
import ViewWeekIcon from "@mui/icons-material/ViewWeek";
import type { AppTab } from "@/context/AppContext";
import { useI18n } from "@/shared/i18n";

export type TabMenuAction =
  "refresh" | "close" | "closeOthers" | "closeRight" | "closeAll" | "fullscreen" | "favorite";

type TabContextMenuProps = {
  tab: AppTab;
  x: number;
  y: number;
  onAction: (action: TabMenuAction, tab: AppTab) => void;
  onClose: () => void;
  /** When true, "close right" is disabled (the tab is already rightmost). */
  isLastTab?: boolean;
};

const MENU_ITEMS: {
  action: TabMenuAction;
  labelKey: string;
  icon: typeof RefreshIcon;
  disabled?: (tab: AppTab, ctx: { isLastTab?: boolean }) => boolean;
}[] = [
  { action: "refresh", labelKey: "shell.tab.refresh", icon: RefreshIcon },
  {
    action: "close",
    labelKey: "shell.tab.close",
    icon: CloseIcon,
    disabled: (tab) => tab.id === "dashboard",
  },
  { action: "closeOthers", labelKey: "shell.tab.closeOthers", icon: TabIcon },
  {
    action: "closeRight",
    labelKey: "shell.tab.closeRight",
    icon: ChevronRightIcon,
    disabled: (tab, ctx) => tab.id === "dashboard" || !!ctx.isLastTab,
  },
  { action: "closeAll", labelKey: "shell.tab.closeAll", icon: ViewWeekIcon },
  { action: "fullscreen", labelKey: "shell.tab.fullscreen", icon: FullscreenIcon },
  { action: "favorite", labelKey: "shell.tab.favorite", icon: StarIcon },
];

export function TabContextMenu({ tab, x, y, onAction, onClose, isLastTab }: TabContextMenuProps) {
  const { t } = useI18n();

  return (
    <Menu
      open
      onClose={onClose}
      anchorReference="anchorPosition"
      anchorPosition={{ top: y, left: x }}
      slotProps={{ paper: { sx: { minWidth: 168 } } }}
    >
      {MENU_ITEMS.map(({ action, labelKey, icon: Icon, disabled }) => {
        const isDisabled = disabled?.(tab, { isLastTab }) ?? false;
        return (
          <MenuItem
            key={action}
            disabled={isDisabled}
            onClick={() => {
              if (isDisabled) return;
              onAction(action, tab);
              onClose();
            }}
          >
            <ListItemIcon>
              <Icon fontSize="small" />
            </ListItemIcon>
            <ListItemText primary={t(labelKey)} slotProps={{ primary: { variant: "body2" } }} />
          </MenuItem>
        );
      })}
    </Menu>
  );
}
