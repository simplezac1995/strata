// SPDX-License-Identifier: Apache-2.0
import { Box } from "@mui/material";
import { PageHeader } from "@/components/ui/index";
import { useI18n } from "@/shared/i18n";
import { SWAGGER_UI_URL } from "@/shared/config";
import { PageStack } from "@/shared/pageKit/PageLayout";

/**
 * Built-in API documentation (#14). Embeds the backend Swagger UI in a full-height
 * iframe. The URL is proxied in dev via vite.config.ts and served by the backend in
 * production.
 */
export function ApiDocs() {
  const { t } = useI18n();

  return (
    <PageStack sx={{ flex: 1, minHeight: 0 }}>
      <PageHeader title={t("nav.api.docs")} subtitle={t("tools.apiDocs.subtitle")} />
      <Box
        sx={{
          flex: 1,
          minHeight: 0,
          height: "calc(100vh - 260px)",
          border: 1,
          borderColor: "divider",
          borderRadius: 2,
          overflow: "hidden",
          bgcolor: "background.paper",
        }}
      >
        <Box
          component="iframe"
          src={SWAGGER_UI_URL}
          title={t("nav.api.docs")}
          sx={{ width: "100%", height: "100%", border: 0 }}
        />
      </Box>
    </PageStack>
  );
}

export default ApiDocs;
