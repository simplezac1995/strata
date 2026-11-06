// SPDX-License-Identifier: Apache-2.0
import { beforeEach, describe, expect, it, vi } from "vitest";
import { renderHook, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ApiError } from "@/shared/api/client";
import { usePagedQuery } from "@/shared/hooks/usePagedQuery";
import { useListQuery } from "@/shared/hooks/useListQuery";
import { useMutationWithToast } from "@/shared/hooks/useMutationWithToast";
import type { PageResult } from "@/shared/types";

vi.mock("../pageKit/Toast", () => ({
  toast: { success: vi.fn(), error: vi.fn(), info: vi.fn() },
}));

import { toast } from "@/shared/pageKit/Toast";

function createTestQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
        gcTime: Infinity,
      },
    },
  });
}

function createWrapper(client: QueryClient) {
  return function Wrapper({ children }: { children: ReactNode }) {
    return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
  };
}

describe("usePagedQuery", () => {
  let client: QueryClient;

  beforeEach(() => {
    vi.clearAllMocks();
    client = createTestQueryClient();
  });

  it("loads the first page on mount and exposes rows/total", async () => {
    const pageResult: PageResult<{ id: string }> = {
      content: [{ id: "1" }, { id: "2" }],
      page: 0,
      size: 10,
      totalElements: 42,
      totalPages: 5,
    };
    const fetch = vi.fn(async () => pageResult);
    const { result } = renderHook(() => usePagedQuery(["test", "paged"], fetch), {
      wrapper: createWrapper(client),
    });

    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.rows).toEqual([{ id: "1" }, { id: "2" }]);
    expect(result.current.total).toBe(42);
    expect(result.current.page).toBe(1);
    expect(fetch).toHaveBeenCalledWith(0, 10, undefined);
    expect(toast.error).not.toHaveBeenCalled();
  });

  it("passes 0-based pageIndex to fetch when page changes", async () => {
    const fetch = vi.fn(async (): Promise<PageResult<{ id: string }>> => ({
      content: [],
      page: 0,
      size: 10,
      totalElements: 0,
      totalPages: 0,
    }));
    const { result } = renderHook(() => usePagedQuery(["test", "page2"], fetch), {
      wrapper: createWrapper(client),
    });

    await waitFor(() => expect(result.current.loading).toBe(false));
    result.current.setPage(3);
    await waitFor(() => expect(fetch).toHaveBeenLastCalledWith(2, 10, undefined));
  });

  it("toasts loadFailed when fetch rejects", async () => {
    const fetch = vi.fn(async () => {
      throw new Error("network down");
    });
    renderHook(() => usePagedQuery(["test", "error"], fetch), {
      wrapper: createWrapper(client),
    });
    await waitFor(() => expect(toast.error).toHaveBeenCalledWith("toast.loadFailed"));
  });

  it("exposes stable reload that invalidates the key family", async () => {
    const fetch = vi.fn(async (): Promise<PageResult<{ id: string }>> => ({
      content: [],
      page: 0,
      size: 10,
      totalElements: 0,
      totalPages: 0,
    }));
    const { result } = renderHook(() => usePagedQuery(["test", "reload"], fetch), {
      wrapper: createWrapper(client),
    });
    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(fetch).toHaveBeenCalledTimes(1);

    result.current.reload();
    await waitFor(() => expect(fetch).toHaveBeenCalledTimes(2));
  });
});

describe("useListQuery", () => {
  let client: QueryClient;

  beforeEach(() => {
    vi.clearAllMocks();
    client = createTestQueryClient();
  });

  it("loads data on mount and exposes data/loading", async () => {
    const loader = vi.fn(async () => [{ id: "a" }, { id: "b" }]);
    const { result } = renderHook(() => useListQuery(["test", "list"], loader), {
      wrapper: createWrapper(client),
    });

    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.data).toEqual([{ id: "a" }, { id: "b" }]);
    expect(toast.error).not.toHaveBeenCalled();
  });

  it("toasts loadFailed when loader rejects", async () => {
    const loader = vi.fn(async () => {
      throw new Error("fail");
    });
    renderHook(() => useListQuery(["test", "listerror"], loader), {
      wrapper: createWrapper(client),
    });
    await waitFor(() => expect(toast.error).toHaveBeenCalledWith("toast.loadFailed"));
  });
});

describe("useMutationWithToast", () => {
  let client: QueryClient;

  beforeEach(() => {
    vi.clearAllMocks();
    client = createTestQueryClient();
  });

  it("toasts success and resolves on mutateAsync", async () => {
    const mutationFn = vi.fn(async (input: string) => `result:${input}`);
    const { result } = renderHook(
      () =>
        useMutationWithToast(mutationFn, {
          success: "toast.saved",
          failure: "toast.saveFailed",
        }),
      { wrapper: createWrapper(client) },
    );

    const data = await result.current.mutateAsync("hello");
    expect(data).toBe("result:hello");
    expect(toast.success).toHaveBeenCalledWith("toast.saved");
    expect(toast.error).not.toHaveBeenCalled();
  });

  it("toasts the ApiError message on failure", async () => {
    const mutationFn = vi.fn(async () => {
      throw new ApiError("validation failed", 400, 1001);
    });
    const { result } = renderHook(
      () =>
        useMutationWithToast(mutationFn, {
          success: "toast.saved",
          failure: "toast.saveFailed",
        }),
      { wrapper: createWrapper(client) },
    );

    await expect(result.current.mutateAsync()).rejects.toThrow("validation failed");
    expect(toast.error).toHaveBeenCalledWith("validation failed");
    expect(toast.success).not.toHaveBeenCalled();
  });

  it("falls back to the failure key for unknown errors", async () => {
    const mutationFn = vi.fn(async () => {
      throw new Error("unknown");
    });
    const { result } = renderHook(
      () =>
        useMutationWithToast(mutationFn, {
          success: "toast.saved",
          failure: "toast.saveFailed",
        }),
      { wrapper: createWrapper(client) },
    );

    await expect(result.current.mutateAsync()).rejects.toThrow("unknown");
    expect(toast.error).toHaveBeenCalledWith("toast.saveFailed");
  });

  it("calls onSuccess callback after toast", async () => {
    const onSuccess = vi.fn();
    const mutationFn = vi.fn(async (n: number) => n * 2);
    const { result } = renderHook(
      () =>
        useMutationWithToast(
          mutationFn,
          { success: "toast.saved", failure: "toast.saveFailed" },
          { onSuccess },
        ),
      { wrapper: createWrapper(client) },
    );

    await result.current.mutateAsync(21);
    expect(onSuccess).toHaveBeenCalledWith(42, 21);
  });
});
