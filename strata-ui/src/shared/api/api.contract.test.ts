// SPDX-License-Identifier: Apache-2.0
import { beforeEach, describe, expect, it, vi } from "vitest";

const apiGet = vi.hoisted(() => vi.fn());
const apiPost = vi.hoisted(() => vi.fn());
const apiPut = vi.hoisted(() => vi.fn());
const apiDelete = vi.hoisted(() => vi.fn());

vi.mock("@/shared/api/client", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/shared/api/client")>();
  return {
    ...actual,
    default: { defaults: {} },
    apiGet,
    apiPost,
    apiPut,
    apiDelete,
    apiGetBlob: vi.fn(),
    ApiError: class ApiError extends Error {},
  };
});

import { authApi } from "@/shared/api/auth";
import { menuApi, roleApi, userApi } from "@/shared/api/system";

describe("API contract smoke", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    apiGet.mockResolvedValue([]);
    apiPost.mockResolvedValue(undefined);
    apiPut.mockResolvedValue(undefined);
    apiDelete.mockResolvedValue(undefined);
  });

  it("userApi uses system user endpoints", async () => {
    await userApi.list();
    expect(apiGet).toHaveBeenCalledWith("/api/v1/system/users");

    await userApi.page("kw", 1, 20);
    expect(apiGet).toHaveBeenCalledWith("/api/v1/system/users/page?page=1&size=20&keyword=kw");

    await userApi.create({
      username: "u",
      password: "secret12",
      nickname: null,
      enabled: true,
      roleIds: [],
    });
    expect(apiPost).toHaveBeenCalledWith("/api/v1/system/users", expect.any(Object));

    await userApi.remove("9");
    expect(apiDelete).toHaveBeenCalledWith("/api/v1/system/users/9");
  });

  it("roleApi uses system role endpoints", async () => {
    await roleApi.list();
    expect(apiGet).toHaveBeenCalledWith("/api/v1/system/roles");

    await roleApi.page("admin", 1, 20);
    expect(apiGet).toHaveBeenCalledWith("/api/v1/system/roles/page?page=1&size=20&keyword=admin");

    await roleApi.create({ code: "dev", name: "Dev", enabled: true, menuIds: ["1"] });
    expect(apiPost).toHaveBeenCalledWith("/api/v1/system/roles", expect.any(Object));
  });

  it("menuApi uses system menu endpoints", async () => {
    await menuApi.list();
    expect(apiGet).toHaveBeenCalledWith("/api/v1/system/menus");
  });

  it("authApi uses auth endpoints", async () => {
    await authApi.forgotPassword("admin");
    expect(apiPost).toHaveBeenCalledWith("/api/v1/system/auth/forgot-password", {
      username: "admin",
    });

    await authApi.changePassword("old", "newsecret");
    expect(apiPost).toHaveBeenCalledWith("/api/v1/system/auth/change-password", {
      oldPassword: "old",
      newPassword: "newsecret",
    });
  });
});
