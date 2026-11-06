import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import { visualizer } from "rollup-plugin-visualizer";
import { resolve } from "node:path";
import type { IncomingHttpHeaders } from "node:http";

const apiProxyTarget = process.env.VITE_API_PROXY_TARGET ?? "http://localhost:8080";
const analyze = process.env.ANALYZE === "true";

/**
 * 后端对所有响应统一下发 `X-Frame-Options: DENY` 与
 * `Content-Security-Policy: frame-ancestors 'none'`（见后端 SecurityHeaders），
 * 内嵌 Swagger UI 的 iframe 会被浏览器以"拒绝连接"拦截。dev 下 SPA 与后端经 vite
 * 代理同源访问，这里把 Swagger UI 相关响应放宽为允许同源内嵌；生产由后端按路径放行。
 */
function relaxFrameHeaders(headers: IncomingHttpHeaders) {
  delete headers["x-frame-options"];
  const csp = headers["content-security-policy"];
  if (typeof csp === "string") {
    headers["content-security-policy"] = csp.replace(
      /frame-ancestors[^;]*/g,
      "frame-ancestors 'self'",
    );
  }
}

/**
 * 微前端 / Module Federation 预留（disabled by default）。
 *
 * 当前 Strata 是单体前端（单 SPA 入口），不启用模块联邦。这里仅为未来按业务域
 * （casino / ai / system …）拆成独立可部署子应用预留接入点：
 *  - 引入 `@module-federation/vite` 作为 plugins 之一；
 *  - 在 build.rollupOptions 之外增加 `federation` 配置（name / filename / exposes / remotes / shared）；
 *  - remotes 通过运行时路由懒加载（与现有 `src/routes/lazyPages.ts` 对齐）。
 *
 * 启用前必须先独立立项评估：shared 版本对齐（react / react-dom / @mui）、样式隔离、
 * 路由与权限合并、独立部署流水线。在未立项前不要在这里实际挂载联邦插件，以免
 * 改变打包产物与 chunk 体积门禁（单 chunk ≤ 600 kB）。
 */

export default defineConfig({
  plugins: [
    react(),
    analyze &&
      visualizer({
        filename: "dist/stats.html",
        title: "Strata UI Bundle Analysis",
        gzipSize: true,
        brotliSize: true,
        open: true,
      }),
  ].filter(Boolean),
  resolve: {
    alias: {
      "@": resolve(import.meta.dirname, "./src"),
    },
  },
  optimizeDeps: {
    include: [
      "echarts-for-react/lib/core",
      "echarts/core",
      "echarts/charts",
      "echarts/components",
      "echarts/renderers",
    ],
  },
  build: {
    // 目标浏览器：支持原生 ES 模块、动态导入和 import.meta
    target: "es2022",
    // CSS 代码分割：每个入口的 CSS 单独打包
    cssCodeSplit: true,
    // 资源内联限制：小于 4KB 的资源内联为 base64，减少 HTTP 请求
    assetsInlineLimit: 4096,
    // 生产环境关闭 source map 以减小构建体积（错误追踪使用 Sentry 等服务）
    sourcemap: false,
    // Chunk 大小警告阈值：bpmn-js 独立 chunk 约 570kB，属正常范围
    chunkSizeWarningLimit: 1000,
    rollupOptions: {
      output: {
        // 静态资源文件名添加内容哈希，便于长期缓存
        assetFileNames: "assets/[name]-[hash][extname]",
        chunkFileNames: "assets/[name]-[hash].js",
        entryFileNames: "assets/[name]-[hash].js",
        manualChunks(id) {
          if (id.includes("node_modules/@mui")) return "mui";
          if (id.includes("node_modules/axios")) return "axios";
          // Sentry 独立 chunk：仅在 errorReporter 动态 import 时加载，不进首屏
          if (id.includes("node_modules/@sentry")) return "sentry";
          // ECharts + zrender split into a cacheable vendor chunk loaded only by chart pages
          if (id.includes("node_modules/echarts") || id.includes("node_modules/zrender")) {
            return "echarts";
          }
          // i18n 语言包拆分：zh 和 en 独立 chunk，按需加载
          if (id.includes("/shared/i18n/zh")) return "i18n-zh";
          if (id.includes("/shared/i18n/en")) return "i18n-en";
          if (
            id.includes("node_modules/react-dom") ||
            id.includes("node_modules/react-router") ||
            id.includes("node_modules/react/")
          ) {
            return "react-vendor";
          }
          if (id.includes("node_modules/@tanstack/react-query")) return "react-query";
          if (id.includes("node_modules/bpmn-js") || id.includes("node_modules/diagram-js")) {
            return "bpmn-js";
          }
          // Emotion 样式库拆分（MUI 依赖）
          if (id.includes("node_modules/@emotion")) return "emotion";
          // Rich-text editor: shared components import it statically; keep it out of the entry chunk
          if (id.includes("node_modules/@tiptap")) return "tiptap";
          // Any other dependency must not land in the entry chunk: group remaining vendor code by package.
          if (id.includes("node_modules")) {
            const match = id.match(/node_modules\/((?:@[^/]+\/)?[^/]+)/);
            if (match) return `vendor-${match[1].replace("@", "").replace(/\//g, "-")}`;
          }
        },
      },
    },
  },
  server: {
    port: 5173,
    proxy: {
      "/api": apiProxyTarget,
      "/actuator": apiProxyTarget,
      "/swagger-ui": {
        target: apiProxyTarget,
        configure: (proxy) => {
          proxy.on("proxyRes", (proxyRes) => relaxFrameHeaders(proxyRes.headers));
        },
      },
      "/v3/api-docs": {
        target: apiProxyTarget,
        configure: (proxy) => {
          proxy.on("proxyRes", (proxyRes) => relaxFrameHeaders(proxyRes.headers));
        },
      },
    },
  },
});
