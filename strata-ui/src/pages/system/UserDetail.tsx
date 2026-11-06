// SPDX-License-Identifier: Apache-2.0
import { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import PersonOutlineIcon from "@mui/icons-material/PersonOutlined";
import ShieldOutlinedIcon from "@mui/icons-material/ShieldOutlined";
import CalendarTodayOutlinedIcon from "@mui/icons-material/CalendarTodayOutlined";
import VerifiedIcon from "@mui/icons-material/Verified";
import AccessTimeOutlinedIcon from "@mui/icons-material/AccessTimeOutlined";
import LoginOutlinedIcon from "@mui/icons-material/LoginOutlined";
import LockOutlinedIcon from "@mui/icons-material/LockOutlined";
import CancelOutlinedIcon from "@mui/icons-material/CancelOutlined";
import LaptopOutlinedIcon from "@mui/icons-material/LaptopOutlined";
import {
  Avatar,
  Box,
  Checkbox,
  FormControlLabel,
  Grid,
  Stack,
  Tab,
  Tabs,
  Typography,
} from "@mui/material";
import { Button, StatusChip } from "@/components/ui/index";
import { useI18n } from "@/shared/i18n";
import { loginLogApi, roleApi, userApi, type UserUpdatePayload } from "@/shared/api/system";
import { useMutationWithToast } from "@/shared/hooks/useMutationWithToast";
import { toast } from "@/shared/pageKit/Toast";
import { ContentCard, ListRow, PageStack } from "@/shared/pageKit/PageLayout";
import type { LoginLogDto, RoleDto, UserDto } from "@/shared/types";

type Tab = "profile" | "roles" | "activity" | "sessions";

export function UserDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { t } = useI18n();
  const [tab, setTab] = useState<Tab>("profile");

  const [user, setUser] = useState<UserDto | null>(null);
  const [roles, setRoles] = useState<RoleDto[]>([]);
  const [logs, setLogs] = useState<LoginLogDto[]>([]);
  const [selectedRoleIds, setSelectedRoleIds] = useState<string[]>([]);

  const saveRolesMutation = useMutationWithToast(
    ({ userId, payload }: { userId: string; payload: UserUpdatePayload }) =>
      userApi.update(userId, payload),
    { success: "toast.saved", failure: "toast.saveFailed" },
  );

  const toggleEnabledMutation = useMutationWithToast(
    ({ userId, payload }: { userId: string; payload: UserUpdatePayload }) =>
      userApi.update(userId, payload),
    { success: "toast.saved", failure: "toast.saveFailed" },
  );

  const busy = saveRolesMutation.isPending || toggleEnabledMutation.isPending;

  useEffect(() => {
    if (!id) return;
    let cancelled = false;
    userApi
      .get(id)
      .then((u) => {
        if (cancelled) return;
        setUser(u);
        setSelectedRoleIds(u.roleIds);
      })
      .catch(() => toast.error("toast.loadFailed"));
    roleApi
      .list()
      .then((r) => {
        if (!cancelled) setRoles(r);
      })
      .catch(() => {});
    loginLogApi
      .page({ username: undefined, page: 0, size: 20 })
      .then((r) => {
        if (!cancelled) setLogs(r.content);
      })
      .catch(() => {});
    return () => {
      cancelled = true;
    };
  }, [id]);

  if (!id) return null;
  const displayName = user?.nickname?.trim() || user?.username || "—";
  const roleNames = user
    ? user.roleIds
        .map((rid) => roles.find((r) => r.id === rid)?.name)
        .filter(Boolean)
        .join(", ")
    : "—";
  const ownLogs = user ? logs.filter((l) => l.username === user.username) : [];
  const sessions = ownLogs.filter((l) => l.status === "success");
  const lastLogin = sessions[0]?.createdAt;

  async function handleSaveRoles() {
    if (!user) return;
    try {
      await saveRolesMutation.mutateAsync({
        userId: user.id,
        payload: {
          nickname: user.nickname ?? null,
          enabled: user.enabled,
          password: null,
          roleIds: selectedRoleIds,
        } satisfies UserUpdatePayload,
      });
      setUser({ ...user, roleIds: selectedRoleIds });
    } catch {
      // toast already shown
    }
  }

  async function handleToggleEnabled() {
    if (!user) return;
    const enabled = !user.enabled;
    try {
      await toggleEnabledMutation.mutateAsync({
        userId: user.id,
        payload: {
          nickname: user.nickname ?? null,
          enabled,
          password: null,
          roleIds: user.roleIds,
        } satisfies UserUpdatePayload,
      });
      setUser({ ...user, enabled });
    } catch {
      // toast already shown
    }
  }

  const TABS: { id: Tab; label: string }[] = [
    { id: "profile", label: t("nav.profile") },
    { id: "roles", label: t("system.userDetail.rolesPermissions") },
    { id: "activity", label: t("system.userDetail.activity") },
    { id: "sessions", label: t("system.userDetail.sessions") },
  ];

  return (
    <Box sx={{ maxWidth: 960, mx: "auto" }}>
      <PageStack>
        <Box sx={{ alignSelf: "flex-start", mb: -1 }}>
          <Button variant="text" size="sm" color="default" onClick={() => navigate(-1)}>
            <ArrowBackIcon sx={{ fontSize: 14 }} />
            {t("system.userDetail.backToUsers")}
          </Button>
        </Box>

        <ContentCard>
          <Stack
            direction={{ xs: "column", sm: "row" }}
            spacing={2}
            sx={{ alignItems: { sm: "center" }, justifyContent: "space-between" }}
          >
            <Stack direction="row" spacing={2} sx={{ alignItems: "center" }}>
              <Avatar
                sx={{
                  width: { xs: 56, sm: 64 },
                  height: { xs: 56, sm: 64 },
                  borderRadius: 3,
                  bgcolor: "primary.main",
                  fontSize: 20,
                  fontWeight: 700,
                }}
              >
                {displayName.slice(0, 2).toUpperCase()}
              </Avatar>
              <Box>
                <Stack direction="row" spacing={1} sx={{ alignItems: "center", flexWrap: "wrap" }}>
                  <Typography variant="h6" sx={{ fontWeight: 700 }}>
                    {displayName}
                  </Typography>
                  {user?.enabled ? (
                    <VerifiedIcon sx={{ fontSize: 18, color: "primary.main" }} />
                  ) : null}
                </Stack>
                <Typography variant="body2" color="text.secondary">
                  {user?.username}
                </Typography>
                <Stack
                  direction="row"
                  spacing={1}
                  sx={{ alignItems: "center", flexWrap: "wrap", mt: 0.75 }}
                >
                  <StatusChip enabled={!!user?.enabled} />
                  <Typography variant="caption" color="text.disabled">
                    ·
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    {roleNames}
                  </Typography>
                </Stack>
              </Box>
            </Stack>
            <Stack direction="row" spacing={1} sx={{ alignSelf: { sm: "flex-start" } }}>
              <Button
                variant="outlined"
                color="default"
                size="sm"
                onClick={() => void handleToggleEnabled()}
                disabled={busy}
              >
                <LockOutlinedIcon sx={{ fontSize: 14 }} />
                {user?.enabled ? t("common.suspend") : t("common.reactivate")}
              </Button>
            </Stack>
          </Stack>
        </ContentCard>

        <Tabs
          value={tab}
          onChange={(_, v: Tab) => setTab(v)}
          variant="scrollable"
          scrollButtons="auto"
          sx={{ borderBottom: 1, borderColor: "divider" }}
        >
          {TABS.map((item) => (
            <Tab key={item.id} value={item.id} label={item.label} />
          ))}
        </Tabs>

        {tab === "profile" ? (
          <Grid container spacing={2}>
            {[
              {
                Icon: PersonOutlineIcon,
                label: t("common.username"),
                value: user?.username ?? "—",
              },
              { Icon: ShieldOutlinedIcon, label: t("system.userDetail.role"), value: roleNames },
              {
                Icon: CalendarTodayOutlinedIcon,
                label: t("system.userDetail.joined"),
                value: user?.createdAt ? new Date(user.createdAt).toLocaleDateString() : "—",
              },
              {
                Icon: AccessTimeOutlinedIcon,
                label: t("system.userDetail.lastLogin"),
                value: lastLogin ? new Date(lastLogin).toLocaleString() : "—",
              },
            ].map(({ Icon, label, value }) => (
              <Grid key={label} size={{ xs: 12, sm: 6 }}>
                <ContentCard>
                  <Stack direction="row" spacing={1.5} sx={{ alignItems: "center" }}>
                    <Box
                      sx={{
                        width: 32,
                        height: 32,
                        borderRadius: 2,
                        bgcolor: "primary.50",
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "center",
                        flexShrink: 0,
                      }}
                    >
                      <Icon sx={{ fontSize: 16, color: "primary.main" }} />
                    </Box>
                    <Box>
                      <Typography
                        variant="caption"
                        color="text.secondary"
                        sx={{ fontWeight: 600, textTransform: "uppercase", letterSpacing: 0.5 }}
                      >
                        {label}
                      </Typography>
                      <Typography variant="body2" sx={{ fontWeight: 500, mt: 0.25 }}>
                        {value}
                      </Typography>
                    </Box>
                  </Stack>
                </ContentCard>
              </Grid>
            ))}
          </Grid>
        ) : null}

        {tab === "roles" ? (
          <ContentCard>
            <Typography variant="subtitle2" sx={{ fontWeight: 600, mb: 2 }}>
              {t("system.userDetail.assignRole")}
            </Typography>
            <Stack spacing={1} sx={{ mb: 3 }}>
              {roles.map((r) => {
                const checked = selectedRoleIds.includes(r.id);
                return (
                  <Box
                    key={r.id}
                    sx={{
                      p: 1.75,
                      borderRadius: 3,
                      border: 2,
                      borderColor: checked ? "primary.main" : "divider",
                      bgcolor: checked ? "primary.50" : "transparent",
                      "&:hover": { bgcolor: checked ? "primary.50" : "action.hover" },
                    }}
                  >
                    <FormControlLabel
                      control={
                        <Checkbox
                          checked={checked}
                          onChange={(e) =>
                            setSelectedRoleIds((prev) =>
                              e.target.checked ? [...prev, r.id] : prev.filter((x) => x !== r.id),
                            )
                          }
                          color="primary"
                        />
                      }
                      label={
                        <Box>
                          <Typography variant="body2" sx={{ fontWeight: 500 }}>
                            {r.name}
                          </Typography>
                          <Typography
                            variant="caption"
                            color="text.secondary"
                            sx={{ fontFamily: "monospace" }}
                          >
                            {r.code}
                          </Typography>
                        </Box>
                      }
                      sx={{ m: 0, width: "100%" }}
                    />
                  </Box>
                );
              })}
            </Stack>
            <Button
              variant="contained"
              size="sm"
              onClick={() => void handleSaveRoles()}
              disabled={busy}
            >
              {busy ? t("common.saving") : t("common.saveChanges")}
            </Button>
          </ContentCard>
        ) : null}

        {tab === "activity" ? (
          <ContentCard>
            {ownLogs.length === 0 ? (
              <Typography
                variant="body2"
                color="text.secondary"
                sx={{ textAlign: "center", py: 4 }}
              >
                {t("system.userDetail.noLoginRecordsYet")}
              </Typography>
            ) : (
              <Stack divider={<Box sx={{ borderBottom: 1, borderColor: "divider" }} />}>
                {ownLogs.map((log) => {
                  const Icon = log.status === "success" ? LoginOutlinedIcon : CancelOutlinedIcon;
                  return (
                    <ListRow
                      key={log.id}
                      primary={
                        <>
                          {log.status === "success"
                            ? t("system.userDetail.loggedIn")
                            : t("error.login.failed")}
                          {log.browser ? ` · ${log.browser}` : ""}
                        </>
                      }
                      secondary={log.ip}
                      end={
                        <Typography
                          variant="caption"
                          color="text.secondary"
                          sx={{ whiteSpace: "nowrap" }}
                        >
                          {new Date(log.createdAt).toLocaleString()}
                        </Typography>
                      }
                      action={
                        <Box
                          sx={{
                            width: 32,
                            height: 32,
                            borderRadius: "50%",
                            bgcolor: "primary.50",
                            display: "flex",
                            alignItems: "center",
                            justifyContent: "center",
                            mr: 1,
                          }}
                        >
                          <Icon sx={{ fontSize: 14, color: "primary.main" }} />
                        </Box>
                      }
                    />
                  );
                })}
              </Stack>
            )}
          </ContentCard>
        ) : null}

        {tab === "sessions" ? (
          <Stack spacing={1.5}>
            {sessions.length === 0 ? (
              <ContentCard>
                <Typography
                  variant="body2"
                  color="text.secondary"
                  sx={{ textAlign: "center", py: 4 }}
                >
                  {t("system.userDetail.noLoginRecordsYet")}
                </Typography>
              </ContentCard>
            ) : (
              sessions.map((s) => (
                <ContentCard key={s.id} padding={2}>
                  <Stack direction="row" spacing={1.5} sx={{ alignItems: "center" }}>
                    <Box
                      sx={{
                        width: 36,
                        height: 36,
                        borderRadius: 2,
                        bgcolor: "action.hover",
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "center",
                        flexShrink: 0,
                      }}
                    >
                      <LaptopOutlinedIcon sx={{ fontSize: 18, color: "text.secondary" }} />
                    </Box>
                    <Box sx={{ flex: 1, minWidth: 0 }}>
                      <Typography variant="body2" sx={{ fontWeight: 500 }}>
                        {s.browser ?? t("system.userDetail.unknownDevice")}
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        {s.ip}
                      </Typography>
                    </Box>
                    <Typography variant="caption" color="text.secondary" sx={{ flexShrink: 0 }}>
                      {new Date(s.createdAt).toLocaleString()}
                    </Typography>
                  </Stack>
                </ContentCard>
              ))
            )}
          </Stack>
        ) : null}
      </PageStack>
    </Box>
  );
}

export default UserDetail;
