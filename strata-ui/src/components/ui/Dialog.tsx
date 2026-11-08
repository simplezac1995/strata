// SPDX-License-Identifier: Apache-2.0
import type { ReactNode } from "react";
import {
  Box,
  Dialog as MuiDialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Stack,
  Typography,
  alpha,
} from "@mui/material";
import type { DialogProps as MuiDialogProps } from "@mui/material";
import { WarningAmber } from "@mui/icons-material";
import { useI18n } from "@/shared/i18n";
import { Button } from "./Button";

// Dialog: fully inherits MUI9 DialogProps and forwards all props.
export interface DialogProps extends Omit<MuiDialogProps, "fullWidth" | "title"> {
  title: ReactNode;
  maxWidth?: MuiDialogProps["maxWidth"];
}

export function Dialog({ open, onClose, title, children, maxWidth = "sm", ...props }: DialogProps) {
  return (
    <MuiDialog open={open} onClose={onClose} fullWidth maxWidth={maxWidth} {...props}>
      <DialogTitle sx={{ pb: 1 }}>{title}</DialogTitle>
      <DialogContent dividers sx={{ p: 3, "&:first-of-type": { pt: 1.5 } }}>
        {children}
      </DialogContent>
    </MuiDialog>
  );
}

export function ConfirmDialog({
  open,
  onClose,
  onConfirm,
  title,
  description,
  confirmText,
  confirmColor = "danger",
  confirmDisabled,
}: {
  open: boolean;
  onClose: () => void;
  onConfirm: () => void;
  title: string;
  description: string;
  confirmText?: string;
  confirmColor?: "danger" | "primary";
  confirmDisabled?: boolean;
}) {
  const { t } = useI18n();
  return (
    <MuiDialog open={open} onClose={onClose} fullWidth maxWidth="xs">
      <DialogTitle>
        <Stack direction="row" spacing={1.5} sx={{ alignItems: "center" }}>
          <Box
            sx={{
              width: 40,
              height: 40,
              borderRadius: 2,
              flexShrink: 0,
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              bgcolor: (theme) => alpha(theme.palette.error.main, 0.12),
              color: "error.main",
            }}
          >
            <WarningAmber />
          </Box>
          <Typography component="div" sx={{ fontWeight: 600 }}>
            {t(title)}
          </Typography>
        </Stack>
      </DialogTitle>
      <DialogContent>
        <Typography variant="body2" color="text.secondary">
          {t(description)}
        </Typography>
        <Typography variant="caption" color="error" sx={{ display: "block", mt: 0.5 }}>
          {t("common.irreversible")}
        </Typography>
      </DialogContent>
      <DialogActions>
        <Button variant="outlined" color="default" size="sm" onClick={onClose}>
          {t("common.cancel")}
        </Button>
        <Button
          variant="contained"
          color={confirmColor === "danger" ? "danger" : "primary"}
          size="sm"
          onClick={onConfirm}
          disabled={confirmDisabled}
        >
          {t(confirmText ?? "common.delete")}
        </Button>
      </DialogActions>
    </MuiDialog>
  );
}
