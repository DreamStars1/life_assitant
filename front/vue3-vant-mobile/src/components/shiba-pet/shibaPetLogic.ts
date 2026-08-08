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
