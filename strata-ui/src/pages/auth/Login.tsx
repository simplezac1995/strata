// SPDX-License-Identifier: Apache-2.0
import { useCallback, useState } from "react";
import { Link as RouterLink, useNavigate } from "react-router-dom";
import {
  Avatar,
  Box,
  Card,
  CardActionArea,
  Checkbox,
  Chip,
  CircularProgress,
  FormControlLabel,
  IconButton,
  InputAdornment,
  Link,
  Stack,
  Typography,
} from "@mui/material";
import ArrowForwardIcon from "@mui/icons-material/ArrowForward";
import BusinessOutlinedIcon from "@mui/icons-material/BusinessOutlined";
import ChevronRightIcon from "@mui/icons-material/ChevronRight";
import LanguageOutlinedIcon from "@mui/icons-material/LanguageOutlined";
import LockOutlinedIcon from "@mui/icons-material/LockOutlined";
import RefreshOutlinedIcon from "@mui/icons-material/RefreshOutlined";
import ShieldOutlinedIcon from "@mui/icons-material/ShieldOutlined";
import VisibilityOffOutlinedIcon from "@mui/icons-material/VisibilityOffOutlined";
import VisibilityOutlinedIcon from "@mui/icons-material/VisibilityOutlined";
import BoltOutlinedIcon from "@mui/icons-material/BoltOutlined";
import { useApp } from "@/context/AppContext";
import type { Tenant } from "@/context/AppContext";
import { Button, TextField } from "@/components/ui/index";
import { useAuthStore } from "@/shared/auth/store";
import { ApiError, apiGet } from "@/shared/api/client";
import { useI18n } from "@/shared/i18n";
import { AuthFormCard, AuthLayout } from "@/pages/auth/AuthLayout";
import {
  AuthFormErrorSlot,
  AuthFormFooter,
  AuthFormHeading,
  AuthFormStack,
  AuthSubmitButton,
} from "@/pages/auth/AuthFormFields";
import { authIconTileSx } from "@/theme/pageStyles";

const FEATURES = [
  { icon: <ShieldOutlinedIcon fontSize="small" />, textKey: "auth.login.feature.rbac" },
  { icon: <LanguageOutlinedIcon fontSize="small" />, textKey: "auth.login.feature.i18n" },
  { icon: <BoltOutlinedIcon fontSize="small" />, textKey: "auth.login.feature.ai" },
];

const HERO_TAGS = ["50+ Screens", "30+ Components", "Apache 2.0"];

function HeroBullet({ icon, text }: { icon: React.ReactNode; text: string }) {
  return (
    <Stack direction="row" spacing={1.75} sx={{ alignItems: "center" }}>
      <Box
        sx={{
          width: 32,
          height: 32,
          borderRadius: 1.5,
          bgcolor: "rgba(255,255,255,0.12)",
          border: "1px solid rgba(255,255,255,0.15)",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          color: "rgba(255,255,255,0.8)",
        }}
      >
        {icon}
      </Box>
      <Typography variant="body2" sx={{ color: "rgba(255,255,255,0.78)" }}>
        {text}
      </Typography>
    </Stack>
  );
}

export function Login() {
  const navigate = useNavigate();
  const { setCurrentTenant, tenantsEnabled, tenants } = useApp();
  const { t, tError } = useI18n();
  const [step, setStep] = useState<"tenant" | "login">(tenantsEnabled ? "tenant" : "login");
  const [selectedTenant, setSelectedTenant] = useState<Tenant | null>(null);
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [showPw, setShowPw] = useState(false);
  const [remember, setRemember] = useState(false);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [captchaId, setCaptchaId] = useState("");
  const [captchaQuestion, setCaptchaQuestion] = useState("");
  const [captchaCode, setCaptchaCode] = useState("");
  const [captchaVisible, setCaptchaVisible] = useState(false);
  const [captchaLoading, setCaptchaLoading] = useState(false);

  const loadCaptcha = useCallback(async () => {
    setCaptchaLoading(true);
    try {
      const data = await apiGet<{ captchaId: string; question: string }>(
        "/api/v1/system/auth/captcha",
      );
      setCaptchaId(data.captchaId);
      setCaptchaQuestion(data.question);
      setCaptchaCode("");
    } catch {
      // Keep the previous question if the refresh call fails; the user can retry.
    } finally {
      setCaptchaLoading(false);
    }
  }, []);

  const handleTenantSelect = (tenant: Tenant) => {
    setSelectedTenant(tenant);
    setCurrentTenant(tenant);
    setStep("login");
  };

  const handleLogin = async () => {
    if (!username || !password) {
      setError(t("auth.login.credentialsRequired"));
      return;
    }
    setLoading(true);
    setError("");
    try {
      await useAuthStore
        .getState()
        .login(
          username,
          password,
          captchaVisible ? { captchaId, captchaCode: captchaCode.trim() } : undefined,
        );
      navigate("/dashboard");
    } catch (err) {
      if (err instanceof ApiError && err.code === 40010) {
        setError(t("auth.login.captcha.required"));
        setCaptchaVisible(true);
        await loadCaptcha();
      } else if (err instanceof ApiError && err.code === 40011) {
        setError(t("auth.login.captcha.wrong"));
        setCaptchaVisible(true);
        await loadCaptcha();
      } else if (err instanceof ApiError && err.code === 40012) {
        setError(t("auth.login.captcha.expired"));
        setCaptchaVisible(true);
        await loadCaptcha();
      } else {
        const message =
          err instanceof ApiError ? err.message : t("error.login.failed.check.username.password");
        setError(tError(message));
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthLayout
      heroTitle={
        <>
          {t("auth.login.hero.titleLine1")}
          <br />
          {t("auth.login.hero.titleLine2")}
        </>
      }
      heroSubtitle={t("auth.login.hero.subtitle")}
      heroBullets={FEATURES.map((f) => (
        <HeroBullet key={f.textKey} icon={f.icon} text={t(f.textKey)} />
      ))}
      heroTags={HERO_TAGS}
    >
      {step === "tenant" ? (
        <AuthFormCard>
          <Box sx={authIconTileSx}>
            <BusinessOutlinedIcon />
          </Box>
          <Typography variant="h5" sx={{ fontWeight: 700, mb: 1 }}>
            {t("auth.login.selectWorkspace")}
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
            {t("auth.login.chooseTenant")}
          </Typography>
          <Stack spacing={1}>
            {tenants.map((tenant) => (
              <Card key={tenant.id} variant="outlined" sx={{ borderRadius: 2 }}>
                <CardActionArea
                  onClick={() => handleTenantSelect(tenant)}
                  sx={{
                    p: 1.75,
                    display: "flex",
                    alignItems: "center",
                    gap: 1.5,
                    justifyContent: "flex-start",
                  }}
                >
                  <Avatar sx={{ width: 36, height: 36, bgcolor: tenant.color, fontSize: 12 }}>
                    {tenant.avatar}
                  </Avatar>
                  <Box sx={{ flex: 1, minWidth: 0, textAlign: "left" }}>
                    <Typography variant="body2" sx={{ fontWeight: 600 }} noWrap>
                      {tenant.name}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      {tenant.slug}.strata.io
                    </Typography>
                  </Box>
                  <Chip
                    label={tenant.plan}
                    size="small"
                    color={
                      tenant.plan === "enterprise"
                        ? "secondary"
                        : tenant.plan === "pro"
                          ? "info"
                          : "default"
                    }
                    sx={{ textTransform: "uppercase", fontWeight: 600, fontSize: 10 }}
                  />
                  <ChevronRightIcon fontSize="small" color="action" />
                </CardActionArea>
              </Card>
            ))}
          </Stack>
          <Button variant="text" fullWidth onClick={() => setStep("login")} sx={{ mt: 2 }}>
            {t("auth.login.skipTenant")}
          </Button>
          <Typography
            variant="caption"
            color="text.secondary"
            sx={{ mt: 2.5, textAlign: "center", display: "block" }}
          >
            {t("auth.login.noAccount")}{" "}
            <Link component={RouterLink} to="/register" underline="hover" sx={{ fontWeight: 600 }}>
              {t("auth.login.registerCta")}
            </Link>
          </Typography>
        </AuthFormCard>
      ) : (
        <>
          <AuthFormCard>
            {selectedTenant ? (
              <Stack
                direction="row"
                spacing={1}
                sx={{
                  alignItems: "center",
                  mb: 2,
                  p: 1.25,
                  borderRadius: 1.5,
                  bgcolor: "action.hover",
                  border: 1,
                  borderColor: "divider",
                }}
              >
                <Avatar sx={{ width: 24, height: 24, bgcolor: selectedTenant.color, fontSize: 10 }}>
                  {selectedTenant.avatar}
                </Avatar>
                <Typography variant="caption" sx={{ fontWeight: 500, flex: 1 }}>
                  {selectedTenant.name}
                </Typography>
                <Button variant="text" size="sm" onClick={() => setStep("tenant")}>
                  {t("auth.login.changeTenant")}
                </Button>
              </Stack>
            ) : null}

            <AuthFormStack>
              <AuthFormHeading
                icon={<LockOutlinedIcon />}
                title={t("auth.signIn")}
                subtitle={t("auth.login.credentialsHint")}
              />

              <AuthFormErrorSlot message={error} />

              <TextField
                label={t("common.username")}
                placeholder={t("auth.login.usernamePlaceholder")}
                value={username}
                onChange={(e) => {
                  setUsername(e.target.value);
                  setError("");
                }}
                slotProps={{ htmlInput: { "data-testid": "login-username" } }}
              />

              <Box>
                <Stack direction="row" sx={{ justifyContent: "space-between", mb: 0.5 }}>
                  <Typography variant="caption" sx={{ fontWeight: 500 }} color="text.secondary">
                    {t("common.password")}
                  </Typography>
                  <Link
                    component={RouterLink}
                    to="/forgot-password"
                    variant="caption"
                    underline="hover"
                  >
                    {t("auth.login.forgotPassword")}
                  </Link>
                </Stack>
                <TextField
                  type={showPw ? "text" : "password"}
                  placeholder="•••••••"
                  value={password}
                  onChange={(e) => {
                    setPassword(e.target.value);
                    setError("");
                  }}
                  onKeyDown={(e) => e.key === "Enter" && handleLogin()}
                  slotProps={{
                    htmlInput: { "data-testid": "login-password" },
                    input: {
                      endAdornment: (
                        <InputAdornment position="end">
                          <IconButton
                            size="small"
                            edge="end"
                            onClick={() => setShowPw((v) => !v)}
                            aria-label={t(
                              showPw ? "auth.login.hidePassword" : "auth.login.showPassword",
                            )}
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
              </Box>

              {captchaVisible ? (
                <Box>
                  <Typography variant="caption" sx={{ fontWeight: 500 }} color="text.secondary">
                    {t("auth.login.captcha.label")}
                  </Typography>
                  <Stack direction="row" spacing={1.5} sx={{ alignItems: "center", mt: 0.5 }}>
                    <Box
                      component="button"
                      type="button"
                      onClick={loadCaptcha}
                      title={t("auth.login.captcha.refreshHint")}
                      sx={{
                        display: "inline-flex",
                        alignItems: "center",
                        gap: 0.75,
                        px: 1.25,
                        py: 0.75,
                        borderRadius: 1,
                        border: 1,
                        borderColor: "divider",
                        bgcolor: "action.hover",
                        fontFamily: "monospace",
                        fontSize: 14,
                        fontWeight: 600,
                        whiteSpace: "nowrap",
                        cursor: "pointer",
                      }}
                    >
                      {captchaQuestion}
                      {captchaLoading ? (
                        <CircularProgress size={14} color="inherit" />
                      ) : (
                        <RefreshOutlinedIcon fontSize="small" />
                      )}
                    </Box>
                    <TextField
                      placeholder={t("auth.login.captcha.placeholder")}
                      value={captchaCode}
                      onChange={(e) => {
                        setCaptchaCode(e.target.value);
                        setError("");
                      }}
                      onKeyDown={(e) => e.key === "Enter" && handleLogin()}
                      slotProps={{ htmlInput: { "data-testid": "login-captcha" } }}
                    />
                  </Stack>
                </Box>
              ) : null}

              <FormControlLabel
                control={
                  <Checkbox size="small" checked={remember} onChange={(_, v) => setRemember(v)} />
                }
                label={
                  <Typography variant="body2" color="text.secondary">
                    {t("auth.login.rememberMe")}
                  </Typography>
                }
              />

              <AuthSubmitButton
                loading={loading}
                loadingLabel={t("auth.signingIn")}
                data-testid="login-submit"
                onClick={handleLogin}
                endIcon={!loading ? <ArrowForwardIcon sx={{ fontSize: 20 }} /> : undefined}
              >
                {t("auth.signIn")}
              </AuthSubmitButton>
            </AuthFormStack>

            <AuthFormFooter sx={{ mt: 2.5 }}>
              {t("auth.login.noAccount")}{" "}
              <Link
                component={RouterLink}
                to="/register"
                underline="hover"
                sx={{ fontWeight: 600 }}
              >
                {t("auth.login.registerCta")}
              </Link>
            </AuthFormFooter>

            <Box sx={{ mt: 2, pt: 2, borderTop: 1, borderColor: "divider" }}>
              <AuthFormFooter>
                {t("auth.login.demoCredentials")}{" "}
                <Box
                  component="code"
                  sx={{
                    fontFamily: "monospace",
                    fontSize: 11,
                    bgcolor: "action.hover",
                    px: 0.75,
                    py: 0.25,
                    borderRadius: 0.5,
                  }}
                >
                  admin / admin123
                </Box>
              </AuthFormFooter>
            </Box>
          </AuthFormCard>

          <Typography
            variant="caption"
            color="text.disabled"
            sx={{ mt: 2.5, textAlign: "center", display: "block" }}
          >
            Strata Admin · Open Core · Apache 2.0
          </Typography>
        </>
      )}
    </AuthLayout>
  );
}

export default Login;
