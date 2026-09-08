export const SHIBA_PET_ALLOWED_NAMES = ['cc', '小星露'] as const

/** 第几次点击进入 bruise_1（前两次为 hit_1 / hit_2） */
export const HITS_TO_BRUISE = 3

export const HIT_FLASH_MS = 420

export const BRUISE_MS = 6000

/** GIF 单段时长（源片约 5.08s） */
export const ANIM_MS = 5100

export const IDLE_WAG_MIN_MS = 7000

export const IDLE_WAG_JITTER_MS = 4000

export const LONG_PRESS_MS = 450

export const POS_STORAGE_KEY = 'shiba-pet-pos'

export const DRAG_THRESHOLD_PX = 8

export const PET_WIDTH = 228

export const PET_HEIGHT = 136

export const DEFAULT_BOTTOM_OFFSET = 72

export const DEFAULT_RIGHT_OFFSET = 12

export const VIEWPORT_MARGIN = 8

export const SHIBA_GREET_EVENT = 'shiba-pet-greet'

export type ShibaMood = 'idle' | 'hit_1' | 'hit_2' | 'bruise_1' | 'bruise_2' | 'bruise_3'

export type ShibaAnim = 'wag' | 'crawl' | 'bow' | 'greet'

export type BruiseLevel = 0 | 1 | 2 | 3

export interface ShibaPetLogicState {
  displayMood: ShibaMood
  hitCount: number
  bruiseLevel: BruiseLevel
}

export function isShibaPetAllowed(fullName: string | null | undefined): boolean {
  if (!fullName)
    return false
  return (SHIBA_PET_ALLOWED_NAMES as readonly string[]).includes(fullName)
}

export function createShibaPetLogicState(): ShibaPetLogicState {
  return { displayMood: 'idle', hitCount: 0, bruiseLevel: 0 }
}

function bruiseMood(level: BruiseLevel): ShibaMood {
  if (level <= 0)
    return 'idle'
  if (level === 1)
    return 'bruise_1'
  if (level === 2)
    return 'bruise_2'
  return 'bruise_3'
}

/** 一次轻点：递进 hit → bruise；淤青中再点会加重并刷新恢复计时 */
export function applyClick(state: ShibaPetLogicState): {
  state: ShibaPetLogicState
  startBruiseTimer: boolean
} {
  if (state.bruiseLevel > 0) {
    const next = Math.min(3, state.bruiseLevel + 1) as BruiseLevel
    return {
      state: {
        displayMood: 'hit_2',
        hitCount: 0,
        bruiseLevel: next,
      },
      startBruiseTimer: true,
    }
  }

  const hitCount = state.hitCount + 1
  if (hitCount >= HITS_TO_BRUISE) {
    return {
      state: { displayMood: 'hit_2', hitCount: 0, bruiseLevel: 1 },
      startBruiseTimer: true,
    }
  }
  return {
    state: {
      displayMood: hitCount === 1 ? 'hit_1' : 'hit_2',
      hitCount,
      bruiseLevel: 0,
    },
    startBruiseTimer: false,
  }
}

export function afterHitFlash(state: ShibaPetLogicState): ShibaPetLogicState {
  if (state.bruiseLevel > 0) {
    return { ...state, displayMood: bruiseMood(state.bruiseLevel) }
  }
  return { ...state, displayMood: 'idle' }
}

export function clearBruise(_state: ShibaPetLogicState): ShibaPetLogicState {
  return { displayMood: 'idle', hitCount: 0, bruiseLevel: 0 }
}

export function pickPetAnim(): Extract<ShibaAnim, 'crawl' | 'bow'> {
  return Math.random() < 0.5 ? 'crawl' : 'bow'
}

export function emitShibaGreet(): void {
  if (typeof window === 'undefined')
    return
  window.dispatchEvent(new CustomEvent(SHIBA_GREET_EVENT))
}

export function clampPos(
  x: number,
  y: number,
  viewportW: number,
  viewportH: number,
  width = PET_WIDTH,
  height = PET_HEIGHT,
  margin = VIEWPORT_MARGIN,
): { x: number, y: number } {
  const maxX = Math.max(margin, viewportW - width - margin)
  const maxY = Math.max(margin, viewportH - height - margin)
  return {
    x: Math.min(maxX, Math.max(margin, x)),
    y: Math.min(maxY, Math.max(margin, y)),
  }
}

export function defaultPos(viewportW: number, viewportH: number): { x: number, y: number } {
  return clampPos(
    viewportW - PET_WIDTH - DEFAULT_RIGHT_OFFSET,
    viewportH - PET_HEIGHT - DEFAULT_BOTTOM_OFFSET,
    viewportW,
    viewportH,
  )
}

export function loadPos(): { x: number, y: number } | null {
  if (typeof localStorage === 'undefined')
    return null
  try {
    const raw = localStorage.getItem(POS_STORAGE_KEY)
    if (!raw)
      return null
    const parsed = JSON.parse(raw) as { x?: unknown, y?: unknown }
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
