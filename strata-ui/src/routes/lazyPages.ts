// SPDX-License-Identifier: Apache-2.0
import { lazyPage, namedPage } from "@/shared/pageKit/lazyPage";

// Auth
export const Login = lazyPage(() => import("@/pages/auth/Login"));
export const ForgotPassword = lazyPage(() => import("@/pages/auth/ForgotPassword"));
export const ResetPassword = lazyPage(() => import("@/pages/auth/ResetPassword"));
export const Register = lazyPage(() => import("@/pages/auth/Register"));
export const VerifyEmail = lazyPage(() => import("@/pages/auth/VerifyEmail"));
export const SsoCallback = lazyPage(() => import("@/pages/auth/SsoCallback"));

// Core pages
export const Dashboard = lazyPage(() => import("@/pages/Dashboard"));
export const Foundations = lazyPage(() => import("@/pages/Foundations"));
export const ComponentGallery = lazyPage(() => import("@/pages/ComponentGallery"));
export const Cover = lazyPage(() => import("@/pages/Cover"));
export const Notifications = lazyPage(() => import("@/pages/Notifications"));
export const StatesBoard = namedPage(() => import("@/pages/States"), "StatesBoard");
export const NotFound = lazyPage(() => import("@/pages/NotFound"));
export const Error403 = namedPage(() => import("@/pages/ErrorPages"), "Error403");
export const Error500 = namedPage(() => import("@/pages/ErrorPages"), "Error500");

// System
export const Users = lazyPage(() => import("@/pages/system/Users"));
export const Roles = lazyPage(() => import("@/pages/system/Roles"));
export const Menus = lazyPage(() => import("@/pages/system/Menus"));
export const Departments = lazyPage(() => import("@/pages/system/Departments"));
export const Posts = lazyPage(() => import("@/pages/system/Posts"));
export const Dictionaries = lazyPage(() => import("@/pages/system/Dictionaries"));
export const Parameters = lazyPage(() => import("@/pages/system/Parameters"));
export const Files = lazyPage(() => import("@/pages/system/Files"));
export const I18n = lazyPage(() => import("@/pages/system/I18n"));
export const SiteSettings = lazyPage(() => import("@/pages/system/SiteSettings"));
export const UserDetail = lazyPage(() => import("@/pages/system/UserDetail"));
export const Webhooks = lazyPage(() => import("@/pages/system/Webhooks"));
export const EmailTemplates = lazyPage(() => import("@/pages/system/EmailTemplates"));
export const Announcements = lazyPage(() => import("@/pages/system/Announcements"));
export const OperationLogs = namedPage(
  () => import("@/pages/system/logs/OperationLogs"),
  "OperationLogs",
);
export const LoginLogs = namedPage(() => import("@/pages/system/logs/LoginLogs"), "LoginLogs");
export const AuditLogs = namedPage(() => import("@/pages/system/logs/AuditLogs"), "AuditLogs");
export const Tenants = namedPage(() => import("@/pages/system/simple/Tenants"), "Tenants");
export const Clients = namedPage(() => import("@/pages/system/simple/Clients"), "Clients");
export const Sessions = namedPage(() => import("@/pages/system/simple/Sessions"), "Sessions");
export const SensitiveWords = namedPage(
  () => import("@/pages/system/simple/SensitiveWords"),
  "SensitiveWords",
);
export const Regions = namedPage(() => import("@/pages/system/simple/Regions"), "Regions");
export const Monitor = namedPage(() => import("@/pages/system/Monitor"), "Monitor");
export const CsWorkbench = namedPage(() => import("@/pages/system/CsWorkbench"), "CsWorkbench");

// Demo
export const Products = lazyPage(() => import("@/pages/demo/Products"));

// Tools
export const CodeGenerator = namedPage(
  () => import("@/pages/tools/parts/CodeGenerator"),
  "CodeGenerator",
);
export const CacheMonitor = namedPage(
  () => import("@/pages/tools/parts/CacheMonitor"),
  "CacheMonitor",
);
export const Schedules = namedPage(() => import("@/pages/tools/parts/Schedules"), "Schedules");
export const MessagePush = namedPage(
  () => import("@/pages/tools/parts/MessagePush"),
  "MessagePush",
);
export const ApiDocs = namedPage(() => import("@/pages/tools/ApiDocs"), "ApiDocs");

// Plugins
export const PluginsOverview = namedPage(
  () => import("@/pages/plugins/parts/PluginsOverview"),
  "PluginsOverview",
);
export const StorageSettings = namedPage(
  () => import("@/pages/plugins/parts/StorageSettings"),
  "StorageSettings",
);
export const PaymentsStripe = namedPage(
  () => import("@/pages/plugins/parts/PaymentsStripe"),
  "PaymentsStripe",
);
export const AiPluginEmpty = namedPage(
  () => import("@/pages/plugins/parts/AiPluginEmpty"),
  "AiPluginEmpty",
);

// Settings
export const Profile = namedPage(() => import("@/pages/settings/parts/Profile"), "Profile");
export const Security = namedPage(() => import("@/pages/settings/parts/Security"), "Security");
export const Appearance = namedPage(
  () => import("@/pages/settings/parts/Appearance"),
  "Appearance",
);
export const Language = namedPage(() => import("@/pages/settings/parts/Language"), "Language");
export const ApiKeys = lazyPage(() => import("@/pages/settings/ApiKeys"));
export const Team = lazyPage(() => import("@/pages/settings/Team"));
export const NotificationSettings = lazyPage(() => import("@/pages/settings/NotificationSettings"));

// AI
export const AiOverview = namedPage(() => import("@/pages/ai/config/AiOverview"), "AiOverview");
export const Providers = namedPage(() => import("@/pages/ai/config/Providers"), "Providers");
export const Models = namedPage(() => import("@/pages/ai/config/Models"), "Models");
export const VectorStores = namedPage(
  () => import("@/pages/ai/config/VectorStores"),
  "VectorStores",
);
export const Prompts = namedPage(() => import("@/pages/ai/config/Prompts"), "Prompts");
export const Terminology = namedPage(() => import("@/pages/ai/config/Terminology"), "Terminology");
export const KnowledgeBases = namedPage(
  () => import("@/pages/ai/knowledge/KnowledgeBases"),
  "KnowledgeBases",
);
export const Documents = namedPage(() => import("@/pages/ai/knowledge/Documents"), "Documents");
export const Chunks = namedPage(() => import("@/pages/ai/knowledge/Chunks"), "Chunks");
export const Datasets = namedPage(() => import("@/pages/ai/knowledge/Datasets"), "Datasets");
export const Skills = namedPage(() => import("@/pages/ai/skills/Skills"), "Skills");
export const InterfaceSkills = namedPage(
  () => import("@/pages/ai/skills/InterfaceSkills"),
  "InterfaceSkills",
);
export const McpServers = namedPage(() => import("@/pages/ai/skills/McpServers"), "McpServers");
export const Agents = namedPage(() => import("@/pages/ai/agents/Agents"), "Agents");
export const AiChat = namedPage(() => import("@/pages/ai/workspace/AiChat"), "AiChat");
export const Proofreading = namedPage(
  () => import("@/pages/ai/proofreading/ProofreadingEditor"),
  "ProofreadingEditor",
);
export const ImageGeneration = namedPage(
  () => import("@/pages/ai/workspace/ImageGeneration"),
  "ImageGeneration",
);
export const AudioTts = namedPage(() => import("@/pages/ai/workspace/AudioTts"), "AudioTts");
export const SafetyCheck = namedPage(
  () => import("@/pages/ai/workspace/SafetyCheck"),
  "SafetyCheck",
);
export const AiUsage = namedPage(() => import("@/pages/ai/workspace/AiUsage"), "AiUsage");
export const UsageLogs = namedPage(() => import("@/pages/ai/workspace/UsageLogs"), "UsageLogs");
export const StatsDashboard = namedPage(
  () => import("@/pages/ai/workspace/StatsDashboard"),
  "StatsDashboard",
);
export const AiBilling = namedPage(() => import("@/pages/ai/workspace/AiBilling"), "AiBilling");
export const AiLabeling = namedPage(() => import("@/pages/ai/workspace/AiLabeling"), "AiLabeling");
export const WorkspaceChat = namedPage(
  () => import("@/pages/ai/assistant/WorkspaceChat"),
  "WorkspaceChat",
);
export const Presentations = namedPage(
  () => import("@/pages/ai/assistant/Presentations"),
  "Presentations",
);
export const CodeReview = namedPage(() => import("@/pages/ai/assistant/CodeReview"), "CodeReview");
export const CopilotStates = namedPage(
  () => import("@/pages/ai/assistant/CopilotStates"),
  "CopilotStates",
);
export const ProofreadingHistory = namedPage(
  () => import("@/pages/ai/proofreading/ProofreadingHistory"),
  "ProofreadingHistory",
);
export const Lexicon = namedPage(
  () => import("@/pages/ai/proofreading/ProofreadingLexicon"),
  "ProofreadingLexicon",
);
export const Rules = namedPage(
  () => import("@/pages/ai/proofreading/ProofreadingRules"),
  "ProofreadingRules",
);
export const Bidding = namedPage(() => import("@/pages/ai/more/Bidding"), "Bidding");
export const VideoGeneration = namedPage(
  () => import("@/pages/ai/more/VideoGeneration"),
  "VideoGeneration",
);
export const Poster = namedPage(() => import("@/pages/ai/more/Poster"), "Poster");
export const OcrTemplates = namedPage(() => import("@/pages/ai/more/OcrTemplates"), "OcrTemplates");
export const QaChat = namedPage(() => import("@/pages/ai/qa/QaChat"), "QaChat");
export const Chat2Sql = namedPage(() => import("@/pages/ai/sql/Chat2Sql"), "Chat2Sql");
export const DataSources = namedPage(() => import("@/pages/ai/sql/DataSources"), "DataSources");
export const DataTables = namedPage(() => import("@/pages/ai/sql/DataTables"), "DataTables");
export const DataReports = namedPage(() => import("@/pages/ai/sql/DataReports"), "DataReports");
export const LinkageRules = namedPage(
  () => import("@/pages/ai/linkage/LinkageRules"),
  "LinkageRules",
);
export const OfficeWorkspace = namedPage(
  () => import("@/pages/ai/office/OfficeWorkspace"),
  "OfficeWorkspace",
);
export const AiReports = namedPage(() => import("@/pages/ai/office/AiReports"), "AiReports");
export const Inspection = namedPage(() => import("@/pages/ai/office/Inspection"), "Inspection");
export const OfficialDocuments = namedPage(
  () => import("@/pages/ai/office/OfficialDocuments"),
  "OfficialDocuments",
);
export const SuperForm = namedPage(() => import("@/pages/ai/office/SuperForm"), "SuperForm");
export const AssetStorage = namedPage(
  () => import("@/pages/ai/office/AssetStorage"),
  "AssetStorage",
);
export const ResourceManager = namedPage(
  () => import("@/pages/ai/office/ResourceManager"),
  "ResourceManager",
);
export const Presentation = namedPage(
  () => import("@/pages/ai/office/Presentation"),
  "Presentation",
);
export const BidDocument = namedPage(() => import("@/pages/ai/office/BidDocument"), "BidDocument");
export const AiMindMap = namedPage(() => import("@/pages/ai/vision/AiMindMap"), "AiMindMap");
export const AiVision = namedPage(() => import("@/pages/ai/vision/AiVision"), "AiVision");
export const AiImageGen = namedPage(() => import("@/pages/ai/vision/AiImageGen"), "AiImageGen");
export const AiAudio = namedPage(() => import("@/pages/ai/vision/AiAudio"), "AiAudio");
export const AiVideo = namedPage(() => import("@/pages/ai/vision/AiVideo"), "AiVideo");
export const AiPoster = namedPage(() => import("@/pages/ai/vision/AiPoster"), "AiPoster");
export const AiSecurity = namedPage(() => import("@/pages/ai/vision/AiSecurity"), "AiSecurity");
export const AgentCanvas = namedPage(() => import("@/pages/ai/agents/AgentCanvas"), "AgentCanvas");
export const BillingOverview = namedPage(
  () => import("@/pages/ai/billing/BillingOverview"),
  "BillingOverview",
);

// Support

// System (additional)
export const AuditChanges = lazyPage(() => import("@/pages/system/AuditChanges"));

// Ops
export const OpsAnnouncements = lazyPage(() => import("@/pages/ops/Announcements"));
export const OpsPopups = lazyPage(() => import("@/pages/ops/Popups"));
export const OpsSignIn = lazyPage(() => import("@/pages/ops/SignIn"));
export const OpsDailyTasks = lazyPage(() => import("@/pages/ops/DailyTasks"));
export const OpsApiStats = lazyPage(() => import("@/pages/ops/ApiStats"));
export const OpsHealthCheck = lazyPage(() => import("@/pages/ops/HealthCheck"));
export const OpsScheduledTasks = lazyPage(() => import("@/pages/ops/ScheduledTasks"));
export const OpsBackups = lazyPage(() => import("@/pages/ops/Backups"));
export const OpsBigScreen = lazyPage(() => import("@/pages/ops/BigScreen"));
