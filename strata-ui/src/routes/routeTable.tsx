// SPDX-License-Identifier: Apache-2.0
/**
 * Declarative route table 鈥?single source of truth for the app's routes (ADR-0020 decision 4).
 *
 * Rendered by `appRoutes.tsx`; consumed by `scripts/verify-menu-routes.mjs` for the menu鈫攔oute
 * consistency check. Keep this file a plain literal tree (no functions/JSX) so both consumers
 * can parse it:
 *  - `path`: route segment (relative to parent, or absolute for standalone top-level routes)
 *  - `index` + `redirect`: index route that redirects (e.g. `/` 鈫?`/dashboard`)
 *  - `page`: lazy page key from `lazyPages` (the component whitelist)
 *  - `perm`: required authority/authorities, rendered as `<Perm anyOf={...}>` (empty = public page)
 *  - `wrapper`: layout shell for a subtree 鈥?`"auth"` = RequireAuth, `"shell"` = AppShell
 *  - `children`: nested routes (group nodes may carry only `path`)
 */
export interface RouteSpec {
  path?: string;
  index?: boolean;
  redirect?: string;
  page?: string;
  perm?: string[];
  wrapper?: "auth" | "shell";
  children?: RouteSpec[];
}

export const ROUTE_TABLE: RouteSpec[] = [
  { path: "/login", page: "Login" },
  { path: "/forgot-password", page: "ForgotPassword" },
  { path: "/reset-password", page: "ResetPassword" },
  { path: "/register", page: "Register" },
  { path: "/verify-email", page: "VerifyEmail" },
  { path: "/sso-callback", page: "SsoCallback" },
  { path: "/403", page: "Error403" },
  { path: "/500", page: "Error500" },
  {
    wrapper: "auth",
    children: [
      {
        wrapper: "shell",
        children: [
          { index: true, redirect: "/dashboard" },
          { path: "dashboard", page: "Dashboard" },
          { path: "cover", page: "Cover" },
          { path: "foundations", page: "Foundations" },
          { path: "components", page: "ComponentGallery" },
          { path: "states", page: "StatesBoard" },
          { path: "notifications", page: "Notifications" },
          {
            path: "system",
            children: [
              { path: "users", page: "Users", perm: ["system:user:list"] },
              { path: "users/:id", page: "UserDetail", perm: ["system:user:list"] },
              { path: "roles", page: "Roles", perm: ["system:role:list"] },
              { path: "menus", page: "Menus", perm: ["system:menu:list"] },
              { path: "departments", page: "Departments", perm: ["system:dept:list"] },
              { path: "posts", page: "Posts", perm: ["system:post:list"] },
              { path: "tenants", page: "Tenants", perm: ["system:tenant:list"] },
              { path: "logs/operation", page: "OperationLogs", perm: ["system:operationlog:list"] },
              { path: "logs/login", page: "LoginLogs", perm: ["system:loginlog:list"] },
              { path: "logs/audit", page: "AuditLogs", perm: ["system:auditlog:list"] },
              { path: "dictionaries", page: "Dictionaries", perm: ["system:dict:list"] },
              { path: "parameters", page: "Parameters", perm: ["system:param:list"] },
              { path: "files", page: "Files", perm: ["system:file:list"] },
              { path: "i18n", page: "I18n", perm: ["system:i18n:list"] },
              { path: "clients", page: "Clients", perm: ["system:client:list"] },
              { path: "sessions", page: "Sessions", perm: ["system:loginlog:list"] },
              { path: "sensitive", page: "SensitiveWords", perm: ["system:sensitiveword:list"] },
              { path: "regions", page: "Regions", perm: ["system:region:list"] },
              { path: "site-settings", page: "SiteSettings", perm: ["system:sitesetting:list"] },
              { path: "webhooks", page: "Webhooks", perm: ["system:webhook:list"] },
              {
                path: "email-templates",
                page: "EmailTemplates",
                perm: ["system:emailtemplate:list"],
              },
              { path: "announcements", page: "Announcements", perm: ["system:announcement:list"] },
              { path: "monitor", page: "Monitor", perm: ["system:monitor:list"] },
              { path: "cs", page: "CsWorkbench", perm: ["system:cs:list"] },
              { path: "audit-changes", page: "AuditChanges", perm: ["system:audit:list"] },
            ],
          },
          {
            path: "casino",
            children: [
              { path: "wallet/tron", redirect: "/casino/wallet/tron/hot" },
              { path: "wallet/bsc", redirect: "/casino/wallet/bsc/hot" },
              { path: "wallet/wallets", redirect: "/casino/wallet/tron/hot" },
            ],
          },
          {
            path: "demo",
            children: [{ path: "products", page: "Products", perm: ["demo:product:list"] }],
          },
          {
            path: "tools",
            children: [
              { path: "code-generator", page: "CodeGenerator", perm: ["generator:list"] },
              { path: "cache", page: "CacheMonitor", perm: ["system:cache:list"] },
              { path: "schedules", page: "Schedules", perm: ["system:job:list"] },
              { path: "message-push", page: "MessagePush", perm: ["system:messagechannel:list"] },
              { path: "api-docs", page: "ApiDocs", perm: ["system:cache:list"] },
            ],
          },
          {
            path: "plugins",
            children: [
              { index: true, page: "PluginsOverview" },
              { path: "storage", page: "StorageSettings" },
              { path: "payments", page: "PaymentsStripe" },
              { path: "ai", page: "AiPluginEmpty" },
            ],
          },
          {
            path: "ai",
            children: [
              { path: "overview", page: "AiOverview" },
              { path: "providers", page: "Providers", perm: ["ai:provider:list"] },
              { path: "models", page: "Models", perm: ["ai:model:list"] },
              { path: "vector-stores", page: "VectorStores", perm: ["ai:vectorstore:list"] },
              { path: "prompts", page: "Prompts", perm: ["ai:prompt:list"] },
              { path: "terminology", page: "Terminology", perm: ["ai:term:list"] },
              { path: "knowledge-bases", page: "KnowledgeBases", perm: ["ai:knowledge:list"] },
              { path: "documents", page: "Documents", perm: ["ai:document:list"] },
              { path: "chunks", page: "Chunks", perm: ["ai:knowledge:list"] },
              { path: "datasets", page: "Datasets", perm: ["ai:dataset:list"] },
              { path: "skills", page: "Skills", perm: ["ai:skill:list"] },
              { path: "interface-skills", page: "InterfaceSkills", perm: ["ai:skill:list"] },
              { path: "mcp-servers", page: "McpServers", perm: ["ai:mcp:list"] },
              { path: "agents", page: "Agents", perm: ["ai:agent:list"] },
              { path: "workspace", page: "WorkspaceChat", perm: ["ai:chat:list"] },
              { path: "chat", page: "AiChat", perm: ["ai:chat:list"] },
              { path: "presentations", page: "Presentations", perm: ["ai:chat:list"] },
              { path: "code-review", page: "CodeReview", perm: ["ai:chat:list"] },
              { path: "copilot", page: "CopilotStates", perm: ["ai:chat:list"] },
              { path: "proofreading", page: "Proofreading", perm: ["ai:chat:list"] },
              { path: "proofreading/history", page: "ProofreadingHistory", perm: ["ai:chat:list"] },
              { path: "proofreading/lexicon", page: "Lexicon", perm: ["ai:chat:list"] },
              { path: "proofreading/rules", page: "Rules", perm: ["ai:chat:list"] },
              { path: "bidding", page: "Bidding", perm: ["ai:chat:list"] },
              { path: "image", page: "ImageGeneration", perm: ["ai:chat:list"] },
              { path: "video", page: "VideoGeneration", perm: ["ai:chat:list"] },
              { path: "audio", page: "AudioTts", perm: ["ai:chat:list"] },
              { path: "poster", page: "Poster", perm: ["ai:chat:list"] },
              { path: "ocr", page: "OcrTemplates", perm: ["ai:chat:list"] },
              { path: "safety", page: "SafetyCheck", perm: ["ai:chat:list"] },
              { path: "usage", page: "AiUsage", perm: ["ai:chat:list"] },
              { path: "usage-logs", page: "UsageLogs", perm: ["ai:billing:list"] },
              { path: "stats", page: "StatsDashboard", perm: ["ai:billing:list"] },
              { path: "billing", page: "BillingOverview", perm: ["ai:chat:list"] },
              { path: "labeling", page: "AiLabeling", perm: ["ai:chat:list"] },
              { path: "qa", page: "QaChat", perm: ["ai:chat:list"] },
              { path: "sql", page: "Chat2Sql", perm: ["ai:chat:list"] },
              { path: "sql/datasources", page: "DataSources", perm: ["ai:chat:list"] },
              { path: "sql/tables", page: "DataTables", perm: ["ai:chat:list"] },
              { path: "sql/reports", page: "DataReports", perm: ["ai:chat:list"] },
              { path: "linkage", page: "LinkageRules", perm: ["ai:linkage:list"] },
              { path: "office", page: "OfficeWorkspace", perm: ["ai:chat:list"] },
              { path: "office/reports", page: "AiReports", perm: ["ai:chat:list"] },
              { path: "office/inspection", page: "Inspection", perm: ["ai:chat:list"] },
              { path: "office/documents", page: "OfficialDocuments", perm: ["ai:chat:list"] },
              { path: "office/super-form", page: "SuperForm", perm: ["ai:chat:list"] },
              { path: "office/storage", page: "AssetStorage", perm: ["ai:chat:list"] },
              { path: "office/asset-storage", page: "AssetStorage", perm: ["ai:chat:list"] },
              { path: "office/resources", page: "ResourceManager", perm: ["ai:chat:list"] },
              { path: "office/resource-manager", page: "ResourceManager", perm: ["ai:chat:list"] },
              { path: "office/code-review", page: "CodeReview", perm: ["ai:chat:list"] },
              { path: "office/presentation", page: "Presentation", perm: ["ai:chat:list"] },
              { path: "office/bid", page: "BidDocument", perm: ["ai:chat:list"] },
              { path: "vision/mindmap", page: "AiMindMap", perm: ["ai:chat:list"] },
              { path: "vision/image", page: "AiImageGen", perm: ["ai:chat:list"] },
              { path: "vision/audio", page: "AiAudio", perm: ["ai:chat:list"] },
              { path: "vision/video", page: "AiVideo", perm: ["ai:chat:list"] },
              { path: "vision/poster", page: "AiPoster", perm: ["ai:chat:list"] },
              { path: "vision/security", page: "AiSecurity", perm: ["ai:chat:list"] },
              { path: "vision/ocr", page: "AiVision", perm: ["ai:chat:list"] },
              { path: "agents/canvas", page: "AgentCanvas", perm: ["ai:agent:list"] },
            ],
          },
          {
            path: "settings",
            children: [
              { path: "profile", page: "Profile" },
              { path: "security", page: "Security" },
              { path: "appearance", page: "Appearance" },
              { path: "language", page: "Language" },
              { path: "api-keys", page: "ApiKeys", perm: ["system:apikey:list"] },
              { path: "team", page: "Team" },
              { path: "notifications", page: "NotificationSettings" },
            ],
          },
          {
            path: "ops",
            children: [
              { path: "announcements", page: "OpsAnnouncements", perm: ["system:ops:list"] },
              { path: "popups", page: "OpsPopups", perm: ["system:ops:list"] },
              { path: "signin", page: "OpsSignIn", perm: ["system:ops:list"] },
              { path: "daily-tasks", page: "OpsDailyTasks", perm: ["system:ops:list"] },
              { path: "api-stats", page: "OpsApiStats", perm: ["system:ops:list"] },
              { path: "health-check", page: "OpsHealthCheck", perm: ["system:ops:list"] },
              { path: "scheduled-tasks", page: "OpsScheduledTasks", perm: ["system:ops:list"] },
              { path: "backups", page: "OpsBackups", perm: ["system:ops:list"] },
              { path: "big-screen", page: "OpsBigScreen", perm: ["system:dashboard:list"] },
            ],
          },
          {
            path: "compliance",
            children: [
              { path: "geo", page: "ComplianceGeo", perm: ["system:geo:list"] },
              {
                path: "responsible",
                page: "ComplianceResponsible",
                perm: ["system:responsible:list"],
              },
              { path: "aml", page: "ComplianceAml", perm: ["system:aml:list"] },
              { path: "channel", page: "ComplianceChannel", perm: ["system:channel:list"] },
              { path: "rate", page: "ComplianceRate", perm: ["system:rate:list"] },
              { path: "kyc", page: "ComplianceKyc", perm: ["system:kyc:list"] },
            ],
          },
          { path: "*", page: "NotFound" },
        ],
      },
    ],
  },
];
