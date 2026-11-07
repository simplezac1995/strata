// SPDX-License-Identifier: Apache-2.0
// React context and tree helpers shared by the sidebar rows.

import { createContext } from "react";
import type { NavItem } from "@/components/shell/navigation";

export interface SidebarNavCtx {
  openIds: Set<string>;
  toggleGroup: (id: string, siblings: NavItem[]) => void;
  menuAccordion: boolean;
}

export function collectDescendantIds(item: NavItem): string[] {
  const ids: string[] = [];
  for (const child of item.children ?? []) {
    ids.push(child.id);
    ids.push(...collectDescendantIds(child));
  }
  return ids;
}

export const SidebarNavContext = createContext<SidebarNavCtx | null>(null);

export function collectOpenParents(
  items: NavItem[],
  pathname: string,
  acc: string[] = [],
): string[] {
  for (const item of items) {
    if (item.children?.length) {
      if (isItemActive(item, pathname)) {
        acc.push(item.id);
        collectOpenParents(item.children, pathname, acc);
      }
    }
  }
  return acc;
}

export function isItemActive(item: NavItem, pathname: string): boolean {
  if (item.path && pathname === item.path) return true;
  return (item.children ?? []).some((c) => isItemActive(c, pathname));
}

export function firstPath(item: NavItem): string | undefined {
  if (item.path) return item.path;
  for (const child of item.children ?? []) {
    const p = firstPath(child);
    if (p) return p;
  }
  return undefined;
}
