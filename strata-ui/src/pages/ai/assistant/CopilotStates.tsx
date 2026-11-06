// SPDX-License-Identifier: Apache-2.0
import { Badge, Box, Fab, Grid, IconButton, Stack, Typography } from "@mui/material";
import CloseIcon from "@mui/icons-material/Close";
import ArrowForwardIcon from "@mui/icons-material/ArrowForward";
import SmartToyOutlinedIcon from "@mui/icons-material/SmartToyOutlined";
import { Card, TextField } from "@/components/ui/index";
import { AiPageHeader } from "@/components/ui/index";
import { useI18n } from "@/shared/i18n";
import { InfoBanner } from "@/shared/pageKit/InfoBanner";
import { ContentCard, PageStack } from "@/shared/pageKit/PageLayout";

function TypingDots() {
  return (
    <Stack direction="row" spacing={0.5} sx={{ px: 1.5, py: 1 }}>
      {[0, 1, 2].map((d) => (
        <Box
          key={d}
          sx={{
            width: 6,
            height: 6,
            borderRadius: "50%",
            bgcolor: "text.secondary",
            animation: "bounce 1s infinite",
            animationDelay: `${d * 150}ms`,
            "@keyframes bounce": {
              "0%, 100%": { transform: "translateY(0)" },
              "50%": { transform: "translateY(-4px)" },
            },
          }}
        />
      ))}
    </Stack>
  );
}

function ChatBubble({
  children,
  variant,
}: {
  children: React.ReactNode;
  variant: "assistant" | "user";
}) {
  const isUser = variant === "user";
  return (
    <Box
      sx={{
        maxWidth: 144,
        borderRadius: 3,
        px: 1.25,
        py: 0.75,
        fontSize: 12,
        ...(isUser
          ? {
              bgcolor: "primary.main",
              color: "primary.contrastText",
              borderTopRightRadius: 4,
              alignSelf: "flex-end",
            }
          : {
              bgcolor: "action.hover",
              color: "text.primary",
              borderTopLeftRadius: 4,
            }),
      }}
    >
      {children}
    </Box>
  );
}

function CopilotPreviewCard({
  children,
  label,
  tall,
}: {
  children: React.ReactNode;
  label: string;
  tall?: boolean;
}) {
  return (
    <Stack spacing={1.5} sx={{ alignItems: "center" }}>
      <Card
        sx={{
          width: "100%",
          height: tall ? 288 : 160,
          display: "flex",
          flexDirection: "column",
          overflow: "hidden",
          bgcolor: (theme) => (theme.palette.mode === "dark" ? "rgba(0,0,0,0.2)" : "grey.50"),
          ...(!tall ? { alignItems: "flex-end", justifyContent: "flex-end", p: 2 } : {}),
        }}
      >
        {children}
      </Card>
      <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 500 }}>
        {label}
      </Typography>
    </Stack>
  );
}

function CopilotIcon({ size = 14 }: { size?: number }) {
  return <SmartToyOutlinedIcon sx={{ fontSize: size }} />;
}

function AvatarDot({ small }: { small?: boolean }) {
  return (
    <Box
      sx={{
        width: small ? 16 : 20,
        height: small ? 16 : 20,
        borderRadius: "50%",
        bgcolor: "primary.main",
        color: "primary.contrastText",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        flexShrink: 0,
      }}
    >
      <CopilotIcon size={small ? 9 : 11} />
    </Box>
  );
}

export function CopilotStates() {
  const { t } = useI18n();
  return (
    <PageStack>
      <AiPageHeader title={t("ai.assistant.ai33FloatingCopilot")} />
      <Typography variant="body2" color="text.secondary" sx={{ mb: 0.5 }}>
        {t("ai.assistant.componentStatesCollapsedFabExpandedTrayActiveConversa")}
      </Typography>
      <InfoBanner>
        {t("ai.assistant.designStatesOnlyInteractiveChatUsesWorkspaceQAApis")}
      </InfoBanner>

      <Grid container spacing={3}>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <CopilotPreviewCard label={t("ai.assistant.copilot.collapsedFab")}>
            <Fab color="primary" size="medium" disabled sx={{ boxShadow: 4 }}>
              <CopilotIcon size={22} />
            </Fab>
          </CopilotPreviewCard>
        </Grid>

        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <CopilotPreviewCard label={t("ai.assistant.copilot.withNotification")}>
            <Badge badgeContent={2} color="error">
              <Fab color="primary" size="medium" disabled sx={{ boxShadow: 4 }}>
                <CopilotIcon size={22} />
              </Fab>
            </Badge>
          </CopilotPreviewCard>
        </Grid>

        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <CopilotPreviewCard label={t("ai.assistant.copilot.expandedTray")} tall>
            <Box sx={{ width: "100%", display: "flex", flexDirection: "column", flex: 1 }}>
              <Stack
                direction="row"
                sx={{
                  alignItems: "center",
                  justifyContent: "space-between",
                  px: 1.5,
                  py: 1,
                  bgcolor: "primary.main",
                  color: "primary.contrastText",
                  borderBottom: 1,
                  borderColor: "divider",
                }}
              >
                <Stack direction="row" spacing={1} sx={{ alignItems: "center" }}>
                  <CopilotIcon size={16} />
                  <Typography variant="caption" sx={{ fontWeight: 600 }}>
                    {t("shell.copilot.sessionName")}
                  </Typography>
                </Stack>
                <IconButton
                  size="small"
                  aria-label={t("common.close")}
                  sx={{ color: "rgba(255,255,255,0.7)" }}
                  disabled
                >
                  <CloseIcon sx={{ fontSize: 14 }} />
                </IconButton>
              </Stack>
              <Box sx={{ flex: 1, p: 1.5, overflow: "hidden" }}>
                <Stack direction="row" spacing={1}>
                  <AvatarDot />
                  <ChatBubble variant="assistant">
                    {t("ai.assistant.hiIMYourAdminCopilotHowCanIHelp")}
                  </ChatBubble>
                </Stack>
              </Box>
              <Box sx={{ px: 1.5, pb: 1.5 }}>
                <Stack
                  direction="row"
                  spacing={0.75}
                  sx={{
                    alignItems: "center",
                    bgcolor: "action.hover",
                    borderRadius: 3,
                    px: 1.25,
                    py: 0.75,
                  }}
                >
                  <TextField
                    size="small"
                    placeholder={t("shell.copilot.placeholder")}
                    disabled
                    variant="standard"
                    slotProps={{ input: { disableUnderline: true, sx: { fontSize: 12 } } }}
                    sx={{ flex: 1 }}
                  />
                  <ArrowForwardIcon sx={{ fontSize: 16, color: "primary.main" }} />
                </Stack>
              </Box>
            </Box>
          </CopilotPreviewCard>
        </Grid>

        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <CopilotPreviewCard label={t("ai.assistant.copilot.activeChatLoading")} tall>
            <Box sx={{ width: "100%", display: "flex", flexDirection: "column", flex: 1 }}>
              <Stack
                direction="row"
                sx={{
                  alignItems: "center",
                  justifyContent: "space-between",
                  px: 1.5,
                  py: 1,
                  bgcolor: "primary.main",
                  color: "primary.contrastText",
                }}
              >
                <Stack direction="row" spacing={1} sx={{ alignItems: "center" }}>
                  <CopilotIcon size={16} />
                  <Typography variant="caption" sx={{ fontWeight: 600 }}>
                    {t("shell.copilot.sessionName")}
                  </Typography>
                </Stack>
                <IconButton
                  size="small"
                  aria-label={t("common.close")}
                  sx={{ color: "rgba(255,255,255,0.7)" }}
                  disabled
                >
                  <CloseIcon sx={{ fontSize: 14 }} />
                </IconButton>
              </Stack>
              <Stack spacing={1} sx={{ flex: 1, p: 1.25, overflow: "hidden", fontSize: 12 }}>
                <Stack direction="row" spacing={0.75}>
                  <AvatarDot small />
                  <ChatBubble variant="assistant">{t("ai.assistant.howCanIHelp")}</ChatBubble>
                </Stack>
                <ChatBubble variant="user">{t("ai.assistant.showMeActiveUsers")}</ChatBubble>
                <Stack direction="row" spacing={0.75}>
                  <AvatarDot small />
                  <ChatBubble variant="assistant">
                    {t("ai.assistant.found8ActiveUsersOnlineRightNow")}
                  </ChatBubble>
                </Stack>
                <Stack direction="row" spacing={0.75}>
                  <AvatarDot small />
                  <Box sx={{ bgcolor: "action.hover", borderRadius: 3, borderTopLeftRadius: 4 }}>
                    <TypingDots />
                  </Box>
                </Stack>
              </Stack>
              <Box sx={{ px: 1.25, pb: 1.25 }}>
                <Stack
                  direction="row"
                  spacing={0.5}
                  sx={{
                    alignItems: "center",
                    bgcolor: "action.hover",
                    borderRadius: 3,
                    px: 1,
                    py: 0.5,
                  }}
                >
                  <TextField
                    size="small"
                    defaultValue={t("ai.assistant.listUsersInEngineering")}
                    disabled
                    variant="standard"
                    slotProps={{ input: { disableUnderline: true, sx: { fontSize: 10 } } }}
                    sx={{ flex: 1 }}
                  />
                  <ArrowForwardIcon sx={{ fontSize: 16, color: "primary.main" }} />
                </Stack>
              </Box>
            </Box>
          </CopilotPreviewCard>
        </Grid>
      </Grid>

      <Box sx={{ mt: 4, pt: 3, borderTop: 1, borderColor: "divider" }}>
        <Typography variant="subtitle2" sx={{ fontWeight: 600, mb: 1 }}>
          {t("ai.assistant.designStates")}
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          {t("ai.assistant.showcaseOnlyForLiveChatUseWorkspaceOrAiChatPagesW")}
        </Typography>
        <ContentCard>
          <Stack direction="row" spacing={1.5} sx={{ alignItems: "center" }}>
            <Box
              sx={{
                width: 40,
                height: 40,
                borderRadius: "50%",
                bgcolor: "primary.main",
                color: "primary.contrastText",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
              }}
            >
              <CopilotIcon size={22} />
            </Box>
            <Box>
              <Typography variant="body2" sx={{ fontWeight: 500 }}>
                {t("shell.copilot.fabTitle")}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                {t("ai.assistant.designMockNotConnectedInMvp")}
              </Typography>
            </Box>
          </Stack>
        </ContentCard>
      </Box>
    </PageStack>
  );
}

export default CopilotStates;
