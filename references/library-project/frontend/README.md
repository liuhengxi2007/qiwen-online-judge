# Frontend Template

这是一个可直接复用的 `Vite + React + TypeScript + Zustand + shadcn/ui` 模板。

当前模板额外内置了一套适合和 Codex / agent 协作的 mock 架构：

- 用户点击 mock 结果时，只走前端本地状态和页面跳转
- 用户输入文字反馈时，才通过 `control server -> agent` 发送
- 前端不会直接连接本地 `stdio` 形式的 Codex，也不会直接访问本地子进程

当前模板已经补齐了这些基础设施：

- `npm` 作为默认包管理器，并固定 `packageManager: npm@10.9.2`
- `@/*` 路径别名
- `zustand` 状态管理模板 store
- `shadcn/ui` 所需的 `components.json`
- Tailwind CSS v4 基础能力
- `cn()` 工具函数
- 一组常用 `shadcn` 基础组件：`button`、`badge`、`card`、`dialog`、`input`、`label`、`separator`、`sheet`、`textarea`

## 开发命令

```bash
npm install
npm run dev
```

常用脚本：

```bash
npm run typecheck
npm run lint
npm run lint:fix
npm run build
npm run preview
```

## shadcn/ui 使用方式

这个项目已经完成了 `shadcn` 初始化需要的关键配置，后续只需要直接加组件：

```bash
npm run ui:add -- button
npm run ui:add -- card dialog sheet
```

生成组件时会默认使用这些别名：

- `@/components`
- `@/components/ui`
- `@/hooks`
- `@/lib`
- `@/lib/utils`

当前项目已经内置的常用组件目录：

- [src/components/ui/button.tsx](/home/yang/projects/commons/frontend-sample/src/components/ui/button.tsx)
- [src/components/ui/badge.tsx](/home/yang/projects/commons/frontend-sample/src/components/ui/badge.tsx)
- [src/components/ui/card.tsx](/home/yang/projects/commons/frontend-sample/src/components/ui/card.tsx)
- [src/components/ui/dialog.tsx](/home/yang/projects/commons/frontend-sample/src/components/ui/dialog.tsx)
- [src/components/ui/input.tsx](/home/yang/projects/commons/frontend-sample/src/components/ui/input.tsx)
- [src/components/ui/label.tsx](/home/yang/projects/commons/frontend-sample/src/components/ui/label.tsx)
- [src/components/ui/separator.tsx](/home/yang/projects/commons/frontend-sample/src/components/ui/separator.tsx)
- [src/components/ui/sheet.tsx](/home/yang/projects/commons/frontend-sample/src/components/ui/sheet.tsx)
- [src/components/ui/textarea.tsx](/home/yang/projects/commons/frontend-sample/src/components/ui/textarea.tsx)

## Zustand 模板入口

默认 store 在 [src/stores/use-app-store.ts](/home/yang/projects/commons/frontend-sample/src/stores/use-app-store.ts)。

这个 store 目前已经放了这些模板字段：

- `theme`
- `activeToolPanel`
- `mockRoute`

你后续可以直接在这里继续扩展全局 UI 状态、用户信息或轻量业务状态。

## Mock 约定

这份模板现在明确区分两条链路：

- `Mock result click -> local UI`
  - 例如登录成功、权限不足、库存耗尽，这些都只更新前端状态并切换 mock 路由
- `Typed feedback -> control server -> agent`
  - 当你输入“还缺少一种情况”或“应该补一个页面”这类文字意见时，才会通过 feedback bridge 发给 agent

默认示例页现在只保留一个左侧“事件”按钮，用来模拟非按钮交互。
如果某个按钮交互本身还缺状态分支或跳转页，直接在对应的 mock 弹窗里点“这个交互有遗漏”即可进入文字反馈。

对应实现文件：

- [src/App.tsx](/home/yang/projects/commons/frontend-sample/src/App.tsx)
- [src/components/MockSystemProvider.tsx](/home/yang/projects/commons/frontend-sample/src/components/MockSystemProvider.tsx)
- [src/components/FeedbackModal.tsx](/home/yang/projects/commons/frontend-sample/src/components/FeedbackModal.tsx)
- [src/components/MockInteractionModal.tsx](/home/yang/projects/commons/frontend-sample/src/components/MockInteractionModal.tsx)
- [src/lib/agent-feedback-client.ts](/home/yang/projects/commons/frontend-sample/src/lib/agent-feedback-client.ts)

## 关键文件

- [components.json](/home/yang/projects/commons/frontend-sample/components.json)
- [postcss.config.mjs](/home/yang/projects/commons/frontend-sample/postcss.config.mjs)
- [vite.config.ts](/home/yang/projects/commons/frontend-sample/vite.config.ts)
- [tsconfig.app.json](/home/yang/projects/commons/frontend-sample/tsconfig.app.json)
- [src/index.css](/home/yang/projects/commons/frontend-sample/src/index.css)
- [src/lib/utils.ts](/home/yang/projects/commons/frontend-sample/src/lib/utils.ts)
- [src/components/ui/button.tsx](/home/yang/projects/commons/frontend-sample/src/components/ui/button.tsx)
- [src/lib/agent-feedback-client.ts](/home/yang/projects/commons/frontend-sample/src/lib/agent-feedback-client.ts)

## 关于 Tailwind 方案

当前项目保留了 `vite@8`，因此没有使用 `@tailwindcss/vite` 插件，而是改用 Tailwind CSS v4 的 PostCSS 集成方式。这个方案对当前依赖树更稳定，也不影响 `shadcn/ui` 的后续使用。

## Agent Bridge

[src/lib/agent-feedback-client.ts](/home/yang/projects/commons/frontend-sample/src/lib/agent-feedback-client.ts) 里固定写死了：

- `const CONTROL_SERVER_ORIGIN = 'http://127.0.0.1:4311'`
- POST `http://127.0.0.1:4311/api/workspace/turn`
- 请求体格式：`{ prompt: string, mode: 'mock' }`

这部分是给“文字反馈”用的，不是给 mock 结果点击用的。
