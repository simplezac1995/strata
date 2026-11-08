// SPDX-License-Identifier: Apache-2.0
import { create } from "zustand";
import { useCallback } from "react";
import { zh } from "@/shared/i18n/zh";
import { en } from "@/shared/i18n/en";

export type Lang = "zh" | "en";

/** Backend error detail (English) -> short key lookup. */
export const errorKeyMap: Record<string, string> = {
  "Request failed": "error.request.failed",
  "Request failed with status code 502": "error.network.unavailable",
  "Request failed with status code 503": "error.network.unavailable",
  "Request failed with status code 504": "error.network.unavailable",
  "Network Error": "error.network.offline",
  "Bad request": "error.bad.request",
  Unauthorized: "error.unauthorized",
  Forbidden: "error.forbidden",
  "Resource not found": "error.resource.not.found",
  Conflict: "error.conflict",
  "Too many requests": "error.too.many.requests",
  "Internal server error": "error.internal.server.error",
  "Validation failed": "error.validation.failed",
  "File is empty": "error.file.is.empty",
  "Failed to read upload": "error.failed.to.read.upload",
  "File not found": "error.file.not.found",
  "Failed to read file": "error.failed.to.read.file",
  "No object storage configured": "error.no.object.storage.configured",
  "SKU already exists": "error.sku.already.exists",
  "Product not found": "error.product.not.found",
  "No payment gateway configured": "error.no.payment.gateway.configured",
  "Payment not found": "error.payment.not.found",
  "MCP server not found": "error.mcp.server.not.found",
  "Knowledge base not found": "error.knowledge.base.not.found",
  "Document not found": "error.document.not.found",
  "Agent not found": "error.agent.not.found",
  "Invalid username or password": "error.invalid.username.or.password",
  "Invalid refresh token": "error.invalid.refresh.token",
  "Refresh token revoked": "error.refresh.token.revoked",
  "User not found": "error.user.not.found",
  "Announcement not found": "error.announcement.not.found",
  "Ai vector store not found": "error.ai.vector.store.not.found",
  "Ai term already exists": "error.ai.term.already.exists",
  "Ai term not found": "error.ai.term.not.found",
  "Skill not found": "error.skill.not.found",
  "Ai provider code already exists": "error.ai.provider.code.already.exists",
  "Ai provider not found": "error.ai.provider.not.found",
  "Menu not found": "error.menu.not.found",
  "Ai prompt code already exists": "error.ai.prompt.code.already.exists",
  "Ai prompt not found": "error.ai.prompt.not.found",
  "Post code already exists": "error.post.code.already.exists",
  "Post not found": "error.post.not.found",
  "Email template code already exists": "error.email.template.code.already.exists",
  "Email template not found": "error.email.template.not.found",
  "Ai model already exists": "error.ai.model.already.exists",
  "Ai model not found": "error.ai.model.not.found",
  "Param key already exists": "error.param.key.already.exists",
  "Param not found": "error.param.not.found",
  "Too many failed attempts, try again later": "error.too.many.failed.attempts.try.again.later",
  "Blog post not found": "error.blog.post.not.found",
  "Dict type code already exists": "error.dict.type.code.already.exists",
  "Dict type not found": "error.dict.type.not.found",
  "Department cannot be its own parent": "error.department.cannot.be.its.own.parent",
  "Department has children": "error.department.has.children",
  "Department not found": "error.department.not.found",
  "Client id already exists": "error.client.id.already.exists",
  "Client not found": "error.client.not.found",
  "Dict value already exists in this type": "error.dict.value.already.exists.in.this.type",
  "Dict data not found": "error.dict.data.not.found",
  "Role code already exists": "error.role.code.already.exists",
  "Role not found": "error.role.not.found",
  "Region code already exists": "error.region.code.already.exists",
  "Region not found": "error.region.not.found",
  "Sensitive word already exists": "error.sensitive.word.already.exists",
  "Sensitive word not found": "error.sensitive.word.not.found",
  "Tenant code already exists": "error.tenant.code.already.exists",
  "Tenant not found": "error.tenant.not.found",
  "Username already exists": "error.username.already.exists",
  "Webhook not found": "error.webhook.not.found",
  "Current password is incorrect": "error.current.password.is.incorrect",
  "Email already exists": "error.email.already.exists",
  "Invalid or expired reset token": "error.invalid.or.expired.reset.token",
  "Dataset not found": "error.dataset.not.found",
};

const STORAGE_KEY = "strata_lang";

function detectDefault(): Lang {
  const fromEnv = import.meta.env.VITE_DEFAULT_LANG;
  if (fromEnv === "en" || fromEnv === "zh") return fromEnv;
  const stored = localStorage.getItem(STORAGE_KEY);
  if (stored === "en" || stored === "zh") return stored;
  return "zh";
}

type I18nState = {
  lang: Lang;
  setLang: (lang: Lang) => void;
};

export const useI18nStore = create<I18nState>((set) => ({
  lang: detectDefault(),
  setLang: (lang) => {
    localStorage.setItem(STORAGE_KEY, lang);
    set({ lang });
  },
}));

/**
 * Translate a semantic key, then fall back to the other dictionary and raw key.
 */
export function tr(lang: Lang, key: string): string {
  if (lang === "zh") {
    return zh[key] ?? en[key] ?? key;
  }
  return en[key] ?? zh[key] ?? key;
}

/** Resolve a backend English error detail to its translated message. */
export function tError(lang: Lang, detail: string): string {
  return tr(lang, errorKeyMap[detail] ?? detail);
}

/** React hook - components re-render on language switch. */
export function useI18n() {
  const lang = useI18nStore((s) => s.lang);
  const setLang = useI18nStore((s) => s.setLang);
  const t = useCallback((key: string) => tr(lang, key), [lang]);
  const translateError = useCallback((detail: string) => tError(lang, detail), [lang]);
  return {
    lang,
    setLang,
    t,
    tError: translateError,
  };
}
