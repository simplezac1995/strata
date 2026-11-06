// SPDX-License-Identifier: Apache-2.0
import { Box, ButtonBase, Grid, Stack, Typography } from "@mui/material";
import ArrowForwardIcon from "@mui/icons-material/ArrowForward";
import BoltOutlinedIcon from "@mui/icons-material/BoltOutlined";
import LockOutlinedIcon from "@mui/icons-material/LockOutlined";
import SmartToyOutlinedIcon from "@mui/icons-material/SmartToyOutlined";
import StarOutlineOutlinedIcon from "@mui/icons-material/StarOutlineOutlined";
import type { SvgIconComponent } from "@mui/icons-material";
import { Card } from "@/components/ui/index";
import { ProBadge, AiBadge } from "@/components/ui/index";
import { useNavigate } from "react-router-dom";
import { useI18n } from "@/shared/i18n";
import { PageGrid, PageStack } from "@/shared/pageKit/PageLayout";
import { authHeroPanelSx } from "@/theme/pageStyles";
import type { SxProps, Theme } from "@mui/material/styles";

interface SitemapGroup {
  id: string;
  title: string;
  sx: SxProps<Theme>;
  items: { label: string; path: string; badge?: "pro" | "ai" }[];
}

const GROUPS: SitemapGroup[] = [
  {
    id: "foundations",
    title: "00—2 Foundations & Components",
    sx: {
      bgcolor: (theme) => (theme.palette.mode === "dark" ? "grey.900" : "grey.100"),
      borderColor: "divider",
    },
    items: [
      { label: "Cover + Sitemap", path: "/cover" },
      { label: "Foundations", path: "/foundations" },
      { label: "Components", path: "/components" },
    ],
  },
  {
    id: "shell",
    title: "03 Shell & Auth",
    sx: {
      bgcolor: (theme) =>
        theme.palette.mode === "dark" ? "rgba(49, 46, 129, 0.2)" : "rgba(238, 242, 255, 1)",
      borderColor: (theme) =>
        theme.palette.mode === "dark" ? "rgba(129, 140, 248, 0.3)" : "rgba(199, 210, 254, 1)",
    },
    items: [
      { label: "A01 Login (Desktop)", path: "/login" },
      { label: "A02 App Shell (Expanded + Collapsed)", path: "/dashboard" },
      { label: "A03 Lock Screen", path: "/dashboard" },
      { label: "A04 Dark Shell", path: "/settings/appearance" },
      { label: "A05 Compact Shell + Products", path: "/demo/products" },
      { label: "A06 States Board", path: "/states" },
    ],
  },
  {
    id: "system",
    title: "04 System",
    sx: {
      bgcolor: (theme) =>
        theme.palette.mode === "dark" ? "rgba(12, 74, 110, 0.2)" : "rgba(240, 249, 255, 1)",
      borderColor: (theme) =>
        theme.palette.mode === "dark" ? "rgba(56, 189, 248, 0.3)" : "rgba(186, 230, 253, 1)",
    },
    items: [
      { label: "S01 Users", path: "/system/users" },
      { label: "S02 Roles", path: "/system/roles" },
      { label: "S03 Menus", path: "/system/menus" },
      { label: "S04 Departments", path: "/system/departments" },
      { label: "S05 Posts", path: "/system/posts" },
      { label: "S06 Tenants", path: "/system/tenants", badge: "pro" },
      { label: "S07 Operation Logs", path: "/system/logs/operation" },
      { label: "S08 Login Logs", path: "/system/logs/login" },
      { label: "S09 Dictionaries", path: "/system/dictionaries" },
      { label: "S10 Parameters", path: "/system/parameters" },
      { label: "S11 Files", path: "/system/files" },
      { label: "S12 Internationalization", path: "/system/i18n" },
      { label: "S13 Clients", path: "/system/clients" },
      { label: "S14 Sessions", path: "/system/sessions" },
      { label: "S15 Site Settings", path: "/system/site-settings" },
      { label: "S16 Sensitive Words + Regions", path: "/system/sensitive" },
    ],
  },
  {
    id: "demo",
    title: "05 Demo / Tools / Plugins / Settings",
    sx: {
      bgcolor: (theme) =>
        theme.palette.mode === "dark" ? "rgba(6, 78, 59, 0.2)" : "rgba(236, 253, 245, 1)",
      borderColor: (theme) =>
        theme.palette.mode === "dark" ? "rgba(52, 211, 153, 0.3)" : "rgba(167, 243, 208, 1)",
    },
    items: [
      { label: "D01 Products (Golden Template)", path: "/demo/products" },
      { label: "T01 Code Generator", path: "/tools/code-generator" },
      { label: "T02 Cache Monitor", path: "/tools/cache" },
      { label: "T03 Schedules", path: "/tools/schedules" },
      { label: "T04 Message Push", path: "/tools/message-push" },
      { label: "P01 Plugins Overview", path: "/plugins" },
      { label: "P02 Storage Settings", path: "/plugins/storage" },
      { label: "P03 Payments / Stripe", path: "/plugins/payments" },
      { label: "P04 AI Pack Empty State", path: "/plugins/ai" },
      { label: "U01 Profile", path: "/settings/profile" },
      { label: "U02 Security", path: "/settings/security" },
      { label: "U03 Appearance", path: "/settings/appearance" },
      { label: "U04 Language", path: "/settings/language" },
    ],
  },
  {
    id: "ai",
    title: "06 AI PACK",
    sx: {
      bgcolor: (theme) =>
        theme.palette.mode === "dark" ? "rgba(76, 29, 149, 0.2)" : "rgba(245, 243, 255, 1)",
      borderColor: (theme) =>
        theme.palette.mode === "dark" ? "rgba(167, 139, 250, 0.3)" : "rgba(221, 214, 254, 1)",
    },
    items: [
      { label: "AI01 Overview", path: "/ai/overview", badge: "ai" },
      { label: "AI02 Providers", path: "/ai/providers", badge: "ai" },
      { label: "AI03 Models", path: "/ai/models", badge: "ai" },
      { label: "AI04 Vector Stores", path: "/ai/vector-stores", badge: "ai" },
      { label: "AI05 Prompts", path: "/ai/prompts", badge: "ai" },
      { label: "AI06 Terminology", path: "/ai/terminology", badge: "ai" },
      { label: "AI07 Knowledge Bases + Docs", path: "/ai/knowledge-bases", badge: "ai" },
      { label: "AI08 Datasets", path: "/ai/datasets", badge: "ai" },
      { label: "AI09 Skills (Card Grid)", path: "/ai/skills", badge: "ai" },
      { label: "AI10 Interface Skills", path: "/ai/interface-skills", badge: "ai" },
      { label: "AI11 MCP Servers", path: "/ai/mcp-servers", badge: "ai" },
      { label: "AI12 Agents", path: "/ai/agents", badge: "ai" },
      { label: "AI13 Usage Statistics", path: "/ai/usage", badge: "ai" },
      { label: "AI14 AI Billing", path: "/ai/billing", badge: "ai" },
      { label: "AI15 Data Labeling", path: "/ai/labeling", badge: "ai" },
      { label: "AI20 Workspace Chat (NL →Admin)", path: "/ai/workspace", badge: "ai" },
      { label: "AI21 Chat / Q&A", path: "/ai/chat", badge: "ai" },
      { label: "AI22 Proofreading Workbench", path: "/ai/proofreading", badge: "ai" },
      { label: "AI23 Proofreading History", path: "/ai/proofreading/history", badge: "ai" },
      { label: "AI24 Lexicon", path: "/ai/proofreading/lexicon", badge: "ai" },
      { label: "AI25 Rules Tree", path: "/ai/proofreading/rules", badge: "ai" },
      { label: "AI26 Bidding Wizard", path: "/ai/bidding", badge: "ai" },
      { label: "AI27 Image Generation", path: "/ai/image", badge: "ai" },
      { label: "AI28 Video Generation", path: "/ai/video", badge: "ai" },
      { label: "AI29 Audio / TTS", path: "/ai/audio", badge: "ai" },
      { label: "AI30 Poster", path: "/ai/poster", badge: "ai" },
      { label: "AI31 OCR Templates", path: "/ai/ocr", badge: "ai" },
      { label: "AI32 Safety Check", path: "/ai/safety", badge: "ai" },
      { label: "AI33 Floating Copilot States", path: "/ai/copilot", badge: "ai" },
      { label: "Presentations", path: "/ai/presentations", badge: "ai" },
      { label: "Code Review", path: "/ai/code-review", badge: "ai" },
    ],
  },
  {
    id: "pro",
    title: "07 PRO",
    sx: {
      bgcolor: (theme) =>
        theme.palette.mode === "dark" ? "rgba(120, 53, 15, 0.2)" : "rgba(255, 251, 235, 1)",
      borderColor: (theme) =>
        theme.palette.mode === "dark" ? "rgba(251, 191, 36, 0.3)" : "rgba(253, 230, 138, 1)",
    },
    items: [
      { label: "C01 Workflow", path: "/pro/workflow", badge: "pro" },
      { label: "C02 Tenants / Billing", path: "/pro/billing", badge: "pro" },
      { label: "C03 Content / Blog", path: "/pro/blog", badge: "pro" },
    ],
  },
];

const HERO_CHIPS: { Icon: SvgIconComponent; key: string }[] = [
  { Icon: BoltOutlinedIcon, key: "docs.cover.stack" },
  { Icon: LockOutlinedIcon, key: "docs.cover.rbacJwtMultiTenant" },
  { Icon: SmartToyOutlinedIcon, key: "docs.cover.aiPackLlmRagAgents" },
  { Icon: StarOutlineOutlinedIcon, key: "docs.cover.proFeaturesClearlyMarked" },
];

const STATS = [
  { label: "docs.cover.totalScreens", val: "50+" },
  { label: "docs.cover.systemScreens", val: "16" },
  { label: "docs.cover.aiPackScreens", val: "25+" },
  { label: "docs.cover.30Components", val: "30+" },
];

export function Cover() {
  const { t } = useI18n();
  const navigate = useNavigate();
  return (
    <PageStack>
      <Box
        sx={(theme) => ({
          ...authHeroPanelSx(theme),
          borderRadius: 3,
          p: 5,
          mb: 4,
          color: "#fff",
          position: "relative",
          overflow: "hidden",
        })}
      >
        <Box
          sx={{
            position: "absolute",
            inset: 0,
            opacity: 0.1,
            backgroundImage:
              "linear-gradient(rgba(255,255,255,.2) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,.2) 1px, transparent 1px)",
            backgroundSize: "40px 40px",
          }}
        />
        <Box sx={{ position: "relative", zIndex: 1 }}>
          <Stack direction="row" spacing={1.5} sx={{ alignItems: "center", mb: 3 }}>
            <Box
              sx={{
                width: 48,
                height: 48,
                borderRadius: 3,
                bgcolor: "rgba(255,255,255,0.2)",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                fontWeight: 700,
                fontSize: 20,
              }}
            >
              S
            </Box>
            <Box>
              <Typography variant="h5" sx={{ fontWeight: 600 }}>
                {t("docs.cover.strataAdmin")}
              </Typography>
              <Typography variant="body2" sx={{ color: "rgba(255,255,255,0.7)" }}>
                {t("docs.cover.openCoreModularMonolithAdminApache20")}
              </Typography>
            </Box>
          </Stack>
          <Typography variant="h6" sx={{ color: "rgba(255,255,255,0.8)", maxWidth: 640, mb: 3 }}>
            {t("docs.cover.aCompleteModularMonolithAdminPlatformForG")}
          </Typography>
          <Stack direction="row" spacing={1} sx={{ flexWrap: "wrap" }}>
            {HERO_CHIPS.map(({ Icon, key }) => (
              <Stack
                key={key}
                direction="row"
                spacing={1}
                sx={{
                  alignItems: "center",
                  bgcolor: "rgba(255,255,255,0.1)",
                  borderRadius: 3,
                  px: 1.5,
                  py: 1,
                }}
              >
                <Icon sx={{ fontSize: 18, color: "rgba(255,255,255,0.9)" }} />
                <Typography variant="body2" sx={{ color: "rgba(255,255,255,0.85)" }}>
                  {t(key)}
                </Typography>
              </Stack>
            ))}
          </Stack>
        </Box>
      </Box>

      <PageGrid columns={{ xs: 12, sm: 6, md: 3 }}>
        {STATS.map((s) => (
          <Card key={s.label} sx={{ p: 2, textAlign: "center" }}>
            <Typography variant="h4" sx={{ fontWeight: 600 }} color="primary.main">
              {s.val}
            </Typography>
            <Typography variant="caption" color="text.secondary" sx={{ mt: 0.5, display: "block" }}>
              {t(s.label)}
            </Typography>
          </Card>
        ))}
      </PageGrid>

      <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
        {t("docs.cover.sitemapAllScreens")}
      </Typography>

      <Stack spacing={2}>
        {GROUPS.map((group) => (
          <Box
            key={group.id}
            sx={{
              borderRadius: 3,
              border: 1,
              p: 2.5,
              ...group.sx,
            }}
          >
            <Typography variant="subtitle1" sx={{ fontWeight: 600, mb: 1.5 }}>
              {t(group.title)}
            </Typography>
            <Grid container spacing={1}>
              {group.items.map((item) => (
                <Grid key={item.path + item.label} size={{ xs: 12, sm: 6, md: 4, lg: 3 }}>
                  <ButtonBase
                    onClick={() => navigate(item.path)}
                    sx={{
                      width: "100%",
                      display: "flex",
                      alignItems: "center",
                      gap: 0.75,
                      px: 1.5,
                      py: 1,
                      bgcolor: "background.paper",
                      borderRadius: 3,
                      textAlign: "left",
                      border: 1,
                      borderColor: "divider",
                      transition: "background-color 0.2s",
                      "&:hover": {
                        bgcolor: (theme) =>
                          theme.palette.mode === "dark"
                            ? "rgba(79, 70, 229, 0.15)"
                            : "rgba(238, 242, 255, 1)",
                        "& .MuiTypography-root": { color: "primary.main" },
                        "& .MuiSvgIcon-root": { color: "primary.main" },
                      },
                    }}
                  >
                    <Typography
                      variant="caption"
                      sx={{
                        fontWeight: 500,
                        flex: 1,
                        color: "text.primary",
                        transition: "color 0.2s",
                      }}
                    >
                      {t(item.label)}
                    </Typography>
                    {item.badge === "pro" && <ProBadge />}
                    {item.badge === "ai" && <AiBadge />}
                    <ArrowForwardIcon
                      sx={{ fontSize: 14, color: "text.disabled", transition: "color 0.2s" }}
                    />
                  </ButtonBase>
                </Grid>
              ))}
            </Grid>
          </Box>
        ))}
      </Stack>

      <Typography
        variant="caption"
        color="text.disabled"
        sx={{
          mt: 4,
          pt: 3,
          borderTop: 1,
          borderColor: "divider",
          textAlign: "center",
          display: "block",
        }}
      >
        {t("docs.cover.strataAdminOpenCoreApache20BuiltWithReact19Vite8")}
      </Typography>
    </PageStack>
  );
}

export default Cover;
