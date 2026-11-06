// SPDX-License-Identifier: Apache-2.0
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Avatar, Box, IconButton, InputAdornment, Stack, Typography } from "@mui/material";
import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import CheckCircleOutlineIcon from "@mui/icons-material/CheckCircleOutlined";
import KeyOutlinedIcon from "@mui/icons-material/KeyOutlined";
import VisibilityOffOutlinedIcon from "@mui/icons-material/VisibilityOffOutlined";
import VisibilityOutlinedIcon from "@mui/icons-material/VisibilityOutlined";
import { authApi } from "@/shared/api/auth";
import { ApiError } from "@/shared/api/client";
import { toast } from "@/shared/pageKit/Toast";
import { useI18n } from "@/shared/i18n";
import { Button, TextField } from "@/components/ui/index";
import { AuthFormCard, AuthLayout } from "@/pages/auth/AuthLayout";
import {
  AuthFormErrorSlot,
  AuthFormHeading,
  AuthFormStack,
  AuthSubmitButton,
} from "@/pages/auth/AuthFormFields";

const HERO_BULLETS = [
  "auth.atLeast8Characters",
  "auth.mixOfUpperLowercase",
  "auth.atLeastOneNumberOrSymbol",
] as const;

const STRENGTH_COLORS = ["", "error.main", "warning.main", "success.main", "success.dark"] as const;

function HeroBullet({ text }: { text: string }) {
  return (
    <Stack direction="row" spacing={1.5} sx={{ alignItems: "center" }}>
      <CheckCircleOutlineIcon
        sx={{ fontSize: 15, color: "rgba(255,255,255,0.5)", flexShrink: 0 }}
      />
      <Typography variant="body2" sx={{ color: "rgba(255,255,255,0.65)" }}>
        {text}
      </Typography>
    </Stack>
  );
}

function StrengthBar({ password }: { password: string }) {
  const { t } = useI18n();
  const checks = [
    password.length >= 8,
    /[A-Z]/.test(password),
    /[0-9]/.test(password),
    /[^A-Za-z0-9]/.test(password),
  ];
  const score = checks.filter(Boolean).length;
  const labels = [
    "",
    t("auth.passwordStrength.weak"),
    t("auth.passwordStrength.fair"),
    t("auth.passwordStrength.strong"),
    t("auth.passwordStrength.veryStrong"),
  ];

  if (!password) return null;

  return (
    <Box sx={{ mt: 1 }}>
      <Stack direction="row" spacing={0.5} sx={{ mb: 0.5 }}>
        {[1, 2, 3, 4].map((i) => (
          <Box
            key={i}
            sx={{
              flex: 1,
              height: 4,
              borderRadius: 1,
              bgcolor: i <= score ? STRENGTH_COLORS[score] : "action.selected",
              transition: "background-color 0.2s",
            }}
          />
        ))}
      </Stack>
      <Typography variant="caption" sx={{ fontWeight: 600, color: STRENGTH_COLORS[score] }}>
        {labels[score]}
      </Typography>
    </Box>
  );
}

export function ResetPassword() {
  const navigate = useNavigate();
  const { t } = useI18n();
  const [token, setToken] = useState("");
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [showPw, setShowPw] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const handleSubmit = async () => {
    if (!token) {
      setError(t("auth.reset.tokenRequired"));
      return;
    }
    if (!password || !confirm) {
      setError(t("auth.reset.allFieldsRequired"));
      return;
    }
    if (password.length < 6) {
      setError(t("error.password.must.be.at.least.6.characters"));
      return;
    }
    if (password !== confirm) {
      setError(t("error.passwords.do.not.match"));
      return;
    }
    setLoading(true);
    setError("");
    try {
      await authApi.resetPassword(token, password);
      toast.success("toast.passwordReset");
      navigate("/login");
    } catch (err) {
      if (err instanceof ApiError) toast.error(err.message);
    } finally {
      setLoading(false);
    }
  };

  const confirmMismatch = Boolean(confirm && confirm !== password);

  return (
    <AuthLayout
      heroTitle={
        <>
          {t("auth.reset.hero.line1")}
          <br />
          {t("auth.reset.hero.line2")}
        </>
      }
      heroSubtitle={t("auth.reset.hero.hint")}
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
          <KeyOutlinedIcon sx={{ fontSize: 28, color: "rgba(255,255,255,0.8)" }} />
        </Avatar>
      }
      heroBullets={HERO_BULLETS.map((key) => (
        <HeroBullet key={key} text={t(key)} />
      ))}
    >
      <AuthFormCard>
        <AuthFormStack>
          <AuthFormHeading
            icon={<KeyOutlinedIcon />}
            title={t("auth.reset.newPassword")}
            subtitle={t("auth.reset.formHint")}
          />

          <AuthFormErrorSlot message={error} />

          <TextField
            label={t("auth.reset.token")}
            placeholder={t("auth.reset.tokenPlaceholder")}
            value={token}
            onChange={(e) => {
              setToken(e.target.value);
              setError("");
            }}
          />

          <Box>
            <TextField
              label={t("auth.reset.newPassword")}
              type={showPw ? "text" : "password"}
              placeholder="•••••••"
              value={password}
              onChange={(e) => {
                setPassword(e.target.value);
                setError("");
              }}
              slotProps={{
                input: {
                  endAdornment: (
                    <InputAdornment position="end">
                      <IconButton
                        size="small"
                        edge="end"
                        aria-label={showPw ? t("common.hidePassword") : t("common.showPassword")}
                        onClick={() => setShowPw((v) => !v)}
                      >
                        {showPw ? (
                          <VisibilityOffOutlinedIcon fontSize="small" />
                        ) : (
                          <VisibilityOutlinedIcon fontSize="small" />
                        )}
                      </IconButton>
                    </InputAdornment>
                  ),
                },
              }}
            />
            <StrengthBar password={password} />
          </Box>

          <TextField
            label={t("auth.reset.confirmPassword")}
            type={showConfirm ? "text" : "password"}
            placeholder="•••••••"
            value={confirm}
            error={confirmMismatch ? t("error.passwords.don.t.match") : undefined}
            onChange={(e) => setConfirm(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && handleSubmit()}
            slotProps={{
              input: {
                endAdornment: (
                  <InputAdornment position="end">
                    <IconButton
                      size="small"
                      edge="end"
                      aria-label={showConfirm ? t("common.hidePassword") : t("common.showPassword")}
                      onClick={() => setShowConfirm((v) => !v)}
                    >
                      {showConfirm ? (
                        <VisibilityOffOutlinedIcon fontSize="small" />
                      ) : (
                        <VisibilityOutlinedIcon fontSize="small" />
                      )}
                    </IconButton>
                  </InputAdornment>
                ),
              },
            }}
          />

          <AuthSubmitButton
            loading={loading}
            loadingLabel={t("auth.reset.updating")}
            onClick={handleSubmit}
          >
            {t("auth.reset.submit")}
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
    </AuthLayout>
  );
}

export default ResetPassword;
