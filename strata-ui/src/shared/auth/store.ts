// SPDX-License-Identifier: Apache-2.0
import { create } from "zustand";
import { apiGet, apiPost, SESSION_REFRESHED_EVENT, ACCESS_TOKEN_KEY } from "@/shared/api/client";
import { getDefaultWebSocketClient } from "@/shared/websocket";
import { bootstrapReferenceData } from "@/shared/query/bootstrap";
import type { MenuDto, TokenResponse, UserDto } from "@/shared/types";

type LoginOptions = {
  captchaId?: string;
  captchaCode?: string;
};

/**
 * Authorities whose route guard should force a fresh server-side snapshot before
 * granting access. These gate money-touching surfaces; a stale client-side
 * authority list could otherwise let a recently-demoted admin keep clicking
 * through wallet pages.
 */
const SENSITIVE_AUTHORITY_PREFIXES = ["casino:wallet:"];

/**
 * How long a client-side authority snapshot is trusted before the sensitive-route
 * guard re-fetches it. Kept short for wallet surfaces.
 */
export const AUTHORITIES_SNAPSHOT_TTL_MS = 5 * 60 * 1000;

/** Shape returned by `GET /api/v1/system/auth/me` (current user + fresh menu tree). */
type CurrentSessionDto = { user: UserDto; menus: MenuDto[] };

/**
 * Is `authority` one of the sensitive surfaces whose access must be re-verified
 * server-side before granting entry? Exported so the route guard can decide
 * whether to consult the snapshot TTL.
 */
export function isSensitiveAuthority(authority: string): boolean {
  return SENSITIVE_AUTHORITY_PREFIXES.some((prefix) => authority.startsWith(prefix));
}

type AuthState = {
  user: UserDto | null;
  menus: MenuDto[];
  authorities: string[];
  hydrated: boolean;
  /** Epoch ms of the last successful server-side authority snapshot (0 = never). */
  authoritiesRefreshedAt: number;
  login: (username: string, password: string, options?: LoginOptions) => Promise<void>;
  logout: () => Promise<void>;
  hydrate: () => void;
  setUser: (user: UserDto) => void;
  hasAuthority: (authority: string) => boolean;
  /** Re-fetch the current user + menu tree from the server and refresh the snapshot. */
  refreshAuthorities: () => Promise<void>;
  /**
   * Re-fetch only when the snapshot is older than `maxAgeMs` (or never fetched).
   * Safe to call on every sensitive navigation; it de-dupes to a single in-flight
   * request and no-ops when fresh.
   */
  ensureAuthoritiesFresh: (maxAgeMs: number) => Promise<void>;
};

const REFRESH_TOKEN_KEY = "strata_refresh_token";
const USER_KEY = "strata_user";
const MENUS_KEY = "strata_menus";

/**
 * KNOWN RISK (P2-22, not yet remediated): the refresh token is stored in
 * `localStorage` under {@link REFRESH_TOKEN_KEY} and read by the axios client
 * on every request. localStorage is readable from any same-origin script, so a
 * stored-XSS could exfiltrate the refresh token and survive access-token
 * expiry. The proper fix is an httpOnly, `Secure`, `SameSite=strict` refresh
 * cookie issued by `/auth/login` and refreshed via `/auth/refresh` without the
 * token ever touching JS — but that migration has a large blast radius
 * (interceptor, logout, refresh-retry, MFA verify, social login) and is
 * deliberately out of scope here. We additionally cannot shorten the refresh
 * TTL from the frontend (it is a server-side decision). Tracked under
 * docs/08 §8 security debt. Do NOT assume this is fully fixed.
 */
/** localStorage key the NotificationCenter persists its in-memory list under. */
const NOTIFICATIONS_STORAGE_KEY = "strata_notifications";

/** De-duplicates concurrent refreshAuthorities calls so only one /auth/me flies. */
let authoritiesRefreshInFlight: Promise<void> | null = null;

function collectAuthorities(menus: MenuDto[]): string[] {
  const result: string[] = [];
  const walk = (nodes: MenuDto[]) => {
    for (const node of nodes) {
      if (node.permission) result.push(node.permission);
      if (node.children?.length) walk(node.children);
    }
  };
  walk(menus);
  return [...new Set(result)];
}

function persistSession(token: TokenResponse) {
  localStorage.setItem(ACCESS_TOKEN_KEY, token.accessToken);
  localStorage.setItem(REFRESH_TOKEN_KEY, token.refreshToken);
  localStorage.setItem(USER_KEY, JSON.stringify(token.user));
  localStorage.setItem(MENUS_KEY, JSON.stringify(token.menus));
}

function clearSession() {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
  localStorage.removeItem(MENUS_KEY);
}

export const useAuthStore = create<AuthState>((set, get) => ({
  user: null,
  menus: [],
  authorities: [],
  hydrated: false,
  authoritiesRefreshedAt: 0,
  async login(username, password, options) {
    const body: Record<string, string> = { username, password };
    if (options?.captchaId) body.captchaId = options.captchaId;
    if (options?.captchaCode) body.captchaCode = options.captchaCode;
    const token = await apiPost<TokenResponse>("/api/v1/system/auth/login", body);
    persistSession(token);
    set({
      user: token.user,
      menus: token.menus,
      authorities: collectAuthorities(token.menus),
      authoritiesRefreshedAt: Date.now(),
    });
    // Boot the realtime channel explicitly — boot-time initWebSocket only runs
    // when a token already exists, so a fresh login would otherwise stay silent.
    getDefaultWebSocketClient().connect();
    // Warm low-frequency reference data (dictionaries/params/menus/roles/...) so
    // the first list/form page renders dropdowns from cache instead of cold.
    bootstrapReferenceData();
  },
  async logout() {
    try {
      const refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY);
      if (refreshToken) {
        await apiPost<void>("/api/v1/system/auth/logout", { refreshToken });
      }
    } catch {
      // ignore network failures on logout
    }
    clearSession();
    set({ user: null, menus: [], authorities: [], authoritiesRefreshedAt: 0 });
    // Tear down the realtime channel and drop cached notifications so the next
    // session (or another user) never sees stale messages from the previous one.
    getDefaultWebSocketClient().disconnect();
    localStorage.removeItem(NOTIFICATIONS_STORAGE_KEY);
  },
  hydrate() {
    const userRaw = localStorage.getItem(USER_KEY);
    const menusRaw = localStorage.getItem(MENUS_KEY);
    const token = localStorage.getItem(ACCESS_TOKEN_KEY);
    try {
      if (token && userRaw && menusRaw) {
        const user = JSON.parse(userRaw) as UserDto;
        const menus = JSON.parse(menusRaw) as MenuDto[];
        // Restore the persisted snapshot but do NOT trust it as fresh: boot will
        // call refreshAuthorities() to re-sync from the server.
        set({
          user,
          menus,
          authorities: collectAuthorities(menus),
          hydrated: true,
          authoritiesRefreshedAt: 0,
        });
        return;
      }
    } catch {
      clearSession();
    }
    set({ hydrated: true });
  },
  hasAuthority(authority) {
    return get().authorities.includes(authority);
  },
  setUser(user) {
    localStorage.setItem(USER_KEY, JSON.stringify(user));
    set({ user });
  },
  async refreshAuthorities() {
    // De-dupe concurrent callers: the route guard and boot may race.
    if (authoritiesRefreshInFlight) {
      return authoritiesRefreshInFlight;
    }
    authoritiesRefreshInFlight = (async () => {
      try {
        const session = await apiGet<CurrentSessionDto>("/api/v1/system/auth/me");
        // Only adopt the fresh snapshot if we are still the same logged-in user;
        // a logout mid-flight must not repopulate the store.
        if (!localStorage.getItem(ACCESS_TOKEN_KEY)) return;
        localStorage.setItem(USER_KEY, JSON.stringify(session.user));
        localStorage.setItem(MENUS_KEY, JSON.stringify(session.menus));
        set({
          user: session.user,
          menus: session.menus,
          authorities: collectAuthorities(session.menus),
          authoritiesRefreshedAt: Date.now(),
        });
        // Same cache warm as a fresh login; also covers page-refresh session restore.
        bootstrapReferenceData();
      } catch {
        // Non-fatal: keep the existing snapshot. The axios interceptor already
        // handles a hard 401 (clearSessionAndRedirect).
      } finally {
        authoritiesRefreshInFlight = null;
      }
    })();
    return authoritiesRefreshInFlight;
  },
  async ensureAuthoritiesFresh(maxAgeMs) {
    const state = get();
    if (!state.user) return;
    const age = Date.now() - state.authoritiesRefreshedAt;
    if (state.authoritiesRefreshedAt > 0 && age < maxAgeMs) return;
    await state.refreshAuthorities();
  },
}));

window.addEventListener(SESSION_REFRESHED_EVENT, () => {
  useAuthStore.getState().hydrate();
});
