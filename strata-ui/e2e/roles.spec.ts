import { test, expect } from "@playwright/test";
import { loginAsAdmin } from "./support/auth";

test("roles list loads", async ({ page }) => {
  await loginAsAdmin(page);
  await page.goto("/system/roles");
  await expect(page.getByRole("heading", { name: /roles|角色/i })).toBeVisible();
});
