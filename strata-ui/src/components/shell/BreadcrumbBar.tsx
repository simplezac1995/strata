// SPDX-License-Identifier: Apache-2.0
import { useLocation, useNavigate } from "react-router-dom";
import { Breadcrumbs as MuiBreadcrumbs, Link, Typography } from "@mui/material";
import NavigateNextIcon from "@mui/icons-material/NavigateNext";
import { useI18n } from "@/shared/i18n";

const SEGMENT_KEYS: Record<string, string> = {
  dashboard: "nav.dashboard",
  system: "nav.system",
  users: "nav.users",
  roles: "nav.roles",
  menus: "nav.menus",
  departments: "nav.departments",
  posts: "nav.posts",
  tenants: "nav.tenants",
  logs: "nav.logs",
  operation: "nav.operation.logs",
  login: "nav.login.logs",
  audit: "nav.audit.logs",
  dictionaries: "nav.dictionaries",
  parameters: "nav.parameters",
  files: "nav.files",
  i18n: "nav.i18n",
  clients: "nav.clients",
  sessions: "nav.sessions",
  sensitive: "nav.sensitive.words",
  regions: "nav.regions",
  "site-settings": "settings.site.title",
  webhooks: "nav.webhooks",
  "email-templates": "nav.email.templates",
  announcements: "nav.announcements",
  demo: "nav.demo",
  products: "nav.products",
  payments: "nav.payments",
  tools: "nav.tools",
  "code-generator": "nav.code.generator",
  cache: "nav.cache.monitor",
  schedules: "nav.schedules",
  "message-push": "nav.message.push",
  "api-docs": "nav.api.docs",
  ai: "nav.ai",
  plugins: "nav.plugins",
  storage: "nav.storage",
  overview: "nav.overview",
  providers: "nav.providers",
  models: "nav.models",
  "vector-stores": "nav.vector.stores",
  prompts: "nav.prompts",
  terminology: "nav.terminology",
  "knowledge-bases": "nav.knowledge.bases",
  documents: "nav.documents",
  datasets: "nav.datasets",
  skills: "nav.skills",
  "interface-skills": "nav.interface.skills",
  "mcp-servers": "nav.mcp.servers",
  agents: "nav.agents",
  workspace: "nav.workspace",
  chat: "nav.chat",
  proofreading: "nav.proofreading",
  presentations: "nav.presentations",
  bidding: "nav.bidding",
  "code-review": "nav.code.review",
  image: "nav.image",
  video: "nav.video",
  audio: "nav.audio.tts",
  poster: "nav.poster",
  ocr: "nav.ocr.templates",
  safety: "nav.safety.check",
  usage: "nav.usage.statistics",
  tasks: "nav.task.history",
  billing: "nav.billing",
  labeling: "nav.labeling",
  copilot: "nav.copilot.states",
  settings: "nav.settings",
  profile: "nav.profile",
  security: "nav.security",
  appearance: "nav.appearance",
  language: "nav.language",
  "api-keys": "nav.api.keys",
  team: "nav.team.members",
  notifications: "nav.notifications",
  pro: "nav.pro",
  workflow: "nav.workflow",
  blog: "nav.blog",
  cover: "nav.cover",
  foundations: "nav.foundations",
  components: "nav.components",
  states: "nav.states",
};

/**
 * Independent breadcrumb row rendered under the TopBar in AppShell.
 * Last segment is non-interactive; earlier segments navigate.
 */
export function BreadcrumbBar() {
  const location = useLocation();
  const navigate = useNavigate();
  const { t } = useI18n();
  const parts = location.pathname.split("/").filter(Boolean);
  const crumbs = [
    { label: t("shell.home"), path: "/" },
    ...parts.map((p, i) => ({
      label: SEGMENT_KEYS[p]
        ? t(SEGMENT_KEYS[p])
        : t(p.charAt(0).toUpperCase() + p.slice(1).replace(/-/g, " ")),
      path: "/" + parts.slice(0, i + 1).join("/"),
    })),
  ];

  return (
    <MuiBreadcrumbs
      separator={<NavigateNextIcon sx={{ fontSize: 16 }} />}
      sx={{
        fontSize: "0.875rem",
        color: "text.secondary",
        minWidth: 0,
        "& .MuiBreadcrumbs-separator": { mx: 0.5 },
      }}
    >
      {crumbs.map((c, i) => {
        const isLast = i === crumbs.length - 1;
        if (isLast) {
          return (
            <Typography
              key={c.path}
              variant="body2"
              sx={{
                fontWeight: 600,
                color: "text.primary",
                maxWidth: { xs: "10rem", sm: "16rem" },
                overflow: "hidden",
                textOverflow: "ellipsis",
                whiteSpace: "nowrap",
              }}
            >
              {c.label}
            </Typography>
          );
        }
        return (
          <Link
            key={c.path}
            component="button"
            underline="hover"
            variant="body2"
            onClick={() => navigate(c.path)}
            sx={{
              color: "text.secondary",
              "&:hover": { color: "primary.main" },
              maxWidth: { xs: "10rem", sm: "16rem" },
              overflow: "hidden",
              textOverflow: "ellipsis",
              whiteSpace: "nowrap",
            }}
          >
            {c.label}
          </Link>
        );
      })}
    </MuiBreadcrumbs>
  );
}
