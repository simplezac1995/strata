// SPDX-License-Identifier: Apache-2.0
import { useNavigate } from "react-router-dom";
import { Box, Stack, Typography } from "@mui/material";
import { Button } from "@/components/ui/index";
import { useI18n } from "@/shared/i18n";

export function NotFound() {
  const navigate = useNavigate();
  const { t } = useI18n();
  return (
    <Box
      sx={{
        minHeight: "100vh",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        bgcolor: "background.default",
      }}
    >
      <Box sx={{ textAlign: "center" }}>
        <Typography
          sx={{
            fontSize: { xs: 96, sm: 128 },
            fontWeight: 600,
            lineHeight: 1,
            color: "action.selected",
          }}
        >
          404
        </Typography>
        <Typography variant="h5" sx={{ fontWeight: 700, mt: 1, mb: 1.5 }}>
          {t("error.notFound.title")}
        </Typography>
        <Typography color="text.secondary" sx={{ mb: 4 }}>
          {t("error.notFound.desc")}
        </Typography>
        <Stack direction="row" spacing={1.5} sx={{ justifyContent: "center" }}>
          <Button variant="contained" onClick={() => navigate("/dashboard")}>
            {t("common.goHome")}
          </Button>
          <Button variant="outlined" color="default" onClick={() => navigate(-1)}>
            {t("common.goBack")}
          </Button>
        </Stack>
      </Box>
    </Box>
  );
}

export default NotFound;
