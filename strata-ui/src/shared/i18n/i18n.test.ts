// SPDX-License-Identifier: Apache-2.0
import { describe, it, expect } from "vitest";
import { tr, tError, errorKeyMap } from "@/shared/i18n/index";
import { zh } from "@/shared/i18n/zh";
import { en } from "@/shared/i18n/en";

describe("tr short-key lookup chain", () => {
  it("translates a known short key in zh and en", () => {
    expect(tr("zh", "common.save")).toBe(zh["common.save"]);
    expect(tr("en", "common.save")).toBe("Save");
  });

  it("falls back to the key itself when missing everywhere", () => {
    expect(tr("zh", "no.such.key")).toBe("no.such.key");
    expect(tr("en", "no.such.key")).toBe("no.such.key");
  });
});

describe("tError backend detail translation", () => {
  it("maps a known backend detail to its translated text", () => {
    expect(tError("zh", "SKU already exists")).toBe(zh["error.sku.already.exists"]);
    expect(tError("en", "SKU already exists")).toBe(en["error.sku.already.exists"]);
  });

  it("returns unknown details unchanged", () => {
    expect(tError("zh", "Some brand-new error")).toBe("Some brand-new error");
  });

  it("P0 regression: every errorKeyMap value exists in both dictionaries", () => {
    for (const [detail, key] of Object.entries(errorKeyMap)) {
      expect(zh[key], `zh missing key for "${detail}"`).toBeDefined();
      expect(en[key], `en missing key for "${detail}"`).toBeDefined();
    }
  });

  it("P0 regression: no en entry is a self-referential short key", () => {
    for (const [key, value] of Object.entries(en)) {
      expect(value, `en["${key}"] is the key itself`).not.toBe(key);
    }
  });
});
