// SPDX-License-Identifier: Apache-2.0
import { useEffect, useState } from "react";
import { initWebVitals, type WebVitalData, type WebVitalsConfig } from "./webVitals";

/**
 * Web Vitals 监控 Hook
 *
 * 在应用根组件中使用，自动初始化 Web Vitals 监控并采集指标。
 *
 * @example
 * ```tsx
 * function App() {
 *   const { metrics } = useWebVitals({
 *     reportEndpoint: "/api/v1/metrics/frontend",
 *   });
 *   return <div>...</div>;
 * }
 * ```
 */
export function useWebVitals(config: WebVitalsConfig = {}) {
  const [metrics, setMetrics] = useState<Record<string, WebVitalData>>({});
  const [initialized, setInitialized] = useState(false);

  useEffect(() => {
    if (initialized) return;

    initWebVitals({
      ...config,
      onReport: (metric) => {
        setMetrics((prev) => ({
          ...prev,
          [metric.name]: metric,
        }));
        config.onReport?.(metric);
      },
    });

    setInitialized(true);
  }, [config, initialized]);

  /**
   * 获取指定指标的当前值
   */
  const getMetric = (name: string): WebVitalData | undefined => metrics[name];

  /**
   * 获取所有指标的评级摘要
   */
  const getSummary = () => {
    const values = Object.values(metrics);
    return {
      total: values.length,
      good: values.filter((m) => m.rating === "good").length,
      needsImprovement: values.filter((m) => m.rating === "needs-improvement").length,
      poor: values.filter((m) => m.rating === "poor").length,
    };
  };

  return {
    metrics,
    initialized,
    getMetric,
    getSummary,
  };
}
