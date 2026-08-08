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
