// SPDX-License-Identifier: Apache-2.0
import { useState } from "react";
import { Avatar, Box, IconButton, InputAdornment, Paper, Stack, Typography } from "@mui/material";
import { keyframes } from "@mui/material/styles";
import LockOutlinedIcon from "@mui/icons-material/LockOutlined";
import LockOpenOutlinedIcon from "@mui/icons-material/LockOpenOutlined";
import VisibilityOffOutlinedIcon from "@mui/icons-material/VisibilityOffOutlined";
import VisibilityOutlinedIcon from "@mui/icons-material/VisibilityOutlined";
import { useApp } from "@/context/AppContext";
import { useI18n } from "@/shared/i18n";
import { Button, TextField } from "@/components/ui/index";

const shake = keyframes`
  0%, 100% { transform: translateX(0); }
  20% { transform: translateX(-6px); }
  40% { transform: translateX(6px); }
  60% { transform: translateX(-4px); }
  80% { transform: translateX(4px); }
`;

export function LockScreen() {
  const { t } = useI18n();
  const { setLocked } = useApp();
  const [password, setPassword] = useState("");
  const [showPw, setShowPw] = useState(false);
  const [shaking, setShaking] = useState(false);

  const tryUnlock = () => {
    if (!password) return;
    if (password === "wrong") {
      setShaking(true);
      setTimeout(() => setShaking(false), 500);
      return;
    }
    setLocked(false);
  };

  return (
    <Box
      sx={{
        position: "fixed",
        inset: 0,
        zIndex: 100,
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        justifyContent: "center",
        backdropFilter: "blur(24px) saturate(140%)",
        bgcolor: "rgba(8, 12, 20, 0.72)",
      }}
    >
      <Typography
        variant="caption"
        sx={{
          color: "rgba(255,255,255,0.3)",
          fontWeight: 500,
          letterSpacing: "0.2em",
          textTransform: "uppercase",
          mb: 4,
          userSelect: "none",
        }}
      >
        {t("auth.lock.screen.locked")}
      </Typography>

      <Avatar
        sx={{
          width: 72,
          height: 72,
          mb: 2,
          fontSize: 24,
          fontWeight: 600,
          background: (theme) =>
            `linear-gradient(135deg, ${theme.palette.primary.main}, ${theme.palette.primary.light})`,
        }}
      >
        JD
      </Avatar>
      <Typography sx={{ color: "#fff", fontSize: 15, fontWeight: 600, mb: 0.5 }}>
        {t("auth.lock.demo.user")}
      </Typography>
      <Typography variant="caption" sx={{ color: "rgba(255,255,255,0.4)", mb: 4 }}>
        {t("auth.lock.demo.role")}
      </Typography>

      <Paper
        elevation={0}
        sx={{
          width: "100%",
          maxWidth: 320,
          p: 3,
          borderRadius: 3,
          bgcolor: "rgba(255,255,255,0.06)",
          backdropFilter: "blur(12px)",
          border: "1px solid rgba(255,255,255,0.1)",
          animation: shaking ? `${shake} 0.3s ease` : "none",
        }}
      >
        <Stack direction="row" spacing={1} sx={{ alignItems: "center", mb: 2 }}>
          <LockOutlinedIcon sx={{ fontSize: 14, color: "rgba(255,255,255,0.4)" }} />
          <Typography variant="caption" sx={{ color: "rgba(255,255,255,0.4)" }}>
            {t("auth.lock.enter.password")}
          </Typography>
        </Stack>

        <TextField
          type={showPw ? "text" : "password"}
          placeholder={t("common.password")}
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && tryUnlock()}
          autoFocus
          slotProps={{
            input: {
              endAdornment: (
                <InputAdornment position="end">
                  <IconButton
                    size="small"
                    edge="end"
                    aria-label={showPw ? t("common.hidePassword") : t("common.showPassword")}
                    onClick={() => setShowPw((v) => !v)}
                    sx={{
                      color: "rgba(255,255,255,0.3)",
                      "&:hover": { color: "rgba(255,255,255,0.6)" },
                    }}
                  >
                    {showPw ? (
                      <VisibilityOffOutlinedIcon fontSize="small" />
                    ) : (
                      <VisibilityOutlinedIcon fontSize="small" />
                    )}
                  </IconButton>
                </InputAdornment>
              ),
              sx: {
                bgcolor: "rgba(255,255,255,0.08)",
                borderRadius: 2,
                color: "#fff",
                "& fieldset": { borderColor: "rgba(255,255,255,0.1)" },
                "& input::placeholder": { color: "rgba(255,255,255,0.2)", opacity: 1 },
              },
            },
          }}
          sx={{ mb: 2 }}
        />

        <Button
          variant="contained"
          fullWidth
          onClick={tryUnlock}
          startIcon={<LockOpenOutlinedIcon sx={{ fontSize: 16 }} />}
        >
          {t("auth.lock.unlock")}
        </Button>
      </Paper>

      <Typography variant="caption" sx={{ color: "rgba(255,255,255,0.2)", mt: 2.5 }}>
        {t("auth.lock.auto.inactive")}
      </Typography>
    </Box>
  );
}

export default LockScreen;
