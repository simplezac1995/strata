// SPDX-License-Identifier: Apache-2.0
/**
 * Tree-shaken ECharts wrapper.
 *
 * Registers only the chart types, components and renderer actually used by Strata so the
 * bundler can drop the rest of ECharts/zrender. Never import the default `echarts-for-react`
 * or from `echarts` in a chart component — that pulls the full (~1 MB) bundle into the chunk.
 */
import type { ComponentProps } from "react";
// `/lib/core` is a CJS module (`exports.default = EChartsReactCore`). Under Vite dev
// pre-bundling its default interop can surface the whole module namespace (an object)
// instead of the class, which crashes React with "Element type is invalid ... got object".
// Unwrap defensively: the class in production build already lands on `.default`.
import EChartsReactCoreModule from "echarts-for-react/lib/core";
import * as echarts from "echarts/core";
import { BarChart, LineChart, PieChart } from "echarts/charts";
import {
  GridComponent,
  LegendComponent,
  TitleComponent,
  TooltipComponent,
} from "echarts/components";
import { CanvasRenderer } from "echarts/renderers";

echarts.use([
  BarChart,
  LineChart,
  PieChart,
  GridComponent,
  LegendComponent,
  TitleComponent,
  TooltipComponent,
  CanvasRenderer,
]);

// Unwrap the CJS default interop defensively (see import comment above).
const EChartsReactCore =
  (
    EChartsReactCoreModule as {
      default?: typeof EChartsReactCoreModule;
    }
  ).default ?? EChartsReactCoreModule;

type EChartsProps = Omit<ComponentProps<typeof EChartsReactCore>, "echarts">;

export function ECharts(props: EChartsProps) {
  return <EChartsReactCore echarts={echarts} {...props} />;
}
