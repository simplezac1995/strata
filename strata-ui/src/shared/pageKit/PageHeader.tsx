// SPDX-License-Identifier: Apache-2.0
import type { ReactNode } from "react";
import { IconButton, Stack, Typography } from "@mui/material";
import { ArrowBack } from "@mui/icons-material";
import { useI18n } from "@/shared/i18n";

/** Page title block — unified page header (title + optional subtitle/actions). */
export function PageHeader({
  title,
  subtitle,
  actions,
  onBack,
}: {
  title: ReactNode;
  subtitle?: ReactNode;
  actions?: ReactNode;
  onBack?: () => void;
}) {
  const { t } = useI18n();
  return (
    <Stack
      direction={{ xs: "column", sm: "row" }}
      spacing={2}
      sx={{
        mb: 0.5,
        justifyContent: "space-between",
        alignItems: { xs: "stretch", sm: "flex-end" },
        gap: { xs: 1.5, sm: 2 },
      }}
    >
      <Stack direction="row" spacing={1.25} sx={{ minWidth: 0, flex: 1, alignItems: "flex-start" }}>
        {onBack ? (
          <IconButton
            size="small"
            onClick={onBack}
            aria-label={t("common.back")}
            sx={{ mt: 0.25, flexShrink: 0 }}
          >
            <ArrowBack />
          </IconButton>
        ) : null}
        <Stack spacing={0.75} sx={{ minWidth: 0 }}>
          <Typography
            variant="h2"
            component="h1"
            sx={{
              fontSize: { xs: "1.25rem", sm: "1.375rem" },
              fontWeight: 700,
              letterSpacing: "-0.02em",
              lineHeight: 1.3,
            }}
          >
            {title}
          </Typography>
          {subtitle ? (
            <Typography variant="body2" color="text.secondary" sx={{ maxWidth: "48rem" }}>
              {subtitle}
            </Typography>
          ) : null}
        </Stack>
      </Stack>
      {actions ? (
        <Stack
          direction="row"
          spacing={1}
          sx={{
            flexShrink: 0,
            flexWrap: "wrap",
            justifyContent: { xs: "flex-start", sm: "flex-end" },
          }}
        >
          {actions}
        </Stack>
      ) : null}
    </Stack>
  );
}
