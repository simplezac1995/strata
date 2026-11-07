// SPDX-License-Identifier: Apache-2.0
import { useEffect, type ReactNode } from "react";
import { Navigate, Outlet } from "react-router-dom";
import {
  useAuthStore,
  AUTHORITIES_SNAPSHOT_TTL_MS,
  isSensitiveAuthority,
} from "@/shared/auth/store";
import { PageFallback } from "@/shared/pageKit/lazyPage";

export function RequireAuth() {
  const user = useAuthStore((s) => s.user);
  const hydrated = useAuthStore((s) => s.hydrated);
  if (!hydrated) return <PageFallback />;
  if (!user) return <Navigate to="/login" replace />;
  return <Outlet />;
}

/**
 * Route-level permission guard: any one of `anyOf` authorities grants access.
 *
 * <p>For sensitive authorities (money-touching surfaces such as `casino:wallet:*`)
 * this guard treats the client-side authority list as a bounded snapshot: on entry,
 * if the snapshot is older than {@link AUTHORITIES_SNAPSHOT_TTL_MS} it kicks off a
 * background re-fetch of `/auth/me`. Render continues against the current list, and
 * once the fresh snapshot lands a demoted admin is redirected to /403 on the
 * re-render. The backend still enforces the same authority on every request — this
 * is defense-in-depth against a stale tab, not the primary control.
 */
export function Perm({ anyOf, children }: { anyOf: string[]; children: ReactNode }) {
  const authorities = useAuthStore((s) => s.authorities);

  useEffect(() => {
    if (anyOf.some(isSensitiveAuthority)) {
      // Fire-and-forget; the store subscription below re-renders when it resolves.
      void useAuthStore.getState().ensureAuthoritiesFresh(AUTHORITIES_SNAPSHOT_TTL_MS);
    }
    // anyOf is static per route; the snapshot refresh is a one-shot on entry.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  if (anyOf.length > 0 && !anyOf.some((a) => authorities.includes(a))) {
    return <Navigate to="/403" replace />;
  }
  return <>{children}</>;
}
