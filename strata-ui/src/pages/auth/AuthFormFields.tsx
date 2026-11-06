// SPDX-License-Identifier: Apache-2.0
import type { ReactNode } from "react";
import { Box, CircularProgress, Stack, Typography } from "@mui/material";
import type { SxProps, Theme } from "@mui/material/styles";
import { Alert, Button } from "@/components/ui/index";
import { authIconTileSx } from "@/theme/pageStyles";

/** Consistent vertical rhythm inside auth cards. */
export function AuthFormStack({ children }: { children: ReactNode }) {
  return <Stack spacing={2.5}>{children}</Stack>;
}

export function AuthFormHeading({
  icon,
  title,
  subtitle,
  align = "left",
}: {
  icon?: ReactNode;
  title: ReactNode;
  subtitle?: ReactNode;
  align?: "left" | "center";
}) {
  const centered = align === "center";
  return (
    <Stack
      spacing={1.5}
      sx={{
        textAlign: align,
        alignItems: centered ? "center" : "stretch",
      }}
    >
      {icon ? (
        <Box
          sx={{
            ...authIconTileSx,
            mb: 0,
            ...(centered ? { mx: "auto" } : {}),
          }}
        >
          {icon}
        </Box>
      ) : null}
      <Box>
        <Typography variant="h5" component="h2" sx={{ fontWeight: 700, lineHeight: 1.3 }}>
          {title}
        </Typography>
        {subtitle ? (
          <Typography variant="body2" color="text.secondary" sx={{ mt: 0.75, lineHeight: 1.5 }}>
            {subtitle}
          </Typography>
        ) : null}
      </Box>
    </Stack>
  );
}

/** Inline form-level error (API / submit). */
export function AuthFormError({ message }: { message?: string }) {
  if (!message) return null;
  return <Alert type="error">{message}</Alert>;
}

/** Reserved slot below heading — reduces layout jump when errors appear. */
export function AuthFormErrorSlot({
  message,
  reserve = false,
}: {
  message?: string;
  reserve?: boolean;
}) {
  if (!message && !reserve) return null;
  return (
    <Box sx={{ minHeight: reserve ? 40 : 0, boxSizing: "border-box" }}>
      <AuthFormError message={message} />
    </Box>
  );
}

const endIconSlotSx: SxProps<Theme> = {
  width: 20,
  height: 20,
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
};

export function AuthSubmitButton({
  loading,
  loadingLabel,
  children,
  endIcon,
  ...props
}: {
  loading?: boolean;
  loadingLabel?: string;
  children: ReactNode;
  endIcon?: ReactNode;
} & Omit<React.ComponentProps<typeof Button>, "endIcon">) {
  return (
    <Button
      variant="contained"
      fullWidth
      disabled={loading}
      endIcon={
        <Box sx={endIconSlotSx}>
          {loading ? <CircularProgress size={16} color="inherit" /> : endIcon}
        </Box>
      }
      {...props}
    >
      {loading && loadingLabel ? loadingLabel : children}
    </Button>
  );
}

export function AuthFormFooter({ children, sx }: { children: ReactNode; sx?: SxProps<Theme> }) {
  return (
    <Typography
      variant="caption"
      color="text.secondary"
      component="div"
      sx={[
        { textAlign: "center", lineHeight: 1.6 },
        ...(sx ? (Array.isArray(sx) ? sx : [sx]) : []),
      ]}
    >
      {children}
    </Typography>
  );
}
