// SPDX-License-Identifier: Apache-2.0
// Icon picker popover used when editing a menu item.

import { Box, IconButton, Popover, Typography, Button } from "@mui/material";
import { useI18n } from "@/shared/i18n";
import { ICON_PALETTE } from "@/components/shell/navIcons";
import { NavIcon } from "@/components/shell/NavIcon";

export function IconPicker({
  anchorEl,
  onPick,
  onClose,
  current,
}: {
  anchorEl: HTMLElement | null;
  onPick: (name: string) => void;
  onClose: () => void;
  current: string;
}) {
  const { t } = useI18n();
  return (
    <Popover
      open={Boolean(anchorEl)}
      anchorEl={anchorEl}
      onClose={onClose}
      anchorOrigin={{ vertical: "top", horizontal: "right" }}
      transformOrigin={{ vertical: "top", horizontal: "left" }}
      slotProps={{ paper: { sx: { width: 208, p: 1.5 } } }}
    >
      <Typography variant="overline" color="text.secondary" sx={{ display: "block", mb: 1 }}>
        {t("shell.sidebar.pickIcon")}
      </Typography>
      <Box sx={{ display: "grid", gridTemplateColumns: "repeat(6, 1fr)", gap: 0.5 }}>
        {ICON_PALETTE.map(({ name, Comp }) => (
          <IconButton
            key={name}
            size="small"
            title={name}
            aria-label={name}
            color={current === name ? "primary" : "default"}
            onClick={() => {
              onPick(name);
              onClose();
            }}
          >
            <NavIcon icon={Comp} size={13} />
          </IconButton>
        ))}
      </Box>
      <Button fullWidth size="small" onClick={onClose} sx={{ mt: 1 }}>
        {t("common.cancel")}
      </Button>
    </Popover>
  );
}
