// SPDX-License-Identifier: Apache-2.0
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { TokenResponse } from "@/shared/types";

const mockToken = vi.hoisted(
  () =>
    ({
      accessToken: "access-1",
      refreshToken: "refresh-1",
      tokenType: "Bearer",
      expiresIn: 7200,
      user: { id: "1", username: "admin", nickname: "Admin", enabled: true, roleIds: [] },
      menus: [
        {
          id: "m1",
          parentId: null,
          name: "Demo",
          type: "directory",
          path: "/demo",
          component: null,
          permission: null,
          sortOrder: 1,
          visible: true,
          enabled: true,
          children: [
            {
              id: "m2",
              parentId: "m1",
              name: "Products",
              type: "menu",
              path: "products",
              component: "demo/ProductList",
              permission: "demo:product:list",
              sortOrder: 1,
              visible: true,
              enabled: true,
              children: [],
            },
            {
              id: "m3",
              parentId: "m1",
              name: "Create",
              type: "button",
              path: null,
              component: null,
              permission: "demo:product:create",
              sortOrder: 1,
              visible: true,
              enabled: true,
              children: [],
            },
          ],
        },
      ],
    }) satisfies TokenResponse,
);

const mockWs = vi.hoisted(() => ({
  connect: vi.fn(),
  disconnect: vi.fn(),
}));

// Fresh /auth/me payload used by the authority-snapshot tests.
const mockMe = vi.hoisted(() => ({
  user: { id: "1", username: "admin", nickname: "Admin", enabled: true, roleIds: [] },
  menus: [
    {
      id: "w1",
      parentId: null,
      name: "Wallet",
      type: "menu",
      path: "wallet",
      component: null,
      permission: "casino:wallet:hot",
      sortOrder: 1,
      visible: true,
      enabled: true,
      children: [],
    },
  ],
}));

const apiGetMock = vi.hoisted(() => vi.fn(async () => mockMe));

vi.mock("@/shared/websocket", () => ({
  getDefaultWebSocketClient: vi.fn(() => mockWs),
}));

vi.mock("../api/client", () => ({
  apiPost: vi.fn(async () => mockToken),
  apiGet: apiGetMock,
  SESSION_REFRESHED_EVENT: "strata-session-refreshed",
  ACCESS_TOKEN_KEY: "strata_access_token",
}));

import { useAuthStore, isSensitiveAuthority } from "@/shared/auth/store";

const TOKEN_KEY = "strata_access_token";
const NOTIFICATIONS_KEY = "strata_notifications";

describe("useAuthStore", () => {
  beforeEach(() => {
    localStorage.clear();
    vi.useRealTimers();
    apiGetMock.mockClear();
    mockWs.connect.mockClear();
    mockWs.disconnect.mockClear();
    useAuthStore.setState({
      user: null,
      menus: [],
      authorities: [],
      hydrated: false,
      authoritiesRefreshedAt: 0,
    });
  });

  it("login persists the session and collects authorities from the menu tree", async () => {
    await useAuthStore.getState().login("admin", "admin123");

    const state = useAuthStore.getState();
    expect(localStorage.getItem(TOKEN_KEY)).toBe("access-1");
    expect(state.user?.username).toBe("admin");
    expect(state.authorities).toEqual(["demo:product:list", "demo:product:create"]);
    expect(state.hasAuthority("demo:product:create")).toBe(true);
    expect(state.hasAuthority("demo:product:delete")).toBe(false);
    // A fresh login counts as a fresh snapshot.
    expect(state.authoritiesRefreshedAt).toBeGreaterThan(0);
  });

  it("logout clears storage and state", async () => {
    await useAuthStore.getState().login("admin", "admin123");
    await useAuthStore.getState().logout();

    expect(localStorage.getItem(TOKEN_KEY)).toBeNull();
    expect(useAuthStore.getState().user).toBeNull();
    expect(useAuthStore.getState().menus).toEqual([]);
    expect(useAuthStore.getState().authoritiesRefreshedAt).toBe(0);
  });

  it("login opens the WebSocket connection", async () => {
    await useAuthStore.getState().login("admin", "admin123");
    expect(mockWs.connect).toHaveBeenCalledTimes(1);
  });

  it("logout disconnects the WebSocket and clears the notification cache", async () => {
    await useAuthStore.getState().login("admin", "admin123");
    localStorage.setItem(NOTIFICATIONS_KEY, JSON.stringify([{ id: "n1" }]));

    await useAuthStore.getState().logout();

    expect(mockWs.disconnect).toHaveBeenCalledTimes(1);
    expect(localStorage.getItem(NOTIFICATIONS_KEY)).toBeNull();
  });

  it("hydrate restores a persisted session", async () => {
    await useAuthStore.getState().login("admin", "admin123");
    useAuthStore.setState({ user: null, menus: [], authorities: [], hydrated: false });

    useAuthStore.getState().hydrate();
    expect(useAuthStore.getState().user?.username).toBe("admin");
    expect(useAuthStore.getState().hydrated).toBe(true);
    // Hydrate deliberately does NOT mark the snapshot fresh (boot re-fetches it).
    expect(useAuthStore.getState().authoritiesRefreshedAt).toBe(0);
  });

  it("hydrate survives corrupted localStorage", () => {
    localStorage.setItem(TOKEN_KEY, "token");
    localStorage.setItem("strata_user", "{broken json");
    localStorage.setItem("strata_menus", "[]");

    useAuthStore.getState().hydrate();

    const state = useAuthStore.getState();
    expect(state.user).toBeNull();
    expect(state.hydrated).toBe(true);
    expect(localStorage.getItem(TOKEN_KEY)).toBeNull();
  });

  it("refreshAuthorities re-fetches /auth/me and updates user/menus/authorities", async () => {
    await useAuthStore.getState().login("admin", "admin123");
    expect(useAuthStore.getState().authorities).not.toContain("casino:wallet:hot");

    await useAuthStore.getState().refreshAuthorities();

    expect(apiGetMock).toHaveBeenCalledWith("/api/v1/system/auth/me");
    const state = useAuthStore.getState();
    expect(state.authorities).toEqual(["casino:wallet:hot"]);
    expect(state.authoritiesRefreshedAt).toBeGreaterThan(0);
    expect(localStorage.getItem("strata_menus")).toContain("casino:wallet:hot");
  });

  it("refreshAuthorities de-dupes concurrent calls to a single in-flight request", async () => {
    await useAuthStore.getState().login("admin", "admin123");

    const [a, b] = await Promise.all([
      useAuthStore.getState().refreshAuthorities(),
      useAuthStore.getState().refreshAuthorities(),
    ]);

    await Promise.all([a, b]);
    // Both awaited; exactly one network call should have been made.
    expect(apiGetMock).toHaveBeenCalledTimes(1);
  });

  it("ensureAuthoritiesFresh no-ops while the snapshot is young", async () => {
    await useAuthStore.getState().login("admin", "admin123");
    apiGetMock.mockClear();

    await useAuthStore.getState().ensureAuthoritiesFresh(5 * 60 * 1000);

    expect(apiGetMock).not.toHaveBeenCalled();
  });

  it("ensureAuthoritiesFresh re-fetches when the snapshot is stale", async () => {
    await useAuthStore.getState().login("admin", "admin123");
    // Age the snapshot beyond the TTL.
    useAuthStore.setState({ authoritiesRefreshedAt: Date.now() - 10 * 60 * 1000 });
    apiGetMock.mockClear();

    await useAuthStore.getState().ensureAuthoritiesFresh(5 * 60 * 1000);

    expect(apiGetMock).toHaveBeenCalledWith("/api/v1/system/auth/me");
  });

  it("isSensitiveAuthority flags casino:wallet:* prefixes only", () => {
    expect(isSensitiveAuthority("casino:wallet:hot")).toBe(true);
    expect(isSensitiveAuthority("casino:wallet:cold")).toBe(true);
    expect(isSensitiveAuthority("casino:player:list")).toBe(false);
    expect(isSensitiveAuthority("system:user:list")).toBe(false);
  });
});
