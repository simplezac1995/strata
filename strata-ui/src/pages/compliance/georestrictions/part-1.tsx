// SPDX-License-Identifier: Apache-2.0
// Split out of GeoRestrictions.tsx by scripts/auto-split.mjs.

import { type GeoRestrictionAction } from "@/shared/api/compliance";

export type GeoTab = "restrictions" | "regions";

export type RestrictionForm = {
  ipRange: string;
  country: string;
  action: GeoRestrictionAction;
  reason: string;
  enabled: boolean;
};

export type RegionForm = {
  countryCode: string;
  countryName: string;
  registerBlocked: boolean;
  loginBlocked: boolean;
};

export const ACTION_TONE: Record<GeoRestrictionAction, "error" | "success" | "default"> = {
  DENY: "error",
  ALLOW: "success",
};

export const RESTRICTIONS_KEY = ["api", "compliance", "geo", "restrictions"];

export const REGIONS_KEY = ["api", "compliance", "geo", "regions"];
