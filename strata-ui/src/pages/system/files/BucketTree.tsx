// SPDX-License-Identifier: Apache-2.0
import FolderIcon from "@mui/icons-material/Folder";
import Box from "@mui/material/Box";
import { Button } from "@/components/ui/index";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";

import { useI18n } from "@/shared/i18n";

import { ALL_BUCKET } from "./fileHelpers";

/** The left bucket tree of the file browser: "all files" plus one entry per bucket. */
export function BucketTree({
  buckets,
  bucket,
  onSelect,
}: {
  buckets: string[];
  bucket: string;
  onSelect: (bucket: string) => void;
}) {
  const { t } = useI18n();

  return (
    <Box
      sx={{
        width: 200,
        flexShrink: 0,
        display: { xs: "none", md: "block" },
      }}
    >
      <Stack spacing={0.5}>
        <Button
          variant={bucket === ALL_BUCKET ? "contained" : "text"}
          size="sm"
          onClick={() => onSelect(ALL_BUCKET)}
          sx={{ justifyContent: "flex-start" }}
        >
          <FolderIcon fontSize="small" sx={{ mr: 1 }} />
          {t("system.files.allFiles")}
        </Button>
        {buckets.map((name) => (
          <Button
            key={name}
            variant={bucket === name ? "contained" : "text"}
            size="sm"
            onClick={() => onSelect(name)}
            sx={{ justifyContent: "flex-start", pl: 3 }}
          >
            <Typography variant="caption" sx={{ fontFamily: "monospace" }}>
              {name}
            </Typography>
          </Button>
        ))}
      </Stack>
    </Box>
  );
}
