<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores'
import * as echarts from 'echarts'
import { getPointsBalance, getPointsHistory, addPoints } from '@/api/modules/partner-points'
import type { PointsRecord } from '@/api/modules/partner-points'
import { getTodayCheckin, getWeeklyCheckin } from '@/api/modules/partner-checkin'
import type { CheckinRecord } from '@/api/modules/partner-checkin'
import { fetchSharedRecords } from '@/api/modules/shared-records'
import { fetchSharedMediaList } from '@/api/modules/shared-media'

const { t } = useI18n()
const router = useRouter()
const userStore = useUserStore()

const hasPartner = computed(() => !!userStore.userInfo.partnerId)
const partnerName = computed(() => userStore.partnerName || '对方')
const myId = computed(() => userStore.userInfo?.id || '')
const partnerUserId = computed(() => userStore.userInfo?.partnerId || '')

const daysTogether = computed(() => {
  if (!userStore.userInfo.createdAt) return 0
  const created = new Date(userStore.userInfo.createdAt)
  const now = new Date()
  return Math.floor((now.getTime() - created.getTime()) / (1000 * 60 * 60 * 24))
})

// Points
const pointsBalance = ref(0)
const historyList = ref<PointsRecord[]>([])
const showAddPopup = ref(false)
const showSubPopup = ref(false)
const popupAmount = ref(1)
const popupReason = ref('')

// Stats
const recordsCount = ref(0)
const mediaFinishedCount = ref(0)

// Check-in (mine only)
const todayCheckinList = ref<CheckinRecord[]>([])
const myToday = computed(() => todayCheckinList.value.filter(c => !myId.value || c.userId === myId.value))
const wakeChecked = computed(() => myToday.value.some(c => c.checkinType === 'wake'))
const sleepChecked = computed(() => myToday.value.some(c => c.checkinType === 'sleep'))
const wakeTime = computed(() => extractHm(myToday.value.find(c => c.checkinType === 'wake')?.checkinTime))
const sleepTime = computed(() => extractHm(myToday.value.find(c => c.checkinType === 'sleep')?.checkinTime))

// Sleep chart data
interface SleepDay {
  date: string
  wake: string | null
  sleep: string | null
  partnerWake: string | null
  partnerSleep: string | null
}
const sleepData = ref<SleepDay[]>([])

function businessNow(base = new Date()): Date {
  const d = new Date(base)
  // ponytail: 作息日界 4 点，与后端 PartnerCheckinService.businessDate 一致
  if (d.getHours() < 4)
    d.setDate(d.getDate() - 1)
  return d
}

function buildDateLabels(): string[] {
  const labels: string[] = []
  const base = businessNow()
  for (let i = 6; i >= 0; i--) {
    const d = new Date(base)
    d.setDate(base.getDate() - i)
    labels.push(`${d.getMonth() + 1}/${d.getDate()}`)
  }
  return labels
}

function dateKey(offset: number): string {
  const d = businessNow()
  d.setDate(d.getDate() - offset)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function extractHm(checkinTime: string | number[] | null | undefined): string {
  if (checkinTime == null || checkinTime === '')
    return ''
  if (Array.isArray(checkinTime)) {
    const h = Number(checkinTime[3] ?? 0)
    const mi = Number(checkinTime[4] ?? 0)
    return `${String(h).padStart(2, '0')}:${String(mi).padStart(2, '0')}`
  }
  const matched = String(checkinTime).match(/(\d{1,2}):(\d{2})/)
  if (matched)
    return `${matched[1]!.padStart(2, '0')}:${matched[2]}`
  const d = new Date(String(checkinTime).replace(' ', 'T'))
  if (!Number.isNaN(d.getTime()))
    return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
  return ''
}

function normalizeDateKey(v: string | number[] | null | undefined): string {
  if (v == null || v === '')
    return ''
  if (Array.isArray(v)) {
    return `${v[0]}-${String(v[1]).padStart(2, '0')}-${String(v[2]).padStart(2, '0')}`
  }
  return String(v).slice(0, 10)
}

function toMinutes(time: string | null, type: 'wake' | 'sleep'): number | null {
  if (!time)
    return null
  const parts = time.split(':')
  const h = Number(parts[0])
  const m = Number(parts[1])
  if (Number.isNaN(h) || Number.isNaN(m))
    return null
  let mins = h * 60 + m
  if (type === 'sleep' && h < 12)
    mins += 1440
  return mins
}

function groupByUser(checkins: CheckinRecord[], userId: string) {
  const grouped: Record<string, { wake: string | null, sleep: string | null }> = {}
  for (const c of checkins.filter(item => item.userId === userId)) {
    const key = normalizeDateKey(c.checkinDate)
    if (!key)
      continue
    if (!grouped[key])
      grouped[key] = { wake: null, sleep: null }
    const time = extractHm(c.checkinTime)
    if (c.checkinType === 'wake')
      grouped[key]!.wake = time || null
    else
      grouped[key]!.sleep = time || null
  }
  return grouped
}

function buildSleepDataFromWeekly(checkins: CheckinRecord[]): SleepDay[] {
  const labels = buildDateLabels()
  const mine = groupByUser(checkins, myId.value)
  const partner = partnerUserId.value ? groupByUser(checkins, partnerUserId.value) : {}
  return labels.map((date, i) => {
    const key = dateKey(6 - i)
    return {
      date,
      wake: mine[key]?.wake ?? null,
      sleep: mine[key]?.sleep ?? null,
      partnerWake: partner[key]?.wake ?? null,
      partnerSleep: partner[key]?.sleep ?? null,
    }
  })
}

function cssVar(name: string, fallback: string): string {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim() || fallback
}

let chart: echarts.ECharts | null = null

function initChart() {
  nextTick(() => {
    const el = document.getElementById('dashboard-sleep-chart')
    if (!el) return
    chart?.dispose()
    chart = echarts.init(el)
    const dates = sleepData.value.map(d => d.date)
    const primary = cssVar('--van-primary-color', '#e8905e')
    const danger = cssVar('--van-tag-danger-color', '#d97a6e')
    const success = cssVar('--van-tag-success-color', '#7ec8a0')
    const warning = cssVar('--van-tag-warning-color', '#e8b05e')
    const text2 = cssVar('--van-text-color-2', '#8b7a6b')

    chart.setOption({
      tooltip: {
        trigger: 'axis',
        formatter: (params: any) => {
          const i = params[0]?.dataIndex
          if (i == null) return ''
          const d = sleepData.value[i!]
          if (!d) return ''
          return [
            d.date,
            `我·${t('dashboard.wakeUp')}: ${d.wake ?? '-'}`,
            `我·${t('dashboard.goToSleep')}: ${d.sleep ?? '-'}`,
            `${partnerName.value}·${t('dashboard.wakeUp')}: ${d.partnerWake ?? '-'}`,
            `${partnerName.value}·${t('dashboard.goToSleep')}: ${d.partnerSleep ?? '-'}`,
          ].join('<br/>')
        },
      },
      grid: { left: 42, right: 12, top: 8, bottom: 20 },
      xAxis: {
        type: 'category',
        data: dates,
        axisLabel: { fontSize: 9, color: text2, interval: 0, rotate: 20 },
        axisLine: { show: false },
        axisTick: { show: false },
      },
      yAxis: {
        type: 'value',
        min: 300,
        max: 1620,
        show: false,
        splitLine: { show: false },
      },
      series: [
        {
          name: `我·${t('dashboard.wakeUp')}`,
          type: 'line',
          data: sleepData.value.map(d => toMinutes(d.wake, 'wake')),
          smooth: true,
          symbol: 'circle',
          symbolSize: 4,
          lineStyle: { color: primary, width: 1.5 },
          itemStyle: { color: primary },
        },
        {
          name: `我·${t('dashboard.goToSleep')}`,
          type: 'line',
          data: sleepData.value.map(d => toMinutes(d.sleep, 'sleep')),
          smooth: true,
          symbol: 'circle',
          symbolSize: 4,
          lineStyle: { color: danger, width: 1.5, type: 'dashed' },
          itemStyle: { color: danger },
        },
        {
          name: `${partnerName.value}·${t('dashboard.wakeUp')}`,
          type: 'line',
          data: sleepData.value.map(d => toMinutes(d.partnerWake, 'wake')),
          smooth: true,
          symbol: 'circle',
          symbolSize: 4,
          lineStyle: { color: success, width: 1.5 },
          itemStyle: { color: success },
        },
        {
          name: `${partnerName.value}·${t('dashboard.goToSleep')}`,
          type: 'line',
          data: sleepData.value.map(d => toMinutes(d.partnerSleep, 'sleep')),
          smooth: true,
          symbol: 'circle',
          symbolSize: 4,
          lineStyle: { color: warning, width: 1.5, type: 'dashed' },
          itemStyle: { color: warning },
        },
      ],
      animation: false,
    })
  })
}

async function loadData() {
  try {
    const res = await getPointsBalance()
    pointsBalance.value = res.data ?? 0
  }
  catch {
    pointsBalance.value = 0
  }

  try {
    const res = await getPointsHistory(1, 5)
    historyList.value = res.data?.records ?? []
  }
  catch {
    historyList.value = []
  }

  try {
    const res = await getTodayCheckin()
    todayCheckinList.value = res.data ?? []
  }
  catch {
    todayCheckinList.value = []
  }

  try {
    const res = await getWeeklyCheckin()
    sleepData.value = buildSleepDataFromWeekly(res.data ?? [])
  }
  catch {
    sleepData.value = buildDateLabels().map(date => ({
      date,
      wake: null,
      sleep: null,
      partnerWake: null,
      partnerSleep: null,
    }))
  }

  try {
    const res = await fetchSharedRecords({ page: 1, size: 1 })
    recordsCount.value = res.data?.total ?? 0
  }
  catch {
    recordsCount.value = 0
  }

  try {
    const res = await fetchSharedMediaList({ page: 1, size: 1, status: 'finished' })
    mediaFinishedCount.value = res.data?.total ?? 0
  }
  catch {
    mediaFinishedCount.value = 0
  }

  initChart()
}

function openAdd() {
  popupAmount.value = 1
  popupReason.value = ''
  showAddPopup.value = true
}

function openSub() {
  popupAmount.value = 1
  popupReason.value = ''
  showSubPopup.value = true
}

async function confirmAdd() {
  if (!popupReason.value.trim())
    return
  try {
    await addPoints(popupAmount.value, popupReason.value.trim())
    showAddPopup.value = false
    await loadData()
  }
  catch { /* notify handled by interceptor */ }
}

async function confirmSub() {
  if (!popupReason.value.trim())
    return
  try {
    await addPoints(-popupAmount.value, popupReason.value.trim())
    showSubPopup.value = false
    await loadData()
  }
  catch { /* notify handled by interceptor */ }
}

onMounted(() => {
  sleepData.value = buildDateLabels().map(date => ({
    date,
    wake: null,
    sleep: null,
    partnerWake: null,
    partnerSleep: null,
  }))
  loadData()
})

onUnmounted(() => chart?.dispose())
</script>

<template>
  <div class="dashboard-page">
    <!-- No partner empty state -->
    <div v-if="!hasPartner" class="empty-state">
      <van-icon name="friends-o" size="64" color="#ccc" />
      <p class="empty-text">{{ $t('dashboard.noPartner') }}</p>
      <van-button type="primary" round @click="router.push('/share')">
        {{ $t('share.goToBind') }}
      </van-button>
    </div>

    <template v-else>
      <!-- Row 1: 2-col grid -->
      <div class="card-grid">
        <!-- 纪念日 Card -->
        <div class="card card-anniv" @click="router.push('/share')">
          <div class="card-icon">💕</div>
          <div class="anniv-number">{{ daysTogether }}</div>
          <div class="anniv-unit">{{ $t('dashboard.days') }}</div>
          <div class="anniv-label">{{ $t('dashboard.anniversary') }}</div>
          <div class="anniv-partner">{{ partnerName }}</div>
        </div>

        <!-- 积分 Card -->
        <div class="card card-points">
          <div class="card-icon">⭐</div>
          <div class="points-number" :class="{ negative: pointsBalance < 0 }">
            {{ pointsBalance }}
          </div>
          <div class="points-label">{{ $t('dashboard.points') }}</div>
          <div class="points-actions">
            <van-button size="mini" round type="primary" @click.stop="openAdd">+</van-button>
            <van-button size="mini" round type="danger" @click.stop="openSub">-</van-button>
          </div>
        </div>
      </div>

      <!-- Row 2: 积分记录 (full width) -->
      <div class="card card-wide" @click="router.push('/partner/dashboard/points')">
        <div class="wide-header">
          <span>{{ $t('dashboard.pointsHistory') }}</span>
          <van-icon name="arrow" color="#ccc" />
        </div>
        <div v-if="historyList.length === 0" class="empty-hint">
          {{ $t('common.noData') }}
        </div>
        <div v-else class="history-scroll">
          <div v-for="item in historyList.slice(0, 5)" :key="item.id" class="history-row">
            <span class="hr-reason">{{ item.reason }}</span>
            <span class="hr-change" :class="{ add: item.pointsChange > 0, sub: item.pointsChange < 0 }">
              {{ item.pointsChange > 0 ? '+' : '' }}{{ item.pointsChange }}
            </span>
          </div>
        </div>
      </div>

      <!-- Row 3: 2-col grid -->
      <div class="card-grid">
        <!-- 数据统计 Card -->
        <div class="card card-stats" @click="router.push('/share')">
          <div class="card-icon">📊</div>
          <div class="stats-grid-inner">
            <div class="stat-item">
              <div class="stat-num">{{ recordsCount }}</div>
              <div class="stat-lbl">{{ $t('dashboard.recordsCount') }}</div>
            </div>
            <div class="stat-item">
              <div class="stat-num">{{ mediaFinishedCount }}</div>
              <div class="stat-lbl">{{ $t('dashboard.mediaFinished') }}</div>
            </div>
          </div>
        </div>

        <!-- 作息打卡 Card -->
        <div class="card card-sleep" @click="router.push('/partner/dashboard/sleep')">
          <div class="card-icon">😴</div>
          <div class="sleep-summary">
            <div class="sleep-stat" :class="{ done: wakeChecked }">
              {{ $t('dashboard.wakeUp') }}: {{ wakeChecked ? wakeTime : $t('dashboard.notCheckedIn') }}
            </div>
            <div class="sleep-stat" :class="{ done: sleepChecked }">
              {{ $t('dashboard.goToSleep') }}: {{ sleepChecked ? sleepTime : $t('dashboard.notCheckedIn') }}
            </div>
          </div>
        </div>
      </div>

      <!-- Row 4: 近7天作息折线图 (full width) -->
      <div class="card card-wide" @click="router.push('/partner/dashboard/sleep')">
        <div class="wide-header">
          <span>{{ $t('dashboard.sleepTrend') }}</span>
          <van-icon name="arrow" color="#ccc" />
        </div>
        <div id="dashboard-sleep-chart" class="sleep-chart" />
      </div>

      <!-- 加分弹窗 -->
      <van-action-sheet v-model:show="showAddPopup" :title="$t('dashboard.addPoints')" :close-on-click-action="false">
        <div class="popup-body">
          <div class="popup-amount add">+{{ popupAmount }}</div>
          <van-stepper v-model="popupAmount" :min="1" :max="100" />
          <van-field v-model="popupReason" :placeholder="$t('dashboard.reasonPlaceholder')" clearable :maxlength="50" required />
          <van-button type="primary" block round :disabled="!popupReason.trim()" @click="confirmAdd">
            {{ $t('dashboard.confirmAdd') }}
          </van-button>
        </div>
      </van-action-sheet>

      <!-- 扣分弹窗 -->
      <van-action-sheet v-model:show="showSubPopup" :title="$t('dashboard.subPoints')" :close-on-click-action="false">
        <div class="popup-body">
          <div class="popup-amount sub">-{{ popupAmount }}</div>
          <van-stepper v-model="popupAmount" :min="1" :max="100" />
          <van-field v-model="popupReason" :placeholder="$t('dashboard.reasonPlaceholder')" clearable :maxlength="50" required />
          <van-button type="danger" block round :disabled="!popupReason.trim()" @click="confirmSub">
            {{ $t('dashboard.confirmSub') }}
          </van-button>
        </div>
      </van-action-sheet>
    </template>
  </div>
</template>

<style scoped>
.dashboard-page {
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 100vh;
  background: var(--van-background);
  color: var(--van-text-color);
  padding-bottom: 80px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  gap: 16px;
}

.empty-text {
  color: var(--van-text-color-2);
  font-size: 14px;
  text-align: center;
  line-height: 1.5;
}

.empty-hint {
  color: var(--van-text-color-3);
  font-size: 12px;
  text-align: center;
  padding: 8px 0;
}

.card-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.card {
  background: var(--van-background-2);
  border-radius: 12px;
  padding: 14px;
  border: 1px solid var(--van-border-color);
  display: flex;
  flex-direction: column;
  cursor: pointer;
}

.card-icon {
  font-size: 22px;
  margin-bottom: 6px;
}

.card-anniv {
  background: linear-gradient(135deg, var(--van-primary-color) 0%, #d4784a 100%);
  color: white;
  align-items: center;
  justify-content: center;
  text-align: center;
  border: none;
}

.anniv-number {
  font-size: 32px;
  font-weight: 700;
  line-height: 1.1;
}

.anniv-unit {
  font-size: 12px;
  opacity: 0.8;
}

.anniv-label {
  font-size: 11px;
  opacity: 0.8;
}

.anniv-partner {
  font-size: 12px;
  opacity: 0.7;
  margin-top: 2px;
}

.card-points {
  align-items: center;
  justify-content: center;
  text-align: center;
}

.points-number {
  font-size: 32px;
  font-weight: 700;
  color: var(--van-tag-success-color);
  line-height: 1.1;
}

.points-number.negative {
  color: var(--van-tag-danger-color);
}

.points-label {
  font-size: 11px;
  color: var(--van-text-color-3);
  margin: 2px 0 6px;
}

.points-actions {
  display: flex;
  gap: 8px;
}

.card-wide {
  flex-direction: column;
}

.wide-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  font-weight: 500;
  color: var(--van-text-color);
  margin-bottom: 6px;
}

.history-scroll {
  display: flex;
  flex-direction: column;
}

.history-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 5px 0;
  font-size: 12px;
  border-bottom: 1px solid var(--van-border-color);
}

.history-row:last-child {
  border-bottom: none;
}

.hr-reason {
  color: var(--van-text-color);
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hr-change {
  font-weight: 600;
  margin-left: 8px;
}

.hr-change.add {
  color: var(--van-tag-success-color);
}

.hr-change.sub {
  color: var(--van-tag-danger-color);
}

.card-stats {
  justify-content: center;
}

.stats-grid-inner {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px;
}

.stat-item {
  text-align: center;
}

.stat-num {
  font-size: 22px;
  font-weight: 700;
  color: var(--van-primary-color);
}

.stat-lbl {
  font-size: 10px;
  color: var(--van-text-color-3);
}

.card-sleep {
  justify-content: center;
}

.sleep-summary {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.sleep-stat {
  font-size: 12px;
  color: var(--van-text-color-3);
  padding: 4px 8px;
  background: var(--van-tag-default-color);
  border-radius: 6px;
  text-align: center;
}

.sleep-stat.done {
  color: var(--van-tag-success-color);
  background: color-mix(in srgb, var(--van-tag-success-color) 18%, white);
}

.sleep-chart {
  width: 100%;
  height: 130px;
}

.popup-body {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.popup-amount {
  font-size: 36px;
  font-weight: 700;
  text-align: center;
}

.popup-amount.add {
  color: var(--van-tag-success-color);
}

.popup-amount.sub {
  color: var(--van-tag-danger-color);
}
</style>

<route lang="json5">
{
  name: 'PartnerDashboard'
}
</route>
