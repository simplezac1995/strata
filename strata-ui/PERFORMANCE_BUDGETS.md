# Strata UI 性能预算与优化指南

## 性能预算（Performance Budgets）

### 核心指标

| 指标                     | 预算     | 说明                       |
| ------------------------ | -------- | -------------------------- |
| **首屏 JS 总量（gzip）** | ≤ 300 kB | 主入口 + 关键依赖          |
| **单个 chunk 大小**      | ≤ 600 kB | bpmn-js 例外（独立懒加载） |
| **首屏 CSS 总量**        | ≤ 150 kB | MUI 主题 + 页面样式        |
| **LCP（最大内容绘制）**  | ≤ 2.5s   | 首屏主要内容加载时间       |
| **FID（首次输入延迟）**  | ≤ 100ms  | 用户首次交互响应时间       |
| **CLS（累积布局偏移）**  | ≤ 0.1    | 页面布局稳定性             |
| **TTI（可交互时间）**    | ≤ 3.5s   | 页面完全可交互时间         |

### Bundle 大小预算

| Chunk                  | 预算（原始） | 预算（gzip） | 当前大小              | 状态 |
| ---------------------- | ------------ | ------------ | --------------------- | ---- |
| **react-vendor**       | ≤ 250 kB     | ≤ 80 kB      | 216.88 kB / 69.58 kB  | ✅   |
| **mui**                | ≤ 400 kB     | ≤ 120 kB     | 382.59 kB / 112.17 kB | ✅   |
| **emotion**            | ≤ 40 kB      | ≤ 15 kB      | 33.10 kB / 13.19 kB   | ✅   |
| **axios**              | ≤ 50 kB      | ≤ 20 kB      | 47.13 kB / 17.87 kB   | ✅   |
| **react-query**        | ≤ 35 kB      | ≤ 10 kB      | 28.33 kB / 8.71 kB    | ✅   |
| **i18n-zh**            | ≤ 80 kB      | ≤ 25 kB      | 72.21 kB / 23.34 kB   | ✅   |
| **i18n-en**            | ≤ 80 kB      | ≤ 25 kB      | 73.50 kB / 20.90 kB   | ✅   |
| **index（主入口）**    | ≤ 150 kB     | ≤ 40 kB      | 126.46 kB / 35.57 kB  | ✅   |
| **bpmn-js**（懒加载）  | ≤ 600 kB     | ≤ 170 kB     | 568.55 kB / 163.46 kB | ✅   |
| **页面 chunk（平均）** | ≤ 15 kB      | ≤ 5 kB       | 5-12 kB / 1.5-4 kB    | ✅   |

## 已实施的优化措施

### 1. 代码分割（Code Splitting）

- **路由级懒加载**：87 个页面全部使用 `lazyPage()` 懒加载
- **第三方库拆分**：mui、react-vendor、axios、react-query、bpmn-js、emotion 独立 chunk
- **i18n 语言包拆分**：zh 和 en 独立 chunk，可并行加载和长期缓存
- **CSS 代码分割**：`cssCodeSplit: true`，每个入口 CSS 单独打包

### 2. 构建优化

- **target: es2022**：利用现代浏览器原生支持，减小转译开销（相比 es2020 减小约 8%）
- **assetsInlineLimit: 4096**：小于 4KB 的资源内联为 base64，减少 HTTP 请求
- **内容哈希文件名**：`[name]-[hash][extname]`，便于长期缓存
- **sourcemap: false**：生产环境关闭 source map，减小构建体积
- **chunkSizeWarningLimit: 1000**：合理的 chunk 大小警告阈值

### 3. MUI 优化

- **命名导入**：150 处 MUI 导入全部使用命名导入（`import { Box, Button } from "@mui/material"`），支持 Tree Shaking
- **MUI icons 按需导入**：183 处图标导入全部按需导入
- **Emotion 独立 chunk**：MUI 依赖的 Emotion 样式库独立打包，可长期缓存

### 4. 运行时性能监控

- **Web Vitals 监控**：集成 `web-vitals 6.2.1`，监控 LCP、FID、CLS、TTFB
- **useWebVitals Hook**：自定义 Hook 收集性能指标，可上报到后端
- **性能测试**：`useWebVitals.test.ts` 7 个测试用例

## 后续优化建议

### P0 - 高优先级

1. **i18n 语言包真正按需加载**
   - 当前：zh 和 en 同时加载（145 kB）
   - 优化：根据用户语言动态加载，首屏只加载当前语言（节省约 73 kB）
   - 实现：修改 `src/shared/i18n/index.tsx`，使用动态导入 + Zustand store

2. **MUI 主题按需引入**
   - 当前：完整 MUI 主题（382 kB）
   - 优化：使用 `@mui/material/styles` 的 `createTheme` 按需引入组件样式
   - 预期节省：50-100 kB

### P1 - 中优先级

3. **图片优化**
   - 使用 WebP/AVIF 格式
   - 实现图片懒加载（`loading="lazy"`）
   - 添加响应式图片（`srcset`）

4. **虚拟列表优化**
   - 大数据量表格使用 `VirtualList` 组件
   - 已实现：`src/components/ui/VirtualList.tsx`
   - 待应用：用户列表、角色列表、日志列表等大数据量页面

5. **React.memo 优化**
   - 高频重渲染组件使用 `React.memo`
   - 复杂计算使用 `useMemo` / `useCallback`

### P2 - 低优先级

6. **Service Worker 离线缓存**
   - 使用 Workbox 实现静态资源缓存
   - 实现 API 响应缓存（stale-while-revalidate）

7. **预加载关键资源**
   - 使用 `<link rel="preload">` 预加载关键字体和图片
   - 使用 `import()` 预加载下一个可能访问的页面

8. **Bundle 分析自动化**
   - CI/CD 集成 `rollup-plugin-visualizer`
   - 自动检测超出预算的 chunk，阻止合并

## 性能验证命令

```bash
# 构建并分析 Bundle
pnpm analyze

# 运行性能测试
pnpm test -- --run src/shared/performance

# 预览生产构建
pnpm preview

# Lighthouse 性能审计
# 在 Chrome DevTools 中运行 Lighthouse
```

## 参考资源

- [Web Vitals](https://web.dev/vitals/)
- [React 性能优化](https://react.dev/learn/render-and-commit)
- [Vite 构建优化](https://vitejs.dev/guide/build.html)
- [MUI 性能优化](https://mui.com/material-ui/guides/minimizing-bundle-size/)
- [Code Splitting](https://react.dev/learn/lazy-loading)
