# van-dialog 内容区可滚动

## 背景

记录页「添加/编辑一起看过的」使用居中 `van-dialog`，简介字段为 `type="textarea"` + `autosize`。简介变长后弹窗内容被撑高，内容区无滚动，用户够不到封面上传与底部确认，表现为无法继续修改/添加。

同类对比：

| 容器 | 是否可滚 | 代表 |
|------|----------|------|
| `van-dialog`（默认） | 否 | 记录添加/编辑、健康多字段弹窗、影音更新进度 |
| `van-popup` + `max-height` | 是 | 设置模板/令牌列表、记录分类管理 |
| `van-action-sheet` | 是 | 待办/日程表单 |

## 目标

- 所有居中 `van-dialog` 在内容过高时，中间内容区可垂直滑动
- 短弹窗行为与现网一致（不出现无意义的滚动感）
- 记录页长简介场景下可完成编辑并保存

## 非目标

- 不抽 `DialogScrollBody` / `AppDialog` 等组件或 composable
- 不把长表单改造成 `van-action-sheet`
- 不改各页面模板结构与业务逻辑
- 不为简介 textarea 单独加 `max-height`（若实测仍难用再跟进）
- 不改后端与各页面模板结构

## 交付

用户可感知的行为修复：前端 `package.json` version patch bump，后端 `application.version` 对齐，根目录 `CHANGELOG.md` 记一笔。

## 方案

在全局样式 `front/vue3-vant-mobile/src/styles/app.less` 增加对 Dialog 内容区的覆盖（与现有 `.van-image-preview` 全局覆盖同风格）：

```less
/* ponytail: van-dialog 默认不限高；长表单（如简介 autosize）会撑出视口且无法滚动 */
.van-dialog__content {
  max-height: min(60vh, 480px);
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}
```

- 标题与底部「取消/确认」仍由 Vant 固定在内容区外，只滚中间 body
- 内容高度低于上限时无滚动，短弹窗外观不变
- 全局 `::-webkit-scrollbar { width: 0 }` 仍生效：可滑但不显示滚动条

备选已否决：

- 逐页包可滚容器：易漏改，维护成本高
- 封装 Dialog 组件：抽象收益薄，改动面大
- 长表单改 action-sheet：交互形态变化，超出「可滑动」目标

## 影响面

自动覆盖所有 `van-dialog`，包括但不限于：

- `pages/records/index.vue`：添加/编辑一起看过的、重命名分类
- `pages/share/media/[id].vue`：更新进度
- `pages/health/index.vue`：档案/饮食/体重等表单弹窗
- `pages/settings/index.vue`：添加/编辑模板、新建令牌（短表单，通常不触发滚动）

已用 action-sheet / 带 max-height 的 popup 的页面不改。

## 边界

- 软键盘弹出时 `60vh` 仍相对视口；若某机型偏紧，后续可将上限调到 `50vh`，本次不预埋多套值
- Dialog 内再开的 `van-calendar` / `van-picker`（底部 popup）叠在上层，不受本样式影响
- 嵌套滚动（弹窗滚 + 超长 textarea 内滚）在常见简介长度下可接受；若简介极端长再考虑给该 field 限高

## 验证

手工：

1. 记录页编辑长简介 → 能滚到封面与确认并保存
2. 健康「目标体重」等短弹窗 → 仍居中、无多余滚动感
3. 影音「更新进度」多行文本 → 确认按钮仍可达

无单测：纯 CSS，无业务分支。
