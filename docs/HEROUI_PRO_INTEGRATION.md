# HeroUI Pro 设计接口与运行时边界

## 当前结论

本项目已在用户批准后迁移为 React 19 + TypeScript + Vite + Tailwind CSS v4，并直接使用
HeroUI v3 与 HeroUI Pro 复合组件。旧 Vue 文件只作为迁移对照，不参与当前构建。

因此“对接 HeroUI Pro”定义为：

```text
HeroUI Pro 官方网站 / MCP
  -> 查询真实组件名、结构、状态、主题与可访问性 API
  -> @hero-local/*（本机 HeroUI Pro 授权源码回退）
  -> React 共享组件与业务页面
```

## 已核对的官方组件映射（2026-09-11）

| 本项目模式 | HeroUI Pro / OSS 官方候选 | React 使用方式 |
| --- | --- | --- |
| 应用壳与导航 | `app-layout`, `sidebar`, `navbar` | 已直接用于正式应用壳 |
| 指标总览 | `kpi`, `kpi-group`, `widget` | 已直接用于工作台、履约和驾驶舱 |
| 合同台账 | `data-grid`, `search-field`, `pagination`, `chip` | `DataGrid` 已用于合同台账 |
| 批量与上下文动作 | `action-bar`, `sheet` | 仅在有上下文时浮现，避免布局跳动 |
| 创建流程 | `stepper`, `form`, `text-field`, `select`, `date-picker` | `Stepper` 已用于三类创建路径 |
| 详情与证据 | `tabs`, `timeline`, `tooltip`, `resizable` | `Resizable` 已用于原文与审查证据对照 |
| 数据驾驶舱 | `area-chart`, `kpi-group`, `data-grid` | `AreaChart` 与 `KPIGroup` 已用于大屏 |
| 文件与异常 | `drop-zone`, `list-view`, `empty-state`, `alert` | 展示上传、处理、失败和重试 |

映射的机器可读版本位于 `frontend/src/design-system/heroUiReference.ts`。

## 每次开发的查询流程

1. 先访问官方 HeroUI Pro 页面确认候选。
2. 调用 MCP `list_components`，不得凭记忆猜组件名。
3. 调用 `get_component_docs` 核对包来源、复合结构、属性和可访问性接口。
4. 需要主题或 BEM 样式时调用 `get_theme_variables` / `get_css`。
5. 优先通过项目共享层封装，业务页面不依赖组件内部实现。
6. 运行类型检查、构建和真实浏览器路径。

## 当前直接使用 HeroUI Pro 的条件

直接运行时集成必须持续满足：

- React 19 与 Tailwind CSS v4 兼容性已验证；
- `@heroui/react` / `@heroui-pro/react` 版本与许可证已冻结；
- v3 不再使用旧 `HeroUIProvider`；
- Pro 导入封装在项目 `shared/ui`，业务页不依赖组件内部实现；
- 旧 Vue 文件不参与构建，避免运行时和全局样式污染；
- 组件测试、可访问性、构建与浏览器集成验收全部通过。

当 Pro npm 包未下载授权组件产物时，使用 Vite/TypeScript 的 `@hero-local/*` 别名指向
`D:\WorkProject\HeroUIPro\herouipro-v3\src\components`。当前 `AppLayout`、`Sidebar`、`Navbar`、
`KPI`、`KPIGroup`、`DataGrid`、`Stepper`、`Resizable`、`AreaChart` 已通过该路径直接复用。
