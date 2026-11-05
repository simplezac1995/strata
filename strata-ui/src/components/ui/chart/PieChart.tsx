// SPDX-License-Identifier: Apache-2.0
import { useMemo } from "react";
import { useTheme } from "@mui/material/styles";
import { ECharts as ReactECharts } from "./ECharts";
import type { PieChartProps } from "./types";
import { CHART_COLORS } from "./types";

/**
 * Pie chart component based on ECharts.
 *
 * Displays data as a pie or donut chart with optional labels and legend.
 *
 * @example
 * ```tsx
 * <PieChart
 *   title="Traffic Sources"
 *   data={[
 *     { name: 'Direct', value: 335 },
 *     { name: 'Search', value: 310 },
 *     { name: 'Social', value: 234 },
 *     { name: 'Referral', value: 135 },
 *   ]}
 *   height={300}
 *   donut
 * />
 * ```
 */
export function PieChart({
  title,
  data,
  height = 300,
  radius,
  donut = false,
  showLabels = true,
  showLegend = true,
  showTooltip = true,
  option,
}: PieChartProps) {
  const theme = useTheme();
  const { background, text, divider } = theme.palette;

  const chartOption = useMemo(() => {
    const pieRadius = radius || (donut ? ["40%", "70%"] : "70%");

    return {
      title: title ? { text: title, left: "center", top: 10 } : undefined,
      tooltip: showTooltip
        ? {
            trigger: "item",
            formatter: "{b}: {c} ({d}%)",
            backgroundColor: background.paper,
            borderColor: divider,
            textStyle: { color: text.primary },
          }
        : undefined,
      legend: showLegend
        ? {
            orient: "horizontal",
            bottom: 10,
            type: "scroll",
            textStyle: { color: text.secondary },
          }
        : undefined,
      series: [
        {
          name: title || "Pie Chart",
          type: "pie",
          radius: pieRadius,
          center: ["50%", showLegend ? "45%" : "50%"],
          avoidLabelOverlap: true,
          itemStyle: {
            borderRadius: 6,
            borderColor: background.paper,
            borderWidth: 2,
          },
          label: showLabels
            ? {
                show: true,
                formatter: "{b}: {d}%",
                color: text.secondary,
                fontSize: 12,
              }
            : { show: false },
          labelLine: showLabels ? { show: true, length: 10, length2: 10 } : { show: false },
          emphasis: {
            label: {
              show: true,
              fontSize: 14,
              fontWeight: "bold",
            },
            itemStyle: {
              shadowBlur: 10,
              shadowOffsetX: 0,
              shadowColor: "rgba(0, 0, 0, 0.2)",
            },
          },
          data: data.map((item, index) => ({
            ...item,
            itemStyle: item.color
              ? { color: item.color }
              : { color: CHART_COLORS[index % CHART_COLORS.length] },
          })),
        },
      ],
      ...option,
    };
  }, [
    title,
    data,
    radius,
    donut,
    showLabels,
    showLegend,
    showTooltip,
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
