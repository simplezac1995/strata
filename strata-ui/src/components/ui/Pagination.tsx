// SPDX-License-Identifier: Apache-2.0
import {
  FormControl,
  MenuItem,
  Pagination as MuiPagination,
  Select as MuiSelect,
  Stack,
  Typography,
} from "@mui/material";
import { useI18n } from "@/shared/i18n";

export function Pagination({
  total,
  page,
  pageSize,
  onPage,
  onPageSize,
}: {
  total: number;
  page: number;
  pageSize: number;
  onPage: (p: number) => void;
  onPageSize: (s: number) => void;
}) {
  const { t } = useI18n();
  const totalPages = Math.max(1, Math.ceil(total / pageSize));
  return (
    <Stack
      direction={{ xs: "column", sm: "row" }}
      spacing={2}
      sx={{
        mt: 2.5,
        pt: 2,
        px: 2,
        pb: 1.5,
        alignItems: { xs: "stretch", sm: "center" },
        justifyContent: "space-between",
        borderTop: 1,
        borderColor: "divider",
      }}
    >
      <Typography variant="body2" color="text.secondary">
        {t("common.totalRecords").replace("{n}", String(total))}
      </Typography>
      <Stack direction="row" spacing={2} sx={{ alignItems: "center" }}>
        <FormControl size="small" sx={{ minWidth: 120 }}>
          <MuiSelect value={pageSize} onChange={(e) => onPageSize(Number(e.target.value))}>
            {[10, 20, 50, 100].map((s) => (
              <MenuItem key={s} value={s}>
                {t("common.pageSize").replace("{s}", String(s))}
              </MenuItem>
            ))}
          </MuiSelect>
        </FormControl>
        <MuiPagination
          count={totalPages}
          page={page}
          onChange={(_, p) => onPage(p)}
          color="primary"
          size="small"
        />
      </Stack>
    </Stack>
  );
}
