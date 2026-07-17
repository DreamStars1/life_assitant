<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, computed } from 'vue'
import * as echarts from 'echarts'
import { showToast } from 'vant'
import { useUserStore } from '@/stores'
import { getTodayCheckin, getWeeklyCheckin, doCheckin } from '@/api/modules/partner-checkin'
import type { CheckinRecord } from '@/api/modules/partner-checkin'

const { t } = useI18n()
const userStore = useUserStore()
const myId = computed(() => userStore.userInfo?.id || '')
const partnerId = computed(() => userStore.userInfo?.partnerId || '')
const partnerName = computed(() => userStore.partnerName || '对方')

const todayCheckins = ref<CheckinRecord[]>([])
const weeklyCheckins = ref<CheckinRecord[]>([])

const myToday = computed(() => todayCheckins.value.filter(c => c.userId === myId.value))
const wakeRecord = computed(() => myToday.value.find(c => c.checkinType === 'wake'))
const sleepRecord = computed(() => myToday.value.find(c => c.checkinType === 'sleep'))
const wakeDone = computed(() => !!wakeRecord.value)
const sleepDone = computed(() => !!sleepRecord.value)

function businessNow(base = new Date()): Date {
  const d = new Date(base)
  // ponytail: 作息日界 4 点，与后端 PartnerCheckinService.businessDate 一致
  if (d.getHours() < 4)
    d.setDate(d.getDate() - 1)
  return d
}

function getDateLabels(): string[] {
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

const dateLabels = ref(getDateLabels())

interface SleepChartDay {
  date: string
  wake: string | null
  sleep: string | null
  partnerWake: string | null
  partnerSleep: string | null
}

const sleepData = ref<SleepChartDay[]>(
  dateLabels.value.map(date => ({
    date,
    wake: null,
    sleep: null,
    partnerWake: null,
    partnerSleep: null,
  })),
)

const labelMineWake = computed(() => `我·${t('dashboard.wakeUp')}`)
const labelMineSleep = computed(() => `我·${t('dashboard.goToSleep')}`)
const labelPartnerWake = computed(() => `${partnerName.value}·${t('dashboard.wakeUp')}`)
const labelPartnerSleep = computed(() => `${partnerName.value}·${t('dashboard.goToSleep')}`)

function toMinutes(time: string | null, type: 'wake' | 'sleep' = 'wake'): number | null {
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

function extractTime(checkinTime: string | number[] | null | undefined): string {
  if (checkinTime == null || checkinTime === '')
    return ''
  if (Array.isArray(checkinTime)) {
    const h = Number(checkinTime[3] ?? 0)
    const mi = Number(checkinTime[4] ?? 0)
    return `${String(h).padStart(2, '0')}:${String(mi).padStart(2, '0')}`
  }
  const s = String(checkinTime)
  const matched = s.match(/(\d{1,2}):(\d{2})/)
  if (matched)
    return `${matched[1]!.padStart(2, '0')}:${matched[2]}`
  const d = new Date(s.replace(' ', 'T'))
  if (!Number.isNaN(d.getTime())) {
    return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
  }
  return ''
}

function normalizeDateKey(v: string | number[] | null | undefined): string {
  if (v == null || v === '')
    return ''
  if (Array.isArray(v)) {
    const y = v[0]
    const m = String(v[1]).padStart(2, '0')
    const d = String(v[2]).padStart(2, '0')
    return `${y}-${m}-${d}`
  }
  return String(v).slice(0, 10)
}

function groupByUser(checkins: CheckinRecord[], userId: string) {
  const grouped: Record<string, { wake: string | null, sleep: string | null }> = {}
  for (const c of checkins.filter(item => item.userId === userId)) {
    const key = normalizeDateKey(c.checkinDate)
    if (!key)
      continue
    if (!grouped[key])
      grouped[key] = { wake: null, sleep: null }
    const time = extractTime(c.checkinTime)
    if (c.checkinType === 'wake')
      grouped[key]!.wake = time || null
    else
      grouped[key]!.sleep = time || null
  }
  return grouped
}

function buildChartFromWeekly(checkins: CheckinRecord[]): SleepChartDay[] {
  const mine = groupByUser(checkins, myId.value)
  const partner = partnerId.value ? groupByUser(checkins, partnerId.value) : {}
  return dateLabels.value.map((date, i) => {
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

let chart: echarts.ECharts | null = null

function cssVar(name: string, fallback: string): string {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim() || fallback
}

function initChart() {
  nextTick(() => {
    const el = document.getElementById('sleep-detail-chart')
    if (!el)
      return
    chart?.dispose()
    chart = echarts.init(el)

    const dates = dateLabels.value
    const primary = cssVar('--van-primary-color', '#e8905e')
    const danger = cssVar('--van-tag-danger-color', '#d97a6e')
    const success = cssVar('--van-tag-success-color', '#7ec8a0')
    const warning = cssVar('--van-tag-warning-color', '#e8b05e')
    const text2 = cssVar('--van-text-color-2', '#8b7a6b')
    const border = cssVar('--van-border-color', '#f0e6d8')

    const legend = [
      labelMineWake.value,
      labelMineSleep.value,
      labelPartnerWake.value,
      labelPartnerSleep.value,
    ]

    chart.setOption({
      tooltip: {
        trigger: 'axis',
        formatter: (params: any) => {
          if (!params || params.length === 0)
            return ''
          const i = params[0]?.dataIndex
          if (i == null)
            return ''
          const self = sleepData.value[i!]
          if (!self)
            return ''
          return [
            `<b>${dates[i!]}</b>`,
            `${labelMineWake.value}: ${self.wake ?? '-'}`,
            `${labelMineSleep.value}: ${self.sleep ?? '-'}`,
            `${labelPartnerWake.value}: ${self.partnerWake ?? '-'}`,
            `${labelPartnerSleep.value}: ${self.partnerSleep ?? '-'}`,
          ].join('<br/>')
        },
      },
      legend: {
        data: legend,
        bottom: 0,
        textStyle: { fontSize: 10, color: text2 },
        itemWidth: 12,
        itemHeight: 8,
      },
      grid: { left: 50, right: 16, top: 16, bottom: 56 },
      xAxis: {
        type: 'category',
        data: dates,
        axisLabel: { fontSize: 10, color: text2 },
        axisLine: { show: false },
      },
      yAxis: {
        type: 'value',
        min: 300,
        max: 1620,
        axisLabel: {
          fontSize: 10,
          color: text2,
          formatter: (v: number) => {
            const h = Math.floor(v / 60) % 24
            const m = v % 60
            return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`
          },
        },
        splitLine: { lineStyle: { color: border } },
      },
      series: [
        {
          name: labelMineWake.value,
          type: 'line',
          data: sleepData.value.map(d => toMinutes(d.wake, 'wake')),
          smooth: true,
          symbolSize: 6,
          lineStyle: { color: primary, width: 2 },
          itemStyle: { color: primary },
        },
        {
          name: labelMineSleep.value,
          type: 'line',
          data: sleepData.value.map(d => toMinutes(d.sleep, 'sleep')),
          smooth: true,
          symbolSize: 6,
          lineStyle: { color: danger, width: 2, type: 'dashed' },
          itemStyle: { color: danger },
        },
        {
          name: labelPartnerWake.value,
          type: 'line',
          data: sleepData.value.map(d => toMinutes(d.partnerWake, 'wake')),
          smooth: true,
          symbolSize: 6,
          lineStyle: { color: success, width: 2 },
          itemStyle: { color: success },
        },
        {
          name: labelPartnerSleep.value,
          type: 'line',
          data: sleepData.value.map(d => toMinutes(d.partnerSleep, 'sleep')),
          smooth: true,
          symbolSize: 6,
          lineStyle: { color: warning, width: 2, type: 'dashed' },
          itemStyle: { color: warning },
        },
      ],
      animation: false,
    })
  })
}

async function loadData() {
  try {
    const res = await getTodayCheckin()
    todayCheckins.value = res.data ?? []
  }
  catch {
    todayCheckins.value = []
  }

  try {
    const res = await getWeeklyCheckin()
    weeklyCheckins.value = res.data ?? []
    sleepData.value = buildChartFromWeekly(weeklyCheckins.value)
  }
  catch {
    sleepData.value = dateLabels.value.map(date => ({
      date,
      wake: null,
      sleep: null,
      partnerWake: null,
      partnerSleep: null,
    }))
  }

  initChart()
}

async function handleCheckin(type: 'wake' | 'sleep') {
  try {
    await doCheckin(type)
    showToast(type === 'wake' ? '已打卡起床' : '已打卡睡觉')
    await loadData()
  }
  catch { /* notify handled by interceptor */ }
}

function formatTime(value: string | number[]): string {
  return extractTime(value)
}

onMounted(loadData)
onUnmounted(() => chart?.dispose())
</script>

<template>
  <div class="sleep-page">
    <div class="section-title">{{ $t('dashboard.todayCheckin') }}</div>
    <div class="checkin-row">
      <div class="checkin-card" :class="{ done: wakeDone }">
        <div class="checkin-icon">🌅</div>
        <div class="checkin-label">{{ $t('dashboard.wakeUp') }}</div>
        <div v-if="wakeDone" class="checkin-time">
          {{ formatTime(wakeRecord!.checkinTime) }}
        </div>
        <van-button
          v-else
          size="small"
          round
          type="primary"
          @click="handleCheckin('wake')"
        >
          {{ $t('dashboard.checkin') }}
        </van-button>
      </div>

      <div class="checkin-card" :class="{ done: sleepDone }">
        <div class="checkin-icon">🌙</div>
        <div class="checkin-label">{{ $t('dashboard.goToSleep') }}</div>
        <div v-if="sleepDone" class="checkin-time">
          {{ formatTime(sleepRecord!.checkinTime) }}
        </div>
        <van-button
          v-else
          size="small"
          round
          :type="wakeDone ? 'danger' : 'default'"
          @click="handleCheckin('sleep')"
        >
          {{ $t('dashboard.checkin') }}
        </van-button>
      </div>
    </div>

    <div class="chart-card">
      <div class="chart-title">{{ $t('dashboard.sleepTrend') }}</div>
      <div id="sleep-detail-chart" class="sleep-chart" />
    </div>
  </div>
</template>

<style scoped>
.sleep-page {
  padding: 16px;
  min-height: 100vh;
  background: var(--van-background);
  color: var(--van-text-color);
  padding-bottom: 80px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--van-text-color);
  margin-bottom: 10px;
}

.checkin-row {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.checkin-card {
  flex: 1;
  text-align: center;
  padding: 20px 12px;
  border-radius: 12px;
  background: var(--van-background-2);
  border: 1px solid var(--van-border-color);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.checkin-card.done {
  background: color-mix(in srgb, var(--van-tag-success-color) 18%, white);
  border-color: color-mix(in srgb, var(--van-tag-success-color) 35%, white);
}

.checkin-icon {
  font-size: 32px;
}

.checkin-label {
  font-size: 14px;
  color: var(--van-text-color-2);
  font-weight: 500;
}

.checkin-time {
  font-size: 20px;
  font-weight: 700;
  color: var(--van-tag-success-color);
}

.chart-card {
  background: var(--van-background-2);
  border-radius: 12px;
  padding: 16px;
  border: 1px solid var(--van-border-color);
}

.chart-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--van-text-color);
  margin-bottom: 12px;
}

.sleep-chart {
  width: 100%;
  height: 260px;
}
</style>

<route lang="json5">
{
  name: 'PartnerDashboardSleep'
}
</route>
