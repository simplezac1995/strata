// SPDX-License-Identifier: Apache-2.0
import { beforeEach, describe, expect, it, vi } from "vitest";
import { AxiosError } from "axios";
import type { AxiosAdapter, AxiosResponse, InternalAxiosRequestConfig } from "axios";
import client, {
  ApiError,
  SESSION_REFRESHED_EVENT,
  IDEMPOTENCY_HEADER,
  apiGet,
  apiPost,
} from "@/shared/api/client";

function apiOk(config: InternalAxiosRequestConfig, data: unknown): AxiosResponse {
  return {
    data: { code: 0, message: "", data },
    status: 200,
    statusText: "OK",
    headers: {},
    config,
  };
}

describe("client", () => {
  beforeEach(() => {
    localStorage.clear();
    client.defaults.adapter = undefined;
    vi.restoreAllMocks();
  });

  it("ApiError carries message, status and code", () => {
    const err = new ApiError("boom", 400, 1001);
    expect(err.message).toBe("boom");
    expect(err.status).toBe(400);
    expect(err.code).toBe(1001);
    expect(err.name).toBe("ApiError");
  });

  it("attaches the stored access token as the Authorization header", async () => {
    localStorage.setItem("strata_access_token", "token-1");
    let authHeader: unknown;
    const adapter: AxiosAdapter = async (config) => {
      authHeader = config.headers.Authorization;
      return apiOk(config, { id: "1" });
    };
    client.defaults.adapter = adapter;

    const result = await apiGet<{ id: string }>("/api/v1/foo");

    expect(result).toEqual({ id: "1" });
    expect(authHeader).toBe("Bearer token-1");
  });

  it("omits the Authorization header when no token is stored", async () => {
    let authHeader: unknown = "sentinel";
    const adapter: AxiosAdapter = async (config) => {
      authHeader = config.headers.Authorization;
      return apiOk(config, { id: "1" });
    };
    client.defaults.adapter = adapter;

    await apiGet("/api/v1/foo");

    expect(authHeader).toBeUndefined();
  });

  it("silently refreshes on 401, persists new tokens, and retries once", async () => {
    localStorage.setItem("strata_refresh_token", "refresh-1");
    const dispatchSpy = vi.spyOn(window, "dispatchEvent");
    let calls = 0;
    const adapter: AxiosAdapter = async (config) => {
      calls += 1;
      if ((config.url ?? "").includes("/api/v1/system/auth/refresh")) {
        return apiOk(config, {
          accessToken: "access-2",
          refreshToken: "refresh-2",
          tokenType: "Bearer",
          expiresIn: 7200,
          user: { id: "1", username: "admin", nickname: "Admin", enabled: true, roleIds: [] },
          menus: [],
        });
      }
      if (calls === 1) {
        const response401: AxiosResponse = {
          data: {},
          status: 401,
          statusText: "Unauthorized",
          headers: {},
          config,
        };
        throw new AxiosError("Unauthorized", "ERR_BAD_RESPONSE", config, undefined, response401);
      }
      return apiOk(config, { id: "1" });
    };
    client.defaults.adapter = adapter;

    const result = await apiGet<{ id: string }>("/api/v1/foo");

    expect(result).toEqual({ id: "1" });
    expect(localStorage.getItem("strata_access_token")).toBe("access-2");
    expect(localStorage.getItem("strata_refresh_token")).toBe("refresh-2");
    const refreshed = dispatchSpy.mock.calls.some(
      ([event]) => (event as Event).type === SESSION_REFRESHED_EVENT,
    );
    expect(refreshed).toBe(true);
  });

  it("clears the session and redirects to /login when the silent refresh itself 401s", async () => {
    localStorage.setItem("strata_access_token", "access-1");
    localStorage.setItem("strata_refresh_token", "refresh-1");
    localStorage.setItem("strata_user", JSON.stringify({ id: "1" }));
    localStorage.setItem("strata_menus", "[]");
    // jsdom pathname starts at "/", so clearSessionAndRedirect will attempt the redirect.
    expect(window.location.pathname.startsWith("/login")).toBe(false);

    let calls = 0;
    const adapter: AxiosAdapter = async (config) => {
      calls += 1;
      const response401: AxiosResponse = {
        data: {},
        status: 401,
        statusText: "Unauthorized",
        headers: {},
        config,
      };
      throw new AxiosError("Unauthorized", "ERR_BAD_RESPONSE", config, undefined, response401);
    };
    client.defaults.adapter = adapter;

    await expect(apiGet("/api/v1/foo")).rejects.toThrow();

    // Session storage must be wiped after a failed refresh.
    expect(localStorage.getItem("strata_access_token")).toBeNull();
    expect(localStorage.getItem("strata_refresh_token")).toBeNull();
    expect(localStorage.getItem("strata_user")).toBeNull();
    expect(localStorage.getItem("strata_menus")).toBeNull();
    // The original request was attempted once; the refresh was attempted, both 401.
    expect(calls).toBeGreaterThanOrEqual(2);
  });

  it("clears the session and redirects when there is no refresh token to retry with", async () => {
    localStorage.setItem("strata_access_token", "access-1");

    let calls = 0;
    const adapter: AxiosAdapter = async (config) => {
      calls += 1;
      const response401: AxiosResponse = {
        data: {},
        status: 401,
        statusText: "Unauthorized",
        headers: {},
        config,
      };
      throw new AxiosError("Unauthorized", "ERR_BAD_RESPONSE", config, undefined, response401);
    };
    client.defaults.adapter = adapter;

    await expect(apiGet("/api/v1/foo")).rejects.toThrow();

    expect(localStorage.getItem("strata_access_token")).toBeNull();
    // No refresh token means no refresh call was made — only the original request 401'd.
    expect(calls).toBe(1);
  });

  it("attaches an Idempotency-Key to writes and reuses the same key across the 401 retry", async () => {
    localStorage.setItem("strata_refresh_token", "refresh-1");
    const seenKeys: unknown[] = [];
    let calls = 0;
    const adapter: AxiosAdapter = async (config) => {
      const isRefresh = (config.url ?? "").includes("/api/v1/system/auth/refresh");
      if (!isRefresh) {
        seenKeys.push(config.headers[IDEMPOTENCY_HEADER]);
      }
      calls += 1;
      if (isRefresh) {
        return apiOk(config, {
          accessToken: "access-2",
          refreshToken: "refresh-2",
          tokenType: "Bearer",
          expiresIn: 7200,
          user: { id: "1", username: "admin" },
          menus: [],
        });
      }
      if (calls === 1) {
        const response401: AxiosResponse = {
          data: {},
          status: 401,
          statusText: "Unauthorized",
          headers: {},
          config,
        };
        throw new AxiosError("Unauthorized", "ERR_BAD_RESPONSE", config, undefined, response401);
      }
      return apiOk(config, { id: "1" });
    };
    client.defaults.adapter = adapter;

    await apiPost("/api/v1/system/users", { username: "new-user" });

    expect(seenKeys).toHaveLength(2);
    expect(seenKeys[0]).toBeTruthy();
    expect(seenKeys[0]).toBe(seenKeys[1]); // the 401 refresh-retry must reuse the same key
    expect(String(seenKeys[0])).toMatch(
      /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i,
    );
  });

  it("omits Idempotency-Key for GET, auth login and multipart requests", async () => {
    const seen: Record<string, unknown> = {};
    const adapter: AxiosAdapter = async (config) => {
      seen[config.url ?? ""] = config.headers[IDEMPOTENCY_HEADER];
      return apiOk(config, {});
    };
    client.defaults.adapter = adapter;

    await apiGet("/api/v1/system/users");
    await apiPost("/api/v1/system/auth/login", { username: "a", password: "b" });
    await apiPost("/api/v1/system/files", new FormData(), {
      headers: { "Content-Type": "multipart/form-data; boundary=xyz" },
    });

    expect(seen["/api/v1/system/users"]).toBeUndefined();
    expect(seen["/api/v1/system/auth/login"]).toBeUndefined();
    expect(seen["/api/v1/system/files"]).toBeUndefined();
  });

  it("honours a caller-supplied business Idempotency-Key over the generated one", async () => {
    let seen: unknown = "sentinel";
    const adapter: AxiosAdapter = async (config) => {
      seen = config.headers[IDEMPOTENCY_HEADER];
      return apiOk(config, {});
    };
    client.defaults.adapter = adapter;

    await apiPost(
      "/api/v1/system/orders",
      { amount: 1 },
      { headers: { [IDEMPOTENCY_HEADER]: "biz-key-123" } },
    );

    expect(seen).toBe("biz-key-123");
  });
});
