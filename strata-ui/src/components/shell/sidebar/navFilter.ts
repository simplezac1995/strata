// SPDX-License-Identifier: Apache-2.0
// Permission filtering for the sidebar tree.

import { isSeparator } from "@/components/shell/navigation";
import type { NavItem, NavEntry } from "@/components/shell/navigation";

export function filterItems(items: NavItem[], has: (perm?: string) => boolean): NavItem[] {
  const out: NavItem[] = [];
  for (const item of items) {
    if (item.devOnly && !import.meta.env.DEV) continue;
    if (item.perm && !has(item.perm)) continue;
    if (item.children) {
      const children = filterItems(item.children, has);
      if (children.length === 0) continue;
      out.push({ ...item, children });
    } else {
      out.push(item);
    }
  }
  return out;
}

/** Prunes the top-level NAV by authority, keeping separators only before visible groups. */
export function filterNav(entries: NavEntry[], has: (perm?: string) => boolean): NavEntry[] {
  const out: NavEntry[] = [];
  let pendingSep: string | null = null;
  for (const e of entries) {
    if (isSeparator(e)) {
      pendingSep = e.label;
      continue;
    }
    if (e.devOnly && !import.meta.env.DEV) continue;
    if (e.perm && !has(e.perm)) continue;
    let children = e.children;
    if (children) {
      children = filterItems(children, has);
      if (children.length === 0) continue;
    }
    if (pendingSep && out.length > 0) out.push({ type: "separator", label: pendingSep });
    pendingSep = null;
    out.push(children ? { ...e, children } : e);
  }
  return out;
}
