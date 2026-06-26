## Context

前端 `vue3-vant-mobile` 使用基于文件系统的自动路由（`vue-router/auto-routes`），所有页面文件存在于 `src/pages/` 下即自动注册为可访问路由。当前 `todos/`、`events/` 和 `lifelog/` 页面文件完整存在，TabBar 直接引用这些路由，用户可无缝点击访问，但后端完全没有对应的 Controller。

## Goals / Non-Goals

**Goals:**
- 用户看不到未实现功能的导航入口
- 用户手动输入 URL 也不会看到报错页面，而是友好的"功能开发中"提示
- 隐藏后不影响已有功能（认证、伴侣绑定、共享记录）
- 所有隐藏可逆，后端实现后不需要前端大改就能恢复

**Non-Goals:**
- 不改动后端代码
- 不删除任何文件，只做条件渲染/注释
- 不引入 feature flag 系统（per YAGNI，用最简单的 `v-if` + 常量即可）
- 不修改 API 层代码

## Decisions

### 1. 隐藏策略：局部 v-if + 路由占位，不搞全局守卫

| 方案 | 评价 |
|------|------|
| 全局路由守卫拦截 `/todos` `/events` | 多了一层间接性，要维护路由名单 |
| **v-if 隐藏导航入口 + 页面内显示占位** | 最直接，用户看不到入口就不会点，误入 URL 也有友好提示 |

**选后者**。理由：TabBar 本身就是 `v-for` 渲染的，加个 filter 就能去掉入口。页面组件内顶部加个"功能开发中"占位即可，改动点少且直观。

### 2. 占位方式：简单文字提示，不搞复杂组件

被隐藏的页面显示居中文字"🚧 功能开发中，敬请期待"，通过 `v-if="false"` 将原有内容整体包住不符合 Vue 习惯，改用 `defineExpose` / 在 `<script setup>` 顶部声明常量来控制展示。

最简方式：在页面组件最外层加一个 `v-if` 切换：

```vue
<template>
  <ComingSoon v-if="!ENABLED" />
  <div v-else>
    <!-- 原有内容 -->
  </div>
</template>
```

但 `ComingSoon` 组件不值得抽（YAGNI），直接 inline：

```vue
<template>
  <div v-if="FEATURE_ENABLED_TODOS" class="page-placeholder">
    <van-icon name="underway-o" size="48" />
    <p>功能开发中，敬请期待</p>
  </div>
  <div v-else>
    <!-- 原模板内容 -->
  </div>
</template>
```

### 3. 常量管理：不搞 config 文件，直接页面内定义

就三个页面需要控制（todos、events、lifelog），每个页面顶部写一行 `const ENABLED = false` 即可。恢复时改成 `true` 就行。

TabBar 的 entry list 同理，直接把两项注释掉或标 `show: false` 并在 `v-for` 中过滤。

### 4. Push 设置的处理

`src/pages/settings/index.vue` 中的推送开关不需要加占位页面，直接删掉那组 UI 代码块即可——设置页面本身是正常功能，只是推送这个区块不可用。

## Risks / Trade-offs

- **[恢复成本]** 隐藏是纯前端改动，后端实现对应 API 后，恢复时需要手动将 `ENABLED = false` 改回 `true`，取消注释 TabBar 项。每个页面改动约 2 行 → 风险低
- **[手动 URL]** 用户仍可通过直接输入 `/todos` 访问隐藏页面。页面内会显示"功能开发中"而非 404，不会报错 → 可接受
- **[SEO 无关]** 这是带登录的 PWA，不存在 SEO 问题
- **[误隐藏]** 只动 TabBar 和页面文件，不去动 router/index.ts、api/ 或 store，确保不会影响登录/注册等核心流程
