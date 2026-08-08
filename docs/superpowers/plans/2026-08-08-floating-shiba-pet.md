# Floating Shiba Pet Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为白名单用户（`fullName` 为 `cc` / `小星露`）在 App 全局挂载可拖拽 Q 版柴犬：点 3 次变淤青，约 5 秒恢复。

**Architecture:** 纯 TS 状态机（可自检）+ `ShibaFace` SVG + `FloatingShibaPet` 悬浮层挂到 `App.vue`；白名单读 `userStore.userInfo.fullName`；位置 `localStorage`；零新依赖、无后端。

**Tech Stack:** Vue 3 + Pinia + Vant 4（现有）；Node 22 `--experimental-strip-types` 跑自检；无 Vitest。

**Spec:** `docs/superpowers/specs/2026-08-08-floating-shiba-pet-design.md`

## Global Constraints

- 白名单精确匹配：`['cc', '小星露']` ↔ `userInfo.fullName`
- 满 **3** 次点击 → `bruised`；约 **5** 秒恢复；受击闪 **250ms**
- 淤青期间再点可闪受击，**不重置/不延长** 恢复计时
- 拖拽阈值 **8px**；位置键 **`shiba-pet-pos`**；命中/淤青不持久化
- `z-index: 1800`（低于 Vant Dialog/Popup 约 2000+）
- 默认右下角，预留 TabBar（`bottom: 72px`, `right: 12px`）
- 不引入 npm 依赖；不抄 Mochi 商业 SVG，自写简化 Q 版
- PowerShell：`git commit -m "msg"`；路径双引号
- 发版：前端 `1.6.0` → `1.7.0` + 根 `CHANGELOG.md`；不改后端 `application.version`

## 文件结构

```
front/vue3-vant-mobile/src/components/shiba-pet/
  shibaPetLogic.ts              # 白名单 + 状态机 + clamp/pos IO
  shibaPetLogic.selfcheck.ts    # assert 自检（Node 跑）
  ShibaFace.vue                 # mood 驱动 SVG
  FloatingShibaPet.vue          # 拖拽、点击、挂载可见性

front/vue3-vant-mobile/src/App.vue   # 挂载 FloatingShibaPet
front/vue3-vant-mobile/package.json  # version bump
CHANGELOG.md
```

---

### Task 1: 纯逻辑 + 自检

**Files:**
- Create: `front/vue3-vant-mobile/src/components/shiba-pet/shibaPetLogic.ts`
- Create: `front/vue3-vant-mobile/src/components/shiba-pet/shibaPetLogic.selfcheck.ts`

**Interfaces:**
- Produces:
  - `SHIBA_PET_ALLOWED_NAMES`, `HITS_TO_BRUISE`(=3), `HIT_FLASH_MS`(=250), `BRUISE_MS`(=5000), `POS_STORAGE_KEY`(=`'shiba-pet-pos'`), `DRAG_THRESHOLD_PX`(=8), `PET_SIZE`(=88)
  - `isShibaPetAllowed(fullName: string | null | undefined): boolean`
  - `ShibaMood = 'normal' | 'hit' | 'bruised'`
  - `ShibaPetLogicState { displayMood: ShibaMood; hitCount: number; bruised: boolean }`
  - `createShibaPetLogicState(): ShibaPetLogicState`
  - `applyClick(state): { state: ShibaPetLogicState; startBruiseTimer: boolean }`
  - `afterHitFlash(state): ShibaPetLogicState` — flash 结束后：bruised→`bruised`，否则 `normal`
  - `clearBruise(state): ShibaPetLogicState` — 恢复 normal，hitCount=0, bruised=false
  - `clampPos(x, y, viewportW, viewportH, size, margin): { x: number; y: number }`
  - `loadPos(): { x: number; y: number } | null`
  - `savePos(x: number, y: number): void`
  - `defaultPos(viewportW, viewportH): { x: number; y: number }` — 右下，bottom 72 / right 12

- [ ] **Step 1: 写 `shibaPetLogic.ts`**

```ts
export const SHIBA_PET_ALLOWED_NAMES = ['cc', '小星露'] as const
export const HITS_TO_BRUISE = 3
export const HIT_FLASH_MS = 250
export const BRUISE_MS = 5000
export const POS_STORAGE_KEY = 'shiba-pet-pos'
export const DRAG_THRESHOLD_PX = 8
export const PET_SIZE = 88
export const DEFAULT_BOTTOM_OFFSET = 72
export const DEFAULT_RIGHT_OFFSET = 12
export const VIEWPORT_MARGIN = 8

export type ShibaMood = 'normal' | 'hit' | 'bruised'

export interface ShibaPetLogicState {
  displayMood: ShibaMood
  hitCount: number
  bruised: boolean
}

export function isShibaPetAllowed(fullName: string | null | undefined): boolean {
  if (!fullName)
    return false
  return (SHIBA_PET_ALLOWED_NAMES as readonly string[]).includes(fullName)
}

export function createShibaPetLogicState(): ShibaPetLogicState {
  return { displayMood: 'normal', hitCount: 0, bruised: false }
}

/** 一次点击：进入 hit；未淤青时累计；满 3 次标记 bruised 并要求启动计时（仅首次进入淤青） */
export function applyClick(state: ShibaPetLogicState): {
  state: ShibaPetLogicState
  startBruiseTimer: boolean
} {
  if (state.bruised) {
    return {
      state: { ...state, displayMood: 'hit' },
      startBruiseTimer: false,
    }
  }
  const hitCount = state.hitCount + 1
  if (hitCount >= HITS_TO_BRUISE) {
    return {
      state: { displayMood: 'hit', hitCount: 0, bruised: true },
      startBruiseTimer: true,
    }
  }
  return {
    state: { displayMood: 'hit', hitCount, bruised: false },
    startBruiseTimer: false,
  }
}

export function afterHitFlash(state: ShibaPetLogicState): ShibaPetLogicState {
  return {
    ...state,
    displayMood: state.bruised ? 'bruised' : 'normal',
  }
}

export function clearBruise(state: ShibaPetLogicState): ShibaPetLogicState {
  return { displayMood: 'normal', hitCount: 0, bruised: false }
}

export function clampPos(
  x: number,
  y: number,
  viewportW: number,
  viewportH: number,
  size = PET_SIZE,
  margin = VIEWPORT_MARGIN,
): { x: number; y: number } {
  const maxX = Math.max(margin, viewportW - size - margin)
  const maxY = Math.max(margin, viewportH - size - margin)
  return {
    x: Math.min(maxX, Math.max(margin, x)),
    y: Math.min(maxY, Math.max(margin, y)),
  }
}

export function defaultPos(viewportW: number, viewportH: number): { x: number; y: number } {
  return clampPos(
    viewportW - PET_SIZE - DEFAULT_RIGHT_OFFSET,
    viewportH - PET_SIZE - DEFAULT_BOTTOM_OFFSET,
    viewportW,
    viewportH,
  )
}

export function loadPos(): { x: number; y: number } | null {
  if (typeof localStorage === 'undefined')
    return null
  try {
    const raw = localStorage.getItem(POS_STORAGE_KEY)
    if (!raw)
      return null
    const parsed = JSON.parse(raw) as { x?: unknown; y?: unknown }
    if (typeof parsed.x !== 'number' || typeof parsed.y !== 'number')
      return null
    if (!Number.isFinite(parsed.x) || !Number.isFinite(parsed.y))
      return null
    return { x: parsed.x, y: parsed.y }
  }
  catch {
    return null
  }
}

export function savePos(x: number, y: number): void {
  if (typeof localStorage === 'undefined')
    return
  localStorage.setItem(POS_STORAGE_KEY, JSON.stringify({ x, y }))
}
```

- [ ] **Step 2: 写自检 `shibaPetLogic.selfcheck.ts`**

```ts
import assert from 'node:assert/strict'
import {
  afterHitFlash,
  applyClick,
  clearBruise,
  clampPos,
  createShibaPetLogicState,
  HITS_TO_BRUISE,
  isShibaPetAllowed,
} from './shibaPetLogic.ts'

assert.equal(isShibaPetAllowed('cc'), true)
assert.equal(isShibaPetAllowed('小星露'), true)
assert.equal(isShibaPetAllowed('other'), false)
assert.equal(isShibaPetAllowed(undefined), false)
assert.equal(isShibaPetAllowed(''), false)

let s = createShibaPetLogicState()
for (let i = 0; i < HITS_TO_BRUISE - 1; i++) {
  const r = applyClick(s)
  s = afterHitFlash(r.state)
  assert.equal(s.bruised, false)
  assert.equal(s.displayMood, 'normal')
}
{
  const r = applyClick(s)
  assert.equal(r.startBruiseTimer, true)
  assert.equal(r.state.bruised, true)
  s = afterHitFlash(r.state)
  assert.equal(s.displayMood, 'bruised')
}
{
  const r = applyClick(s)
  assert.equal(r.startBruiseTimer, false)
  assert.equal(r.state.bruised, true)
  s = afterHitFlash(r.state)
  assert.equal(s.displayMood, 'bruised')
}
s = clearBruise(s)
assert.equal(s.displayMood, 'normal')
assert.equal(s.bruised, false)
assert.equal(s.hitCount, 0)

const c = clampPos(-100, 9999, 400, 800, 88, 8)
assert.equal(c.x, 8)
assert.equal(c.y, 800 - 88 - 8)

console.log('shibaPetLogic.selfcheck: ok')
```

- [ ] **Step 3: 跑自检（应通过）**

```powershell
cd "D:\life_assistant\front\vue3-vant-mobile"
node --experimental-strip-types "src/components/shiba-pet/shibaPetLogic.selfcheck.ts"
```

Expected: 打印 `shibaPetLogic.selfcheck: ok`，exit code 0。

若 Node 对 `.ts` import 报错，改自检为相对路径无扩展名或按报错微调；保持零新依赖。

- [ ] **Step 4: Commit**

```powershell
git add "front/vue3-vant-mobile/src/components/shiba-pet/shibaPetLogic.ts" "front/vue3-vant-mobile/src/components/shiba-pet/shibaPetLogic.selfcheck.ts"
git commit -m "feat(front): add shiba pet hit/bruise logic and selfcheck"
```

---

### Task 2: ShibaFace SVG

**Files:**
- Create: `front/vue3-vant-mobile/src/components/shiba-pet/ShibaFace.vue`

**Interfaces:**
- Consumes: `ShibaMood` from `./shibaPetLogic`
- Produces: prop `mood: ShibaMood`（默认 `'normal'`）的展示组件

- [ ] **Step 1: 写 `ShibaFace.vue`**

自写简化 Q 版柴犬（圆脸、三角耳、豆眼）。按 mood 切换：

- `normal`：睁眼微笑
- `hit`：眯眼 + 右上角小「疼」字或星星；可加浅红印
- `bruised`：一边眼圈青紫 + 脸上创可贴

骨架如下（SVG path 可微调，但三种 mood 必须可分辨）：

```vue
<script setup lang="ts">
import type { ShibaMood } from './shibaPetLogic'

withDefaults(defineProps<{ mood?: ShibaMood }>(), { mood: 'normal' })
</script>

<template>
  <svg
    class="shiba-face"
    viewBox="0 0 88 88"
    width="88"
    height="88"
    role="img"
    aria-hidden="true"
  >
    <!-- 耳 -->
    <ellipse cx="22" cy="28" rx="12" ry="16" fill="#E8A86B" transform="rotate(-20 22 28)" />
    <ellipse cx="66" cy="28" rx="12" ry="16" fill="#E8A86B" transform="rotate(20 66 28)" />
    <ellipse cx="22" cy="30" rx="6" ry="9" fill="#F5C9A0" transform="rotate(-20 22 30)" />
    <ellipse cx="66" cy="30" rx="6" ry="9" fill="#F5C9A0" transform="rotate(20 66 30)" />
    <!-- 头 -->
    <circle cx="44" cy="48" r="30" fill="#F0B27A" />
    <ellipse cx="44" cy="58" rx="16" ry="12" fill="#FFE6C8" />
    <!-- 眼：normal / bruised 睁眼；hit 眯成线 -->
    <g v-if="mood !== 'hit'">
      <ellipse cx="32" cy="46" rx="4" ry="5" fill="#3a2a20" />
      <ellipse cx="56" cy="46" rx="4" ry="5" fill="#3a2a20" />
    </g>
    <g v-else>
      <path d="M26 46 Q32 42 38 46" stroke="#3a2a20" stroke-width="2" fill="none" />
      <path d="M50 46 Q56 42 62 46" stroke="#3a2a20" stroke-width="2" fill="none" />
    </g>
    <!-- 鼻口 -->
    <ellipse cx="44" cy="56" rx="4" ry="3" fill="#3a2a20" />
    <path d="M44 59 Q40 64 36 62" stroke="#3a2a20" stroke-width="1.5" fill="none" />
    <path d="M44 59 Q48 64 52 62" stroke="#3a2a20" stroke-width="1.5" fill="none" />
    <!-- hit 标识 -->
    <g v-if="mood === 'hit'">
      <text x="68" y="22" font-size="12" fill="#e74c3c" font-family="sans-serif">疼</text>
      <circle cx="58" cy="40" r="4" fill="#ff8a80" opacity="0.85" />
    </g>
    <!-- bruised -->
    <g v-if="mood === 'bruised'">
      <ellipse cx="32" cy="46" rx="8" ry="6" fill="#7b68a6" opacity="0.45" />
      <rect x="50" y="38" width="14" height="8" rx="2" fill="#fff8e7" stroke="#e8b05e" stroke-width="1" />
      <line x1="57" y1="38" x2="57" y2="46" stroke="#e8b05e" stroke-width="1" />
    </g>
  </svg>
</template>

<style scoped>
.shiba-face {
  display: block;
  user-select: none;
  pointer-events: none;
}
</style>
```

- [ ] **Step 2: 目视确认（可选）**

在任意临时页面引入三种 mood，或等 Task 3 一并看。至少保证文件无 TS/模板错误。

- [ ] **Step 3: Commit**

```powershell
git add "front/vue3-vant-mobile/src/components/shiba-pet/ShibaFace.vue"
git commit -m "feat(front): add ShibaFace SVG moods for floating pet"
```

---

### Task 3: FloatingShibaPet + 挂到 App

**Files:**
- Create: `front/vue3-vant-mobile/src/components/shiba-pet/FloatingShibaPet.vue`
- Modify: `front/vue3-vant-mobile/src/App.vue`

**Interfaces:**
- Consumes: Task 1 全部导出；`ShibaFace`；`useUserStore().userInfo.fullName` / `id`
- Produces: 全局悬浮宠物；`v-if` 由 `isShibaPetAllowed(fullName)` 控制（建议在 `App.vue` 或组件内顶部）

- [ ] **Step 1: 写 `FloatingShibaPet.vue`**

要点：

1. `visible = computed(() => isShibaPetAllowed(userStore.userInfo.fullName))`；`v-if="visible"` 包根节点（或 App 外包一层）。
2. 挂载时：`loadPos()` 有则 `clampPos`，否则 `defaultPos(innerWidth, innerHeight)`。
3. `pointerdown` 记录起点；`pointermove` 超过 `DRAG_THRESHOLD_PX` 标 `dragging`，更新 `left/top`；`pointerup`：若拖过则 `savePos`+clamp，否则 `onTap`。
4. `onTap`：`applyClick` → 设 state；若 `startBruiseTimer` 且尚无 bruise timer，则 `setTimeout(BRUISE_MS)` → `clearBruise`（**不**在重复点击时 clearTimeout）。
5. 每次 tap 后 `setTimeout(HIT_FLASH_MS)` → `afterHitFlash`（用递增 token 避免乱序）。
6. 样式：`position: fixed; z-index: 1800; touch-action: none; cursor: pointer;` + `hit` 时 CSS `@keyframes shiba-shake`。
7. 根节点：`role="button"` `aria-label="互动小狗"` `tabindex="0"`；可选 `@keydown.enter.space` 触发同 `onTap`。

```vue
<script setup lang="ts">
import { useUserStore } from '@/stores'
import ShibaFace from './ShibaFace.vue'
import {
  afterHitFlash,
  applyClick,
  BRUISE_MS,
  clearBruise,
  clampPos,
  createShibaPetLogicState,
  defaultPos,
  DRAG_THRESHOLD_PX,
  HIT_FLASH_MS,
  isShibaPetAllowed,
  loadPos,
  PET_SIZE,
  savePos,
  type ShibaPetLogicState,
} from './shibaPetLogic'

const userStore = useUserStore()
const visible = computed(() => isShibaPetAllowed(userStore.userInfo.fullName))

const logic = ref<ShibaPetLogicState>(createShibaPetLogicState())
const left = ref(0)
const top = ref(0)
const shaking = ref(false)

let bruiseTimer: ReturnType<typeof setTimeout> | null = null
let flashToken = 0
let pointerId: number | null = null
let startX = 0
let startY = 0
let originLeft = 0
let originTop = 0
let dragged = false

function placeInitial() {
  const w = window.innerWidth
  const h = window.innerHeight
  const saved = loadPos()
  const pos = saved ? clampPos(saved.x, saved.y, w, h) : defaultPos(w, h)
  left.value = pos.x
  top.value = pos.y
}

onMounted(() => {
  placeInitial()
  window.addEventListener('resize', onResize)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  if (bruiseTimer)
    clearTimeout(bruiseTimer)
})

function onResize() {
  const p = clampPos(left.value, top.value, window.innerWidth, window.innerHeight)
  left.value = p.x
  top.value = p.y
}

function onTap() {
  const result = applyClick(logic.value)
  logic.value = result.state
  shaking.value = true
  if (result.startBruiseTimer && bruiseTimer == null) {
    bruiseTimer = setTimeout(() => {
      logic.value = clearBruise(logic.value)
      bruiseTimer = null
    }, BRUISE_MS)
  }
  const token = ++flashToken
  setTimeout(() => {
    if (token !== flashToken)
      return
    logic.value = afterHitFlash(logic.value)
    shaking.value = false
  }, HIT_FLASH_MS)
}

function onPointerDown(e: PointerEvent) {
  pointerId = e.pointerId
  ;(e.currentTarget as HTMLElement).setPointerCapture(e.pointerId)
  startX = e.clientX
  startY = e.clientY
  originLeft = left.value
  originTop = top.value
  dragged = false
}

function onPointerMove(e: PointerEvent) {
  if (pointerId !== e.pointerId)
    return
  const dx = e.clientX - startX
  const dy = e.clientY - startY
  if (!dragged && Math.hypot(dx, dy) < DRAG_THRESHOLD_PX)
    return
  dragged = true
  const p = clampPos(originLeft + dx, originTop + dy, window.innerWidth, window.innerHeight)
  left.value = p.x
  top.value = p.y
}

function onPointerUp(e: PointerEvent) {
  if (pointerId !== e.pointerId)
    return
  pointerId = null
  if (dragged) {
    const p = clampPos(left.value, top.value, window.innerWidth, window.innerHeight)
    left.value = p.x
    top.value = p.y
    savePos(p.x, p.y)
    return
  }
  onTap()
}
</script>

<template>
  <div
    v-if="visible"
    class="shiba-pet"
    :class="{ 'shiba-pet--shake': shaking }"
    role="button"
    tabindex="0"
    aria-label="互动小狗"
    :style="{ left: `${left}px`, top: `${top}px`, width: `${PET_SIZE}px`, height: `${PET_SIZE}px` }"
    @pointerdown="onPointerDown"
    @pointermove="onPointerMove"
    @pointerup="onPointerUp"
    @pointercancel="onPointerUp"
    @keydown.enter.prevent="onTap"
    @keydown.space.prevent="onTap"
  >
    <ShibaFace :mood="logic.displayMood" />
  </div>
</template>

<style scoped>
.shiba-pet {
  position: fixed;
  z-index: 1800;
  touch-action: none;
  cursor: pointer;
  user-select: none;
  -webkit-user-select: none;
}
.shiba-pet--shake {
  animation: shiba-shake 0.25s linear;
}
@keyframes shiba-shake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-4px) rotate(-3deg); }
  75% { transform: translateX(4px) rotate(3deg); }
}
</style>
```

- [ ] **Step 2: 改 `App.vue`**

在 `<script setup>` 增加：

```ts
import FloatingShibaPet from '@/components/shiba-pet/FloatingShibaPet.vue'
```

在 `<van-config-provider>` 内、`</van-config-provider>` 前增加：

```vue
    <FloatingShibaPet />
```

（组件内部已 `v-if` 白名单，无需再包一层。）

- [ ] **Step 3: 类型检查**

```powershell
cd "D:\life_assistant\front\vue3-vant-mobile"
pnpm typecheck
```

Expected: 无新增错误。

- [ ] **Step 4: 手工验收清单**

1. 用 `fullName=cc` 或 `小星露` 登录 → 右下角出现小狗
2. 点 1～2 次 → 闪「疼」+ 晃动，回到正常
3. 第 3 次 → 鼻青脸肿；约 5 秒恢复
4. 淤青期间再点 → 仍闪受击，不重新计 5 秒
5. 拖到别处 → 刷新后位置保留
6. 其他账号登录 → 无小狗

（可用临时改 store / 改白名单常量做本地验证，验完还原白名单。）

- [ ] **Step 5: Commit**

```powershell
git add "front/vue3-vant-mobile/src/components/shiba-pet/FloatingShibaPet.vue" "front/vue3-vant-mobile/src/App.vue"
git commit -m "feat(front): mount draggable floating shiba pet for whitelist users"
```

---

### Task 4: 版本与 CHANGELOG

**Files:**
- Modify: `front/vue3-vant-mobile/package.json`（`version`: `1.6.0` → `1.7.0`）
- Modify: `CHANGELOG.md`（顶部新增 `## v1.7.0 (2026-08-08)`）

**Interfaces:**
- Consumes: 已合并的功能描述
- Produces: 用户可见版本号与日志对齐

- [ ] **Step 1: bump `package.json`**

```json
"version": "1.7.0",
```

- [ ] **Step 2: 在 `CHANGELOG.md` 顶部（`---` 后）插入**

```markdown
## v1.7.0 (2026-08-08)

### ✨ 新特性

- **悬浮柴犬彩蛋**：`fullName` 为 `cc` / `小星露` 时全局右下角可拖拽 Q 版小狗；点 3 次鼻青脸肿，约 5 秒恢复

### 📱 前端

- `FloatingShibaPet` + `ShibaFace`；位置 `localStorage`；无后端依赖
- 版本号: `1.6.0` → `1.7.0`

---
```

（保留其后原有 `## v1.6.0` 一节。）

- [ ] **Step 3: Commit**

```powershell
git add "front/vue3-vant-mobile/package.json" "CHANGELOG.md"
git commit -m "chore: bump app to v1.7.0 for floating shiba pet"
```

---

## Self-Review (plan vs spec)

| Spec 要求 | Task |
|-----------|------|
| 白名单 `cc` / `小星露` + `fullName` | Task 1 + 3 |
| 全局悬浮、可拖、localStorage | Task 3 |
| 点击受击 → 3 次淤青 → 5s 恢复 | Task 1 + 3 |
| 淤青再点不重置计时 | Task 1 `startBruiseTimer` + Task 3 仅 null 时设 timer |
| SVG 三态 | Task 2 |
| App 挂载、z-index 低于 Dialog | Task 3 |
| 最小自检 | Task 1 |
| 版本 + CHANGELOG | Task 4 |
| 无后端 / 零新依赖 | 全程 |

无 TBD；命名在各 Task `Interfaces` 一致。
