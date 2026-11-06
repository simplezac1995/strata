// SPDX-License-Identifier: Apache-2.0
import { useMemo } from "react";
import { useTheme } from "@mui/material/styles";
import { ECharts as ReactECharts } from "./ECharts";
import type { LineChartProps } from "./types";
import { CHART_COLORS } from "./types";

/**
 * Line chart component based on ECharts.
 *
 * Displays one or more data series as lines, with optional smoothing and area styling.
 *
 * @example
 * ```tsx
 * <LineChart
 *   title="Monthly Revenue"
 *   categories={['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun']}
 *   series={[
 *     { name: '2024', data: [120, 200, 150, 80, 70, 110] },
 *     { name: '2025', data: [200, 150, 220, 180, 250, 300] },
 *   ]}
 *   height={300}
 * />
 * ```
 */
export function LineChart({
  title,
  categories,
  series,
  height = 300,
  smooth = true,
  areaStyle = false,
  showLegend = true,
  showTooltip = true,
  yAxisLabel,
  option,
}: LineChartProps) {
  const theme = useTheme();
  const { background, text, divider } = theme.palette;

  const chartOption = useMemo(() => {
    return {
      title: title ? { text: title, left: "center", top: 10 } : undefined,
      tooltip: showTooltip
        ? {
            trigger: "axis",
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
        type: "category",
        boundaryGap: false,
        data: categories,
        axisLine: { lineStyle: { color: divider } },
        axisLabel: { color: text.secondary },
      },
      yAxis: {
        type: "value",
        name: yAxisLabel,
        nameTextStyle: { color: text.secondary },
        axisLine: { show: false },
        axisLabel: { color: text.secondary },
        splitLine: { lineStyle: { color: divider } },
      },
      series: series.map((s, index) => ({
        name: s.name,
        type: "line",
        data: s.data,
        smooth,
        symbol: "circle",
        symbolSize: 6,
        lineStyle: { width: 2, color: s.color || CHART_COLORS[index % CHART_COLORS.length] },
        itemStyle: { color: s.color || CHART_COLORS[index % CHART_COLORS.length] },
        areaStyle: areaStyle
          ? {
              color: {
                type: "linear",
                x: 0,
                y: 0,
                x2: 0,
                y2: 1,
                colorStops: [
                  {
                    offset: 0,
                    color: `${s.color || CHART_COLORS[index % CHART_COLORS.length]}33`,
                  },
                  {
                    offset: 1,
                    color: `${s.color || CHART_COLORS[index % CHART_COLORS.length]}05`,
                  },
                ],
              },
            }
          : undefined,
      })),
      ...option,
    };
  }, [
    title,
    categories,
    series,
    smooth,
    areaStyle,
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
