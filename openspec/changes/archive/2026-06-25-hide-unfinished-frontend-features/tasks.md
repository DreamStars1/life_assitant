## 1. TabBar — 隐藏未完成功能的导航入口

- [ ] 1.1 打开 `src/components/TabBar.vue`，在 `rootRouteList` 或 Tab 渲染循环中移除/注释「待办」和「日历」两个条目，保留今日、伴侣、我的
- [ ] 1.2 验证 TabBar 只显示 3 个图标：calendar-o（今日）、like-o（伴侣）、contact（我的）

## 2. 今日页面 — 移除未完成功能的快捷操作

- [ ] 2.1 打开 `src/pages/index.vue`，找到"加待办"按钮，用 `v-if="false"` 或注释掉
- [ ] 2.2 同文件中找到快捷"记一笔"里「生活记录」相关的选项，移除或注释
- [ ] 2.3 确认今日页面中调用 `fetchTodos`/`fetchEvents`/`fetchLifeLogs`/`createLifeLog` 的代码不会因缺少后端而报错（API 调用本身有 try/catch 可忽略，但确认不阻塞页面渲染）

## 3. 待办页面 — 显示"功能开发中"占位

- [ ] 3.1 打开 `src/pages/todos/index.vue`，在模板最外层包裹 `v-if` 条件，恒为 false 时显示占位内容（van-icon + "功能开发中，敬请期待"），否则显示原模板
- [ ] 3.2 在 `<script setup>` 顶部添加 `const ENABLED = false`

## 4. 日历页面 — 隐藏入口和"实际"视图

- [ ] 4.1 打开 `src/pages/events/index.vue`，在模板最外层添加 `v-if` 占位（同 todos 方案）
- [ ] 4.2 在 `<script setup>` 顶部添加 `const ENABLED = false`
- [ ] 4.3 确认 `viewMode` 切换中"实际"模式（显示 LifeLog）的逻辑也被屏蔽——直接禁用视图切换或只保留"计划"模式

## 5. 生活记录页面 — 显示"功能开发中"占位

- [ ] 5.1 打开 `src/pages/lifelog/index.vue`，在模板最外层添加 `v-if` 占位
- [ ] 5.2 在 `<script setup>` 顶部添加 `const ENABLED = false`

## 6. 设置页面 — 移除推送通知区块

- [ ] 6.1 打开 `src/pages/settings/index.vue`，找到推送通知/推送开关相关的 UI 代码块，移除或注释

## 7. 编译验证

- [ ] 7.1 运行 `pnpm typecheck` 确认类型检查通过
- [ ] 7.2 运行 `pnpm build:pro` 确认生产构建通过
