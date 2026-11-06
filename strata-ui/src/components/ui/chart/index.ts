// SPDX-License-Identifier: Apache-2.0
/**
 * Chart components based on ECharts.
 *
 * Provides reusable chart components with MUI-styled containers:
 * - LineChart: Line chart with smoothing and area styling
 * - BarChart: Bar chart with horizontal/vertical and stacking options
 * - PieChart: Pie/donut chart with labels and legend
 * - ChartCard: MUI Paper wrapper with title and subtitle
 *
 * @example
 * ```tsx
 * import { ChartCard, LineChart } from '@/components/ui/chart';
 *
 * <ChartCard title="Monthly Revenue">
 *   <LineChart
 *     categories={['Jan', 'Feb', 'Mar']}
 *     series={[{ name: 'Revenue', data: [100, 200, 150] }]}
 *   />
 * </ChartCard>
 * ```
 */

export { LineChart } from "./LineChart";
export { BarChart } from "./BarChart";
export { PieChart } from "./PieChart";
export { ChartCard } from "./ChartCard";
export type {
  ChartDataPoint,
  ChartSeries,
  PieChartItem,
  BaseChartProps,
  LineChartProps,
  BarChartProps,
  PieChartProps,
} from "./types";
export { CHART_COLORS } from "./types";
