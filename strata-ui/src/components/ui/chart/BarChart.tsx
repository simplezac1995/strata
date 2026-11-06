// SPDX-License-Identifier: Apache-2.0
import { useMemo } from "react";
import { useTheme } from "@mui/material/styles";
import { ECharts as ReactECharts } from "./ECharts";
import type { BarChartProps } from "./types";
import { CHART_COLORS } from "./types";

/**
 * Bar chart component based on ECharts.
 *
 * Displays one or more data series as bars, with optional horizontal layout and stacking.
 *
 * @example
 * ```tsx
 * <BarChart
 *   title="Sales by Category"
 *   categories={['Electronics', 'Clothing', 'Food', 'Books']}
 *   series={[
 *     { name: 'Online', data: [450, 320, 280, 200] },
 *     { name: 'Retail', data: [380, 420, 350, 180] },
 *   ]}
 *   height={300}
 * />
 * ```
 */
export function BarChart({
  title,
  categories,
  series,
  height = 300,
  horizontal = false,
  stack = false,
  showLegend = true,
  showTooltip = true,
  yAxisLabel,
  option,
}: BarChartProps) {
  const theme = useTheme();
  const { background, text, divider } = theme.palette;

  const chartOption = useMemo(() => {
    const xAxisType = horizontal ? "value" : "category";
    const yAxisType = horizontal ? "category" : "value";

    return {
      title: title ? { text: title, left: "center", top: 10 } : undefined,
      tooltip: showTooltip
        ? {
            trigger: "axis",
            axisPointer: { type: "shadow" },
            backgroundColor: background.paper,
            borderColor: divider,
            textStyle: { color: text.primary },
          }
        : undefined,
      legend: showLegend
        ? {
            data: series.map((s) => s.name),
            bottom: 10,
            type: "scroll",
            textStyle: { color: text.secondary },
          }
        : undefined,
      grid: {
        left: "3%",
        right: "4%",
        bottom: showLegend ? "15%" : "3%",
        top: title ? "15%" : "5%",
        containLabel: true,
      },
      xAxis: {
        type: xAxisType,
        data: horizontal ? undefined : categories,
        name: horizontal ? yAxisLabel : undefined,
        nameTextStyle: { color: text.secondary },
        axisLine: { lineStyle: { color: divider } },
        axisLabel: { color: text.secondary },
        splitLine: horizontal ? { lineStyle: { color: divider } } : undefined,
      },
      yAxis: {
        type: yAxisType,
        data: horizontal ? categories : undefined,
        name: horizontal ? undefined : yAxisLabel,
        nameTextStyle: { color: text.secondary },
        axisLine: horizontal ? { lineStyle: { color: divider } } : { show: false },
        axisLabel: { color: text.secondary },
        splitLine: horizontal ? undefined : { lineStyle: { color: divider } },
      },
      series: series.map((s, index) => ({
        name: s.name,
        type: "bar",
        data: s.data,
        stack: stack ? "total" : undefined,
        barMaxWidth: 40,
        itemStyle: {
          color: s.color || CHART_COLORS[index % CHART_COLORS.length],
          borderRadius: horizontal ? [0, 4, 4, 0] : [4, 4, 0, 0],
        },
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: "rgba(0, 0, 0, 0.2)",
          },
        },
      })),
      ...option,
    };
  }, [
    title,
    categories,
    series,
    horizontal,
    stack,
    showLegend,
    showTooltip,
    yAxisLabel,
    option,
    background,
    text,
    divider,
  ]);

  return (
    <ReactECharts
      option={chartOption}
      style={{ height, width: "100%" }}
      opts={{ renderer: "canvas" }}
      notMerge
    />
  );
}
