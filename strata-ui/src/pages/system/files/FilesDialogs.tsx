// SPDX-License-Identifier: Apache-2.0
import CloseIcon from "@mui/icons-material/Close";
import Box from "@mui/material/Box";
import MuiDialog from "@mui/material/Dialog";
import IconButton from "@mui/material/IconButton";
import type { RefObject } from "react";

import { Button, ConfirmDialog, Dialog, ImageUpload } from "@/components/ui/index";
import { useI18n } from "@/shared/i18n";

/**
 * The file browser's four dialogs: the crop-upload dialog, the fullscreen image preview, the single delete
 * confirmation and the batch delete confirmation.
 *
 * They were the last 80 lines of Files.tsx, which is now only the page composition and its query/mutation wiring.
 */
export function FilesDialogs({
  uploadOpen,
  onUploadClose,
  onUploaded,
  fileInputRef,
  previewKey,
  previewUrl,
  onPreviewClose,
  deletingKey,
  onDeleteClose,
  onDeleteConfirm,
  batchOpen,
  onBatchClose,
  onBatchConfirm,
  batchPending,
  selectedCount,
}: {
  uploadOpen: boolean;
  onUploadClose: () => void;
  onUploaded: () => void;
  fileInputRef: RefObject<HTMLInputElement | null>;
  previewKey: string | null;
  previewUrl: string | null;
  onPreviewClose: () => void;
  deletingKey: string | null;
  onDeleteClose: () => void;
  onDeleteConfirm: () => void;
  batchOpen: boolean;
  onBatchClose: () => void;
  onBatchConfirm: () => void;
  batchPending: boolean;
  selectedCount: number;
}) {
  const { t } = useI18n();

  return (
    <>
      <Dialog open={uploadOpen} onClose={onUploadClose} title={t("system.files.uploadFile")}>
        <ImageUpload
          value={null}
          onChange={(image) => {
            if (image) {
              onUploadClose();
              onUploaded();
            }
          }}
        />
        <Box sx={{ mt: 2 }}>
          <Button
            variant="outlined"
            color="default"
            size="sm"
            onClick={() => fileInputRef.current?.click()}
          >
            {t("system.files.uploadAnyFile")}
          </Button>
        </Box>
      </Dialog>

      <MuiDialog
        open={previewKey !== null}
        onClose={onPreviewClose}
        fullScreen
        sx={{ bgcolor: "rgba(0,0,0,0.9)" }}
      >
        <IconButton
          aria-label={t("common.close")}
          onClick={onPreviewClose}
          sx={{ position: "absolute", right: 16, top: 16, color: "#fff", zIndex: 1 }}
        >
          <CloseIcon />
        </IconButton>
        <Box
          sx={{
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            height: "100%",
            p: 4,
          }}
        >
          {previewUrl ? (
            <Box
              component="img"
              src={previewUrl}
              alt={previewKey ?? ""}
              sx={{ maxWidth: "100%", maxHeight: "100%", objectFit: "contain" }}
            />
          ) : null}
        </Box>
      </MuiDialog>

      <ConfirmDialog
        open={deletingKey !== null}
        onClose={onDeleteClose}
        onConfirm={onDeleteConfirm}
        title={t("system.files.deleteFile")}
        description={t("system.files.delete.withAsterisk").replace("{name}", deletingKey ?? "")}
        confirmColor="danger"
      />

      <ConfirmDialog
        open={batchOpen}
        onClose={onBatchClose}
        confirmDisabled={batchPending}
        onConfirm={onBatchConfirm}
        title={t("common.batchDeleteTitle")}
        description={t("common.batchDeleteConfirm").replace("{n}", String(selectedCount))}
        confirmColor="danger"
      />
    </>
  );
}
