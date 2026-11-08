// SPDX-License-Identifier: Apache-2.0
import { useState, useRef, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { Avatar, Box, Paper, Stack, TextField as MuiTextField, Typography } from "@mui/material";
import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import RefreshIcon from "@mui/icons-material/Refresh";
import VerifiedUserOutlinedIcon from "@mui/icons-material/VerifiedUserOutlined";
import { authApi } from "@/shared/api/auth";
import { ApiError } from "@/shared/api/client";
import { useI18n } from "@/shared/i18n";
import { toast } from "@/shared/pageKit/Toast";
import { Button } from "@/components/ui/index";
import { AuthFormCard, AuthLayout } from "@/pages/auth/AuthLayout";
import {
  AuthFormError,
  AuthFormHeading,
  AuthFormStack,
  AuthSubmitButton,
} from "@/pages/auth/AuthFormFields";

export function VerifyEmail() {
  const navigate = useNavigate();
  const { t, tError } = useI18n();
  const [otp, setOtp] = useState(["", "", "", "", "", ""]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [countdown, setCountdown] = useState(60);
  const [resending, setResending] = useState(false);
  const inputs = useRef<(HTMLInputElement | null)[]>([]);

  useEffect(() => {
    if (countdown > 0) {
      const timer = setTimeout(() => setCountdown((c) => c - 1), 1000);
      return () => clearTimeout(timer);
    }
  }, [countdown]);

  const handleChange = (i: number, val: string) => {
    if (!/^\d?$/.test(val)) return;
    const next = [...otp];
    next[i] = val;
    setOtp(next);
    setError("");
    if (val && i < 5) inputs.current[i + 1]?.focus();
  };

  const handleKeyDown = (i: number, e: React.KeyboardEvent) => {
    if (e.key === "Backspace" && !otp[i] && i > 0) {
      inputs.current[i - 1]?.focus();
    }
  };

  const handlePaste = (e: React.ClipboardEvent) => {
    e.preventDefault();
    const digits = e.clipboardData.getData("text").replace(/\D/g, "").slice(0, 6).split("");
    const next = [...otp];
    digits.forEach((d, i) => {
      next[i] = d;
    });
    setOtp(next);
    inputs.current[Math.min(digits.length, 5)]?.focus();
  };

  const handleVerify = async () => {
    const code = otp.join("");
    if (code.length < 6) {
      setError(t("auth.verify.codeRequired"));
      return;
    }
    setLoading(true);
    setError("");
    try {
      await authApi.verifyEmail(code);
      toast.success("toast.emailVerified");
      navigate("/login");
    } catch (err) {
      const message = err instanceof ApiError ? err.message : t("auth.verify.invalidCode");
      setError(tError(message));
    } finally {
      setLoading(false);
    }
  };

  const handleResend = () => {
    setResending(true);
    setCountdown(60);
    setOtp(["", "", "", "", "", ""]);
    setResending(false);
    inputs.current[0]?.focus();
  };

  return (
    <AuthLayout
      heroTitle={
        <>
          {t("auth.verify.hero.line1")}
          <br />
          {t("auth.verify.hero.line2")}
        </>
      }
      heroSubtitle={t("auth.verify.hero.subtitle")}
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
          <VerifiedUserOutlinedIcon sx={{ fontSize: 28, color: "rgba(255,255,255,0.8)" }} />
        </Avatar>
      }
      heroBullets={[
        <Paper
          key="demo-hint"
          elevation={0}
          sx={{
            p: 2,
            borderRadius: 2,
            bgcolor: "rgba(255,255,255,0.1)",
            border: "1px solid rgba(255,255,255,0.15)",
          }}
        >
          <Typography
            variant="caption"
            sx={{
              color: "rgba(255,255,255,0.5)",
              fontWeight: 600,
              textTransform: "uppercase",
              letterSpacing: 1,
              display: "block",
              mb: 0.5,
            }}
          >
            {t("auth.verify.demoHint")}
          </Typography>
          <Typography
            sx={{
              color: "#fff",
              fontFamily: "monospace",
              fontSize: 18,
              fontWeight: 700,
              letterSpacing: "0.3em",
            }}
          >
            1 2 3 4 5 6
          </Typography>
        </Paper>,
      ]}
    >
      <AuthFormCard align="center">
        <AuthFormStack>
          <AuthFormHeading
            align="center"
            icon={<VerifiedUserOutlinedIcon />}
            title={t("auth.verify.title")}
            subtitle={t("auth.verify.codeHint")}
          />

          <Stack direction="row" spacing={1} sx={{ justifyContent: "center" }}>
            {otp.map((val, i) => (
              <MuiTextField
                key={i}
                inputRef={(el) => {
                  inputs.current[i] = el;
                }}
                value={val}
                onChange={(e) => handleChange(i, e.target.value)}
                onKeyDown={(e) => handleKeyDown(i, e)}
                onPaste={handlePaste}
                slotProps={{
                  htmlInput: {
                    maxLength: 1,
                    inputMode: "numeric",
                    style: {
                      textAlign: "center",
                      fontSize: 20,
                      fontWeight: 600,
                      padding: "12px 0",
                    },
                  },
                }}
                sx={{
                  width: { xs: 40, sm: 48 },
                  "& .MuiOutlinedInput-root": {
                    bgcolor: val ? "primary.light" : "action.hover",
                    "& fieldset": {
                      borderColor: val ? "primary.main" : "divider",
                    },
                  },
                }}
              />
            ))}
          </Stack>

          {error ? <AuthFormError message={error} /> : null}

          <AuthSubmitButton
            loading={loading}
            loadingLabel={t("auth.verify.verifying")}
            disabled={otp.join("").length < 6}
            onClick={handleVerify}
          >
            {t("auth.verify.title")}
          </AuthSubmitButton>

          <Box sx={{ textAlign: "center" }}>
            {countdown > 0 ? (
              <Typography variant="body2" color="text.secondary">
                {t("auth.verify.resendIn")}{" "}
                <Box component="span" sx={{ fontWeight: 600, color: "text.primary" }}>
                  {countdown}s
                </Box>
              </Typography>
            ) : (
              <Button
                variant="text"
                onClick={handleResend}
                disabled={resending}
                startIcon={
                  <RefreshIcon
                    sx={{
                      fontSize: 14,
                      animation: resending ? "spin 1s linear infinite" : "none",
                      "@keyframes spin": { "100%": { transform: "rotate(360deg)" } },
                    }}
                  />
                }
              >
                {resending ? t("auth.common.sending") : t("auth.verify.resendCode")}
              </Button>
            )}
          </Box>

          <Button
            variant="text"
            fullWidth
            onClick={() => navigate("/login")}
            startIcon={<ArrowBackIcon sx={{ fontSize: 14 }} />}
            sx={{ color: "text.secondary" }}
          >
            {t("auth.backToSignIn")}
          </Button>
        </AuthFormStack>
      </AuthFormCard>
    </AuthLayout>
  );
}

export default VerifyEmail;
