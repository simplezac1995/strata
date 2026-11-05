// SPDX-License-Identifier: Apache-2.0
import type { ReactNode } from "react";
import { useAuthStore } from "@/shared/auth/store";

/** Renders children only when the user has any of the given authorities. */
export function IfPerm({
  anyOf,
  children,
  fallback = null,
}: {
  anyOf: string[];
  children: ReactNode;
  fallback?: ReactNode;
}) {
  const authorities = useAuthStore((s) => s.authorities) ?? [];
  const username = useAuthStore((s) => s.user?.username);
  // Superuser: wildcard authority or built-in admin account sees everything.
  if (authorities.includes("*") || username === "admin") {
    return <>{children}</>;
  }
  if (anyOf.length > 0 && !anyOf.some((a) => authorities.includes(a))) {
    return <>{fallback}</>;
  }
  return <>{children}</>;
}
