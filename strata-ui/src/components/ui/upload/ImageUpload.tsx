// SPDX-License-Identifier: Apache-2.0
import { useCallback, useEffect, useRef, useState } from "react";
import type { ChangeEvent, DragEvent, ClipboardEvent } from "react";
import { Box, CircularProgress, Stack, Typography } from "@mui/material";
import CloudUploadIcon from "@mui/icons-material/CloudUpload";
import DeleteIcon from "@mui/icons-material/Delete";
import type { Area } from "react-easy-crop";
import { fileApi } from "@/shared/api/system";
import { toast } from "@/shared/pageKit/Toast";
import { useI18n } from "@/shared/i18n";
import { Button } from "@/components/ui/Button";
import { resolveUploadedObjectUrl } from "./fileUrl";
import { createCroppedImage, createThumbnail } from "./imageUtils";
import { CropDialog } from "./CropDialog";

export interface UploadedImage {
  url: string;
  thumbnailUrl?: string;
}

export interface ImageUploadProps {
  value?: UploadedImage | null;
  onChange?: (value: UploadedImage | null) => void;
  /** Lock the crop to this aspect (e.g. 1 for a square avatar). Omit for free-form. */
  aspect?: number;
  /** Dropzone hint label. */
  label?: string;
  disabled?: boolean;
}

const ACCEPTED = "image/png,image/jpeg,image/webp,image/gif";

export function ImageUpload({ value, onChange, aspect, label, disabled }: ImageUploadProps) {
  const { t } = useI18n();
  const inputRef = useRef<HTMLInputElement>(null);
  const [cropSrc, setCropSrc] = useState<string | null>(null);
  const [cropOpen, setCropOpen] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [dragOver, setDragOver] = useState(false);

  // Revoke any object URLs we created on unmount.
  const liveUrls = useRef<string[]>([]);
  useEffect(() => {
    return () => {
      // eslint-disable-next-line react-hooks/exhaustive-deps -- revoke everything this component created
      for (const u of liveUrls.current) URL.revokeObjectURL(u);
    };
  }, []);

  const track = useCallback((url: string) => {
    liveUrls.current.push(url);
    return url;
  }, []);

  const openPicker = useCallback(() => {
    if (!disabled) inputRef.current?.click();
  }, [disabled]);

  const handleFile = useCallback(
    (file: File | undefined | null) => {
      if (!file || !file.type.startsWith("image/")) {
        toast.error("upload.error.notImage");
        return;
      }
      const objectUrl = track(URL.createObjectURL(file));
      setCropSrc(objectUrl);
      setCropOpen(true);
    },
    [track],
  );

  const onInputChange = useCallback(
    (e: ChangeEvent<HTMLInputElement>) => {
      handleFile(e.target.files?.[0]);
      e.target.value = "";
    },
    [handleFile],
  );

  const onDrop = useCallback(
    (e: DragEvent<HTMLDivElement>) => {
      e.preventDefault();
      setDragOver(false);
      handleFile(e.dataTransfer.files?.[0]);
    },
    [handleFile],
  );

  const onPaste = useCallback(
    (e: ClipboardEvent<HTMLDivElement>) => {
      const items = e.clipboardData?.items;
      if (!items) return;
      for (const item of Array.from(items)) {
        if (item.type.startsWith("image/")) {
          const file = item.getAsFile();
          if (file) {
            e.preventDefault();
            handleFile(file);
            return;
          }
        }
      }
    },
    [handleFile],
  );

  const handleCropConfirm = useCallback(
    async (area: Area) => {
      if (!cropSrc) return;
      setCropOpen(false);
      setUploading(true);
      try {
        const mainBlob = await createCroppedImage(cropSrc, area, 1600, 0.85);
        const mainFile = new File([mainBlob], "image.jpg", { type: "image/jpeg" });
        const dto = await fileApi.upload(mainFile, "uploads");
        const url = await resolveUploadedObjectUrl(dto);
        track(url);

        let thumbnailUrl: string | undefined;
        try {
          const thumbBlob = await createThumbnail(cropSrc, area, 200);
          thumbnailUrl = track(URL.createObjectURL(thumbBlob));
        } catch {
          thumbnailUrl = undefined;
        }

        onChange?.({ url, thumbnailUrl });
      } catch {
        toast.error("upload.error.failed");
      } finally {
        setUploading(false);
      }
    },
    [cropSrc, onChange, track],
  );

  const handleClear = useCallback(() => {
    onChange?.(null);
  }, [onChange]);

  return (
    <Box>
      <input
        ref={inputRef}
        type="file"
        accept={ACCEPTED}
        hidden
        onChange={onInputChange}
        data-testid="image-upload-input"
      />

      {value?.url ? (
        <Box
          sx={{
            position: "relative",
            width: "100%",
            maxWidth: 320,
            borderRadius: 1,
            overflow: "hidden",
            border: (theme) => `1px solid ${theme.palette.divider}`,
          }}
        >
          <Box
            component="img"
            src={value.url}
            alt="uploaded"
            sx={{ display: "block", width: "100%" }}
          />
          <Stack
            direction="row"
            spacing={1}
            sx={{ p: 0.75, bgcolor: "action.hover", justifyContent: "flex-end" }}
          >
            <Button
              size="sm"
              variant="outlined"
              color="default"
              onClick={openPicker}
              disabled={disabled || uploading}
            >
              {t("upload.replace")}
            </Button>
            <Button
              size="sm"
              variant="text"
              color="danger"
              onClick={handleClear}
              disabled={disabled || uploading}
            >
              <DeleteIcon fontSize="small" />
            </Button>
          </Stack>
        </Box>
      ) : (
        <Box
          role="button"
          tabIndex={0}
          aria-label={label ?? t("upload.dropzoneLabel")}
          onClick={openPicker}
          onKeyDown={(e) => {
            if (e.key === "Enter" || e.key === " ") {
              e.preventDefault();
              openPicker();
            }
          }}
          onDragOver={(e) => {
            e.preventDefault();
            setDragOver(true);
          }}
          onDragLeave={() => setDragOver(false)}
          onDrop={onDrop}
          onPaste={onPaste}
          sx={{
            border: (theme) =>
              `1.5px dashed ${dragOver ? theme.palette.primary.main : theme.palette.divider}`,
            borderRadius: 1,
            px: 2,
            py: 4,
            textAlign: "center",
            cursor: disabled ? "default" : "pointer",
            bgcolor: dragOver ? "action.selected" : "action.hover",
            transition: "background-color .15s",
          }}
        >
          {uploading ? (
            <CircularProgress size={32} />
          ) : (
            <>
              <CloudUploadIcon color="action" sx={{ fontSize: 36, mb: 0.5 }} />
              <Typography variant="body2" color="text.secondary">
                {label ?? t("upload.dropzoneLabel")}
              </Typography>
              <Typography variant="caption" color="text.disabled">
                {t("upload.dropzoneHint")}
              </Typography>
            </>
          )}
        </Box>
      )}

      <CropDialog
        open={cropOpen}
        image={cropSrc}
        aspect={aspect}
        onCancel={() => setCropOpen(false)}
        onConfirm={(area) => void handleCropConfirm(area)}
      />
    </Box>
  );
}

export default ImageUpload;
