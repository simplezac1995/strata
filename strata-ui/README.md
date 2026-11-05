# Strata UI

后台管理前端：**Vite 8 + React 19 + MUI 9**（`@mui/material` + `@mui/icons-material`）+
TanStack Query 5 + Zustand 5 + React Router 7。路由级代码分割、后端驱动权限、语义短键 i18n、
WebSocket 通知与 PWA。

包管理器固定为 **pnpm**（`packageManager: pnpm@11.7.0`），不要用 npm/yarn。

## 快速开始

```bash
pnpm install
pnpm dev          # http://localhost:5173，/api 与 /actuator 代理到 :8080
```

## 命令（`package.json` scripts）

| 命令                                            | 作用                                                                                               |
| :---------------------------------------------- | :------------------------------------------------------------------------------------------------- |
| `pnpm dev`                                      | Vite 开发服务器                                                                                    |
| `pnpm build`                                    | `tsc -b` 类型检查后 `vite build` 产出                                                              |
| `pnpm preview`                                  | 预览构建产物                                                                                       |
| `pnpm typecheck`                                | `tsc -b` 纯类型检查                                                                                |
| `pnpm lint`                                     | `oxlint src`                                                                                       |
| `pnpm test`                                     | `vitest run`（单元/组件测试）                                                                      |
| `pnpm check`                                    | `node scripts/check.mjs` 完整门禁（typecheck + lint + test + i18n + format + build，Windows 友好） |
| `pnpm format` / `pnpm format:check`             | prettier 写盘 / 仅检查                                                                             |
| `pnpm analyze`                                  | 构建并输出 bundle 可视化（`ANALYZE=true`）                                                         |
| `pnpm i18n:verify` / `i18n:scan` / `i18n:patch` | i18n 键校验 / 扫描硬编码 / 补中文覆盖                                                              |
| `pnpm e2e` / `pnpm e2e:install`                 | Playwright 测试 / 安装 chromium                                                                    |
| `pnpm generate:api-types`                       | 由后端 OpenAPI 生成 TS 类型                                                                        |

## 目录结构

```
strata-ui/
├── index.html
├── vite.config.ts              # dev proxy、构建配置
├── vitest.config.ts
├── playwright.config.ts
├── PERFORMANCE_BUDGETS.md      # 性能预算阈值
├── CONVENTIONS.md              # 前端速查
├── public/
│   ├── sw.js                   # PWA Service Worker
│   ├── manifest.webmanifest    # PWA 清单
│   └── offline.html
└── src/
    ├── main.tsx  ·  App.tsx  ·  index.css
    ├── pages/<domain>/         # 路由页面（文件 PascalCase、目录 kebab-case）
    ├── components/
    │   ├── ui/                 # 设计系统薄封装（Button/DataTable/Dialog/chart…）
    │   ├── shell/              # AppShell、Sidebar、TopBar、错误边界
    │   ├── ai/                 # AI 专用字段（ApiKeyField/ModelSelector/ProviderSelector）
    │   └── NotificationCenter/ # WebSocket 通知中心
    ├── routes/                 # lazyPages、appRoutes、路由守卫
    ├── theme/                  # muiTheme.ts 等 MUI 主题
    ├── context/                 # AppContext、AppearanceContext
    └── shared/
        ├── api/<domain>.ts     # 按后端域的类型化 API
        ├── auth/               # zustand 会话与权限
        ├── hooks/              # useCrud / usePagedQuery / useListQuery / useMutationWithToast
        ├── query/              # queryClient、QueryProvider（TanStack Query）
        ├── websocket/          # WebSocketClient、useWebSocket、types
        ├── performance/        # webVitals、useWebVitals
        ├── monitoring/         # errorReporter、performanceMonitor（Sentry / web-vitals）
        ├── pwa/                # Service Worker 注册
        ├── i18n/{zh,en}.ts     # 语义短键字典
        ├── pageKit/            # PageHeader、EmptyState 等页面件（经 ui/ 复用）
        ├── config.ts
        └── types.ts            # 与后端对齐的 DTO
```

## 环境变量

复制 `.env.example` 为 `.env.local` 后按需调整（见 `strata-ui/.env.example`）：

| 变量                   | 默认    | 说明                                                                  |
| :--------------------- | :------ | :-------------------------------------------------------------------- |
| `VITE_DEFAULT_LANG`    | `zh`    | 默认语言（`zh` / `en`）；用户选择存 localStorage 并优先               |
| `VITE_API_BASE_URL`    | 空      | 后端基址；留空则用 Vite dev 代理 `/api → :8080`                       |
| `VITE_API_ENCRYPT`     | `false` | 是否启用 AES-GCM 载荷加密（需后端 `STRATA_ENCRYPT_AES_ENABLED=true`） |
| `VITE_API_ENCRYPT_KEY` | 空      | 须与后端 `strata.plugin.encrypt.aes.key`（base64 32 字节）一致        |

## 性能预算

- 阈值定义见 [`PERFORMANCE_BUDGETS.md`](./PERFORMANCE_BUDGETS.md)，测量工具与封装在
  [`src/shared/performance/`](./src/shared/performance/)（`webVitals.ts`、`useWebVitals.ts`），
  监控上报在 [`src/shared/monitoring/`](./src/shared/monitoring/)。
- 用 `pnpm analyze` 查看 bundle 构成。

## PWA 与 WebSocket

- **PWA**：`public/sw.js`（Service Worker）+ `public/manifest.webmanifest` +
  `public/offline.html`，注册逻辑在 `src/shared/pwa/`。
- **实时通知**：`src/shared/websocket/`（`WebSocketClient` / `useWebSocket`）驱动顶栏
  `components/NotificationCenter/`，接收后端推送并展示未读通知。

## 规范

- 路由 `<Perm anyOf={["system:user:list"]}>` 控制访问；按钮用 `useAuthStore.hasAuthority(...)`。
- 列表页：`PageHeader` + `usePagedQuery` + `QueryForm` + `DataTable`；变更用
  `useMutationWithToast` + `invalidateKeys`；下拉引用数据用 `useListQuery`。
- 文案一律走 `t()`，不新增 `text.*` 键；CI 跑 `pnpm i18n:verify`。
- 完整规范见 [`docs/04-工程规范.md`](../docs/04-工程规范.md)。
