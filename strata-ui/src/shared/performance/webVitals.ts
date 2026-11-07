// SPDX-License-Identifier: Apache-2.0
import { onCLS, onFCP, onINP, onLCP, onTTFB, type Metric } from "web-vitals";

/**
 * Web Vitals 指标类型（Core Web Vitals + 自定义 long task / 业务指标）
 */
export type WebVitalMetric = "CLS" | "FCP" | "INP" | "LCP" | "TTFB" | "custom";

/**
 * Web Vitals 指标数据
 */
export interface WebVitalData {
  name: WebVitalMetric;
  value: number;
  rating: "good" | "needs-improvement" | "poor";
  delta: number;
  id: string;
  navigationType: string;
  /** Effective network type (e.g. '4g'), when available. */
  connection?: string;
  /** Approximate device memory (GB), when available. */
  deviceMemory?: number;
  /** Logical CPU cores, when available. */
  hardwareConcurrency?: number;
  /** Collection epoch ms (buffered/custom metrics). */
  timestamp?: number;
  /** Page URL (buffered/custom metrics). */
  url?: string;
  /** User agent (buffered/custom metrics). */
  userAgent?: string;
}

/**
 * Web Vitals 配置
 */
export interface WebVitalsConfig {
  /** 是否启用本次上报注册，默认 true。为 false 时仅跳过挂载 onReport，不影响已启动的全局监控。 */
  enabled?: boolean;
  /** 上报回调（React Hook 用于收集到本地状态），默认 console 彩色日志 */
  onReport?: (metric: WebVitalData) => void;
  /** 上报到后端的 API 端点；设置后启用批量 sendBeacon 上报 */
  reportEndpoint?: string;
  /** 采样率 0-1，默认 1（全部采集）。按指标独立采样。 */
  sampleRate?: number;
  /** 是否打印到控制台，默认 import.meta.env.DEV */
  logToConsole?: boolean;
  /** 是否将评级作为 tag 上报到 Sentry（若可用），默认 true */
  reportToSentry?: boolean;
  /** 是否追踪 Long Tasks（>50ms），默认 true */
  trackLongTasks?: boolean;
  /** 是否追踪 Navigation Timing，默认 true */
  trackNavigationTiming?: boolean;
  /** 是否追踪慢资源（>1s），默认 true */
  trackResourceTiming?: boolean;
}

/**
 * 评级阈值表（唯一权威来源）。
 * [good 上限, needs-improvement 上限]，超过后者即为 poor。
 */
const RATING_THRESHOLDS: Record<string, [number, number]> = {
  LCP: [2500, 4000],
  CLS: [0.1, 0.25],
  FCP: [1800, 3000],
  TTFB: [800, 1800],
  INP: [200, 500],
};

/** 批量上报的 flush 间隔（ms）。 */
const FLUSH_INTERVAL = 5000;

/**
 * 判断指标评级
 */
function getRating(name: string, value: number): "good" | "needs-improvement" | "poor" {
  const [good, poor] = RATING_THRESHOLDS[name] ?? [2500, 4000];
  if (value <= good) return "good";
  if (value <= poor) return "needs-improvement";
  return "poor";
}

interface MonitorState {
  logToConsole: boolean;
  reportToSentry: boolean;
  reportEndpoint?: string;
  sampleRate: number;
  trackLongTasks: boolean;
  trackNavigationTiming: boolean;
  trackResourceTiming: boolean;
}

const DEFAULT_STATE: MonitorState = {
  logToConsole: import.meta.env.DEV,
  reportToSentry: true,
  reportEndpoint: undefined,
  sampleRate: 1,
  trackLongTasks: true,
  trackNavigationTiming: true,
  trackResourceTiming: true,
};

let state: MonitorState = { ...DEFAULT_STATE };
const reportCallbacks = new Set<(metric: WebVitalData) => void>();
const metricsBuffer: WebVitalData[] = [];
let vitalsStarted = false;
let flushTimer: ReturnType<typeof setInterval> | null = null;

/**
 * 获取设备/环境信息（网络类型、内存、CPU 核数）
 */
function getDeviceInfo(): Pick<
  WebVitalData,
  "connection" | "deviceMemory" | "hardwareConcurrency"
> {
  const nav = navigator as Navigator & {
    connection?: { effectiveType: string };
    deviceMemory?: number;
  };
  return {
    connection: nav.connection?.effectiveType,
    deviceMemory: nav.deviceMemory,
    hardwareConcurrency: navigator.hardwareConcurrency,
  };
}

/**
 * 将 web-vitals Metric 转换为统一格式（附设备与页面上下文）
 */
function toWebVitalData(metric: Metric): WebVitalData {
  return {
    name: metric.name as WebVitalMetric,
    value: metric.value,
    rating: getRating(metric.name, metric.value),
    delta: metric.delta,
    id: metric.id,
    navigationType: metric.navigationType,
    timestamp: Date.now(),
    url: window.location.href,
    userAgent: navigator.userAgent,
    ...getDeviceInfo(),
  };
}

/**
 * 默认控制台上报（按评级着色）
 */
function defaultReport(metric: WebVitalData): void {
  const style =
    metric.rating === "good"
      ? "color: #22c55e"
      : metric.rating === "needs-improvement"
        ? "color: #eab308"
        : "color: #ef4444";
  // eslint-disable-next-line no-console
  console.log(
    `%c[Web Vitals] ${metric.name}: ${metric.value.toFixed(2)}ms (${metric.rating})`,
    style,
  );
}

/**
 * 将评级作为 tag 上报到 Sentry（若可用）
 */
function reportToSentry(metric: WebVitalData): void {
  try {
    const Sentry = (
      window as unknown as { Sentry?: { setTag: (key: string, value: string) => void } }
    ).Sentry;
    if (Sentry?.setTag) {
      Sentry.setTag(`web_vitals_${metric.name.toLowerCase()}`, metric.rating);
    }
  } catch {
    // Sentry 不可用
  }
}

/**
 * 将缓冲指标批量发送到后端（优先 sendBeacon，pagehide 也可送达）
 */
async function flushMetrics(): Promise<void> {
  if (metricsBuffer.length === 0 || !state.reportEndpoint) return;

  const batch = metricsBuffer.splice(0, metricsBuffer.length);
  try {
    const payload = JSON.stringify({ metrics: batch });
    if (navigator.sendBeacon) {
      const blob = new Blob([payload], { type: "application/json" });
      navigator.sendBeacon(state.reportEndpoint, blob);
    } else {
      await fetch(state.reportEndpoint, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: payload,
        keepalive: true,
      });
    }
  } catch (error) {
    console.warn("[Web Vitals] Failed to report metrics:", error);
    // 重新入队（上限保护），等待下次 flush
    if (batch.length < 100) metricsBuffer.unshift(...batch);
  }
}

/**
 * 统一处理一条 Web Vitals 指标：采样 -> 回调 -> 控制台 -> Sentry -> 缓冲
 */
function handleMetric(metric: Metric): void {
  // 按指标采样
  if (Math.random() > state.sampleRate) return;

  const data = toWebVitalData(metric);

  reportCallbacks.forEach((cb) => {
    try {
      cb(data);
    } catch {
      // 消费方异常不得影响监控链路
    }
  });

  if (state.logToConsole) defaultReport(data);
  if (state.reportToSentry) reportToSentry(data);
  if (state.reportEndpoint) metricsBuffer.push(data);
}

/**
 * 追踪 Long Tasks（>50ms）
 */
function startLongTasks(): void {
  if (!("PerformanceObserver" in window)) return;
  try {
    const observer = new PerformanceObserver((list) => {
      for (const entry of list.getEntries()) {
        const data: WebVitalData = {
          name: "custom",
          value: entry.duration,
          rating:
            entry.duration > 200 ? "poor" : entry.duration > 100 ? "needs-improvement" : "good",
          delta: 0,
          id: `longtask-${entry.startTime}`,
          navigationType: "longtask",
          timestamp: Date.now(),
          url: window.location.href,
          userAgent: navigator.userAgent,
          ...getDeviceInfo(),
        };
        if (state.logToConsole) {
          console.warn(`[Web Vitals] Long task: ${entry.duration.toFixed(0)}ms`, entry);
        }
        if (state.reportEndpoint) metricsBuffer.push(data);
      }
    });
    observer.observe({ entryTypes: ["longtask"] });
  } catch {
    // 不支持 longtask 观测
  }
}

/**
 * 追踪慢资源（>1s，仅控制台告警）
 */
function startResourceTiming(): void {
  if (!("PerformanceResourceTiming" in window)) return;
  try {
    const observer = new PerformanceObserver((list) => {
      for (const entry of list.getEntries() as PerformanceResourceTiming[]) {
        if (entry.duration > 1000 && state.logToConsole) {
          console.warn(
            `[Web Vitals] Slow resource: ${entry.name} (${entry.duration.toFixed(0)}ms)`,
          );
        }
      }
    });
    observer.observe({ entryTypes: ["resource"] });
  } catch {
    // 不支持 resource 观测
  }
}

/**
 * 启动批量上报定时器与 pagehide 兜底 flush
 */
function startFlushTimer(): void {
  if (flushTimer) return;
  flushTimer = setInterval(() => {
    void flushMetrics();
  }, FLUSH_INTERVAL);
  window.addEventListener("pagehide", () => {
    void flushMetrics();
  });
  window.addEventListener("beforeunload", () => {
    void flushMetrics();
  });
}

/**
 * 初始化 Web Vitals 监控（幂等：全局仅启动一次，多次调用仅合并配置 / 追加回调）。
 *
 * @example
 * ```ts
 * initWebVitals({
 *   reportEndpoint: "/api/v1/metrics/frontend",
 *   sampleRate: 0.1,
 * });
 * ```
 */
export function initWebVitals(config: WebVitalsConfig = {}): void {
  const {
    enabled = true,
    onReport,
    reportEndpoint,
    sampleRate,
    logToConsole,
    reportToSentry,
    trackLongTasks,
    trackNavigationTiming,
    trackResourceTiming,
  } = config;

  // 追加消费方回调（React Hook 等），无论全局是否已启动
  if (onReport) reportCallbacks.add(onReport);

  if (!enabled) return;

  // 仅合并显式传入的配置项
  if (reportEndpoint !== undefined) state.reportEndpoint = reportEndpoint;
  if (sampleRate !== undefined) state.sampleRate = sampleRate;
  if (logToConsole !== undefined) state.logToConsole = logToConsole;
  if (reportToSentry !== undefined) state.reportToSentry = reportToSentry;
  if (trackLongTasks !== undefined) state.trackLongTasks = trackLongTasks;
  if (trackNavigationTiming !== undefined) state.trackNavigationTiming = trackNavigationTiming;
  if (trackResourceTiming !== undefined) state.trackResourceTiming = trackResourceTiming;

  if (vitalsStarted) return;
  vitalsStarted = true;

  // 采集所有核心指标
  onCLS(handleMetric);
  onFCP(handleMetric);
  onINP(handleMetric);
  onLCP(handleMetric);
  onTTFB(handleMetric);

  if (state.trackLongTasks) startLongTasks();
  if (state.trackResourceTiming) startResourceTiming();

  if (state.reportEndpoint) startFlushTimer();
}

/**
 * 记录一条自定义性能指标（ms）。未启用后端上报时仅写控制台。
 */
export function recordCustomMetric(
  name: string,
  value: number,
  tags?: Record<string, string>,
): void {
  const data: WebVitalData = {
    name: "custom",
    value,
    rating: "good",
    delta: 0,
    id: `custom-${Date.now()}`,
    navigationType: "custom",
    timestamp: Date.now(),
    url: window.location.href,
    userAgent: navigator.userAgent,
    ...getDeviceInfo(),
  };

  if (state.logToConsole) {
    // eslint-disable-next-line no-console
    console.log(`[Web Vitals] Custom metric: ${name} = ${value.toFixed(0)}ms`, tags);
  }
  if (state.reportEndpoint) metricsBuffer.push(data);
}

/**
 * 启动一个性能计时器，用于测量自定义操作。
 *
 * @param label 计时器标签
 * @returns 带 end() 的计时器，end() 返回耗时（ms）
 */
export function startPerformanceTimer(label: string): { end: () => number } {
  const startTime = performance.now();
  return {
    end: () => {
      const duration = performance.now() - startTime;
      recordCustomMetric(label, duration);
      return duration;
    },
  };
}

/**
 * 获取当前性能监控摘要
 */
export function getPerformanceSummary(): {
  url: string;
  timestamp: number;
  metricsCollected: number;
  bufferSize: number;
} {
  return {
    url: window.location.href,
    timestamp: Date.now(),
    metricsCollected: metricsBuffer.length,
    bufferSize: metricsBuffer.length,
  };
}

/**
 * 停止性能监控并清理（最终 flush 后重置单例状态）
 */
export function stopPerformanceMonitoring(): void {
  if (flushTimer) {
    clearInterval(flushTimer);
    flushTimer = null;
  }
  void flushMetrics();
  reportCallbacks.clear();
  vitalsStarted = false;
}

/**
 * Web Vitals 性能预算（用于 CI/CD 检查指标是否达标）
 */
export const WEB_VITALS_BUDGET = {
  LCP: 2500, // 最大内容绘制 < 2.5s
  CLS: 0.1, // 累积布局偏移 < 0.1
  FCP: 1800, // 首次内容绘制 < 1.8s
  TTFB: 800, // 首字节时间 < 800ms
  INP: 200, // 交互到下一次绘制 < 200ms
} as const;
