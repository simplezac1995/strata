// SPDX-License-Identifier: Apache-2.0
import { expect, test } from "@playwright/test";

/**
 * Smoke flow against a real open-source deployment: the minimal application (starter + framework schema) with the
 * exported admin console. Credentials come from the CI job's seeded admin password.
 */
const ADMIN_PASSWORD = process.env.E2E_ADMIN_PASSWORD ?? "admin123";

test("admin can sign in and reach the dashboard", async ({ page }) => {
  await page.goto("/");
  await expect(page).toHaveURL(/\/login/);
  await page.locator("input").first().fill("admin");
  await page.locator('input[type="password"]').fill(ADMIN_PASSWORD);
  await page.getByRole("button", { name: /sign in|log in|登录/i }).click();
  await expect(page).not.toHaveURL(/\/login/, { timeout: 20000 });
  await expect(page.locator("nav, aside").first()).toBeVisible();
});
