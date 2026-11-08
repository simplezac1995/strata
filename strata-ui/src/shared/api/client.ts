// SPDX-License-Identifier: Apache-2.0
import axios from "axios";
import type { AxiosError, AxiosRequestConfig, InternalAxiosRequestConfig } from "axios";
import type { TokenResponse } from "@/shared/types";
import { encryptionHeaders, isApiEncryptionEnabled } from "@/shared/api/apiCrypto";

const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? "",
  timeout: 15000,
});

// Single source of truth for the auth token storage key. The auth store, the
// axios request interceptor, and app boot (main.tsx) all read/write the token
// under this same key — duplicating the literal here previously made main.tsx
// read a stale "auth_token" key that nothing ever wrote.
export const ACCESS_TOKEN_KEY = "strata_access_token";
const REFRESH_TOKEN_KEY = "strata_refresh_token";
const USER_KEY = "strata_user";
const MENUS_KEY = "strata_menus";
const TENANT_ID_KEY = "strata_tenant_id";

/**
 * Idempotency-Key activates the server-side IdempotencyFilter (dev.strata.common.core.idempotency). The
 * filter is opt-in: a write that carries the header is executed once and its first response is replayed
 * for any retry with the same key, so a timed-out POST cannot create the resource twice.
 *
 * The key is generated once per request config and stashed in a WeakMap, so axios' own 401 refresh-retry
 * (which re-submits the very same config object) reuses it instead of minting a second key. A deliberate
 * second submit by the user is a new mutation call with a new config and therefore a new key; callers that
 * need one key across an explicit business intent (e.g. a pay button) can pass a stable key via
 * {@link createIdempotencyKey} + the request config, which always wins.
 */
export const IDEMPOTENCY_HEADER = "Idempotency-Key";
const IDEMPOTENT_METHODS = new Set(["post", "put", "patch", "delete"]);
// Auth endpoints have one-time semantics (login/refresh/register/password reset) and must never be served
// a cached response from the idempotency store.
const NON_IDEMPOTENT_PATH =
  /\/auth\/(login|refresh|register|forgot-password|reset-password|verify-email|logout)/;
const idempotencyKeys = new WeakMap<object, string>();

/** A fresh idempotency key (UUIDv4 where the runtime provides it). Hold it in a ref for one business intent. */
export function createIdempotencyKey(): string {
  if (typeof crypto !== "undefined" && typeof crypto.randomUUID === "function") {
    return crypto.randomUUID();
  }
  const part = () => Math.random().toString(16).slice(2).padStart(8, "0").slice(0, 8);
  return `${part()}-${part().slice(0, 4)}-${part().slice(0, 4)}-${part().slice(0, 4)}-${part()}${part()}`;
}

function wantsIdempotencyKey(config: InternalAxiosRequestConfig): boolean {
  const method = (config.method ?? "").toLowerCase();
  if (!IDEMPOTENT_METHODS.has(method)) return false;
  if (NON_IDEMPOTENT_PATH.test(config.url ?? "")) return false;
  // Multipart uploads are streamed and the server filter skips them; buffering would defeat the cap.
  const headers = config.headers as Record<string, unknown>;
  const contentType = String(headers["Content-Type"] ?? headers["content-type"] ?? "");
  return !contentType.includes("multipart/");
}

/** Dispatched after a silent refresh so the auth store can re-hydrate user/menus/authorities. */
export const SESSION_REFRESHED_EVENT = "strata-session-refreshed";

client.interceptors.request.use((config) => {
  const token = localStorage.getItem(ACCESS_TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  const tenantId = localStorage.getItem(TENANT_ID_KEY);
  if (tenantId) {
    config.headers["X-Tenant-Id"] = tenantId;
  }
  if (isApiEncryptionEnabled()) {
    Object.assign(config.headers, encryptionHeaders(true));
  }
  if (wantsIdempotencyKey(config) && !config.headers[IDEMPOTENCY_HEADER]) {
    let key = idempotencyKeys.get(config);
    if (!key) {
      key = createIdempotencyKey();
      idempotencyKeys.set(config, key);
    }
    config.headers[IDEMPOTENCY_HEADER] = key;
  }
  return config;
});

let refreshPromise: Promise<string | null> | null = null;

async function tryRefresh(): Promise<string | null> {
  const refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY);
  if (!refreshToken) return null;
  try {
    const { data } = await client.post<ApiResult<TokenResponse>>("/api/v1/system/auth/refresh", {
      refreshToken,
    });
    localStorage.setItem(ACCESS_TOKEN_KEY, data.data.accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, data.data.refreshToken);
    localStorage.setItem(USER_KEY, JSON.stringify(data.data.user));
    localStorage.setItem(MENUS_KEY, JSON.stringify(data.data.menus));
    window.dispatchEvent(new Event(SESSION_REFRESHED_EVENT));
    return data.data.accessToken;
  } catch {
    return null;
  }
}

function clearSessionAndRedirect() {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
  localStorage.removeItem(MENUS_KEY);
  const publicAuthPaths = [
    "/login",
    "/sso-callback",
    "/forgot-password",
    "/reset-password",
    "/register",
    "/verify-email",
  ];
  if (!publicAuthPaths.some((path) => window.location.pathname.startsWith(path))) {
    window.location.href = "/login";
  }
}

type RetriableConfig = AxiosRequestConfig & { _retried?: boolean };

/** Standardized error carrying the backend ProblemDetail fields. */
export class ApiError extends Error {
  status: number;
  code: number;

  constructor(message: string, status: number, code: number) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = code;
  }
}

type ProblemDetail = {
  type?: string;
  title?: string;
  status?: number;
  code?: number;
  detail?: string;
  traceId?: string;
  errors?: { field: string; message: string }[];
};

function toApiError(error: AxiosError): ApiError {
  const data = error.response?.data as ProblemDetail | undefined;
  const status = error.response?.status ?? 0;
  const code = data?.code ?? status;

  if (data?.detail) {
    return new ApiError(data.detail, status, code);
  }
  if (data?.title) {
    return new ApiError(data.title, status, code);
  }

  if (status === 502 || status === 503 || status === 504) {
    return new ApiError("error.network.unavailable", status, status);
  }
  if (status >= 500) {
    return new ApiError("error.internal.server.error", status, status);
  }
  if (status === 0 || error.code === "ERR_NETWORK" || error.message === "Network Error") {
    return new ApiError("error.network.offline", 0, 0);
  }
  if (/^Request failed with status code \d+$/i.test(error.message)) {
    return new ApiError("error.request.failed", status, code);
  }

  return new ApiError(error.message || "error.request.failed", status, code);
}

client.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const original = error.config as RetriableConfig | undefined;
    const status = error.response?.status;
    const isAuthRequest = original?.url?.includes("/api/v1/system/auth/");

    if (status === 401 && original && !original._retried && !isAuthRequest) {
      original._retried = true;
      refreshPromise =
        refreshPromise ??
        tryRefresh().finally(() => {
          refreshPromise = null;
        });
      const token = await refreshPromise;
      if (token) {
        original.headers = original.headers ?? {};
        original.headers.Authorization = `Bearer ${token}`;
        return client(original);
      }
    }
    if (status === 401) {
      clearSessionAndRedirect();
    }
    return Promise.reject(toApiError(error));
  },
);

export type ApiResult<T> = {
  code: number;
  message: string;
  data: T;
  traceId?: string;
};

/**
 * Build a query string from a record, skipping `undefined` / `null` / empty-string
 * values. Returns `"?a=1&b=2"` when non-empty, or `""` when every value is skipped,
 * so callers can safely append the result directly to a base URL.
 */
export function toParams(
  record: Record<string, string | number | boolean | undefined | null>,
): string {
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(record)) {
    if (value === undefined || value === null || value === "") continue;
    params.set(key, String(value));
  }
  const q = params.toString();
  return q ? `?${q}` : "";
}

export async function apiGet<T>(url: string) {
  const { data } = await client.get<ApiResult<T>>(url);
  return data.data;
}

export async function apiPost<T>(url: string, body?: unknown, config?: AxiosRequestConfig) {
  const { data } = await client.post<ApiResult<T>>(url, body, config);
  return data.data;
}

export async function apiPut<T>(url: string, body?: unknown, config?: AxiosRequestConfig) {
  const { data } = await client.put<ApiResult<T>>(url, body, config);
  return data.data;
}

export async function apiDelete<T>(url: string, config?: AxiosRequestConfig) {
  const { data } = await client.delete<ApiResult<T>>(url, config);
  return data.data;
}

/** POST that returns a binary body (e.g. codegen zip). */
export async function apiPostBlob(url: string, body?: unknown) {
  try {
    const { data } = await client.post<Blob>(url, body, { responseType: "blob" });
    return data;
  } catch (error) {
    if (error instanceof ApiError) throw error;
    const ax = error as { response?: { data?: Blob; status?: number } };
    const blob = ax.response?.data;
    if (blob instanceof Blob) {
      try {
        const text = await blob.text();
        const problem = JSON.parse(text) as {
          detail?: string;
          title?: string;
          status?: number;
          code?: number;
        };
        throw new ApiError(
          problem.detail || problem.title || "Request failed",
          problem.status ?? ax.response?.status ?? 0,
          problem.code ?? ax.response?.status ?? 0,
        );
      } catch (inner) {
        if (inner instanceof ApiError) throw inner;
      }
    }
    throw error;
  }
}

export default client;
