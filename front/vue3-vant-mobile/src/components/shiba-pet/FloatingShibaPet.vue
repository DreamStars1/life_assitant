<script setup lang="ts">
import { useUserStore } from '@/stores'
import ShibaFace from './ShibaFace.vue'
import {
  afterHitFlash,
  ANIM_MS,
  applyClick,
  BRUISE_MS,
  clampPos,
  clearBruise,
  createShibaPetLogicState,
  defaultPos,
  DRAG_THRESHOLD_PX,
  HIT_FLASH_MS,
  IDLE_WAG_JITTER_MS,
  IDLE_WAG_MIN_MS,
  isShibaPetAllowed,
  loadPos,
  LONG_PRESS_MS,
  PET_HEIGHT,
  PET_WIDTH,
  pickPetAnim,
  savePos,
  SHIBA_GREET_EVENT,

} from './shibaPetLogic'
import type { ShibaAnim, ShibaPetLogicState } from './shibaPetLogic'

const userStore = useUserStore()
const visible = computed(() => isShibaPetAllowed(userStore.userInfo.fullName))

const logic = ref<ShibaPetLogicState>(createShibaPetLogicState())
const left = ref(0)
const top = ref(0)
const shaking = ref(false)
const hitBurst = ref(0)
const anim = ref<ShibaAnim | null>(null)
const animKey = ref(0)
const showGuide = ref(true)

let bruiseTimer: ReturnType<typeof setTimeout> | null = null
let flashTimer: ReturnType<typeof setTimeout> | null = null
let animTimer: ReturnType<typeof setTimeout> | null = null
let wagTimer: ReturnType<typeof setTimeout> | null = null
let longPressTimer: ReturnType<typeof setTimeout> | null = null
let guideTimer: ReturnType<typeof setTimeout> | null = null
let flashToken = 0
let pointerId: number | null = null
let startX = 0
let startY = 0
let originLeft = 0
let originTop = 0
let dragged = false
let longPressed = false

const statusBubble = computed(() => {
  if (anim.value === 'wag')
    return { emoji: '✨', text: '摇尾巴讨好中~' }
  if (anim.value === 'crawl')
    return { emoji: '🐾', text: '开心狗爬！' }
  if (anim.value === 'bow')
    return { emoji: '🙇', text: '磕头讨好~' }
  if (anim.value === 'greet')
    return { emoji: '🙏', text: '给殿下请安' }
  const m = logic.value.displayMood
  if (m === 'hit_1' || m === 'hit_2')
    return { emoji: '💧', text: '呜…好可怜' }
  if (m === 'bruise_1')
    return { emoji: '🩹', text: '有点青了…' }
  if (m === 'bruise_2')
    return { emoji: '🩹', text: '鼻青脸肿中' }
  if (m === 'bruise_3')
    return { emoji: '😭', text: '肿成小面包了' }
  return null
})

function placeInitial() {
  const w = window.innerWidth
  const h = window.innerHeight
  const saved = loadPos()
  const pos = saved
    ? clampPos(saved.x, saved.y, w, h)
    : defaultPos(w, h)
  left.value = pos.x
  top.value = pos.y
}

function clearAnimTimer() {
  if (animTimer) {
    clearTimeout(animTimer)
    animTimer = null
  }
}

function clearWagTimer() {
  if (wagTimer) {
    clearTimeout(wagTimer)
    wagTimer = null
  }
}

function scheduleIdleWag() {
  clearWagTimer()
  if (!visible.value || anim.value)
    return
  const delay = IDLE_WAG_MIN_MS + Math.floor(Math.random() * IDLE_WAG_JITTER_MS)
  wagTimer = setTimeout(() => {
    wagTimer = null
    if (!visible.value || anim.value || shaking.value)
      return
    playAnim('wag')
  }, delay)
}

function playAnim(kind: ShibaAnim) {
  clearAnimTimer()
  clearWagTimer()
  anim.value = kind
  animKey.value += 1
  animTimer = setTimeout(() => {
    animTimer = null
    anim.value = null
    scheduleIdleWag()
  }, ANIM_MS)
}

function onGreet() {
  if (!visible.value)
    return
  playAnim('greet')
}

function dismissGuideSoon() {
  if (guideTimer)
    clearTimeout(guideTimer)
  guideTimer = setTimeout(() => {
    showGuide.value = false
    guideTimer = null
  }, 8000)
}

onMounted(() => {
  placeInitial()
  window.addEventListener('resize', onResize)
  window.addEventListener(SHIBA_GREET_EVENT, onGreet)
  scheduleIdleWag()
  dismissGuideSoon()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  window.removeEventListener(SHIBA_GREET_EVENT, onGreet)
  if (bruiseTimer)
    clearTimeout(bruiseTimer)
  if (flashTimer)
    clearTimeout(flashTimer)
  if (longPressTimer)
    clearTimeout(longPressTimer)
  if (guideTimer)
    clearTimeout(guideTimer)
  clearAnimTimer()
  clearWagTimer()
})

watch(visible, (v) => {
  if (v) {
    showGuide.value = true
    dismissGuideSoon()
    scheduleIdleWag()
  }
  else {
    clearWagTimer()
  }
})

function onResize() {
  const p = clampPos(left.value, top.value, window.innerWidth, window.innerHeight)
  left.value = p.x
  top.value = p.y
}

function armBruiseTimer() {
  if (bruiseTimer)
    clearTimeout(bruiseTimer)
  bruiseTimer = setTimeout(() => {
    logic.value = clearBruise(logic.value)
    bruiseTimer = null
  }, BRUISE_MS)
}

function onTap() {
  if (anim.value && anim.value !== 'wag')
    return
  showGuide.value = false
  if (anim.value === 'wag') {
    clearAnimTimer()
    anim.value = null
  }
  const result = applyClick(logic.value)
  logic.value = result.state
  shaking.value = true
  hitBurst.value += 1
  if (result.startBruiseTimer)
    armBruiseTimer()
  const token = ++flashToken
  if (flashTimer)
    clearTimeout(flashTimer)
  flashTimer = setTimeout(() => {
    flashTimer = null
    if (token !== flashToken)
      return
    logic.value = afterHitFlash(logic.value)
    shaking.value = false
    scheduleIdleWag()
  }, HIT_FLASH_MS)
}

function onPet() {
  if (anim.value && anim.value !== 'wag')
    return
  showGuide.value = false
  playAnim(pickPetAnim())
}

function onPointerDown(e: PointerEvent) {
  pointerId = e.pointerId
  ;(e.currentTarget as HTMLElement).setPointerCapture(e.pointerId)
  startX = e.clientX
  startY = e.clientY
  originLeft = left.value
  originTop = top.value
  dragged = false
  longPressed = false
  if (longPressTimer)
    clearTimeout(longPressTimer)
  longPressTimer = setTimeout(() => {
    longPressTimer = null
    if (!dragged && pointerId != null) {
      longPressed = true
      onPet()
    }
  }, LONG_PRESS_MS)
}

function onPointerMove(e: PointerEvent) {
  if (pointerId !== e.pointerId)
    return
  const dx = e.clientX - startX
  const dy = e.clientY - startY
  if (!dragged && Math.hypot(dx, dy) < DRAG_THRESHOLD_PX)
    return
  dragged = true
  if (longPressTimer) {
    clearTimeout(longPressTimer)
    longPressTimer = null
  }
  const p = clampPos(originLeft + dx, originTop + dy, window.innerWidth, window.innerHeight)
  left.value = p.x
  top.value = p.y
}

function onPointerUp(e: PointerEvent) {
  if (pointerId !== e.pointerId)
    return
  try {
    ;(e.currentTarget as HTMLElement).releasePointerCapture(e.pointerId)
  }
  catch {
    // already released
  }
  pointerId = null
  if (longPressTimer) {
    clearTimeout(longPressTimer)
    longPressTimer = null
  }
  if (dragged) {
    const p = clampPos(left.value, top.value, window.innerWidth, window.innerHeight)
    left.value = p.x
    top.value = p.y
    savePos(p.x, p.y)
    return
  }
  if (longPressed)
    return
  onTap()
}
</script>

<template>
  <div
    v-if="visible"
    class="shiba-pet"
    role="button"
    tabindex="0"
    aria-label="互动小狗，轻点挨打，长按抚摸"
    :style="{ left: `${left}px`, top: `${top}px`, width: `${PET_WIDTH}px`, height: `${PET_HEIGHT}px` }"
    @pointerdown="onPointerDown"
    @pointermove="onPointerMove"
    @pointerup="onPointerUp"
    @pointercancel="onPointerUp"
    @keydown.enter.prevent="onTap"
    @keydown.space.prevent="onTap"
  >
    <div v-if="statusBubble" class="shiba-pet__bubble">
      <span class="shiba-pet__bubble-emoji">{{ statusBubble.emoji }}</span>
      <span class="shiba-pet__bubble-text">{{ statusBubble.text }}</span>
    </div>

    <div
      v-if="showGuide && !statusBubble"
      class="shiba-pet__guide"
    >
      <span class="shiba-pet__chip shiba-pet__chip--tap">👆 轻点</span>
      <span class="shiba-pet__chip shiba-pet__chip--pet">✋ 长按摸摸</span>
    </div>

    <div
      :key="hitBurst"
      class="shiba-pet__motion"
      :class="{ 'shiba-pet__motion--hit': shaking }"
    >
      <span v-if="shaking" class="shiba-pet__burst" />
      <ShibaFace :mood="logic.displayMood" :anim="anim" :anim-key="animKey" />
    </div>
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
  background: transparent;
}

.shiba-pet__motion {
  width: 100%;
  height: 100%;
  position: relative;
  background: transparent;
}

.shiba-pet__motion--hit {
  animation: shiba-knock 0.42s cubic-bezier(0.22, 1.2, 0.36, 1);
}

.shiba-pet__burst {
  position: absolute;
  inset: -8px;
  border-radius: 50%;
  pointer-events: none;
  background: radial-gradient(circle, rgb(255 140 160 / 50%) 0%, rgb(255 140 160 / 0%) 70%);
  animation: shiba-burst 0.42s ease-out forwards;
  z-index: 0;
}

.shiba-pet__motion :deep(.shiba-face) {
  position: relative;
  z-index: 1;
  background: transparent;
}

.shiba-pet__bubble {
  position: absolute;
  left: 50%;
  bottom: calc(100% + 6px);
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 4px;
  max-width: 200px;
  padding: 6px 10px;
  border-radius: 14px;
  background: #fff7f0;
  border: 2px solid #e8905e;
  box-shadow: 0 4px 12px rgb(0 0 0 / 14%);
  white-space: nowrap;
  pointer-events: none;
  z-index: 2;
  animation: bubble-in 0.2s ease-out;
}

.shiba-pet__bubble::after {
  content: '';
  position: absolute;
  left: 50%;
  top: 100%;
  transform: translateX(-50%);
  border: 6px solid transparent;
  border-top-color: #e8905e;
}

.shiba-pet__bubble-emoji {
  font-size: 14px;
  line-height: 1;
}

.shiba-pet__bubble-text {
  font-size: 13px;
  font-weight: 700;
  color: #c45c2a;
  letter-spacing: 0.02em;
}

.shiba-pet__guide {
  position: absolute;
  left: 50%;
  bottom: calc(100% + 6px);
  transform: translateX(-50%);
  display: flex;
  gap: 6px;
  pointer-events: none;
  z-index: 2;
  animation: bubble-in 0.25s ease-out;
}

.shiba-pet__chip {
  display: inline-flex;
  align-items: center;
  padding: 4px 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  line-height: 1.2;
  box-shadow: 0 2px 8px rgb(0 0 0 / 12%);
  white-space: nowrap;
}

.shiba-pet__chip--tap {
  color: #fff;
  background: linear-gradient(135deg, #ff7b7b, #e85d5d);
}

.shiba-pet__chip--pet {
  color: #fff;
  background: linear-gradient(135deg, #6ec6ff, #3aa0e8);
}

@keyframes bubble-in {
  from {
    opacity: 0;
    transform: translateX(-50%) translateY(4px);
  }
  to {
    opacity: 1;
    transform: translateX(-50%) translateY(0);
  }
}

@keyframes shiba-knock {
  0% {
    transform: scale(1) rotate(0deg) translate(0, 0);
  }
  20% {
    transform: scale(0.92, 1.06) rotate(-6deg) translate(-6px, 3px);
  }
  45% {
    transform: scale(1.04, 0.96) rotate(4deg) translate(4px, -2px);
  }
  70% {
    transform: scale(0.98, 1.02) rotate(-2deg) translate(-2px, 1px);
  }
  100% {
    transform: scale(1) rotate(0deg) translate(0, 0);
  }
}

@keyframes shiba-burst {
  0% {
    opacity: 0.9;
    transform: scale(0.6);
  }
  100% {
    opacity: 0;
    transform: scale(1.35);
  }
}
</style>
