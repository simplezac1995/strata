// SPDX-License-Identifier: Apache-2.0
import { vi } from "vitest";

/**
 * jsdom 无 canvas 上下文，echarts 初始化会抛
 * "Cannot set properties of null (setting 'dpr')"。
 * 将 echarts-for-react 替换为轻量占位 div，仅透传 style/className，
 * 使 Dashboard 等含图表的页面在单测环境下可正常渲染。
 *
 * 注意：工厂内使用动态 import（不用 require，项目未引入 @types/node，
 * tsc -b 会对 require 报 TS2591）。
 */
vi.mock("echarts-for-react", async () => {
  const React = await import("react");
  return {
    __esModule: true,
    default: (props: { style?: object; className?: string }) =>
      React.createElement("div", {
        "data-testid": "echarts-mock",
        style: props.style,
        className: props.className,
      }),
  };
});
