// SPDX-License-Identifier: Apache-2.0
import { Navigate, Route } from "react-router-dom";
import { AppShell } from "@/components/shell/AppShell";
import { Perm, RequireAuth } from "@/routes/guards";
import * as P from "@/routes/lazyPages";
import { ROUTE_TABLE, type RouteSpec } from "@/routes/routeTable";
import type { ComponentType, ReactNode } from "react";

/**
 * Renders the declarative {@link ROUTE_TABLE} (see routeTable.tsx). All routes, permissions and
 * shells are declared there — keep this file free of route details so the table stays the single
 * source of truth consumed by appRoutes and the menu↔route verification script.
 */
export function appRoutes() {
  return <>{ROUTE_TABLE.map(renderRoute)}</>;
}

function renderRoute(def: RouteSpec, index: number): ReactNode {
  const children = def.children?.map(renderRoute);
  if (def.index) {
    return <Route key={index} index element={elementFor(def)} />;
  }
  if (def.path === undefined) {
    // pathless layout route (e.g. the RequireAuth wrapper)
    return (
      <Route key={index} element={elementFor(def)}>
        {children}
      </Route>
    );
  }
  return (
    <Route key={index} path={def.path} element={elementFor(def)}>
      {children}
    </Route>
  );
}

function elementFor(def: RouteSpec): ReactNode {
  if (def.redirect) {
    return <Navigate to={def.redirect} replace />;
  }
  if (def.wrapper === "auth") {
    return <RequireAuth />;
  }
  if (def.wrapper === "shell") {
    return <AppShell />;
  }
  if (def.page) {
    const page = (P as unknown as Record<string, ComponentType>)[def.page];
    if (!page) {
      throw new Error(`Unknown lazy page "${def.page}" in routeTable`);
    }
    const Page = page;
    const content = <Page />;
    return def.perm && def.perm.length > 0 ? <Perm anyOf={def.perm}>{content}</Perm> : content;
  }
  return undefined; // plain group node: path-only wrapper for children
}
