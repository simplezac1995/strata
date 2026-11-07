// SPDX-License-Identifier: Apache-2.0
export type PageResult<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type MenuType = "directory" | "menu" | "button";

export type MenuDto = {
  id: string;
  parentId?: string | null;
  name: string;
  type: MenuType;
  path?: string | null;
  component?: string | null;
  permission?: string | null;
  sortOrder: number;
  visible: boolean;
  enabled: boolean;
  children: MenuDto[];
};

export type UserDto = {
  id: string;
  username: string;
  nickname?: string | null;
  email?: string | null;
  enabled: boolean;
  roleIds: string[];
  createdAt?: string | null;
  /** Whether the account has a confirmed TOTP second factor bound (backend `sys_user.totp_enabled`). */
  mfaEnabled?: boolean;
};

export type RoleDto = {
  id: string;
  code: string;
  name: string;
  enabled: boolean;
  menuIds: string[];
};

export type TokenResponse = {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserDto;
  menus: MenuDto[];
};

export type DeptDto = {
  id: string;
  parentId?: string | null;
  name: string;
  sortOrder: number;
  enabled: boolean;
  children: DeptDto[];
};

export type ProductDto = {
  id: string;
  name: string;
  sku: string;
  price: number;
  stock: number;
  description?: string | null;
  enabled: boolean;
};

export type OperationLogDto = {
  id: string;
  moduleName: string;
  actionName: string;
  operatorName?: string | null;
  detail?: string | null;
  createdAt: string;
};

export type AuditLogDto = {
  id: string;
  eventType: string;
  moduleName: string;
  actionName: string;
  resourceType?: string | null;
  resourceId?: string | null;
  operatorName?: string | null;
  ipAddress?: string | null;
  success: boolean;
  detail?: string | null;
  createdAt: string;
};

export type PostDto = {
  id: string;
  code: string;
  name: string;
  sortOrder: number;
  enabled: boolean;
};

export type DictTypeDto = {
  id: string;
  code: string;
  name: string;
  sortOrder: number;
  enabled: boolean;
};

export type DictDataDto = {
  id: string;
  typeId: string;
  value: string;
  label: string;
  sortOrder: number;
  enabled: boolean;
};

export type ParamDto = {
  id: string;
  key: string;
  value: string;
  type: string;
  remark?: string | null;
};

export type LoginLogDto = {
  id: string;
  username: string;
  ip?: string | null;
  location?: string | null;
  browser?: string | null;
  status: "success" | "fail";
  createdAt: string;
};

export type FileInfoDto = {
  key: string;
  bucket: string;
  size: number;
  contentType?: string | null;
  lastModified: string;
};

export type AiProviderDto = {
  id: string;
  code: string;
  name: string;
  type: string;
  baseUrl?: string | null;
  apiKey?: string | null;
  enabled: boolean;
};

export type AiModelDto = {
  id: string;
  providerId: string;
  code: string;
  name: string;
  type: string;
  enabled: boolean;
  /** Model context window; may be absent for providers/models that don't report it. */
  contextWindow?: number | null;
};

export type AiPromptDto = {
  id: string;
  code: string;
  name: string;
  template: string;
  enabled: boolean;
};

export type AiTermDto = {
  id: string;
  term: string;
  translation: string;
  notes?: string | null;
  enabled: boolean;
};

export type AiVectorStoreDto = {
  id: string;
  name: string;
  type: string;
  host?: string | null;
  dimensions: number;
  enabled: boolean;
};

export type AiKnowledgeDto = {
  id: string;
  name: string;
  description?: string | null;
  embeddingModel?: string | null;
  vectorStore?: string | null;
  chunkSize: number;
  chunkOverlap: number;
  enabled: boolean;
};

export type AiDocumentDto = {
  id: string;
  knowledgeId: string;
  name: string;
  sizeKb: number;
  status: string;
  chunks: number;
  enabled: boolean;
  createdAt: string;
};

export type AiSkillDto = {
  id: string;
  name: string;
  type: string;
  path?: string | null;
  description?: string | null;
  enabled: boolean;
};

export type AiMcpServerDto = {
  id: string;
  name: string;
  url: string;
  authType?: string | null;
  authToken?: string | null;
  enabled: boolean;
};

export type AiAgentDto = {
  id: string;
  name: string;
  model?: string | null;
  prompt?: string | null;
  enabled: boolean;
};

export type AiDatasetDto = {
  id: string;
  name: string;
  description?: string | null;
  source: string;
  records: number;
  enabled: boolean;
};

/* ─── P4 domains ────────────────────────────────────────────── */

export type TenantDto = {
  id: string;
  code: string;
  name: string;
  domain?: string | null;
  status: string;
  plan: string;
};

export type OauthClientDto = {
  id: string;
  clientId: string;
  name: string;
  /** Masked by the backend —never returned in responses. */
  clientSecret?: string | null;
  scopes?: string | null;
  enabled: boolean;
};

export type SensitiveWordDto = { id: string; word: string; enabled: boolean };

export type RegionDto = {
  id: string;
  code: string;
  name: string;
  level: number;
  parentId?: string | null;
};

export type WebhookDto = {
  id: string;
  name: string;
  url: string;
  secret?: string | null;
  enabled: boolean;
};

export type EmailTemplateDto = {
  id: string;
  code: string;
  name: string;
  subject: string;
  body?: string | null;
  enabled: boolean;
};

export type AnnouncementDto = {
  id: string;
  title: string;
  content?: string | null;
  enabled: boolean;
  createdAt: string;
  updatedAt?: string | null;
};

export type PaymentDto = {
  id: string;
  orderNo: string;
  amountCents: number;
  currency: string;
  status: string;
};

export type PaymentCreatedDto = {
  id: string;
  orderNo: string;
  checkoutUrl: string;
};

export type PluginStatusDto = {
  storageLocal: boolean;
  paymentStripe: boolean;
};

export type I18nEntryDto = {
  id: string;
  messageKey: string;
  zh: string | null;
  en: string | null;
  updatedBy: string | null;
  updatedAt: string;
};

export type JobDto = {
  id: string;
  name: string;
  jobGroup: string;
  cron: string;
  status: string;
  lastRun: string | null;
  nextRun: string | null;
  enabled: boolean;
};

export type MessageChannelDto = {
  id: string;
  channelType: string;
  configJson: string | null;
  enabled: boolean;
};

export type CacheKeyDto = {
  key: string;
  type: string;
  ttlSeconds: number | null;
};

export type CacheStatusDto = {
  source: "redis" | "local" | "none";
  stub: boolean;
  redisVersion?: string | null;
  usedMemory?: string | null;
  totalKeys?: number | null;
  connectedClients?: number | null;
  hitRate?: number | null;
};

export type ProcessDefinitionDto = {
  key: string;
  name: string;
  version: number;
};

export type PlatformCapabilitiesDto = {
  jobSchedulerStub: boolean;
  workflowRunnerStub: boolean;
  cacheLocalDemo: boolean;
  rowLevelTenantEnabled: boolean;
  workflowEngineAvailable: boolean;
  edition: string;
};

export type ApiKeyDto = {
  id: string;
  name: string;
  keyPrefix: string;
  scopes: string | null;
  enabled: boolean;
  lastUsedAt: string | null;
  createdAt: string;
};

export type ApiKeyCreatedDto = {
  id: string;
  name: string;
  keyPrefix: string;
  plaintextKey: string;
  scopes: string | null;
  enabled: boolean;
  createdAt: string;
};

export type NotificationPrefDto = {
  emailEnabled: boolean;
  inAppEnabled: boolean;
  marketingEnabled: boolean;
  securityEnabled: boolean;
};

export type AiChatSessionDto = {
  id: string;
  title: string;
  modelId: string | null;
  userId: string;
  createdAt: string;
};

export type AiChatMessageDto = {
  id: string;
  sessionId: string;
  role: string;
  content: string;
  createdAt: string;
};

export type WorkflowDto = {
  id: string;
  name: string;
  description: string | null;
  definitionJson: string | null;
  processDefinitionKey: string | null;
  status: string;
  enabled: boolean;
  updatedAt: string;
};

export type WorkflowRunDto = {
  id: string;
  workflowId: string;
  status: string;
  startedAt?: string | null;
  finishedAt?: string | null;
  errorMessage?: string | null;
};

export type BlogPostDto = {
  id: string;
  title: string;
  summary?: string | null;
  content?: string | null;
  published: boolean;
  publishedAt?: string | null;
  createdAt: string;
  updatedAt?: string | null;
};
