// SPDX-License-Identifier: Apache-2.0
import { describe, expect, it } from "vitest";
import * as P from "@/routes/lazyPages";
import { ROUTE_TABLE, type RouteSpec } from "@/routes/routeTable";

function flatten(
  nodes: RouteSpec[] | undefined,
  parent: string,
): { path: string; page?: string }[] {
  const out: { path: string; page?: string }[] = [];
  for (const n of nodes ?? []) {
    const full =
      n.path?.startsWith("/") || n.path === undefined
        ? (n.path ?? parent)
        : `${parent === "/" ? "" : parent}/${n.path}`;
    if (n.page) out.push({ path: full, page: n.page });
    out.push(...flatten(n.children, full));
  }
  return out;
}

describe("routeTable", () => {
  const leaves = flatten(ROUTE_TABLE, "/");

  it("declares every page key that exists in lazyPages", () => {
    const unknown = leaves.map((l) => l.page).filter((page) => page !== undefined && !(page in P));
    expect(unknown).toEqual([]);
  });

  it("covers the whole app route set (auth + shell pages + wildcard)", () => {
    const paths = leaves.map((l) => l.path);
    for (const p of [
      "/login",
      "/system/users",
      "/system/users/:id",
      "/system/logs/operation",
      "/system/audit-changes",
      "/demo/products",
      "/tools/code-generator",
      "/plugins/storage",
      "/ai/interface-skills",
      "/ai/proofreading/lexicon",
      "/settings/api-keys",
      "/ops/announcements",
      "/ops/big-screen",
      "/compliance/geo",
      "/compliance/responsible",
      "/compliance/aml",
      "/compliance/channel",
      "/compliance/rate",
      "/compliance/kyc",
      "/*",
    ]) {
      expect(paths).toContain(p);
    }
    // no duplicate page paths
    expect(new Set(paths).size).toBe(paths.length);
  });

  it("has the expected section sizes", () => {
    const countIn = (prefix: string) =>
      leaves.filter((l) => l.page && (l.path === prefix || l.path.startsWith(`${prefix}/`))).length;
    expect(countIn("/system")).toBe(25);
    expect(countIn("/ai")).toBe(61);
    expect(countIn("/settings")).toBe(7);
    expect(countIn("/tools")).toBe(5);
    expect(countIn("/plugins")).toBe(4);
    expect(countIn("/demo")).toBe(1);
    expect(countIn("/ops")).toBe(9);
    expect(countIn("/compliance")).toBe(6);
  });
});
