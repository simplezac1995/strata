// SPDX-License-Identifier: Apache-2.0
import Box from "@mui/material/Box";
import ImageIcon from "@mui/icons-material/Image";
import { useEffect, useState } from "react";

import { resolveUploadedObjectUrl } from "@/components/ui/upload/fileUrl";
import type { FileInfoDto } from "@/shared/types";

/** Lazily resolves an authenticated blob URL for a single image thumbnail. */
export function ImageThumb({ file, onClick }: { file: FileInfoDto; onClick: () => void }) {
  const [url, setUrl] = useState<string | null>(null);

  useEffect(() => {
    let revoked = false;
    let created: string | null = null;
    resolveUploadedObjectUrl(file)
      .then((resolved) => {
        if (revoked) {
          URL.revokeObjectURL(resolved);
          return;
        }
        created = resolved;
        setUrl(resolved);
      })
      .catch(() => undefined);
    return () => {
      revoked = true;
      if (created) URL.revokeObjectURL(created);
    };
  }, [file]);

  return (
    <Box
      role="button"
      tabIndex={0}
      onClick={onClick}
      onKeyDown={(event) => {
        if (event.key === "Enter") onClick();
      }}
      sx={{
        width: "100%",
        aspectRatio: "1 / 1",
        bgcolor: "action.hover",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        overflow: "hidden",
        cursor: "pointer",
      }}
    >
      {url ? (
        <Box
          component="img"
          src={url}
          alt={file.key}
          sx={{ width: "100%", height: "100%", objectFit: "cover" }}
        />
      ) : (
        <ImageIcon color="disabled" sx={{ fontSize: 40 }} />
      )}
    </Box>
  );
}
