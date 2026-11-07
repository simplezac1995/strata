// SPDX-License-Identifier: Apache-2.0
import { renderHook } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";

// Mock web-vitals module: each on* callback fires synchronously at registration.
vi.mock("web-vitals", () => ({
  onCLS: vi.fn((cb) =>
    cb({ name: "CLS", value: 0.05, delta: 0.05, id: "1", navigationType: "navigate" }),
  ),
  onFCP: vi.fn((cb) =>
    cb({ name: "FCP", value: 1000, delta: 1000, id: "2", navigationType: "navigate" }),
  ),
  onINP: vi.fn((cb) =>
    cb({ name: "INP", value: 150, delta: 150, id: "3", navigationType: "navigate" }),
  ),
  onLCP: vi.fn((cb) =>
    cb({ name: "LCP", value: 2000, delta: 2000, id: "4", navigationType: "navigate" }),
  ),
  onTTFB: vi.fn((cb) =>
    cb({ name: "TTFB", value: 500, delta: 500, id: "5", navigationType: "navigate" }),
  ),
}));

describe("useWebVitals", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    // The merged implementation is a module-level singleton; reset the module
    // registry so each case starts with a fresh (un-initialized) instance.
    vi.resetModules();
  });

  it("should initialize web vitals monitoring", async () => {
    const { useWebVitals } = await import("./useWebVitals");
    const { result } = renderHook(() => useWebVitals({ enabled: true }));
    expect(result.current.initialized).toBe(true);
  });

  it("should collect metrics", async () => {
    const { useWebVitals } = await import("./useWebVitals");
    const { result } = renderHook(() => useWebVitals({ enabled: true }));
    expect(Object.keys(result.current.metrics).length).toBeGreaterThan(0);
  });

  it("should get metric by name", async () => {
    const { useWebVitals } = await import("./useWebVitals");
    const { result } = renderHook(() => useWebVitals({ enabled: true }));
    const lcp = result.current.getMetric("LCP");
    expect(lcp).toBeDefined();
    expect(lcp?.name).toBe("LCP");
    expect(lcp?.value).toBe(2000);
  });

  it("should return undefined for unknown metric", async () => {
    const { useWebVitals } = await import("./useWebVitals");
    const { result } = renderHook(() => useWebVitals({ enabled: true }));
    expect(result.current.getMetric("UNKNOWN")).toBeUndefined();
  });

  it("should get summary", async () => {
    const { useWebVitals } = await import("./useWebVitals");
    const { result } = renderHook(() => useWebVitals({ enabled: true }));
    const summary = result.current.getSummary();
    expect(summary.total).toBeGreaterThan(0);
    expect(summary.good).toBeGreaterThan(0);
  });

  it("should call custom onReport callback", async () => {
    const { useWebVitals } = await import("./useWebVitals");
    const onReport = vi.fn();
    renderHook(() => useWebVitals({ enabled: true, onReport }));
    expect(onReport).toHaveBeenCalled();
  });

  it("should not initialize twice", async () => {
    const { useWebVitals } = await import("./useWebVitals");
    const { result, rerender } = renderHook(() => useWebVitals({ enabled: true }));
    const firstInit = result.current.initialized;
    rerender();
    expect(result.current.initialized).toBe(firstInit);
  });
});
