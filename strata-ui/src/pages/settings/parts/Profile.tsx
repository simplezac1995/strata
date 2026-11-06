// SPDX-License-Identifier: Apache-2.0
import { useState } from "react";
import { Avatar, Box, Grid, Stack, Typography } from "@mui/material";
import { Button, ImageUpload, TextField, type UploadedImage } from "@/components/ui/index";
import { useI18n } from "@/shared/i18n";
import { useAuthStore } from "@/shared/auth/store";
import { userApi } from "@/shared/api/system";
import { profileApi } from "@/shared/api/profile";
import { useMutationWithToast } from "@/shared/hooks/useMutationWithToast";
import { ContentCard } from "@/shared/pageKit/PageLayout";
import {
  PageHero as SettingsHero,
  PageWorkspace as SettingsWorkspace,
} from "@/shared/pageKit/PageWorkspace";

export function Profile() {
  const { t } = useI18n();
  const user = useAuthStore((s) => s.user);
  const [nickname, setNickname] = useState(user?.nickname ?? "");
  const [email, setEmail] = useState(user?.email ?? "");
  const [phone, setPhone] = useState("");
  const [avatar, setAvatar] = useState<UploadedImage | null>(null);

  const displayName = user?.nickname?.trim() || user?.username || "";
  const initials =
    displayName
      .split(" ")
      .filter(Boolean)
      .map((p) => p[0])
      .join("")
      .slice(0, 2)
      .toUpperCase() || "?";

  const saveProfileMutation = useMutationWithToast(
    async () => {
      if (!user) throw new Error("no user");
      await userApi.update(user.id, {
        nickname: nickname.trim() ? nickname : null,
        enabled: user.enabled,
        password: null,
        roleIds: user.roleIds,
      });
      const fresh = await userApi.get(user.id).catch(() => null);
      useAuthStore.getState().setUser(fresh ?? { ...user, nickname: nickname.trim() || null });
    },
    { success: "toast.saved", failure: "toast.saveFailed" },
  );

  const saveEmailMutation = useMutationWithToast(() => profileApi.updateEmail(email.trim()), {
    success: "toast.saved",
    failure: "toast.saveFailed",
  });

  const savePhoneMutation = useMutationWithToast(() => profileApi.updatePhone(phone.trim()), {
    success: "toast.saved",
    failure: "toast.saveFailed",
  });

  const saveAvatarMutation = useMutationWithToast((url: string) => profileApi.updateAvatar(url), {
    success: "toast.saved",
    failure: "toast.saveFailed",
  });

  const handleAvatarChange = (value: UploadedImage | null) => {
    setAvatar(value);
    if (value?.url) void saveAvatarMutation.mutateAsync(value.url).catch(() => {});
  };

  return (
    <SettingsWorkspace>
      <SettingsHero title={t("nav.profile")} subtitle={t("settings.profile.subtitle")} />
      <Grid container spacing={3}>
        <Grid size={{ xs: 12, lg: 4 }}>
          <ContentCard>
            <Stack spacing={2} sx={{ alignItems: "center", textAlign: "center" }}>
              <Avatar
                src={avatar?.url ?? undefined}
                sx={{
                  width: 80,
                  height: 80,
                  bgcolor: "primary.main",
                  fontSize: 28,
                  fontWeight: 700,
                }}
              >
                {!avatar?.url && initials}
              </Avatar>
              <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
                {displayName || t("common.emDash")}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                {user?.username ?? ""}
              </Typography>
              <Box sx={{ width: "100%" }}>
                <ImageUpload
                  value={avatar}
                  onChange={handleAvatarChange}
                  aspect={1}
                  label={t("settings.profile.avatarLabel")}
                />
              </Box>
            </Stack>
          </ContentCard>
        </Grid>

        <Grid size={{ xs: 12, lg: 8 }}>
          <ContentCard>
            <Stack spacing={2}>
              <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
                {t("common.basicInformation")}
              </Typography>
              <Grid container spacing={2}>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <TextField label={t("common.username")} value={user?.username ?? ""} disabled />
                </Grid>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <TextField
                    label={t("common.nickname")}
                    value={nickname}
                    onChange={(e) => setNickname(e.target.value)}
                  />
                </Grid>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <TextField
                    label={t("common.createdAt")}
                    value={
                      user?.createdAt
                        ? new Date(user.createdAt).toLocaleString()
                        : t("common.emDash")
                    }
                    disabled
                  />
                </Grid>
              </Grid>
              <Button
                variant="contained"
                disabled={saveProfileMutation.isPending || !user}
                onClick={() => void saveProfileMutation.mutateAsync().catch(() => {})}
              >
                {saveProfileMutation.isPending ? t("common.saving") : t("common.saveChanges")}
              </Button>
            </Stack>
          </ContentCard>

          <ContentCard sx={{ mt: 3 }}>
            <Stack spacing={2}>
              <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
                {t("settings.profile.contactTitle")}
              </Typography>
              <Grid container spacing={2}>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <TextField
                    label={t("common.email")}
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                  />
                </Grid>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <TextField
                    label={t("common.phone")}
                    value={phone}
                    placeholder={t("settings.profile.phonePlaceholder")}
                    onChange={(e) => setPhone(e.target.value)}
                  />
                </Grid>
              </Grid>
              <Stack direction="row" spacing={1.5}>
                <Button
                  variant="outlined"
                  disabled={saveEmailMutation.isPending || !email.trim()}
                  onClick={() => void saveEmailMutation.mutateAsync().catch(() => {})}
                >
                  {saveEmailMutation.isPending
                    ? t("common.saving")
                    : t("settings.profile.updateEmail")}
                </Button>
                <Button
                  variant="outlined"
                  disabled={savePhoneMutation.isPending || !phone.trim()}
                  onClick={() => void savePhoneMutation.mutateAsync().catch(() => {})}
                >
                  {savePhoneMutation.isPending
                    ? t("common.saving")
                    : t("settings.profile.updatePhone")}
                </Button>
              </Stack>
            </Stack>
          </ContentCard>
        </Grid>
      </Grid>
    </SettingsWorkspace>
  );
}

export default Profile;
