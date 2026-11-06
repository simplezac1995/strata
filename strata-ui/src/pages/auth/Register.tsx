// SPDX-License-Identifier: Apache-2.0
import { useState } from "react";
import { Link as RouterLink, useNavigate } from "react-router-dom";
import {
  Avatar,
  Box,
  CircularProgress,
  Checkbox as MuiCheckbox,
  FormControlLabel,
  IconButton,
  InputAdornment,
  Link,
  Stack,
  Step,
  StepLabel,
  Stepper,
  Typography,
} from "@mui/material";
import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import ArrowForwardIcon from "@mui/icons-material/ArrowForward";
import BusinessOutlinedIcon from "@mui/icons-material/BusinessOutlined";
import CheckCircleOutlineIcon from "@mui/icons-material/CheckCircleOutlined";
import LockOutlinedIcon from "@mui/icons-material/LockOutlined";
import PersonOutlineIcon from "@mui/icons-material/PersonOutlined";
import VisibilityOffOutlinedIcon from "@mui/icons-material/VisibilityOffOutlined";
import VisibilityOutlinedIcon from "@mui/icons-material/VisibilityOutlined";
import { authApi } from "@/shared/api/auth";
import { ApiError } from "@/shared/api/client";
import { toast } from "@/shared/pageKit/Toast";
import { useI18n } from "@/shared/i18n";
import { Button, TextField } from "@/components/ui/index";
import { AuthFormCard, AuthLayout } from "@/pages/auth/AuthLayout";
import { authIconTileSx, authSuccessIconSx } from "@/theme/pageStyles";

const STEPS = ["nav.workspace", "nav.account", "auth.done"] as const;
const STEP_HINTS = [
  "auth.workspaceNameSubdomain",
  "auth.yourAccountDetails",
  "auth.allSet",
] as const;

const STEP_ICONS = [BusinessOutlinedIcon, PersonOutlineIcon, LockOutlinedIcon];

function HeroStepItem({
  stepIndex,
  currentStep,
  labelKey,
  hintKey,
}: {
  stepIndex: number;
  currentStep: number;
  labelKey: string;
  hintKey: string;
}) {
  const { t } = useI18n();
  const Icon = STEP_ICONS[stepIndex];
  const done = stepIndex < currentStep;
  const active = stepIndex <= currentStep;

  return (
    <Stack
      direction="row"
      spacing={2}
      sx={{
        alignItems: "center",
        opacity: stepIndex > currentStep ? 0.4 : 1,
        transition: "opacity 0.2s",
      }}
    >
      <Avatar
        sx={{
          width: 40,
          height: 40,
          borderRadius: 2,
          bgcolor: active ? "rgba(255,255,255,0.2)" : "rgba(255,255,255,0.1)",
          border: "1px solid",
          borderColor: active ? "rgba(255,255,255,0.3)" : "rgba(255,255,255,0.15)",
        }}
      >
        {done ? (
          <CheckCircleOutlineIcon sx={{ fontSize: 16, color: "#fff" }} />
        ) : (
          <Icon sx={{ fontSize: 16, color: "rgba(255,255,255,0.8)" }} />
        )}
      </Avatar>
      <Box>
        <Typography variant="body2" sx={{ color: "#fff", fontWeight: 500 }}>
          {t(labelKey)}
        </Typography>
        <Typography variant="caption" sx={{ color: "rgba(255,255,255,0.55)", mt: 0.25 }}>
          {t(hintKey)}
        </Typography>
      </Box>
    </Stack>
  );
}

export function Register() {
  const navigate = useNavigate();
  const { t } = useI18n();
  const [step, setStep] = useState(0);
  const [company, setCompany] = useState("");
  const [slug, setSlug] = useState("");
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPw, setShowPw] = useState(false);
  const [agreed, setAgreed] = useState(false);
  const [loading, setLoading] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});

  const autoSlug = (v: string) =>
    v
      .toLowerCase()
      .replace(/\s+/g, "-")
      .replace(/[^a-z0-9-]/g, "");

  const validateStep0 = () => {
    const e: Record<string, string> = {};
    if (!company.trim()) e.company = t("error.workspace.name.is.required");
    if (!slug.trim()) e.slug = t("error.subdomain.is.required");
    else if (!/^[a-z0-9-]+$/.test(slug)) e.slug = t("auth.register.slugRule");
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const validateStep1 = () => {
    const e: Record<string, string> = {};
    if (!name.trim()) e.name = t("error.display.name.is.required");
    if (!email.trim()) e.email = t("error.email.is.required");
    else if (!/\S+@\S+\.\S+/.test(email)) e.email = t("auth.register.invalidEmail");
    if (password.length < 6) e.password = t("auth.register.passwordMin");
    if (!agreed) e.agreed = t("auth.register.acceptTerms");
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const handleNext = async () => {
    if (step === 0) {
      if (!validateStep0()) return;
      setStep(1);
      return;
    }
    if (step === 1) {
      if (!validateStep1()) return;
      setLoading(true);
      try {
        await authApi.register({
          username: email,
          nickname: name || null,
          email,
          password,
          workspace: company,
          slug,
        });
        setStep(2);
      } catch (err) {
        if (err instanceof ApiError) toast.error(err.message);
      } finally {
        setLoading(false);
      }
    }
  };

  return (
    <AuthLayout
      heroTitle={
        <>
          {t("auth.register.hero.line1")}
          <br />
          {t("auth.register.hero.line2")}
        </>
      }
      heroSubtitle={t("error.set.up.your.team.s.admin.workspace.in.under.2.mi")}
      heroBullets={STEPS.map((s, i) => (
        <HeroStepItem
          key={s}
          stepIndex={i}
          currentStep={step}
          labelKey={s}
          hintKey={STEP_HINTS[i]}
        />
      ))}
    >
      <Box sx={{ maxWidth: step < 2 ? 400 : 380, mx: "auto", width: "100%" }}>
        {step < 2 ? (
          <Stepper activeStep={step} alternativeLabel sx={{ mb: 3 }}>
            {STEPS.slice(0, 2).map((labelKey) => (
              <Step key={labelKey}>
                <StepLabel>{t(labelKey)}</StepLabel>
              </Step>
            ))}
          </Stepper>
        ) : null}

        {step === 0 ? (
          <AuthFormCard>
            <Box sx={authIconTileSx}>
              <BusinessOutlinedIcon />
            </Box>
            <Typography variant="h5" sx={{ fontWeight: 700 }}>
              {t("nav.workspace")}
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
              {t("auth.register.step1Hint")}
            </Typography>
            <Stack spacing={2}>
              <TextField
                label={t("auth.register.companyName")}
                placeholder={t("auth.register.companyPlaceholder")}
                value={company}
                error={errors.company}
                onChange={(e) => {
                  setCompany(e.target.value);
                  setSlug(autoSlug(e.target.value));
                }}
              />
              <TextField
                label={t("auth.register.subdomain")}
                placeholder={t("auth.register.subdomainPlaceholder")}
                value={slug}
                error={errors.slug}
                onChange={(e) => setSlug(autoSlug(e.target.value))}
                slotProps={{
                  input: {
                    endAdornment: (
                      <InputAdornment position="end">
                        <Typography
                          variant="caption"
                          color="text.secondary"
                          sx={{ whiteSpace: "nowrap" }}
                        >
                          .strata.io
                        </Typography>
                      </InputAdornment>
                    ),
                  },
                }}
              />
              <Button
                variant="contained"
                fullWidth
                onClick={handleNext}
                endIcon={<ArrowForwardIcon sx={{ fontSize: 16 }} />}
              >
                {t("auth.common.continue")}
              </Button>
            </Stack>
            <Typography
              variant="caption"
              color="text.secondary"
              sx={{ mt: 2.5, textAlign: "center", display: "block" }}
            >
              {t("auth.register.hasAccount")}{" "}
              <Link component={RouterLink} to="/login" underline="hover" sx={{ fontWeight: 600 }}>
                {t("auth.signIn")}
              </Link>
            </Typography>
          </AuthFormCard>
        ) : null}

        {step === 1 ? (
          <AuthFormCard>
            <Box sx={authIconTileSx}>
              <PersonOutlineIcon />
            </Box>
            <Typography variant="h5" sx={{ fontWeight: 700 }}>
              {t("auth.register.step2Title")}
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
              {t("auth.register.step2Hint")}
            </Typography>
            <Stack spacing={2}>
              <TextField
                label={t("auth.register.displayName")}
                placeholder={t("auth.register.namePlaceholder")}
                value={name}
                error={errors.name}
                onChange={(e) => setName(e.target.value)}
              />
              <TextField
                type="email"
                label={t("auth.register.workEmail")}
                placeholder={t("auth.common.emailPlaceholder")}
                value={email}
                error={errors.email}
                onChange={(e) => setEmail(e.target.value)}
              />
              <TextField
                label={t("common.password")}
                type={showPw ? "text" : "password"}
                placeholder={t("auth.register.passwordPlaceholder")}
                value={password}
                error={errors.password}
                onChange={(e) => setPassword(e.target.value)}
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
              <Box>
                <FormControlLabel
                  control={
                    <MuiCheckbox size="small" checked={agreed} onChange={(_, v) => setAgreed(v)} />
                  }
                  label={
                    <Typography variant="body2" color="text.secondary">
                      {t("auth.register.agreePrefix")}{" "}
                      <Link component="button" type="button" underline="hover">
                        {t("auth.register.termsOfService")}
                      </Link>{" "}
                      {t("auth.common.and")}{" "}
                      <Link component="button" type="button" underline="hover">
                        {t("auth.register.privacyPolicy")}
                      </Link>
                    </Typography>
                  }
                />
                {errors.agreed ? (
                  <Typography variant="caption" color="error.main">
                    {errors.agreed}
                  </Typography>
                ) : null}
              </Box>
              <Stack direction="row" spacing={1}>
                <Button variant="outlined" onClick={() => setStep(0)} sx={{ minWidth: 48, px: 2 }}>
                  <ArrowBackIcon sx={{ fontSize: 16 }} />
                </Button>
                <Button
                  variant="contained"
                  fullWidth
                  disabled={loading}
                  onClick={handleNext}
                  endIcon={
                    loading ? (
                      <CircularProgress size={16} color="inherit" />
                    ) : (
                      <ArrowForwardIcon sx={{ fontSize: 16 }} />
                    )
                  }
                >
                  {loading ? t("auth.register.creating") : t("auth.register.createWorkspace")}
                </Button>
              </Stack>
            </Stack>
          </AuthFormCard>
        ) : null}

        {step === 2 ? (
          <AuthFormCard align="center">
            <Box sx={(theme) => authSuccessIconSx(theme)}>
              <CheckCircleOutlineIcon sx={{ fontSize: 32, color: "success.main" }} />
            </Box>
            <Typography variant="h5" sx={{ fontWeight: 700 }}>
              {t("auth.register.createdTitle")}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              {t("auth.register.welcome")}{" "}
              <Box component="span" sx={{ fontWeight: 500, color: "text.primary" }}>
                {name}
              </Box>
            </Typography>
            <Typography
              variant="caption"
              color="text.disabled"
              sx={{ mt: 0.5, mb: 3, textAlign: "center", display: "block" }}
            >
              {t("auth.register.yourWorkspace")}{" "}
              <Box
                component="code"
                sx={{
                  fontFamily: "monospace",
                  bgcolor: "action.hover",
                  px: 0.75,
                  py: 0.25,
                  borderRadius: 0.5,
                }}
              >
                {slug}.strata.io
              </Box>
            </Typography>
            <Button
              variant="contained"
              fullWidth
              onClick={() => navigate("/login")}
              endIcon={<ArrowForwardIcon sx={{ fontSize: 16 }} />}
            >
              {t("auth.signIn")}
            </Button>
          </AuthFormCard>
        ) : null}
      </Box>
    </AuthLayout>
  );
}

export default Register;
