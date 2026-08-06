<script setup lang="ts">
import { showConfirmDialog, showToast } from 'vant'
import {
  createHealthMemory,
  createHealthTolerance,
  createHealthTrigger,
  createHealthWeight,
  deleteHealthMeal,
  deleteHealthMemory,
  deleteHealthTolerance,
  deleteHealthTrigger,
  deleteHealthWeight,
  getHealthDaily,
  getHealthProfile,
  getHealthSummary,
  listHealthMeals,
  listHealthMemories,
  listHealthTolerances,
  listHealthTriggers,
  listHealthWeights,
  putHealthDaily,
  putHealthProfile,
  upsertHealthMeal,
} from '@/api/modules/health'
import type {
  HealthDaily,
  HealthMeal,
  HealthMemory,
  HealthProfile,
  HealthSummary,
  HealthTolerance,
  HealthTrigger,
  HealthWeight,
} from '@/api/modules/health'

type MealType = '早餐' | '午餐' | '晚餐'
type WeightFilter = 'morning' | 'evening' | 'all'

const tab = ref(0)
const PAGE_SIZE = { memory: 5, tolerance: 4, trigger: 3 }
const MAX_MEMORY = 100

function todayLocal(): string {
  const d = new Date()
  const p = (n: number): string => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

const selectedDate = ref(todayLocal())
const showCalendar = ref(false)

function toLocalDateStr(d: Date): string {
  const p = (n: number): string => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

function shiftDate(offset: number): void {
  const d = new Date(`${selectedDate.value}T12:00:00`)
  d.setDate(d.getDate() + offset)
  selectedDate.value = toLocalDateStr(d)
}

function dateText(value: string): string {
  const d = new Date(`${value}T12:00:00`)
  const weeks = ['日', '一', '二', '三', '四', '五', '六']
  return `${d.getMonth() + 1}月${d.getDate()}日 · 星期${weeks[d.getDay()]}`
}

function dateShort(value: string): string {
  return dateText(value).replace(/ · 星期.*/, '')
}

function kg(v: number | null | undefined): string {
  return v == null ? '—' : Number(v).toFixed(1)
}

function starText(n: number | null | undefined): string {
  return '★'.repeat(Math.max(1, Math.min(5, n ?? 1)))
}

function onCalendarConfirm(d: Date): void {
  selectedDate.value = toLocalDateStr(d)
  showCalendar.value = false
}

// ---------- Profile ----------
const profile = ref<HealthProfile>({})
const showProfileEdit = ref(false)
const profileForm = reactive({ displayName: '', motto: '', heightCm: '', targetKg: '', restingKcal: '' })

async function loadProfile(): Promise<void> {
  try {
    const res = await getHealthProfile()
    profile.value = res.data ?? {}
  }
  catch {
    profile.value = {}
  }
}

function openProfileEdit(): void {
  profileForm.displayName = profile.value.displayName ?? ''
  profileForm.motto = profile.value.motto ?? ''
  profileForm.heightCm = profile.value.heightCm == null ? '' : String(profile.value.heightCm)
  profileForm.targetKg = profile.value.targetKg == null ? '' : String(profile.value.targetKg)
  profileForm.restingKcal = profile.value.restingKcal == null ? '' : String(profile.value.restingKcal)
  showProfileEdit.value = true
}

async function onProfileEdit(): Promise<void> {
  const body: HealthProfile = {
    displayName: profileForm.displayName.trim() || undefined,
    motto: profileForm.motto.trim() || undefined,
    heightCm: profileForm.heightCm === '' ? null : Number(profileForm.heightCm),
    targetKg: profileForm.targetKg === '' ? null : Number(profileForm.targetKg),
    restingKcal: profileForm.restingKcal === '' ? null : Number(profileForm.restingKcal),
  }
  try {
    const res = await putHealthProfile(body)
    profile.value = res.data ?? body
    showProfileEdit.value = false
    showToast('已保存')
    await loadSummary()
  }
  catch { /* notify handled by interceptor */ }
}

// ---------- Daily (stomach + cycle) ----------
const daily = ref<HealthDaily>({ date: selectedDate.value })

async function loadDaily(): Promise<void> {
  try {
    const res = await getHealthDaily(selectedDate.value)
    daily.value = res.data ?? { date: selectedDate.value }
  }
  catch {
    daily.value = { date: selectedDate.value }
  }
}

const showStomachDialog = ref(false)
const stomachForm = reactive({ stomachStatus: '', stomachNote: '' })

function openStomach(): void {
  stomachForm.stomachStatus = daily.value.stomachStatus ?? ''
  stomachForm.stomachNote = daily.value.stomachNote ?? ''
  showStomachDialog.value = true
}

async function onStomach(): Promise<void> {
  try {
    const res = await putHealthDaily({
      date: selectedDate.value,
      stomachStatus: stomachForm.stomachStatus.trim() || null,
      stomachNote: stomachForm.stomachNote.trim() || null,
      cyclePhase: daily.value.cyclePhase ?? null,
      cycleDay: daily.value.cycleDay ?? null,
    })
    daily.value = res.data ?? daily.value
    showStomachDialog.value = false
    showToast('已保存')
  }
  catch { /* notify handled by interceptor */ }
}

const showCycleDialog = ref(false)
const cycleForm = reactive({ cyclePhase: '', cycleDay: '' })

function openCycle(): void {
  cycleForm.cyclePhase = daily.value.cyclePhase ?? ''
  cycleForm.cycleDay = daily.value.cycleDay == null ? '' : String(daily.value.cycleDay)
  showCycleDialog.value = true
}

async function onCycle(): Promise<void> {
  const dayNum = cycleForm.cycleDay === '' ? null : Math.max(1, Number(cycleForm.cycleDay))
  try {
    const res = await putHealthDaily({
      date: selectedDate.value,
      stomachStatus: daily.value.stomachStatus ?? null,
      stomachNote: daily.value.stomachNote ?? null,
      cyclePhase: cycleForm.cyclePhase.trim() || null,
      cycleDay: dayNum,
    })
    daily.value = res.data ?? daily.value
    showCycleDialog.value = false
    showToast('已保存')
  }
  catch { /* notify handled by interceptor */ }
}

const showBurnDialog = ref(false)
const burnForm = reactive({ burnKcal: '' })

function openBurn(): void {
  burnForm.burnKcal = daily.value.burnKcal != null
    ? String(daily.value.burnKcal)
    : (profile.value.restingKcal != null ? String(profile.value.restingKcal) : '')
  showBurnDialog.value = true
}

async function onBurn(): Promise<void> {
  const raw = burnForm.burnKcal.trim()
  try {
    const res = await putHealthDaily({
      date: selectedDate.value,
      burnKcal: raw === '' ? null : Number(raw),
    })
    daily.value = res.data ?? daily.value
    showBurnDialog.value = false
    showToast('已保存')
    await loadDaily()
  }
  catch { /* notify handled by interceptor */ }
}

// ---------- Target weight ----------
const showTargetDialog = ref(false)
const targetForm = reactive({ targetKg: '' })

function openTarget(): void {
  targetForm.targetKg = profile.value.targetKg == null ? '' : String(profile.value.targetKg)
  showTargetDialog.value = true
}

async function onTarget(): Promise<void> {
  const body: HealthProfile = {
    ...profile.value,
    targetKg: targetForm.targetKg === '' ? null : Number(targetForm.targetKg),
  }
  try {
    const res = await putHealthProfile(body)
    profile.value = res.data ?? body
    showTargetDialog.value = false
    showToast('已保存')
    await loadSummary()
  }
  catch { /* notify handled by interceptor */ }
}

// ---------- Meals ----------
const meals = ref<HealthMeal[]>([])
const mealTypes: Array<{ type: MealType, icon: string }> = [
  { type: '早餐', icon: '🥚' },
  { type: '午餐', icon: '🍱' },
  { type: '晚餐', icon: '🥣' },
]

async function loadMeals(): Promise<void> {
  try {
    const res = await listHealthMeals(selectedDate.value)
    meals.value = res.data ?? []
  }
  catch {
    meals.value = []
  }
}

function mealRow(type: MealType): HealthMeal | undefined {
  return meals.value.find(m => m.mealType === type)
}

const intakeTotal = computed(() => {
  const vals = meals.value.map(m => m.kcal).filter((k): k is number => k != null)
  if (!vals.length)
    return null
  return vals.reduce((a, b) => a + b, 0)
})

const effectiveBurn = computed(() => {
  if (daily.value.burnKcal != null)
    return daily.value.burnKcal
  return profile.value.restingKcal ?? null
})

const showMealDialog = ref(false)
const mealForm = reactive<{ id?: string, mealType: MealType, food: string, proteinG: string, kcal: string, feedback: string }>({
  mealType: '早餐',
  food: '',
  proteinG: '',
  kcal: '',
  feedback: '',
})

function openMeal(type: MealType): void {
  const row = mealRow(type)
  mealForm.id = row?.id
  mealForm.mealType = type
  mealForm.food = row?.food ?? ''
  mealForm.proteinG = row?.proteinG == null ? '' : String(row.proteinG)
  mealForm.kcal = row?.kcal == null ? '' : String(row.kcal)
  mealForm.feedback = row?.feedback ?? ''
  showMealDialog.value = true
}

const showMealTypePicker = ref(false)
const mealTypeColumns = [
  { text: '早餐', value: '早餐' },
  { text: '午餐', value: '午餐' },
  { text: '晚餐', value: '晚餐' },
]

function openMealPicker(): void {
  showMealTypePicker.value = true
}

function onMealTypePick(value: string): void {
  showMealTypePicker.value = false
  openMeal(value as MealType)
}

async function onMeal(): Promise<void> {
  if (!mealForm.food.trim()) {
    showToast('请输入食物')
    return
  }
  try {
    await upsertHealthMeal({
      id: mealForm.id,
      date: selectedDate.value,
      mealType: mealForm.mealType,
      food: mealForm.food.trim(),
      proteinG: mealForm.proteinG === '' ? null : Number(mealForm.proteinG),
      kcal: mealForm.kcal === '' ? null : Number(mealForm.kcal),
      feedback: mealForm.feedback.trim() || null,
    })
    showMealDialog.value = false
    showToast('已保存')
    await loadMeals()
  }
  catch { /* notify handled by interceptor */ }
}

async function onMealDelete(): Promise<void> {
  if (!mealForm.id)
    return
  try {
    await showConfirmDialog({ title: '确认删除', message: '删除该餐记录？' })
  }
  catch {
    return
  }
  try {
    await deleteHealthMeal(mealForm.id)
    showMealDialog.value = false
    showToast('已删除')
    await loadMeals()
  }
  catch { /* notify handled by interceptor */ }
}

// ---------- Weights ----------
const weights = ref<HealthWeight[]>([])
const weightFilter = ref<WeightFilter>('morning')

async function loadWeights(): Promise<void> {
  try {
    const res = await listHealthWeights()
    weights.value = res.data ?? []
  }
  catch {
    weights.value = []
  }
}

const filteredWeights = computed<HealthWeight[]>(() =>
  weights.value
    .filter(w => weightFilter.value === 'all'
      || (weightFilter.value === 'morning' ? w.weightType === '晨重' : w.weightType === '晚重'))
    .slice()
    .sort((a, b) => a.date.localeCompare(b.date)),
)

const todayMorningWeight = computed<HealthWeight | undefined>(() =>
  weights.value
    .filter(w => w.date === selectedDate.value && w.weightType === '晨重')
    .at(-1),
)

const showWeightDialog = ref(false)
const weightForm = reactive({ weightType: '晨重', kg: '' })
const weightTypeColumns = [
  { text: '晨重', value: '晨重' },
  { text: '晚重', value: '晚重' },
]
const showWeightTypePicker = ref(false)

function openWeight(): void {
  weightForm.weightType = '晨重'
  weightForm.kg = ''
  showWeightDialog.value = true
}

async function onWeightBeforeClose(action: string): Promise<boolean> {
  if (action !== 'confirm')
    return true
  const kgVal = Number(weightForm.kg)
  if (!weightForm.kg || Number.isNaN(kgVal)) {
    showToast('请输入体重')
    return false
  }
  try {
    await createHealthWeight({
      date: selectedDate.value,
      weightType: weightForm.weightType,
      kg: kgVal,
      standard: weightForm.weightType === '晨重',
    })
    showToast('已保存')
    await Promise.all([loadWeights(), loadSummary()])
    return true
  }
  catch {
    return false
  }
}

async function onWeightDelete(id: string): Promise<void> {
  try {
    await showConfirmDialog({ title: '确认删除', message: '删除该条体重记录？' })
  }
  catch {
    return
  }
  try {
    await deleteHealthWeight(id)
    showToast('已删除')
    await Promise.all([loadWeights(), loadSummary()])
  }
  catch { /* notify handled by interceptor */ }
}

// ---------- Summary ----------
const summary = ref<HealthSummary>({})

async function loadSummary(): Promise<void> {
  try {
    const res = await getHealthSummary()
    summary.value = res.data ?? {}
  }
  catch {
    summary.value = {}
  }
}

// ---------- Chart ----------
const chartData = computed(() => {
  const rows = filteredWeights.value
  if (rows.length === 0)
    return { points: [] as Array<{ x: number, y: number, last: boolean }>, max: 0, min: 0, line: '', area: '' }
  const vals = rows.map(w => w.kg)
  const low = Math.min(...vals)
  const high = Math.max(...vals)
  const pad = Math.max(0.15, (high - low) * 0.25)
  const min = low - pad
  const max = high + pad
  const span = max - min || 1
  const w = 300
  const h = 150
  const points = rows.map((x, i) => ({
    x: rows.length === 1 ? w / 2 : (i * w) / (rows.length - 1),
    y: h - ((x.kg - min) / span) * h,
    last: i === rows.length - 1,
  }))
  const line = points.map((p, i) => `${i ? 'L' : 'M'}${p.x.toFixed(1)} ${p.y.toFixed(1)}`).join(' ')
  const last = points[points.length - 1]
  const first = points[0]
  const area = last && first ? `${line} L${last.x.toFixed(1)} ${h} L${first.x.toFixed(1)} ${h} Z` : line
  return { points, max, min, line, area }
})

const chartLabels = computed(() => {
  const rows = filteredWeights.value
  if (rows.length === 0)
    return []
  if (rows.length <= 4)
    return rows.map(r => ({ date: r.date, type: r.weightType }))
  const mid = rows[Math.floor((rows.length - 1) / 2)]!
  return [rows[0]!, mid, rows[rows.length - 1]!].map(r => ({ date: r.date, type: r.weightType }))
})

// ---------- Memories ----------
const memories = ref<HealthMemory[]>([])
const memoryPage = ref(1)
const memoryTotalPages = ref(1)
const memoryTotal = ref(0)

async function loadMemories(): Promise<void> {
  try {
    const res = await listHealthMemories(memoryPage.value, PAGE_SIZE.memory)
    const data = res.data
    memories.value = data?.records ?? []
    memoryTotalPages.value = data?.pages ?? 1
    memoryTotal.value = data?.total ?? 0
  }
  catch {
    memories.value = []
    memoryTotalPages.value = 1
    memoryTotal.value = 0
  }
}

function memoryIndex(i: number): string {
  return String((memoryPage.value - 1) * PAGE_SIZE.memory + i + 1).padStart(2, '0')
}

const showMemoryDialog = ref(false)
const memoryForm = reactive({ title: '', detail: '' })

function openMemory(): void {
  if (memoryTotal.value >= MAX_MEMORY) {
    showToast(`最多 ${MAX_MEMORY} 条`)
    return
  }
  memoryForm.title = ''
  memoryForm.detail = ''
  showMemoryDialog.value = true
}

async function onMemory(): Promise<void> {
  if (!memoryForm.title.trim()) {
    showToast('请输入标题')
    return
  }
  try {
    await createHealthMemory({ title: memoryForm.title.trim(), detail: memoryForm.detail.trim() || undefined })
    showMemoryDialog.value = false
    showToast('已添加')
    memoryPage.value = Math.ceil((memoryTotal.value + 1) / PAGE_SIZE.memory) || 1
    await loadMemories()
  }
  catch { /* notify handled by interceptor */ }
}

async function onMemoryDelete(id: string): Promise<void> {
  try {
    await showConfirmDialog({ title: '确认删除', message: '删除该条规律？' })
  }
  catch {
    return
  }
  try {
    await deleteHealthMemory(id)
    showToast('已删除')
    if (memoryPage.value > 1 && memories.value.length <= 1)
      memoryPage.value -= 1
    await loadMemories()
  }
  catch { /* notify handled by interceptor */ }
}

// ---------- Tolerances ----------
const tolerances = ref<HealthTolerance[]>([])
const tolerancePage = ref(1)
const toleranceTotalPages = ref(1)
const toleranceTotal = ref(0)

async function loadTolerances(): Promise<void> {
  try {
    const res = await listHealthTolerances(tolerancePage.value, PAGE_SIZE.tolerance)
    const data = res.data
    tolerances.value = data?.records ?? []
    toleranceTotalPages.value = data?.pages ?? 1
    toleranceTotal.value = data?.total ?? 0
  }
  catch {
    tolerances.value = []
    toleranceTotalPages.value = 1
    toleranceTotal.value = 0
  }
}

const showToleranceDialog = ref(false)
const toleranceForm = reactive({ name: '', level: '舒适' })
const toleranceLevelColumns = [
  { text: '舒适', value: '舒适' },
  { text: '低风险', value: '低风险' },
  { text: '谨慎', value: '谨慎' },
  { text: '高风险', value: '高风险' },
]
const showToleranceLevelPicker = ref(false)

function openTolerance(): void {
  toleranceForm.name = ''
  toleranceForm.level = '舒适'
  showToleranceDialog.value = true
}

async function onTolerance(): Promise<void> {
  if (!toleranceForm.name.trim()) {
    showToast('请输入名称')
    return
  }
  try {
    await createHealthTolerance({ name: toleranceForm.name.trim(), level: toleranceForm.level })
    showToleranceDialog.value = false
    showToast('已添加')
    tolerancePage.value = Math.ceil((toleranceTotal.value + 1) / PAGE_SIZE.tolerance) || 1
    await loadTolerances()
  }
  catch { /* notify handled by interceptor */ }
}

async function onToleranceDelete(id: string): Promise<void> {
  try {
    await showConfirmDialog({ title: '确认删除', message: '删除该条耐受？' })
  }
  catch {
    return
  }
  try {
    await deleteHealthTolerance(id)
    showToast('已删除')
    if (tolerancePage.value > 1 && tolerances.value.length <= 1)
      tolerancePage.value -= 1
    await loadTolerances()
  }
  catch { /* notify handled by interceptor */ }
}

// ---------- Triggers ----------
const triggers = ref<HealthTrigger[]>([])
const triggerPage = ref(1)
const triggerTotalPages = ref(1)
const triggerTotal = ref(0)

async function loadTriggers(): Promise<void> {
  try {
    const res = await listHealthTriggers(triggerPage.value, PAGE_SIZE.trigger)
    const data = res.data
    triggers.value = data?.records ?? []
    triggerTotalPages.value = data?.pages ?? 1
    triggerTotal.value = data?.total ?? 0
  }
  catch {
    triggers.value = []
    triggerTotalPages.value = 1
    triggerTotal.value = 0
  }
}

const showTriggerDialog = ref(false)
const triggerForm = reactive({ name: '', note: '', stars: '3' })

function openTrigger(): void {
  triggerForm.name = ''
  triggerForm.note = ''
  triggerForm.stars = '3'
  showTriggerDialog.value = true
}

async function onTrigger(): Promise<void> {
  if (!triggerForm.name.trim()) {
    showToast('请输入名称')
    return
  }
  try {
    await createHealthTrigger({
      name: triggerForm.name.trim(),
      note: triggerForm.note.trim() || undefined,
      stars: Math.max(1, Math.min(5, Number(triggerForm.stars) || 3)),
    })
    showTriggerDialog.value = false
    showToast('已添加')
    triggerPage.value = Math.ceil((triggerTotal.value + 1) / PAGE_SIZE.trigger) || 1
    await loadTriggers()
  }
  catch { /* notify handled by interceptor */ }
}

async function onTriggerDelete(id: string): Promise<void> {
  try {
    await showConfirmDialog({ title: '确认删除', message: '删除该条触发因素？' })
  }
  catch {
    return
  }
  try {
    await deleteHealthTrigger(id)
    showToast('已删除')
    if (triggerPage.value > 1 && triggers.value.length <= 1)
      triggerPage.value -= 1
    await loadTriggers()
  }
  catch { /* notify handled by interceptor */ }
}

// ---------- Gap to target ----------
const gapText = computed(() => {
  const target = profile.value.targetKg
  const w = todayMorningWeight.value
  if (target == null)
    return '待设置'
  if (w)
    return `距目标 ${kg(w.kg - target)} kg`
  return `目标 ${kg(target)} kg`
})

// ---------- Lifecycle ----------
watch(selectedDate, () => {
  loadDaily()
  loadMeals()
})

onMounted(() => {
  loadProfile()
  loadDaily()
  loadMeals()
  loadWeights()
  loadSummary()
  loadMemories()
  loadTolerances()
  loadTriggers()
})
</script>

<template>
  <div class="health-page">
    <!-- ===== Tab 0: 今日 ===== -->
    <section v-show="tab === 0" class="tab-pane">
      <div class="today-card">
        <div class="today-date">
          {{ dateText(selectedDate) }}
        </div>
        <div class="metrics">
          <div class="metric">
            <span>晨重</span>
            <strong>{{ todayMorningWeight ? `${kg(todayMorningWeight.kg)} kg` : '—' }}</strong>
            <button type="button" class="gap-btn" @click="openTarget">
              {{ gapText }}
            </button>
          </div>
          <button type="button" class="metric metric-btn" @click="openStomach">
            <span>胃状态</span>
            <strong>{{ daily.stomachStatus || '未记录' }}</strong>
            <em>{{ daily.stomachNote || '点击记录' }}</em>
          </button>
          <button type="button" class="metric metric-btn" @click="openCycle">
            <span>周期</span>
            <strong>{{ daily.cyclePhase || '未记录' }}</strong>
            <em>{{ daily.cycleDay != null ? `第 ${daily.cycleDay} 天` : '点击记录' }}</em>
          </button>
        </div>
        <div class="metrics energy-metrics">
          <div class="metric">
            <span>已摄入</span>
            <strong>{{ intakeTotal != null ? `${intakeTotal} kcal` : '—' }}</strong>
          </div>
          <button type="button" class="metric metric-btn" @click="openBurn">
            <span>今日消耗</span>
            <strong>{{ effectiveBurn != null ? `${effectiveBurn} kcal` : '—' }}</strong>
            <em>{{ daily.burnKcal != null ? '已覆盖' : (profile.restingKcal != null ? '默认静息' : '点击设置') }}</em>
          </button>
        </div>
      </div>

      <div class="today-records">
        <div class="today-records-head">
          <span />
          <div class="date-switch">
            <button type="button" aria-label="前一天" @click="shiftDate(-1)">
              ‹
            </button>
            <button type="button" class="chosen-date" @click="showCalendar = true">
              {{ dateShort(selectedDate) }}
            </button>
            <button type="button" aria-label="后一天" @click="shiftDate(1)">
              ›
            </button>
          </div>
          <button type="button" class="add-btn" @click="openMealPicker">
            记饮食
          </button>
        </div>
        <div class="meal-list">
          <button
            v-for="m in mealTypes"
            :key="m.type"
            type="button"
            class="meal-card"
            :class="{ done: !!mealRow(m.type) }"
            @click="openMeal(m.type)"
          >
            <span class="meal-icon">{{ m.icon }}</span>
            <span class="meal-body">
              <strong>{{ m.type }}</strong>
              <small>{{ mealRow(m.type)?.food || '点击记录' }}</small>
              <small v-if="mealRow(m.type)">{{ mealRow(m.type)?.proteinG != null ? `蛋白 ${mealRow(m.type)?.proteinG} g` : '蛋白：待填' }}</small>
              <small v-if="mealRow(m.type)">{{ mealRow(m.type)?.kcal != null ? `热量 ${mealRow(m.type)?.kcal} kcal` : '热量：待填' }}</small>
              <small v-if="mealRow(m.type)">{{ mealRow(m.type)?.feedback ? `反馈：${mealRow(m.type)?.feedback}` : '反馈：待补充' }}</small>
            </span>
            <span class="state">{{ mealRow(m.type) ? (mealRow(m.type)?.feedback ? '已记录' : '待反馈') : '待记录' }} ›</span>
          </button>
        </div>
      </div>
    </section>

    <!-- ===== Tab 1: 档案 ===== -->
    <section v-show="tab === 1" class="tab-pane">
      <div class="tab-top">
        <span class="tab-title">身体档案</span>
        <button type="button" class="add-btn" @click="openProfileEdit">
          编辑
        </button>
      </div>

      <div class="profile-card">
        <div class="person">
          <div class="big-avatar">
            🌿
          </div>
          <div>
            <h2>{{ profile.displayName || '未设置' }}</h2>
            <p>{{ profile.motto || '点击编辑设置昵称与座右铭' }}</p>
          </div>
        </div>
        <div class="stats">
          <div>身高<b>{{ profile.heightCm != null ? `${profile.heightCm} cm` : '待设置' }}</b></div>
          <div>目标体重<b>{{ profile.targetKg != null ? `${profile.targetKg} kg` : '待设置' }}</b></div>
          <div>静息代谢<b>{{ profile.restingKcal != null ? `${profile.restingKcal} kcal` : '待设置' }}</b></div>
        </div>
      </div>

      <div class="section-head">
        <h2>个人健康规律</h2>
        <div class="section-actions">
          <div class="pager">
            <button type="button" :disabled="memoryPage <= 1" @click="memoryPage--; loadMemories()">
              ‹
            </button>
            <span>{{ memoryPage }}/{{ memoryTotalPages }}</span>
            <button type="button" :disabled="memoryPage >= memoryTotalPages" @click="memoryPage++; loadMemories()">
              ›
            </button>
          </div>
          <button type="button" class="add-btn" @click="openMemory">
            添加
          </button>
        </div>
      </div>
      <div class="profile-card">
        <div v-if="memories.length === 0" class="empty">
          暂无规律，点击添加
        </div>
        <div v-for="(m, i) in memories" :key="m.id" class="memory" @click="onMemoryDelete(m.id)">
          <b>{{ memoryIndex(i) }}</b>
          <span><strong>{{ m.title }}</strong><br>{{ m.detail || '' }}</span>
        </div>
      </div>

      <div class="section-head">
        <h2>常见耐受</h2>
        <div class="section-actions">
          <div class="pager">
            <button type="button" :disabled="tolerancePage <= 1" @click="tolerancePage--; loadTolerances()">
              ‹
            </button>
            <span>{{ tolerancePage }}/{{ toleranceTotalPages }}</span>
            <button type="button" :disabled="tolerancePage >= toleranceTotalPages" @click="tolerancePage++; loadTolerances()">
              ›
            </button>
          </div>
          <button type="button" class="add-btn" @click="openTolerance">
            添加
          </button>
        </div>
      </div>
      <div class="profile-card">
        <div v-if="tolerances.length === 0" class="empty">
          暂无耐受记录
        </div>
        <span v-for="t in tolerances" :key="t.id" class="tag" @click="onToleranceDelete(t.id)">
          {{ t.name }} · {{ t.level }}<span class="x">×</span>
        </span>
      </div>

      <div class="section-head">
        <h2>胃部触发因素</h2>
        <div class="section-actions">
          <div class="pager">
            <button type="button" :disabled="triggerPage <= 1" @click="triggerPage--; loadTriggers()">
              ‹
            </button>
            <span>{{ triggerPage }}/{{ triggerTotalPages }}</span>
            <button type="button" :disabled="triggerPage >= triggerTotalPages" @click="triggerPage++; loadTriggers()">
              ›
            </button>
          </div>
          <button type="button" class="add-btn" @click="openTrigger">
            添加
          </button>
        </div>
      </div>
      <div class="profile-card">
        <div v-if="triggers.length === 0" class="empty">
          暂无触发因素
        </div>
        <div v-for="t in triggers" :key="t.id" class="risk" @click="onTriggerDelete(t.id)">
          <span>{{ t.name }}<br><small>{{ t.note || '' }}</small></span>
          <b class="stars" :style="{ color: t.stars <= 1 ? '#7fa46d' : '#d48c4c' }">{{ starText(t.stars) }}</b>
        </div>
      </div>
    </section>

    <!-- ===== Tab 2: 趋势 ===== -->
    <section v-show="tab === 2" class="tab-pane">
      <div class="tab-top">
        <span class="tab-title">数据趋势</span>
        <button type="button" class="add-btn" @click="openWeight">
          记体重
        </button>
      </div>

      <div class="trend-card">
        <div class="section-head" style="margin-top:0">
          <h2>体重趋势</h2>
          <div class="pill-tabs">
            <button type="button" :class="{ on: weightFilter === 'morning' }" @click="weightFilter = 'morning'">
              晨重
            </button>
            <button type="button" :class="{ on: weightFilter === 'evening' }" @click="weightFilter = 'evening'">
              晚重
            </button>
            <button type="button" :class="{ on: weightFilter === 'all' }" @click="weightFilter = 'all'">
              全部
            </button>
          </div>
        </div>
        <div class="trend-highlight">
          {{ summary.latestMorningKg != null ? `${kg(summary.latestMorningKg)} kg` : '—' }}
        </div>
        <div class="trend-note">
          {{ summary.latestMorningKg != null
            ? `7日平均 ${kg(summary.avg7MorningKg)} kg · ${summary.gapToTargetKg != null ? `距目标 ${kg(summary.gapToTargetKg)} kg` : '目标待设置'}`
            : '暂无标准晨重' }}
        </div>
        <div class="chart">
          <span class="axis a1">{{ kg(chartData.max) }}</span>
          <span class="axis a2">{{ kg((chartData.max + chartData.min) / 2) }}</span>
          <span class="axis a3">{{ kg(chartData.min) }}</span>
          <svg viewBox="0 0 300 150" preserveAspectRatio="none">
            <defs>
              <linearGradient id="health-area" x1="0" x2="0" y1="0" y2="1">
                <stop stop-color="#cfe681" stop-opacity=".55" />
                <stop offset="1" stop-color="#cfe681" stop-opacity=".03" />
              </linearGradient>
            </defs>
            <path :d="chartData.area" fill="url(#health-area)" />
            <path :d="chartData.line" fill="none" stroke="#315d43" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" />
            <circle
              v-for="(p, i) in chartData.points"
              :key="i"
              :cx="p.x"
              :cy="p.y"
              :r="p.last ? 5 : 3.5"
              fill="#fff"
              stroke="#315d43"
              stroke-width="2.5"
            />
          </svg>
        </div>
        <div class="days">
          <span v-for="(l, i) in chartLabels" :key="i">
            {{ l.date.slice(5).replace('-', '/') }}<br>{{ l.type.includes('晚') ? '晚' : '晨' }}
          </span>
        </div>
      </div>

      <div class="trend-card">
        <div class="section-head" style="margin-top:0">
          <h2>体重明细</h2>
          <span class="count-tag">共 {{ filteredWeights.length }} 条</span>
        </div>
        <div v-if="filteredWeights.length === 0" class="empty">
          暂无记录
        </div>
        <div v-for="w in [...filteredWeights].reverse()" :key="w.id" class="risk" @click="onWeightDelete(w.id)">
          <span>{{ w.date.slice(5).replace('-', '/') }} · {{ w.weightType }}<br><small>{{ w.standard ? '趋势' : '' }}</small></span>
          <b>{{ kg(w.kg) }} kg</b>
        </div>
      </div>

      <div class="trend-card">
        <div class="section-head" style="margin-top:0">
          <h2>近30天饮食</h2>
        </div>
        <div class="stats" style="border-top:0;margin-top:0;padding-top:0">
          <div>平均摄入<b>{{ summary.avg30IntakeKcal != null ? `${Math.round(Number(summary.avg30IntakeKcal))} kcal` : '—' }}</b></div>
          <div>日均蛋白<b>{{ summary.avg30ProteinG != null ? `${Math.round(summary.avg30ProteinG)} g` : '—' }}</b></div>
          <div>最舒适<b>—</b></div>
        </div>
      </div>
    </section>

    <!-- ===== In-page tabbar ===== -->
    <van-tabbar v-model="tab" :fixed="false" safe-area-inset-bottom class="health-tabbar">
      <van-tabbar-item icon="notes-o">
        今日情况
      </van-tabbar-item>
      <van-tabbar-item icon="user-o">
        身体档案
      </van-tabbar-item>
      <van-tabbar-item icon="chart-trending-o">
        趋势
      </van-tabbar-item>
    </van-tabbar>

    <!-- ===== Calendar ===== -->
    <van-calendar
      v-model:show="showCalendar"
      :min-date="new Date('2020-01-01')"
      :default-date="new Date(`${selectedDate}T12:00:00`)"
      @confirm="onCalendarConfirm"
    />

    <!-- ===== Profile edit ===== -->
    <van-dialog v-model:show="showProfileEdit" title="编辑档案" show-cancel-button @confirm="onProfileEdit">
      <div class="dialog-form">
        <van-field v-model="profileForm.displayName" placeholder="昵称" clearable />
        <van-field v-model="profileForm.motto" placeholder="座右铭（可选）" clearable />
        <van-field v-model="profileForm.heightCm" type="number" placeholder="身高 cm" clearable />
        <van-field v-model="profileForm.targetKg" type="number" placeholder="目标体重 kg（留空取消）" clearable />
        <van-field v-model="profileForm.restingKcal" type="number" placeholder="静息代谢 kcal" clearable />
      </div>
    </van-dialog>

    <!-- ===== Target weight ===== -->
    <van-dialog v-model:show="showTargetDialog" title="目标体重" show-cancel-button @confirm="onTarget">
      <div class="dialog-form">
        <van-field v-model="targetForm.targetKg" type="number" placeholder="目标体重 kg（留空取消目标）" clearable />
      </div>
    </van-dialog>

    <!-- ===== Stomach ===== -->
    <van-dialog v-model:show="showStomachDialog" title="胃状态" show-cancel-button @confirm="onStomach">
      <div class="dialog-form">
        <van-field v-model="stomachForm.stomachStatus" placeholder="胃状态：正常 / 不适 / 反流" clearable />
        <van-field v-model="stomachForm.stomachNote" placeholder="备注（可选）" clearable />
      </div>
    </van-dialog>

    <!-- ===== Cycle ===== -->
    <van-dialog v-model:show="showCycleDialog" title="周期" show-cancel-button @confirm="onCycle">
      <div class="dialog-form">
        <van-field v-model="cycleForm.cyclePhase" placeholder="阶段：月经期 / 卵泡期 / 排卵期 / 黄体期" clearable />
        <van-field v-model="cycleForm.cycleDay" type="number" placeholder="第几天" clearable />
      </div>
    </van-dialog>

    <!-- ===== Burn ===== -->
    <van-dialog v-model:show="showBurnDialog" title="今日消耗" show-cancel-button @confirm="onBurn">
      <div class="dialog-form">
        <van-field v-model="burnForm.burnKcal" type="digit" placeholder="kcal（清空则回退静息）" clearable />
      </div>
    </van-dialog>

    <!-- ===== Meal type picker ===== -->
    <van-popup v-model:show="showMealTypePicker" position="bottom">
      <van-picker
        :columns="mealTypeColumns"
        @confirm="({ selectedValues }: any) => onMealTypePick(selectedValues[0] ?? '早餐')"
        @cancel="showMealTypePicker = false"
      />
    </van-popup>

    <!-- ===== Meal form ===== -->
    <van-dialog v-model:show="showMealDialog" :title="`记录${mealForm.mealType}`" show-cancel-button @confirm="onMeal">
      <div class="dialog-form">
        <van-field v-model="mealForm.food" placeholder="食物（如：鸡蛋 1 个）" clearable />
        <van-field v-model="mealForm.proteinG" type="number" placeholder="蛋白质 g（可空）" clearable />
        <van-field v-model="mealForm.kcal" type="digit" placeholder="热量 kcal（可空）" clearable />
        <van-field v-model="mealForm.feedback" placeholder="反馈感受（可空）" clearable />
      </div>
      <div v-if="mealForm.id" class="dialog-delete">
        <van-button size="small" plain type="danger" @click="onMealDelete">
          删除此餐
        </van-button>
      </div>
    </van-dialog>

    <!-- ===== Weight form ===== -->
    <van-dialog
      v-model:show="showWeightDialog"
      :title="`记体重 · ${dateShort(selectedDate)}`"
      show-cancel-button
      :before-close="onWeightBeforeClose"
    >
      <div class="dialog-form">
        <van-field
          :model-value="weightForm.weightType"
          is-link
          readonly
          label="类型"
          @click="showWeightTypePicker = true"
        />
        <van-field v-model="weightForm.kg" type="number" label="体重" placeholder="kg" clearable />
      </div>
    </van-dialog>
    <van-popup v-model:show="showWeightTypePicker" position="bottom">
      <van-picker
        :columns="weightTypeColumns"
        @confirm="({ selectedValues }: any) => { weightForm.weightType = selectedValues[0] ?? '晨重'; showWeightTypePicker = false }"
        @cancel="showWeightTypePicker = false"
      />
    </van-popup>

    <!-- ===== Memory form ===== -->
    <van-dialog v-model:show="showMemoryDialog" title="添加规律" show-cancel-button @confirm="onMemory">
      <div class="dialog-form">
        <van-field v-model="memoryForm.title" placeholder="规律标题" clearable />
        <van-field v-model="memoryForm.detail" placeholder="补充说明（可选）" clearable />
      </div>
    </van-dialog>

    <!-- ===== Tolerance form ===== -->
    <van-dialog v-model:show="showToleranceDialog" title="添加耐受" show-cancel-button @confirm="onTolerance">
      <div class="dialog-form">
        <van-field v-model="toleranceForm.name" placeholder="食物 / 场景" clearable />
        <van-field
          :model-value="toleranceForm.level"
          is-link
          readonly
          label="等级"
          @click="showToleranceLevelPicker = true"
        />
      </div>
    </van-dialog>
    <van-popup v-model:show="showToleranceLevelPicker" position="bottom">
      <van-picker
        :columns="toleranceLevelColumns"
        @confirm="({ selectedValues }: any) => { toleranceForm.level = selectedValues[0] ?? '舒适'; showToleranceLevelPicker = false }"
        @cancel="showToleranceLevelPicker = false"
      />
    </van-popup>

    <!-- ===== Trigger form ===== -->
    <van-dialog v-model:show="showTriggerDialog" title="添加触发因素" show-cancel-button @confirm="onTrigger">
      <div class="dialog-form">
        <van-field v-model="triggerForm.name" placeholder="触发因素" clearable />
        <van-field v-model="triggerForm.note" placeholder="备注（可选）" clearable />
        <van-field v-model="triggerForm.stars" type="number" label="星级" placeholder="1–5" clearable />
      </div>
    </van-dialog>
  </div>
</template>

<style scoped>
.health-page {
  --green: #315d43;
  --lime: #cfe681;
  --soft: #edf4ea;
  --line: #e5e9e3;
  --ink: #243126;
  --muted: #7a877d;
  --paper: #fcfdf9;
  --shadow: 0 10px 35px rgba(32, 53, 39, 0.08);
  min-height: 100vh;
  background: #f0f3ed;
  color: var(--ink);
  padding: 16px 16px 96px;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.tab-pane {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* ----- Today card ----- */
.today-card {
  background: linear-gradient(135deg, #355f45, #1e4330);
  color: #fff;
  padding: 18px;
  border-radius: 22px;
  box-shadow: var(--shadow);
  position: relative;
  overflow: hidden;
}
.today-card::after {
  content: '';
  width: 115px;
  height: 115px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 50%;
  position: absolute;
  right: -35px;
  top: -55px;
}
.today-date {
  font-size: 12px;
  color: #c8d9ca;
  margin-bottom: 13px;
}
.metrics {
  display: flex;
  gap: 12px;
}
.energy-metrics {
  margin-top: 12px;
}
.metric {
  flex: 1;
  min-width: 0;
  display: block;
  border: 0;
  background: transparent;
  padding: 0;
  text-align: left;
  color: inherit;
  font: inherit;
}
.metric span {
  display: block;
  color: #cad8cc;
  font-size: 11px;
  margin-bottom: 4px;
}
.metric strong {
  font-size: 17px;
  font-weight: 700;
}
.metric em {
  font-style: normal;
  font-size: 11px;
  color: #d9e9a3;
}
.metric-btn {
  cursor: pointer;
}
.gap-btn {
  display: block;
  border: 0;
  background: transparent;
  padding: 0;
  margin-top: 2px;
  font-style: normal;
  font-size: 11px;
  color: #d9e9a3;
  cursor: pointer;
  text-align: left;
}

/* ----- Today records ----- */
.today-records {
  padding: 4px 0;
}
.today-records-head {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  gap: 8px;
  margin: 0 2px 10px;
}
.date-switch {
  justify-self: center;
  display: flex;
  align-items: center;
  gap: 10px;
}
.date-switch button {
  border: 0;
  background: transparent;
  color: var(--green);
  font-size: 20px;
  line-height: 1;
  padding: 2px 4px;
  cursor: pointer;
}
.date-switch .chosen-date {
  font-size: 14px;
  font-weight: 700;
  min-width: 84px;
  text-align: center;
}
.add-btn {
  justify-self: end;
  border: 0;
  background: var(--green);
  color: #fff;
  border-radius: 12px;
  padding: 5px 10px;
  font-size: 12px;
  cursor: pointer;
}
.meal-list {
  display: grid;
  gap: 8px;
}
.meal-card {
  border: 1px solid #dce5d6;
  background: #fff;
  border-radius: 15px;
  padding: 12px 13px;
  text-align: left;
  color: var(--ink);
  display: grid;
  grid-template-columns: 30px 1fr auto;
  gap: 8px;
  align-items: center;
  width: 100%;
  cursor: pointer;
  font: inherit;
}
.meal-card.done {
  background: #f4f8ec;
  border-color: #d7e5c5;
}
.meal-icon {
  font-size: 20px;
}
.meal-body strong {
  font-size: 13px;
  display: block;
}
.meal-body small {
  display: block;
  margin-top: 3px;
  color: var(--muted);
  font-size: 11px;
}
.meal-card .state {
  font-size: 11px;
  color: #6b8572;
}

/* ----- Tab top (profile/trend) ----- */
.tab-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 0 2px;
}
.tab-title {
  font-weight: 750;
  font-size: 18px;
}

/* ----- Profile cards ----- */
.profile-card,
.trend-card {
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 20px;
  padding: 17px;
  margin-bottom: 13px;
}
.person {
  display: flex;
  gap: 12px;
  align-items: center;
}
.person .big-avatar {
  width: 47px;
  height: 47px;
  border-radius: 16px;
  background: #dceaa3;
  display: grid;
  place-items: center;
  font-size: 24px;
}
.person h2 {
  margin: 0;
  font-size: 18px;
}
.person p {
  margin: 4px 0 0;
  color: var(--muted);
  font-size: 12px;
}
.stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  border-top: 1px solid var(--line);
  margin-top: 15px;
  padding-top: 14px;
}
.stats div {
  font-size: 12px;
  color: var(--muted);
}
.stats b {
  display: block;
  font-size: 16px;
  color: var(--ink);
  margin-top: 4px;
  font-weight: 700;
}

/* ----- Section heads ----- */
.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 12px 2px;
  gap: 8px;
}
.section-head h2 {
  font-size: 16px;
  margin: 0;
}
.section-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}
.pager {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--muted);
}
.pager span {
  min-width: 42px;
  text-align: center;
}
.pager button {
  border: 0;
  background: transparent;
  color: var(--green);
  font-size: 20px;
  line-height: 1;
  padding: 2px 4px;
  cursor: pointer;
}
.pager button:disabled {
  opacity: 0.3;
  cursor: default;
}
.count-tag {
  font-size: 12px;
  color: var(--muted);
}

/* ----- Memory / tolerance / trigger lists ----- */
.memory {
  display: flex;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid #edf0ec;
  font-size: 13px;
  cursor: pointer;
}
.memory:last-child {
  border: 0;
}
.memory b {
  color: var(--green);
  min-width: 22px;
  font-weight: 700;
}
.tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border-radius: 10px;
  padding: 5px 8px;
  background: var(--soft);
  color: #54725c;
  font-size: 12px;
  margin: 5px 4px 0 0;
  cursor: pointer;
}
.tag .x {
  border: 0;
  background: transparent;
  color: #9aa49b;
  cursor: pointer;
  padding: 0;
  font-size: 12px;
}
.risk {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 9px;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #edf0ec;
  font-size: 13px;
  cursor: pointer;
}
.risk:last-child {
  border: 0;
}
.risk small {
  color: var(--muted);
}
.stars {
  letter-spacing: 1px;
  font-weight: 700;
}
.empty {
  color: var(--muted);
  font-size: 12px;
  text-align: center;
  padding: 16px 0;
}

/* ----- Trend ----- */
.trend-highlight {
  font-size: 24px;
  margin: 4px 0;
  color: var(--green);
  font-weight: 700;
}
.trend-note {
  color: var(--muted);
  font-size: 12px;
}
.pill-tabs {
  display: flex;
  gap: 7px;
}
.pill-tabs button {
  border: 0;
  border-radius: 15px;
  padding: 6px 10px;
  background: #edf2eb;
  color: #6d796f;
  font-size: 11px;
  cursor: pointer;
}
.pill-tabs .on {
  background: var(--green);
  color: #fff;
}
.chart {
  height: 160px;
  position: relative;
  border-bottom: 1px solid #dce4d8;
  border-left: 1px solid #dce4d8;
  margin: 19px 8px 8px 18px;
}
.chart svg {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  overflow: visible;
}
.axis {
  position: absolute;
  left: -32px;
  font-size: 10px;
  color: #9aa49b;
}
.axis.a1 {
  top: -5px;
}
.axis.a2 {
  top: 66px;
}
.axis.a3 {
  bottom: -6px;
}
.days {
  display: flex;
  justify-content: space-between;
  font-size: 10px;
  color: #9aa49b;
  margin: 0 8px 0 18px;
}

/* ----- In-page tabbar ----- */
.health-tabbar {
  position: relative;
  margin-top: 12px;
}

/* ----- Dialog forms ----- */
.dialog-form {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.dialog-delete {
  display: flex;
  justify-content: center;
  padding: 0 16px 12px;
}
</style>

<route lang="json5">
{ name: 'Health', meta: { title: '健康管家' } }
</route>
