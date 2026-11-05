// SPDX-License-Identifier: Apache-2.0
import type { ReactNode } from "react";
import { describe, expect, it, vi, beforeEach, afterEach } from "vitest";
import { render, screen, cleanup } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import Team from "@/pages/settings/Team";
import Dashboard from "@/pages/Dashboard";
import Login from "@/pages/auth/Login";
import Users from "@/pages/system/Users";
import Products from "@/pages/demo/Products";
import { CodeGenerator } from "@/pages/tools/parts/CodeGenerator";
import { Tenants } from "@/pages/system/simple/Tenants";
import { AiOverview } from "@/pages/ai/config/AiOverview";

vi.mock("@/shared/api/system", () => ({
  userApi: {
    list: vi.fn().mockResolvedValue([]),
    page: vi
      .fn()
      .mockResolvedValue({ content: [], totalElements: 0, page: 0, size: 10, totalPages: 0 }),
  },
  roleApi: { list: vi.fn().mockResolvedValue([]) },
  operationLogApi: { page: vi.fn().mockResolvedValue({ content: [] }) },
  tenantApi: {
    page: vi
      .fn()
      .mockResolvedValue({ content: [], totalElements: 0, page: 0, size: 10, totalPages: 0 }),
  },
}));

vi.mock("@/shared/api/demo", () => ({
  productApi: {
    listAll: vi.fn().mockResolvedValue([]),
    page: vi
      .fn()
      .mockResolvedValue({ content: [], totalElements: 0, page: 0, size: 10, totalPages: 0 }),
  },
}));

vi.mock("@/shared/api/ai", () => ({
  providerApi: { list: vi.fn().mockResolvedValue([]) },
  modelApi: { list: vi.fn().mockResolvedValue([]) },
  promptApi: { list: vi.fn().mockResolvedValue([]) },
  agentApi: { list: vi.fn().mockResolvedValue([]) },
}));

vi.mock("@/shared/auth/store", () => ({
  useAuthStore: (
    selector: (s: { hasAuthority: (a: string) => boolean; login: () => void }) => unknown,
  ) => selector({ hasAuthority: () => true, login: vi.fn() }),
}));

vi.mock("@/context/AppContext", () => ({
  useApp: () => ({
    setCurrentTenant: vi.fn(),
    tenantsEnabled: false,
    tenants: [],
  }),
}));

vi.mock("@/context/AppearanceContext", () => ({
  useAppearance: () => ({
    toggleDark: vi.fn(),
    dark: false,
    lang: "zh" as const,
    setLang: vi.fn(),
  }),
}));

function createTestQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: { retry: false, gcTime: Infinity },
    },
  });
}

function wrap(ui: ReactNode) {
  return render(
    <QueryClientProvider client={createTestQueryClient()}>
      <MemoryRouter>{ui}</MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("page smoke", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    cleanup();
  });

  it("renders Login form", () => {
    wrap(<Login />);
    expect(screen.getByRole("textbox")).toBeTruthy();
    expect(screen.getByRole("button", { name: /登录|sign in/i })).toBeTruthy();
  });

  it("renders Users page header", async () => {
    wrap(<Users />);
    expect(await screen.findByRole("heading", { level: 1 })).toBeTruthy();
  });

  it("renders Products page header", async () => {
    wrap(<Products />);
    expect(await screen.findByRole("heading", { level: 1 })).toBeTruthy();
  });

  it("renders Team page header", async () => {
    wrap(<Team />);
    expect(await screen.findByRole("heading", { level: 1 })).toBeTruthy();
  });

  it("renders Dashboard KPI section", async () => {
    wrap(<Dashboard />);
    expect(await screen.findByRole("heading", { level: 1 })).toBeTruthy();
  });

  it("renders CodeGenerator page header", () => {
    wrap(<CodeGenerator />);
    expect(screen.getByRole("heading", { level: 1 })).toBeTruthy();
  });

  it("renders Tenants page header", () => {
    wrap(<Tenants />);
    expect(screen.getByRole("heading", { level: 1 })).toBeTruthy();
  });

  it("renders AiOverview stats", async () => {
    wrap(<AiOverview />);
    expect(await screen.findByRole("heading", { level: 1 })).toBeTruthy();
  });
});
