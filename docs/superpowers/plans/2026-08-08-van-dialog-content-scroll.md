# van-dialog Content Scroll Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 全局让 Vant `van-dialog` 内容区在过高时可以垂直滑动，修复记录页简介过长后无法继续编辑/添加的问题。

**Architecture:** 仅在 `app.less` 覆盖 `.van-dialog__content`（`max-height` + `overflow-y: auto`）。不改页面模板、不抽组件。短弹窗高度低于上限时行为不变。

**Tech Stack:** Vue 3 + Vant + Less（前端全局样式）

**Spec:** `docs/superpowers/specs/2026-08-08-van-dialog-content-scroll-design.md`

## Global Constraints

- 只改全局样式与版本/CHANGELOG；不改各页面 `van-dialog` 模板与业务逻辑
- 不抽 `DialogScrollBody` / `AppDialog`；不改造成 `van-action-sheet`
- 不为简介 textarea 单独加 `max-height`
- 版本 bump：前端 `1.8.0` → `1.8.1`；后端 `1.5.0-SNAPSHOT` → `1.5.1-SNAPSHOT`；根 `CHANGELOG.md` 新增 `v1.8.1`
- PowerShell：`git commit -m "..."`，路径双引号；勿提交无关脏文件
- 无单测（纯 CSS）；以手工验证为准

## File map

| Path | Role |
|------|------|
| `front/vue3-vant-mobile/src/styles/app.less` | Dialog 内容区限高 + 可滚 |
| `front/vue3-vant-mobile/package.json` | `version` patch |
| `backend/lifeassistant/lifeassistant-server/src/main/resources/config/application.yml` | `application.version` patch |
| `CHANGELOG.md` | `v1.8.1` 条目 |

---

### Task 1: 全局 Dialog 内容可滚

**Files:**
- Modify: `front/vue3-vant-mobile/src/styles/app.less`（在文件末尾、现有 image-preview 覆盖之后追加）

**Interfaces:**
- Consumes: 无
- Produces: 全局选择器 `.van-dialog__content` 具备 `max-height: min(60vh, 480px)`、`overflow-y: auto`、`-webkit-overflow-scrolling: touch`

- [ ] **Step 1: 确认当前文件末尾**

打开 `front/vue3-vant-mobile/src/styles/app.less`，确认末尾是 `.van-image-preview.van-popup--center { ... }` 块（约 L23–35）。不要改动该块。

- [ ] **Step 2: 追加 Dialog 滚动样式**

在文件末尾追加（含 ponytail 注释）：

```less
/* ponytail: van-dialog 默认不限高；长表单（如简介 autosize）会撑出视口且无法滚动 */
.van-dialog__content {
  max-height: min(60vh, 480px);
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}
```

完整文件结果应等价于：保留原有 `*` / `html` / `#app` / scrollbar / image-preview 规则，末尾多上述一块。

- [ ] **Step 3: 手工验证（前端已跑则热更新后测；未跑则 `pnpm dev`）**

在浏览器/手机打开应用后按 spec 三条验证：

1. **记录页** → 编辑一条「一起看过的」→ 简介粘贴多段长文 → 内容区可上下滑 → 能滚到封面上传与「确认」→ 保存成功
2. **健康页** → 打开「目标体重」等短弹窗 → 仍居中、无明显多余滚动条/滚动感
3. **影音详情** →「更新进度」→ textarea 多行 →「确认」仍可达

若 Step 2 样式未生效：确认 `main.ts` 仍 `import '@/styles/app.less'`，硬刷新缓存后再测。

- [ ] **Step 4: Commit**

```powershell
git add "front/vue3-vant-mobile/src/styles/app.less"
git commit -m "fix(front): make van-dialog content scrollable when tall"
```

---

### Task 2: 版本号与 CHANGELOG

**Files:**
- Modify: `front/vue3-vant-mobile/package.json`（`"version"` 字段）
- Modify: `backend/lifeassistant/lifeassistant-server/src/main/resources/config/application.yml`（`application.version`）
- Modify: `CHANGELOG.md`（顶部新增一节）

**Interfaces:**
- Consumes: Task 1 已合并的滚动修复行为
- Produces: 前端 `1.8.1`；后端 `1.5.1-SNAPSHOT`；`CHANGELOG.md` 含 `## v1.8.1 (2026-08-08)`

- [ ] **Step 1: bump 前端 version**

`front/vue3-vant-mobile/package.json`：

```json
"version": "1.8.1"
```

（仅改 `version` 字段，其它字段不动。）

- [ ] **Step 2: bump 后端 application.version**

`backend/lifeassistant/lifeassistant-server/src/main/resources/config/application.yml` 中：

```yaml
application:
  version: 1.5.1-SNAPSHOT
```

（保持该文件现有缩进与周围键；只把 `1.5.0-SNAPSHOT` 改为 `1.5.1-SNAPSHOT`。不要改 `pom.xml` 的 ContiNew 父版本。）

- [ ] **Step 3: 写 CHANGELOG**

在 `CHANGELOG.md` 顶部（`# Life Assistant 更新日志` 与 `---` 之后、现有 `## v1.8.0` 之前）插入：

```markdown
## v1.8.1 (2026-08-08)

### 🐛 修复

- **Dialog 长内容可滚**：居中弹窗内容过高时可垂直滑动，避免记录页简介过长后无法确认/编辑

### 🖥 后端

- 版本号: `1.5.0-SNAPSHOT` → `1.5.1-SNAPSHOT`

### 📱 前端

- 全局 `.van-dialog__content` 限高并 `overflow-y: auto`
- 版本号: `1.8.0` → `1.8.1`

---
```

- [ ] **Step 4: Commit**

```powershell
git add "front/vue3-vant-mobile/package.json" "backend/lifeassistant/lifeassistant-server/src/main/resources/config/application.yml" "CHANGELOG.md"
git commit -m "chore: bump app to v1.8.1 for dialog scroll fix"
```

---

## Spec coverage (self-review)

| Spec 要求 | Task |
|-----------|------|
| `.van-dialog__content` max-height + overflow | Task 1 |
| 不抽组件 / 不改页面模板 | Global Constraints + Task 1 仅改 `app.less` |
| 短弹窗行为不变 | Task 1 Step 3 验证 #2 |
| 记录页长简介可保存 | Task 1 Step 3 验证 #1 |
| patch bump + CHANGELOG | Task 2 |
| 无 textarea 单独限高 | 未列入任何 Task |
