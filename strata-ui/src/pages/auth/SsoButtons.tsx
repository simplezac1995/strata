// SPDX-License-Identifier: Apache-2.0
import { Box, Button, CircularProgress, Divider, Stack } from "@mui/material";
import type { SocialProviderInfo } from "@/shared/auth/oidc";
import { useI18n } from "@/shared/i18n";

type SsoButtonsProps = {
  providers: SocialProviderInfo[];
  samlDisplayName: string | null;
  loading: boolean;
  onSso: (providerId: string) => void;
  onSaml: () => void;
};

/** Login-page SSO row: social-provider buttons plus an optional SAML button. */
export function SsoButtons({
  providers,
  samlDisplayName,
  loading,
  onSso,
  onSaml,
}: SsoButtonsProps) {
  const { t } = useI18n();
  if (providers.length === 0 && !samlDisplayName) {
    return null;
  }
  return (
    <Box sx={{ mt: 2 }}>
      <Divider sx={{ mb: 2 }}>{t("auth.sso.or")}</Divider>
      <Stack spacing={1}>
        {providers.map((provider) => (
          <Button
            key={provider.id}
            variant="outlined"
            fullWidth
            disabled={loading}
            onClick={() => onSso(provider.id)}
            startIcon={loading ? <CircularProgress size={16} color="inherit" /> : undefined}
          >
            {t("auth.sso.continueWith")} {provider.displayName}
          </Button>
        ))}
        {samlDisplayName ? (
          <Button variant="outlined" fullWidth onClick={onSaml}>
            {t("auth.sso.continueWith")} {samlDisplayName}
          </Button>
        ) : null}
      </Stack>
    </Box>
  );
}
