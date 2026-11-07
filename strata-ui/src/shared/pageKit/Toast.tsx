// SPDX-License-Identifier: Apache-2.0
import { create } from "zustand";
import { Alert, Box, IconButton, Snackbar } from "@mui/material";
import CloseIcon from "@mui/icons-material/Close";
import { useI18n } from "@/shared/i18n";

export type ToastKind = "success" | "error" | "info";

interface ToastItem {
  id: number;
  kind: ToastKind;
  message: string;
}

interface ToastState {
  items: ToastItem[];
  push: (kind: ToastKind, message: string) => void;
  dismiss: (id: number) => void;
}

const AUTO_DISMISS_MS = 4000;
let nextId = 1;

export const useToastStore = create<ToastState>((set) => ({
  items: [],
  push: (kind, message) => {
    const id = nextId++;
    set((s) => ({ items: [...s.items, { id, kind, message }] }));
    setTimeout(() => {
      set((s) => ({ items: s.items.filter((t) => t.id !== id) }));
    }, AUTO_DISMISS_MS);
  },
  dismiss: (id) => set((s) => ({ items: s.items.filter((t) => t.id !== id) })),
}));

/** Imperative toast API — messages are i18n short keys, translated at render time. */
export const toast = {
  success: (message: string) => useToastStore.getState().push("success", message),
  error: (message: string) => useToastStore.getState().push("error", message),
  info: (message: string) => useToastStore.getState().push("info", message),
};

/** Rendered once at the app root (App.tsx). */
export function ToastViewport() {
  const items = useToastStore((s) => s.items);
  const dismiss = useToastStore((s) => s.dismiss);
  const { t, tError } = useI18n();
  if (items.length === 0) return null;

  return (
    <Box
      sx={{
        position: "fixed",
        right: 20,
        bottom: 20,
        zIndex: (theme) => theme.zIndex.snackbar,
        display: "flex",
        flexDirection: "column",
        gap: 1,
        width: 320,
        maxWidth: "calc(100vw - 40px)",
      }}
    >
      {items.map((item) => (
        <Snackbar
          key={item.id}
          open
          anchorOrigin={{ vertical: "bottom", horizontal: "right" }}
          sx={{ position: "relative", inset: "auto", transform: "none" }}
        >
          <Alert
            severity={item.kind}
            variant="filled"
            action={
              <IconButton
                size="small"
                color="inherit"
                aria-label={t("common.dismiss")}
                onClick={() => dismiss(item.id)}
              >
                <CloseIcon fontSize="small" />
              </IconButton>
            }
            sx={{ width: "100%", alignItems: "center" }}
          >
            {item.kind === "error" ? tError(item.message) : t(item.message)}
          </Alert>
        </Snackbar>
      ))}
    </Box>
  );
}
