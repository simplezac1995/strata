// SPDX-License-Identifier: Apache-2.0
import { useState, type ReactNode } from "react";
import { IconButton, ListItemIcon, Menu, MenuItem, Stack, useMediaQuery } from "@mui/material";
import { useTheme } from "@mui/material/styles";
import { MoreVert as MoreVertIcon } from "@mui/icons-material";
import { Button } from "./Button";
import { useI18n } from "@/shared/i18n";

export interface RowAction {
  key: string;
  label: string;
  icon?: ReactNode;
  onClick: () => void;
  danger?: boolean;
  disabled?: boolean;
}

/**
 * Responsive row action column:
 * - Desktop (>=md): inline text buttons with icons.
 * - Narrow (<md): a single overflow IconButton opening a Menu.
 */
export function RowActions({ actions }: { actions: RowAction[] }) {
  const theme = useTheme();
  const { t } = useI18n();
  const isNarrow = useMediaQuery(theme.breakpoints.down("xs"));
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const open = Boolean(anchorEl);

  if (actions.length === 0) return null;

  // Desktop (>=md): render inline text buttons.
  if (!isNarrow) {
    return (
      <Stack
        direction="row"
        spacing={0.5}
        sx={{ flexWrap: "nowrap", whiteSpace: "nowrap", display: "inline-flex" }}
      >
        {actions.map((a) => (
          <Button
            key={a.key}
            variant="text"
            size="small"
            color={a.danger ? "danger" : "primary"}
            disabled={a.disabled}
            onClick={a.onClick}
            startIcon={a.icon}
          >
            {a.label}
          </Button>
        ))}
      </Stack>
    );
  }

  // Narrow: overflow menu.
  const handleOpen = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget);
  };
  const handleClose = () => {
    setAnchorEl(null);
  };
  const handleSelect = (action: RowAction) => () => {
    handleClose();
    action.onClick();
  };

  return (
    <>
      <IconButton size="small" aria-label={t("common.moreActions")} onClick={handleOpen}>
        <MoreVertIcon fontSize="small" />
      </IconButton>
      <Menu anchorEl={anchorEl} open={open} onClose={handleClose}>
        {actions.map((a) => (
          <MenuItem
            key={a.key}
            disabled={a.disabled}
            onClick={handleSelect(a)}
            sx={a.danger ? { color: "error.main" } : undefined}
          >
            {a.icon ? (
              <ListItemIcon sx={{ color: a.danger ? "inherit" : undefined }}>{a.icon}</ListItemIcon>
            ) : null}
            {a.label}
          </MenuItem>
        ))}
      </Menu>
    </>
  );
}
