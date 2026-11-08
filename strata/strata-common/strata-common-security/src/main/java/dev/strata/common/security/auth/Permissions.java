// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.security.auth;

/**
 * The authority vocabulary of the admin API, in one place.
 *
 * <p>Every {@code @PreAuthorize} used to repeat the literal twice — once in the annotation and once in the RBAC seed
 * that grants it — so a typo produced a silently unreachable endpoint instead of a compile error. Each constant is the
 * complete Spring Security expression ({@code hasAuthority('...')}), which is what the annotation accepts.
 *
 * <p>Grouped by the leading segment of the authority. When adding an endpoint, add the constant here and grant it in
 * the RBAC seed with the same name; never write a raw authority literal in an annotation.
 */
public final class Permissions {

    private Permissions() {}

    // AI
    public static final String AI_AGENT_CREATE = "hasAuthority('ai:agent:create')";
    public static final String AI_AGENT_DELETE = "hasAuthority('ai:agent:delete')";
    public static final String AI_AGENT_LIST = "hasAuthority('ai:agent:list')";
    public static final String AI_AGENT_UPDATE = "hasAuthority('ai:agent:update')";
    public static final String AI_BILLING_LIST = "hasAuthority('ai:billing:list')";
    public static final String AI_CHAT_CREATE = "hasAuthority('ai:chat:create')";
    public static final String AI_CHAT_DELETE = "hasAuthority('ai:chat:delete')";
    public static final String AI_CHAT_LIST = "hasAuthority('ai:chat:list')";
    public static final String AI_DATASET_CREATE = "hasAuthority('ai:dataset:create')";
    public static final String AI_DATASET_DELETE = "hasAuthority('ai:dataset:delete')";
    public static final String AI_DATASET_LIST = "hasAuthority('ai:dataset:list')";
    public static final String AI_DATASET_UPDATE = "hasAuthority('ai:dataset:update')";
    public static final String AI_DOCUMENT_CREATE = "hasAuthority('ai:document:create')";
    public static final String AI_DOCUMENT_DELETE = "hasAuthority('ai:document:delete')";
    public static final String AI_DOCUMENT_LIST = "hasAuthority('ai:document:list')";
    public static final String AI_DOCUMENT_UPDATE = "hasAuthority('ai:document:update')";
    public static final String AI_KNOWLEDGE_CREATE = "hasAuthority('ai:knowledge:create')";
    public static final String AI_KNOWLEDGE_DELETE = "hasAuthority('ai:knowledge:delete')";
    public static final String AI_KNOWLEDGE_LIST = "hasAuthority('ai:knowledge:list')";
    public static final String AI_KNOWLEDGE_UPDATE = "hasAuthority('ai:knowledge:update')";
    public static final String AI_LABEL_CREATE = "hasAuthority('ai:label:create')";
    public static final String AI_LABEL_DELETE = "hasAuthority('ai:label:delete')";
    public static final String AI_LABEL_LIST = "hasAuthority('ai:label:list')";
    public static final String AI_LABEL_UPDATE = "hasAuthority('ai:label:update')";
    public static final String AI_LINKAGE_CREATE = "hasAuthority('ai:linkage:create')";
    public static final String AI_LINKAGE_DELETE = "hasAuthority('ai:linkage:delete')";
    public static final String AI_LINKAGE_LIST = "hasAuthority('ai:linkage:list')";
    public static final String AI_LINKAGE_UPDATE = "hasAuthority('ai:linkage:update')";
    public static final String AI_MCP_CREATE = "hasAuthority('ai:mcp:create')";
    public static final String AI_MCP_DELETE = "hasAuthority('ai:mcp:delete')";
    public static final String AI_MCP_LIST = "hasAuthority('ai:mcp:list')";
    public static final String AI_MCP_UPDATE = "hasAuthority('ai:mcp:update')";
    public static final String AI_MODEL_CREATE = "hasAuthority('ai:model:create')";
    public static final String AI_MODEL_DELETE = "hasAuthority('ai:model:delete')";
    public static final String AI_MODEL_LIST = "hasAuthority('ai:model:list')";
    public static final String AI_MODEL_UPDATE = "hasAuthority('ai:model:update')";
    public static final String AI_PROMPT_CREATE = "hasAuthority('ai:prompt:create')";
    public static final String AI_PROMPT_DELETE = "hasAuthority('ai:prompt:delete')";
    public static final String AI_PROMPT_LIST = "hasAuthority('ai:prompt:list')";
    public static final String AI_PROMPT_UPDATE = "hasAuthority('ai:prompt:update')";
    public static final String AI_PROVIDER_CREATE = "hasAuthority('ai:provider:create')";
    public static final String AI_PROVIDER_DELETE = "hasAuthority('ai:provider:delete')";
    public static final String AI_PROVIDER_LIST = "hasAuthority('ai:provider:list')";
    public static final String AI_PROVIDER_UPDATE = "hasAuthority('ai:provider:update')";
    public static final String AI_SKILL_CREATE = "hasAuthority('ai:skill:create')";
    public static final String AI_SKILL_DELETE = "hasAuthority('ai:skill:delete')";
    public static final String AI_SKILL_LIST = "hasAuthority('ai:skill:list')";
    public static final String AI_SKILL_UPDATE = "hasAuthority('ai:skill:update')";
    public static final String AI_TERM_CREATE = "hasAuthority('ai:term:create')";
    public static final String AI_TERM_DELETE = "hasAuthority('ai:term:delete')";
    public static final String AI_TERM_LIST = "hasAuthority('ai:term:list')";
    public static final String AI_TERM_UPDATE = "hasAuthority('ai:term:update')";
    public static final String AI_TOOL_CREATE = "hasAuthority('ai:tool:create')";
    public static final String AI_TOOL_LIST = "hasAuthority('ai:tool:list')";
    public static final String AI_VECTORSTORE_CREATE = "hasAuthority('ai:vectorstore:create')";
    public static final String AI_VECTORSTORE_DELETE = "hasAuthority('ai:vectorstore:delete')";
    public static final String AI_VECTORSTORE_LIST = "hasAuthority('ai:vectorstore:list')";
    public static final String AI_VECTORSTORE_UPDATE = "hasAuthority('ai:vectorstore:update')";

    // AUTH
    public static final String AUTHENTICATED = "isAuthenticated()";

    // DEMO
    public static final String DEMO_PAYMENT_CREATE = "hasAuthority('demo:payment:create')";
    public static final String DEMO_PAYMENT_LIST = "hasAuthority('demo:payment:list')";
    public static final String DEMO_PRODUCT_CREATE = "hasAuthority('demo:product:create')";
    public static final String DEMO_PRODUCT_DELETE = "hasAuthority('demo:product:delete')";
    public static final String DEMO_PRODUCT_LIST = "hasAuthority('demo:product:list')";
    public static final String DEMO_PRODUCT_UPDATE = "hasAuthority('demo:product:update')";

    // GENERATOR
    public static final String GENERATOR_CREATE = "hasAuthority('generator:create')";
    public static final String GENERATOR_LIST = "hasAuthority('generator:list')";

    // OPS
    public static final String OPS_ANNOUNCEMENT_CREATE = "hasAuthority('ops:announcement:create')";
    public static final String OPS_ANNOUNCEMENT_DELETE = "hasAuthority('ops:announcement:delete')";
    public static final String OPS_ANNOUNCEMENT_LIST = "hasAuthority('ops:announcement:list')";
    public static final String OPS_ANNOUNCEMENT_UPDATE = "hasAuthority('ops:announcement:update')";
    public static final String OPS_API_STAT_LIST = "hasAuthority('ops:api-stat:list')";
    public static final String OPS_BACKUP_CREATE = "hasAuthority('ops:backup:create')";
    public static final String OPS_BACKUP_DELETE = "hasAuthority('ops:backup:delete')";
    public static final String OPS_BACKUP_LIST = "hasAuthority('ops:backup:list')";
    public static final String OPS_BACKUP_UPDATE = "hasAuthority('ops:backup:update')";
    public static final String OPS_DAILY_TASK_CREATE = "hasAuthority('ops:daily-task:create')";
    public static final String OPS_DAILY_TASK_DELETE = "hasAuthority('ops:daily-task:delete')";
    public static final String OPS_DAILY_TASK_LIST = "hasAuthority('ops:daily-task:list')";
    public static final String OPS_DAILY_TASK_UPDATE = "hasAuthority('ops:daily-task:update')";
    public static final String OPS_HEALTH_LIST = "hasAuthority('ops:health:list')";
    public static final String OPS_POPUP_CREATE = "hasAuthority('ops:popup:create')";
    public static final String OPS_POPUP_DELETE = "hasAuthority('ops:popup:delete')";
    public static final String OPS_POPUP_LIST = "hasAuthority('ops:popup:list')";
    public static final String OPS_POPUP_UPDATE = "hasAuthority('ops:popup:update')";
    public static final String OPS_SCHEDULED_TASK_LIST = "hasAuthority('ops:scheduled-task:list')";
    public static final String OPS_SCHEDULED_TASK_UPDATE = "hasAuthority('ops:scheduled-task:update')";
    public static final String OPS_SIGNIN_LIST = "hasAuthority('ops:signin:list')";
    public static final String OPS_SIGNIN_UPDATE = "hasAuthority('ops:signin:update')";

    // SYSTEM
    public static final String SYSTEM_ANNOUNCEMENT_CREATE = "hasAuthority('system:announcement:create')";
    public static final String SYSTEM_ANNOUNCEMENT_DELETE = "hasAuthority('system:announcement:delete')";
    public static final String SYSTEM_ANNOUNCEMENT_LIST = "hasAuthority('system:announcement:list')";
    public static final String SYSTEM_ANNOUNCEMENT_UPDATE = "hasAuthority('system:announcement:update')";
    public static final String SYSTEM_APIKEY_CREATE = "hasAuthority('system:apikey:create')";
    public static final String SYSTEM_APIKEY_DELETE = "hasAuthority('system:apikey:delete')";
    public static final String SYSTEM_APIKEY_LIST = "hasAuthority('system:apikey:list')";
    public static final String SYSTEM_APIKEY_UPDATE = "hasAuthority('system:apikey:update')";
    public static final String SYSTEM_AUDIT_LIST = "hasAuthority('system:audit:list')";
    public static final String SYSTEM_AUDIT_UPDATE = "hasAuthority('system:audit:update')";
    public static final String SYSTEM_AUDITLOG_LIST = "hasAuthority('system:auditlog:list')";
    public static final String SYSTEM_CACHE_DELETE = "hasAuthority('system:cache:delete')";
    public static final String SYSTEM_CACHE_LIST = "hasAuthority('system:cache:list')";
    public static final String SYSTEM_CLIENT_CREATE = "hasAuthority('system:client:create')";
    public static final String SYSTEM_CLIENT_DELETE = "hasAuthority('system:client:delete')";
    public static final String SYSTEM_CLIENT_LIST = "hasAuthority('system:client:list')";
    public static final String SYSTEM_CLIENT_UPDATE = "hasAuthority('system:client:update')";
    public static final String SYSTEM_CS_CREATE = "hasAuthority('system:cs:create')";
    public static final String SYSTEM_CS_LIST = "hasAuthority('system:cs:list')";
    public static final String SYSTEM_DASHBOARD_LIST = "hasAuthority('system:dashboard:list')";
    public static final String SYSTEM_DEPT_CREATE = "hasAuthority('system:dept:create')";
    public static final String SYSTEM_DEPT_DELETE = "hasAuthority('system:dept:delete')";
    public static final String SYSTEM_DEPT_LIST = "hasAuthority('system:dept:list')";
    public static final String SYSTEM_DEPT_UPDATE = "hasAuthority('system:dept:update')";
    public static final String SYSTEM_DICT_CREATE = "hasAuthority('system:dict:create')";
    public static final String SYSTEM_DICT_DELETE = "hasAuthority('system:dict:delete')";
    public static final String SYSTEM_DICT_LIST = "hasAuthority('system:dict:list')";
    public static final String SYSTEM_DICT_UPDATE = "hasAuthority('system:dict:update')";
    public static final String SYSTEM_EMAILTEMPLATE_CREATE = "hasAuthority('system:emailtemplate:create')";
    public static final String SYSTEM_EMAILTEMPLATE_DELETE = "hasAuthority('system:emailtemplate:delete')";
    public static final String SYSTEM_EMAILTEMPLATE_LIST = "hasAuthority('system:emailtemplate:list')";
    public static final String SYSTEM_EMAILTEMPLATE_UPDATE = "hasAuthority('system:emailtemplate:update')";
    public static final String SYSTEM_FILE_CREATE = "hasAuthority('system:file:create')";
    public static final String SYSTEM_FILE_DELETE = "hasAuthority('system:file:delete')";
    public static final String SYSTEM_FILE_LIST = "hasAuthority('system:file:list')";
    public static final String SYSTEM_FRONTIERERROR_CREATE = "hasAuthority('system:frontiererror:create')";
    public static final String SYSTEM_I18N_CREATE = "hasAuthority('system:i18n:create')";
    public static final String SYSTEM_I18N_DELETE = "hasAuthority('system:i18n:delete')";
    public static final String SYSTEM_I18N_LIST = "hasAuthority('system:i18n:list')";
    public static final String SYSTEM_I18N_UPDATE = "hasAuthority('system:i18n:update')";
    public static final String SYSTEM_JOB_CREATE = "hasAuthority('system:job:create')";
    public static final String SYSTEM_JOB_DELETE = "hasAuthority('system:job:delete')";
    public static final String SYSTEM_JOB_LIST = "hasAuthority('system:job:list')";
    public static final String SYSTEM_JOB_UPDATE = "hasAuthority('system:job:update')";
    public static final String SYSTEM_LOGINLOG_LIST = "hasAuthority('system:loginlog:list')";
    public static final String SYSTEM_MENU_CREATE = "hasAuthority('system:menu:create')";
    public static final String SYSTEM_MENU_DELETE = "hasAuthority('system:menu:delete')";
    public static final String SYSTEM_MENU_LIST = "hasAuthority('system:menu:list')";
    public static final String SYSTEM_MENU_UPDATE = "hasAuthority('system:menu:update')";
    public static final String SYSTEM_MESSAGECHANNEL_LIST = "hasAuthority('system:messagechannel:list')";
    public static final String SYSTEM_MESSAGECHANNEL_UPDATE = "hasAuthority('system:messagechannel:update')";
    public static final String SYSTEM_MONITOR_LIST = "hasAuthority('system:monitor:list')";
    public static final String SYSTEM_NOTIFICATION_CREATE = "hasAuthority('system:notification:create')";
    public static final String SYSTEM_NOTIFICATION_LIST = "hasAuthority('system:notification:list')";
    public static final String SYSTEM_OPERATIONLOG_LIST = "hasAuthority('system:operationlog:list')";
    public static final String SYSTEM_PARAM_CREATE = "hasAuthority('system:param:create')";
    public static final String SYSTEM_PARAM_DELETE = "hasAuthority('system:param:delete')";
    public static final String SYSTEM_PARAM_LIST = "hasAuthority('system:param:list')";
    public static final String SYSTEM_PARAM_UPDATE = "hasAuthority('system:param:update')";
    public static final String SYSTEM_POST_CREATE = "hasAuthority('system:post:create')";
    public static final String SYSTEM_POST_DELETE = "hasAuthority('system:post:delete')";
    public static final String SYSTEM_POST_LIST = "hasAuthority('system:post:list')";
    public static final String SYSTEM_POST_UPDATE = "hasAuthority('system:post:update')";
    public static final String SYSTEM_RECONCILIATION_CREATE = "hasAuthority('system:reconciliation:create')";
    public static final String SYSTEM_RECONCILIATION_LIST = "hasAuthority('system:reconciliation:list')";
    public static final String SYSTEM_REGION_CREATE = "hasAuthority('system:region:create')";
    public static final String SYSTEM_REGION_DELETE = "hasAuthority('system:region:delete')";
    public static final String SYSTEM_REGION_LIST = "hasAuthority('system:region:list')";
    public static final String SYSTEM_REGION_UPDATE = "hasAuthority('system:region:update')";
    public static final String SYSTEM_RISK_CREATE = "hasAuthority('system:risk:create')";
    public static final String SYSTEM_RISK_DELETE = "hasAuthority('system:risk:delete')";
    public static final String SYSTEM_RISK_LIST = "hasAuthority('system:risk:list')";
    public static final String SYSTEM_RISK_UPDATE = "hasAuthority('system:risk:update')";
    public static final String SYSTEM_ROLE_CREATE = "hasAuthority('system:role:create')";
    public static final String SYSTEM_ROLE_DELETE = "hasAuthority('system:role:delete')";
    public static final String SYSTEM_ROLE_LIST = "hasAuthority('system:role:list')";
    public static final String SYSTEM_ROLE_UPDATE = "hasAuthority('system:role:update')";
    public static final String SYSTEM_SEARCH_LIST = "hasAuthority('system:search:list')";
    public static final String SYSTEM_SENSITIVEWORD_CREATE = "hasAuthority('system:sensitiveword:create')";
    public static final String SYSTEM_SENSITIVEWORD_DELETE = "hasAuthority('system:sensitiveword:delete')";
    public static final String SYSTEM_SENSITIVEWORD_LIST = "hasAuthority('system:sensitiveword:list')";
    public static final String SYSTEM_SENSITIVEWORD_UPDATE = "hasAuthority('system:sensitiveword:update')";
    public static final String SYSTEM_SITESETTING_LIST = "hasAuthority('system:sitesetting:list')";
    public static final String SYSTEM_SITESETTING_UPDATE = "hasAuthority('system:sitesetting:update')";
    public static final String SYSTEM_STATS_LIST = "hasAuthority('system:stats:list')";
    public static final String SYSTEM_TENANT_CREATE = "hasAuthority('system:tenant:create')";
    public static final String SYSTEM_TENANT_DELETE = "hasAuthority('system:tenant:delete')";
    public static final String SYSTEM_TENANT_LIST = "hasAuthority('system:tenant:list')";
    public static final String SYSTEM_TENANT_UPDATE = "hasAuthority('system:tenant:update')";
    public static final String SYSTEM_USER_CREATE = "hasAuthority('system:user:create')";
    public static final String SYSTEM_USER_DELETE = "hasAuthority('system:user:delete')";
    public static final String SYSTEM_USER_LIST = "hasAuthority('system:user:list')";
    public static final String SYSTEM_USER_UPDATE = "hasAuthority('system:user:update')";
    public static final String SYSTEM_WEBHOOK_CREATE = "hasAuthority('system:webhook:create')";
    public static final String SYSTEM_WEBHOOK_DELETE = "hasAuthority('system:webhook:delete')";
    public static final String SYSTEM_WEBHOOK_LIST = "hasAuthority('system:webhook:list')";
    public static final String SYSTEM_WEBHOOK_UPDATE = "hasAuthority('system:webhook:update')";
}
