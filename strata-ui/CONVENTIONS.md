# strata-ui 前端规范（速查）

**完整文档：** [工程规范（后端 + 前端）](../docs/04-工程规范.md)（前端见第二部分）· [CRUD 与代码生成](../docs/05-快速开发-CRUD与代码生成.md) · [文档索引](../docs/README.md)

## 必记

- 页面文件 **PascalCase**（`Users.tsx`），域目录 **小写**（`pages/system/`）
- 文案只用 `t('domain.key')`；禁止 `text.*` 与硬编码中英文
- 列表：`PageStack` + `usePagedCrud` + `PageHeader` + `QueryForm` + `DataTable`
- UI 组件文件 | **PascalCase** | `Button.tsx`、`Chips.tsx` | `chips.tsx` |
- 图标：`@mui/icons-material`（禁止 emoji）
- 路由：`lazyPages.ts` → `appRoutes.tsx` → `navigation.ts`
- 质量：`pnpm check`

## CRUD 片段

```tsx
const { t } = useI18n();
const { rows, total, page, setPage, pageSize, setPageSize, loading, reload } = usePagedCrud(
  (pi, size) => api.page(filter, pi, size),
  [filter],
);
```

## 禁止

- 页面内裸 `/api/...` URL
- Tailwind `className` 工具类
- auth 以外跳过 `PageStack` 的随意 `<div>` 根布局
- 未懒加载的巨型 import
- 装饰性注释块、已废弃栈（Tailwind/lucide/Vue）引用
