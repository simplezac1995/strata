// SPDX-License-Identifier: Apache-2.0
// Sidebar navigation entries for the "nav.workspace" group, in display order.

import type { NavEntry } from "@/components/shell/navTypes";
import {
  Target,
  Package,
  Hammer,
  Zap,
  Database,
  Clock,
  Send,
  BookMarked,
  Plug,
  LayoutGrid,
  HardDrive,
  CreditCard,
  Bot,
} from "@/components/shell/navIcons";

export const NAV_WORKSPACE: NavEntry[] = [
  { type: "separator", label: "nav.workspace" },
  {
    id: "demo",
    label: "nav.demo",
    icon: Target,
    children: [
      {
        id: "products",
        label: "nav.products",
        icon: Package,
        path: "/demo/products",
        perm: "demo:product:list",
      },
    ],
  },
  {
    id: "tools",
    label: "nav.tools",
    icon: Hammer,
    children: [
      { id: "code-gen", label: "nav.code.generator", icon: Zap, path: "/tools/code-generator" },
      { id: "cache", label: "nav.cache.monitor", icon: Database, path: "/tools/cache" },
      { id: "schedules", label: "nav.schedules", icon: Clock, path: "/tools/schedules" },
      { id: "message-push", label: "nav.message.push", icon: Send, path: "/tools/message-push" },
      {
        id: "api-docs",
        label: "nav.api.docs",
        icon: BookMarked,
        path: "/tools/api-docs",
        perm: "system:cache:list",
      },
    ],
  },
  {
    id: "plugins",
    label: "nav.plugins",
    icon: Plug,
    children: [
      { id: "plugins-overview", label: "nav.overview", icon: LayoutGrid, path: "/plugins" },
      { id: "storage", label: "nav.storage", icon: HardDrive, path: "/plugins/storage" },
      { id: "payments", label: "nav.payments", icon: CreditCard, path: "/plugins/payments" },
      { id: "ai-plugin", label: "nav.ai", icon: Bot, path: "/plugins/ai" },
    ],
  },
];
