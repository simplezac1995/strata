// SPDX-License-Identifier: Apache-2.0
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Avatar, Box, Link, Stack, Typography } from "@mui/material";
import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import CheckCircleOutlineIcon from "@mui/icons-material/CheckCircleOutlined";
import MailOutlineIcon from "@mui/icons-material/MailOutlined";
import { authApi } from "@/shared/api/auth";
import { ApiError } from "@/shared/api/client";
import { toast } from "@/shared/pageKit/Toast";
import { useI18n } from "@/shared/i18n";
import { Button, TextField } from "@/components/ui/index";
import { AuthFormCard, AuthLayout } from "@/pages/auth/AuthLayout";
import { AuthFormHeading, AuthFormStack, AuthSubmitButton } from "@/pages/auth/AuthFormFields";
import { authSuccessIconSx } from "@/theme/pageStyles";

const HERO_BULLETS = [
  "auth.secureResetLinkViaEmail",
  "auth.linkExpiresIn15Minutes",
  "auth.noAccountChangesUntilConfirmed",
] as const;

function HeroBullet({ text }: { text: string }) {
  return (
    <Stack direction="row" spacing={1.5} sx={{ alignItems: "center" }}>
      <CheckCircleOutlineIcon
        sx={{ fontSize: 16, color: "rgba(255,255,255,0.6)", flexShrink: 0 }}
      />
      <Typography variant="body2" sx={{ color: "rgba(255,255,255,0.65)" }}>
        {text}
      </Typography>
    </Stack>
  );
}

export function ForgotPassword() {
  const navigate = useNavigate();
  const { t } = useI18n();
  const [email, setEmail] = useState("");
  const [loading, setLoading] = useState(false);
  const [sent, setSent] = useState(false);
  const [fieldError, setFieldError] = useState("");

  const handleSubmit = async () => {
    if (!email) {
      setFieldError(t("auth.forgot.emailRequired"));
      return;
    }
    if (!/\S+@\S+\.\S+/.test(email)) {
      setFieldError(t("auth.forgot.invalidEmail"));
      return;
    }
    setLoading(true);
    setFieldError("");
    try {
      await authApi.forgotPassword(email);
      setSent(true);
    } catch (err) {
      if (err instanceof ApiError) toast.error(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthLayout
      heroTitle={
        <>
          {t("auth.forgot.hero.line1")}
          <br />
          {t("auth.forgot.hero.line2")}
        </>
      }
      heroSubtitle={t("auth.forgot.hero.subtitle")}
      heroIcon={
        <Avatar
          sx={{
            width: 64,
            height: 64,
            borderRadius: 2.5,
            bgcolor: "rgba(255,255,255,0.15)",
            border: "1px solid rgba(255,255,255,0.2)",
          }}
        >
          <MailOutlineIcon sx={{ fontSize: 28, color: "rgba(255,255,255,0.8)" }} />
        </Avatar>
      }
      heroBullets={HERO_BULLETS.map((key) => (
        <HeroBullet key={key} text={t(key)} />
      ))}
    >
      {!sent ? (
        <AuthFormCard>
          <AuthFormStack>
            <AuthFormHeading
              icon={<MailOutlineIcon />}
              title={t("auth.forgot.title")}
              subtitle={t("auth.forgot.formHint")}
            />

            <TextField
              type="email"
              label={t("auth.common.emailAddress")}
              placeholder={t("auth.common.emailPlaceholder")}
              value={email}
              error={fieldError}
              onChange={(e) => {
                setEmail(e.target.value);
                setFieldError("");
              }}
              onKeyDown={(e) => e.key === "Enter" && handleSubmit()}
            />

            <AuthSubmitButton
              loading={loading}
              loadingLabel={t("auth.common.sending")}
              onClick={handleSubmit}
            >
              {t("auth.forgot.sendLink")}
            </AuthSubmitButton>
          </AuthFormStack>

          <Button
            variant="text"
            fullWidth
            onClick={() => navigate("/login")}
            startIcon={<ArrowBackIcon sx={{ fontSize: 14 }} />}
            sx={{ mt: 2.5, color: "text.secondary" }}
          >
            {t("auth.backToSignIn")}
          </Button>
        </AuthFormCard>
      ) : (
        <AuthFormCard align="center">
          <AuthFormStack>
            <Box sx={(theme) => ({ ...authSuccessIconSx(theme, 56), mx: "auto" })}>
              <CheckCircleOutlineIcon sx={{ fontSize: 28, color: "success.main" }} />
            </Box>
            <AuthFormHeading
              align="center"
              title={t("auth.forgot.checkEmail")}
              subtitle={
                <>
                  {t("auth.forgot.sentTo")}
                  <br />
                  <Box component="span" sx={{ fontWeight: 500, color: "text.primary" }}>
                    {email}
                  </Box>
                </>
              }
            />
            <Typography
              variant="caption"
              color="text.disabled"
              sx={{ textAlign: "center", display: "block", lineHeight: 1.6 }}
            >
              {t("auth.forgot.devModeHint")}
            </Typography>
            <Typography
              variant="caption"
              color="text.disabled"
              sx={{ textAlign: "center", display: "block", lineHeight: 1.6 }}
            >
              {t("auth.forgot.notReceived")}{" "}
              <Link
                component="button"
                type="button"
                underline="hover"
                onClick={() => setSent(false)}
              >
                {t("auth.common.tryAgain")}
              </Link>
            </Typography>
            <Button
              variant="outlined"
              fullWidth
              onClick={() => navigate("/login")}
              startIcon={<ArrowBackIcon sx={{ fontSize: 14 }} />}
            >
              {t("auth.backToSignIn")}
            </Button>
          </AuthFormStack>
        </AuthFormCard>
      )}
    </AuthLayout>
  );
}

export default ForgotPassword;
