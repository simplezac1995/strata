// SPDX-License-Identifier: Apache-2.0
import {
  Box,
  Card,
  CardContent,
  Chip,
  Divider,
  Grid,
  Paper,
  Stack,
  Typography,
} from "@mui/material";
import { useTheme } from "@mui/material/styles";
import { Badge, PageHeader } from "@/components/ui/index";
import { PageStack } from "@/shared/pageKit/PageLayout";
import { useI18n } from "@/shared/i18n";

export function Foundations() {
  const { t } = useI18n();
  const theme = useTheme();

  const paletteRows = [
    { label: "primary.main", color: theme.palette.primary.main },
    { label: "primary.dark", color: theme.palette.primary.dark },
    { label: "primary.light", color: theme.palette.primary.light },
    { label: "secondary.main", color: theme.palette.secondary.main },
    { label: "success.main", color: theme.palette.success.main },
    { label: "warning.main", color: theme.palette.warning.main },
    { label: ["error", "main"].join("."), color: theme.palette.error.main },
  ];

  const typographyRows = [
    { variant: "h1" as const, sample: "Strata Admin" },
    { variant: "h2" as const, sample: "Users Management" },
    { variant: "h3" as const, sample: "System Configuration" },
    { variant: "h4" as const, sample: "Recent Operations" },
    { variant: "body1" as const, sample: "Configure system users and their access roles" },
    { variant: "body2" as const, sample: "Updated 2 minutes ago · by admin" },
    { variant: "caption" as const, sample: "sys:user:1001 · sk-proj-•••" },
  ];

  const spacingKeys = [1, 2, 3, 4, 5, 6, 8, 10] as const;
  const elevations = [0, 1, 2, 3, 4, 6, 8, 12, 16, 24];

  return (
    <PageStack sx={{ maxWidth: 960 }}>
      <PageHeader
        title={t("docs.foundations.foundations")}
        subtitle={t("docs.foundations.subtitle")}
      />

      <Stack spacing={4}>
        <Box>
          <Typography variant="overline" color="text.secondary">
            {t("docs.foundations.colorPalette")}
          </Typography>
          <Grid container spacing={2}>
            {paletteRows.map((row) => (
              <Grid key={row.label} size={{ xs: 6, sm: 4, md: 3 }}>
                <Paper variant="outlined" sx={{ overflow: "hidden" }}>
                  <Box sx={{ height: 72, bgcolor: row.color }} />
                  <Box sx={{ p: 1.5 }}>
                    <Typography variant="caption" sx={{ fontWeight: 600 }}>
                      {row.label}
                    </Typography>
                    <Typography
                      variant="caption"
                      color="text.secondary"
                      sx={{ display: "block", fontFamily: "monospace" }}
                    >
                      {row.color}
                    </Typography>
                  </Box>
                </Paper>
              </Grid>
            ))}
          </Grid>
        </Box>

        <Box>
          <Typography variant="overline" color="text.secondary">
            {t("docs.foundations.typography")}
          </Typography>
          <Card variant="outlined">
            <CardContent sx={{ p: 0, "&:last-child": { pb: 0 } }}>
              {typographyRows.map((row, index) => (
                <Box key={row.variant}>
                  <Stack direction="row" spacing={3} sx={{ px: 3, py: 2, alignItems: "center" }}>
                    <Typography variant="caption" color="text.secondary" sx={{ width: 72 }}>
                      {row.variant}
                    </Typography>
                    <Typography variant={row.variant}>{row.sample}</Typography>
                  </Stack>
                  {index < typographyRows.length - 1 ? <Divider /> : null}
                </Box>
              ))}
            </CardContent>
          </Card>
        </Box>

        <Box>
          <Typography variant="overline" color="text.secondary">
            {t("docs.foundations.spacingScale")}
          </Typography>
          <Stack direction="row" spacing={2} sx={{ flexWrap: "wrap", alignItems: "flex-end" }}>
            {spacingKeys.map((key) => (
              <Stack key={key} spacing={0.5} sx={{ alignItems: "center" }}>
                <Box
                  sx={{
                    width: theme.spacing(key),
                    height: theme.spacing(key),
                    bgcolor: "primary.light",
                    borderRadius: 1,
                  }}
                />
                <Typography variant="caption" sx={{ fontFamily: "monospace" }}>
                  theme.spacing({key})
                </Typography>
              </Stack>
            ))}
          </Stack>
        </Box>

        <Box>
          <Typography variant="overline" color="text.secondary">
            {t("docs.foundations.borderRadius")}
          </Typography>
          <Stack direction="row" spacing={2} sx={{ flexWrap: "wrap" }}>
            {[theme.shape.borderRadius, 4, 8, 16, 999].map((radius) => (
              <Stack key={String(radius)} spacing={1} sx={{ alignItems: "center" }}>
                <Box
                  sx={{
                    width: 64,
                    height: 64,
                    border: 2,
                    borderColor: "primary.main",
                    bgcolor: "primary.light",
                    borderRadius: radius,
                  }}
                />
                <Typography variant="caption" sx={{ fontFamily: "monospace" }}>
                  {radius === 999 ? "999" : `${radius}px`}
                </Typography>
              </Stack>
            ))}
          </Stack>
        </Box>

        <Box>
          <Typography variant="overline" color="text.secondary">
            {t("docs.foundations.elevationLayers")}
          </Typography>
          <Grid container spacing={2}>
            {elevations.slice(0, 5).map((elevation) => (
              <Grid key={elevation} size={{ xs: 12, sm: 6, md: 4 }}>
                <Paper elevation={elevation} sx={{ p: 2, minHeight: 96 }}>
                  <Typography variant="subtitle2">elevation={elevation}</Typography>
                  <Typography variant="caption" color="text.secondary">
                    MUI Paper shadow token
                  </Typography>
                </Paper>
              </Grid>
            ))}
          </Grid>
        </Box>

        <Box>
          <Typography variant="overline" color="text.secondary">
            {t("docs.foundations.statusBadges")}
          </Typography>
          <Stack direction="row" spacing={1} useFlexGap sx={{ flexWrap: "wrap" }}>
            <Chip label={t("common.enabled")} color="success" size="small" />
            <Chip label={t("common.disabled")} size="small" />
            <Chip label={t("docs.components.warning")} color="warning" size="small" />
            <Chip label={t("docs.components.info")} color="info" size="small" />
            <Badge variant="success">{t("common.enabled")}</Badge>
            <Badge variant="error">{t("common.disabled")}</Badge>
            <Badge variant="pro">{t("docs.foundations.pro")}</Badge>
            <Badge variant="ai">{t("docs.foundations.aiPack2")}</Badge>
          </Stack>
        </Box>
      </Stack>
    </PageStack>
  );
}

export default Foundations;
