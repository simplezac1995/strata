// SPDX-License-Identifier: Apache-2.0
/**
 * Common chart type definitions for ECharts-based components.
 */

/** Basic data point for line/bar charts */
export interface ChartDataPoint {
  name: string;
  value: number;
}

/** Series data for multi-series charts */
export interface ChartSeries {
  name: string;
  data: number[];
  color?: string;
}

/** Pie chart data item */
export interface PieChartItem {
  name: string;
  value: number;
  color?: string;
}

/** Common chart props */
export interface BaseChartProps {
  /** Chart title (displayed in header) */
  title?: string;
  /** Chart subtitle */
  subtitle?: string;
  /** Chart height in pixels */
  height?: number;
  /** Whether to show the legend */
  showLegend?: boolean;
  /** Whether to show tooltips on hover */
  showTooltip?: boolean;
  /** Additional ECharts option overrides */
  option?: Record<string, unknown>;
}

/** Line chart props */
export interface LineChartProps extends BaseChartProps {
  /** X-axis categories */
  categories: string[];
  /** Data series */
  series: ChartSeries[];
  /** Whether to smooth the line */
  smooth?: boolean;
  /** Whether to show area under the line */
  areaStyle?: boolean;
  /** Y-axis label */
  yAxisLabel?: string;
}

/** Bar chart props */
export interface BarChartProps extends BaseChartProps {
  /** X-axis categories */
  categories: string[];
  /** Data series */
  series: ChartSeries[];
  /** Whether to use horizontal bars */
  horizontal?: boolean;
  /** Whether to stack bars */
  stack?: boolean;
  /** Y-axis label */
  yAxisLabel?: string;
}

/** Pie chart props */
export interface PieChartProps extends BaseChartProps {
  /** Pie data items */
  data: PieChartItem[];
  /** Pie radius (e.g., '40%', '70%') */
  radius?: string | [string, string];
  /** Whether to show as donut (ring) */
  donut?: boolean;
  /** Whether to show labels outside the pie */
  showLabels?: boolean;
}

/** Default chart colors */
export const CHART_COLORS = [
  "#1976d2", // primary blue
  "#388e3c", // green
  "#f57c00", // orange
  "#d32f2f", // red
  "#7b1fa2", // purple
  "#0097a7", // cyan
  "#c2185b", // pink
  "#5d4037", // brown
  "#455a64", // blue grey
  "#689f38", // light green
];
