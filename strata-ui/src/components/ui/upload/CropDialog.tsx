// SPDX-License-Identifier: Apache-2.0
import { useCallback, useState } from "react";
import Cropper from "react-easy-crop";
import { Box, Dialog as MuiDialog, DialogActions, DialogTitle, Typography } from "@mui/material";
import type { Area } from "react-easy-crop";
import { Button } from "@/components/ui/Button";
import { useI18n } from "@/shared/i18n";

export interface CropDialogProps {
  open: boolean;
  /** Object URL of the image to crop. */
  image: string | null;
  /** Aspect ratio lock; undefined means free-form crop. */
  aspect?: number;
  onCancel: () => void;
  /** Fired with the absolute-pixel crop rect on the source image. */
  onConfirm: (area: Area) => void;
}

const CROP_ZOOM = 1;

export function CropDialog({ open, image, aspect, onCancel, onConfirm }: CropDialogProps) {
  const { t } = useI18n();
  const [crop, setCrop] = useState({ x: 0, y: 0 });
  const [zoom, setZoom] = useState(1);
  const [croppedAreaPixels, setCroppedAreaPixels] = useState<Area | null>(null);

  const onCropComplete = useCallback((_croppedArea: Area, croppedPixels: Area) => {
    setCroppedAreaPixels(croppedPixels);
  }, []);

  const handleConfirm = () => {
    if (croppedAreaPixels) onConfirm(croppedAreaPixels);
  };

  return (
    <MuiDialog open={open} onClose={onCancel} fullWidth maxWidth="sm">
      <DialogTitle>{t("upload.cropTitle")}</DialogTitle>
      <Box
        sx={{
          position: "relative",
          width: "100%",
          height: 360,
          bgcolor: "action.disabledBackground",
        }}
      >
        {image ? (
          <Cropper
            image={image}
            crop={crop}
            zoom={zoom}
            aspect={aspect ?? CROP_ZOOM}
            cropShape="rect"
            showGrid
            onCropChange={setCrop}
            onZoomChange={setZoom}
            onCropComplete={onCropComplete}
          />
        ) : null}
      </Box>
      <DialogActions sx={{ px: 2, py: 1.5 }}>
        <Typography variant="body2" sx={{ mr: "auto", color: "text.secondary" }}>
          {aspect ? t("upload.aspectSquare") : t("upload.aspectFree")}
        </Typography>
        <Button variant="outlined" color="default" onClick={onCancel}>
          {t("common.cancel")}
        </Button>
        <Button variant="contained" onClick={handleConfirm}>
          {t("common.confirm")}
        </Button>
      </DialogActions>
    </MuiDialog>
  );
}

export default CropDialog;
