// SPDX-License-Identifier: Apache-2.0
import type { SvgIconComponent } from "@mui/icons-material";

export type IconType = SvgIconComponent;

export interface NavItem {
  id: string;
  label: string;
  path?: string;
  icon: IconType;
  badge?: "pro" | "ai";
  children?: NavItem[];
  external?: boolean;
  /** Opens in a new tab when `external` is true (defaults to Swagger UI). */
  externalHref?: string;
  later?: boolean;
  /** Required authority (backend menu permission) — item hidden without it. */
  perm?: string;
  /** Shown only when `import.meta.env.DEV` (design system / demos). */
  devOnly?: boolean;
}

export type NavEntry = NavItem | { type: "separator"; label: string };

export const isSeparator = (e: NavEntry): e is { type: "separator"; label: string } => "type" in e;

export interface SearchNavItem {
  label: string;
  path: string;
  icon: IconType;
  section: string;
  external?: boolean;
  externalHref?: string;
}
