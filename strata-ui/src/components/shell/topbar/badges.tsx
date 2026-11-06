// SPDX-License-Identifier: Apache-2.0
// Small status badges: tenant plan and workspace connection state.

import { Button, Chip } from "@mui/material";
import { useI18n } from "@/shared/i18n";
import type { Tenant } from "@/context/AppContext";
import { useWebSocketStatus } from "@/shared/websocket";

export function TenantBadge({ plan }: { plan: Tenant["plan"] }) {
  const color = plan === "pro" ? "info" : plan === "enterprise" ? "secondary" : "default";
  return (
    <Chip
      label={plan}
      size="small"
      color={color}
      variant="outlined"
      sx={{ height: 18, fontSize: 9 }}
    />
  );
}

/**
 * Realtime-channel liveness indicator. Only surfaces when the socket is visibly
 * unhealthy so the admin knows notifications are stale: amber while the client is
 * auto-reconnecting, red when it has given up (max attempts) — with a manual retry.
 * Healthy / connecting / idle states render nothing to avoid noise.
 */
export function WsStatusBadge() {
  const { t } = useI18n();
  const { state, reconnect } = useWebSocketStatus();

  if (state === "reconnecting") {
    return (
      <Chip
        size="small"
        color="warning"
        variant="outlined"
        label={t("shell.ws.reconnecting")}
        title={t("shell.ws.reconnecting")}
        sx={{ height: 22, fontSize: 11 }}
      />
    );
  }
  if (state === "error") {
    return (
      <Button
        size="small"
        color="error"
        variant="outlined"
        onClick={reconnect}
        title={t("shell.ws.offline")}
        sx={{ height: 22, fontSize: 11, px: 1, borderRadius: 1 }}
      >
        {t("shell.ws.offline")} · {t("shell.ws.reconnect")}
      </Button>
    );
  }
  return null;
}
