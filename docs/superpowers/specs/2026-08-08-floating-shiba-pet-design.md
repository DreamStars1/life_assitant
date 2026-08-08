# 全局悬浮 Q 版柴犬桌宠（白名单）

## 背景

希望在前端提供一只可互动的 Q 版小狗：点击有受击反馈，累计点击后变为鼻青脸肿，一段时间后恢复。功能仅对部分用户可见，作为伴侣产品内的轻量彩蛋，不进入主业务流程。

调研结论：GitHub / 网上没有「可打 Q 版狗 → 淤青」的现成 Vue 组件。`neko-vue` / `vue-pets` 是桌宠但交互模型不符；Lottie/Rive 适合状态动画但需自建素材且引入依赖过重。最快路径是自研悬浮组件 + 自控 SVG 表情。

## 目标

- 登录且 `fullName` 为白名单用户时，多数页面右下角显示可拖拽的 Q 版柴犬
- 每次点击：短暂受击标识（晃动 + 「疼」类提示）
- 累计点击满 3 次：切换为鼻青脸肿；约 5 秒后恢复正常
- 拖拽位置持久化到 `localStorage`
- 零新依赖、无后端 API

## 非目标

- 后端 feature flag / 远程白名单
- 伴侣间同步、积分、排行榜
- 音效、追光标走动、完整桌宠引擎
- Lottie / Rive 动画管线
- 直接复用可能有版权问题的 Mochi Shiba 商业素材（仅作风格参考）

## 架构

```
App.vue
  └─ FloatingShibaPet   （v-if：已登录 && fullName ∈ 白名单）
        ├─ 拖拽 / 定位 / localStorage
        ├─ 点击计数与状态机
        └─ ShibaFace（SVG，mood 驱动）
```

- **挂载**：全局壳（`App.vue`），路由切换不卸载，命中次数与淤青计时在会话内保留
- **层级**：`position: fixed`；高于页面内容，低于 Modal/Dialog
- **默认位置**：右下角，预留 TabBar 高度；可拖拽；松手写入 `localStorage`（键如 `shiba-pet-pos`）
- **白名单**：前端常量 `SHIBA_PET_ALLOWED_NAMES = ['cc', '小星露']`，与 `userStore.userInfo.fullName` **精确匹配**

## 组件

| 单元 | 职责 |
|------|------|
| `FloatingShibaPet.vue` | 可见性、拖拽、点击、状态机、定位 |
| `ShibaFace`（独立文件或同文件内 SVG） | 按 `mood: normal \| hit \| bruised` 渲染 |
| 白名单常量 | 单一改名单入口 |

拖拽与点击区分：`pointerdown` 后移动超过阈值视为拖拽，避免误触点击。

## 状态机

```
idle (normal)
  --click--> hitFlash（~200–300ms：晃动 + 受击标识）
               hitCount++
               if hitCount < 3 → normal
               if hitCount >= 3 → bruised（hitCount 归零）
bruised
  --~5s--> normal
```

- 淤青期间再点：允许再闪受击，**不重置、不延长** 5 秒恢复计时
- 刷新页面：位置从 `localStorage` 恢复；命中次数与淤青**不持久化**，从 `normal` 开始

## 视觉

- 自写 Q 版柴犬 SVG（表情/眼睛/耳朵可随 mood 切换）
- `hit`：短暂眯眼 + 红印或星星等受击标识
- `bruised`：鼻青 + 创可贴一类
- 可点区域 `aria-label`（如「互动小狗」）

## 边界情况

| 情况 | 处理 |
|------|------|
| 未登录 / 非白名单 | 不渲染 |
| 改名离开白名单 | store 更新后自动消失 |
| 拖出屏幕 | clamp 到可视区（留边距） |
| 登录/注册全屏页 | 首版仍显示；若挡输入再降 z-index |
| 无障碍 | 基础 `aria-label`；不逐击读屏解说 |

## 测试

- 最小自检或单测：满 3 次 → `bruised`；约 5s → `normal`；白名单外不可见
- 手工：`cc` / `小星露` 登录可见；拖拽刷新后位置保留；其他账号不可见

## 版本

用户可见新功能：合并前 bump 前端 `package.json`（建议 minor，如 `1.6.0` → `1.7.0`）并更新根 `CHANGELOG.md`。无后端 API/行为变更，不改 `application.version`。
