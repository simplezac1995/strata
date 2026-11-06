// SPDX-License-Identifier: Apache-2.0
import { useEffect, useState } from "react";
import { Link as RouterLink, useNavigate } from "react-router-dom";
import { Box, Card, CircularProgress, Link, Stack, Typography } from "@mui/material";
import { useAuthStore } from "@/shared/auth/store";
import { completeSso, ssoErrorMessage, takeSsoProvider, takeSsoState } from "@/shared/auth/oidc";
import { useI18n } from "@/shared/i18n";

/**
 * Landing page for the OIDC provider redirect: validates the echoed state against the one stored when the flow
 * started, exchanges the authorization code on the server, stores the framework session and enters the console.
 */
export function SsoCallback() {
  const navigate = useNavigate();
  const loginWithToken = useAuthStore((state) => state.loginWithToken);
  const { t } = useI18n();
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const providerError = params.get("error");
    if (providerError) {
      setError(params.get("error_description") || providerError);
      return;
    }
    const code = params.get("code");
    const state = params.get("state");
    const provider = takeSsoProvider();
    const expectedState = takeSsoState();
    if (!code || !state || !provider) {
      setError(t("auth.sso.missingState"));
      return;
    }
    if (expectedState && expectedState !== state) {
      setError(t("auth.sso.failed"));
      return;
    }
    completeSso(provider, code, state)
      .then((token) => {
        loginWithToken(token);
        navigate("/dashboard", { replace: true });
      })
      .catch((err: unknown) => setError(ssoErrorMessage(err) ?? t("auth.sso.failed")));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <Box sx={{ minHeight: "100vh", display: "grid", placeItems: "center", p: 3 }}>
      <Card sx={{ p: 4, maxWidth: 420, width: "100%", textAlign: "center" }}>
        {error ? (
          <Stack spacing={2} sx={{ alignItems: "center" }}>
            <Typography variant="h6">{t("auth.sso.failed")}</Typography>
            <Typography variant="body2" color="text.secondary">
              {error}
            </Typography>
            <Link component={RouterLink} to="/login" underline="hover" sx={{ fontWeight: 600 }}>
              {t("auth.sso.backToLogin")}
            </Link>
          </Stack>
        ) : (
          <Stack spacing={2} sx={{ alignItems: "center" }}>
            <CircularProgress size={32} />
            <Typography variant="body2" color="text.secondary">
              {t("auth.sso.processing")}
            </Typography>
          </Stack>
        )}
      </Card>
    </Box>
  );
}

export default SsoCallback;
