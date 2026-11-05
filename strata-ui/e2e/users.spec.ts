import { test, expect } from "@playwright/test";
import { loginAsAdmin } from "./support/auth";

test("login, open users, create user", async ({ page }) => {
  await loginAsAdmin(page);

  await page.goto("/system/users");
  await expect(page.getByRole("heading", { name: /users|用户/i })).toBeVisible();

  await page.getByTestId("users-create").click();
  const username = `e2e_${Date.now()}`;
  await page.getByTestId("user-form-username").fill(username);
  await page.getByTestId("user-form-password").fill("password123");
  await page.getByTestId("user-form-save").click();

  await expect(page.getByText(username)).toBeVisible();
});
