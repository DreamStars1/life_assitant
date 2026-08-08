<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { showToast } from 'vant'
import { useUserStore } from '@/stores'
import { doCheckin, getTodayCheckin, getWeeklyCheckin } from '@/api/modules/partner-checkin'
import type { CheckinRecord } from '@/api/modules/partner-checkin'
import { emitShibaGreet } from '@/components/shiba-pet/shibaPetLogic'

type RangeDays = 7 | 30

const { t } = useI18n()
const userStore = useUserStore()
const myId = computed(() => userStore.userInfo?.id || '')
const partnerId = computed(() => userStore.userInfo?.partnerId || '')
const partnerName = computed(() => userStore.partnerName || '对方')

const todayCheckins = ref<CheckinRecord[]>([])
const rangeDays = ref<RangeDays>(7)

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

function getDateLabels(days: number): string[] {
  const labels: string[] = []
  const base = businessNow()
  for (let i = days - 1; i >= 0; i--) {
    const d = new Date(base)
    d.setDate(base.getDate() - i)
    labels.push(`${d.getMonth() + 1}/${d.getDate()}`)
  }
  return labels
}

function dateKey(offsetFromEnd: number, days: number): string {
  const d = businessNow()
  d.setDate(d.getDate() - (days - 1 - offsetFromEnd))
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

interface SleepChartDay {
  date: string
  wake: string | null
  sleep: string | null
  partnerWake: string | null
  partnerSleep: string | null
}

const dateLabels = ref(getDateLabels(7))
const sleepData = ref<SleepChartDay[]>([])

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

function emptyDays(labels: string[]): SleepChartDay[] {
  return labels.map(date => ({
    date,
    wake: null,
    sleep: null,
    partnerWake: null,
    partnerSleep: null,
  }))
}

function buildChartFromCheckins(checkins: CheckinRecord[], days: number): SleepChartDay[] {
  const labels = getDateLabels(days)
  dateLabels.value = labels
  const mine = groupByUser(checkins, myId.value)
  const partner = partnerId.value ? groupByUser(checkins, partnerId.value) : {}
  return labels.map((date, i) => {
    const key = dateKey(i, days)
    return {
      date,
      wake: mine[key]?.wake ?? null,
      sleep: mine[key]?.sleep ?? null,
      partnerWake: partner[key]?.wake ?? null,
      partnerSleep: partner[key]?.sleep ?? null,
    }
  })
}

let wakeChart: echarts.ECharts | null = null
let sleepChart: echarts.ECharts | null = null

function cssVar(name: string, fallback: string): string {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim() || fallback
}

function formatAxisTime(v: number): string {
  const h = Math.floor(v / 60) % 24
  const m = v % 60
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`
}

function initOneChart(
  elId: string,
  existing: echarts.ECharts | null,
  opts: {
    mineLabel: string
    partnerLabel: string
    mineData: (number | null)[]
    partnerData: (number | null)[]
    mineColor: string
    partnerColor: string
    yMin: number
    yMax: number
    tooltipMine: (d: SleepChartDay) => string
    tooltipPartner: (d: SleepChartDay) => string
  },
): echarts.ECharts | null {
  const el = document.getElementById(elId)
  if (!el)
    return null
  existing?.dispose()
  const chart = echarts.init(el)

  const dates = dateLabels.value
  const text2 = cssVar('--van-text-color-2', '#8b7a6b')
  const border = cssVar('--van-border-color', '#f0e6d8')
  const days = rangeDays.value
  // ponytail: 30 天标签抽稀，挤了再调 interval
  const labelInterval = days > 7 ? Math.ceil(days / 8) - 1 : 0

  const legend = [opts.mineLabel]
  if (partnerId.value)
    legend.push(opts.partnerLabel)

  const series: echarts.SeriesOption[] = [
    {
      name: opts.mineLabel,
      type: 'line',
      data: opts.mineData,
      smooth: true,
      symbolSize: 6,
      lineStyle: { color: opts.mineColor, width: 2 },
      itemStyle: { color: opts.mineColor },
    },
  ]
  if (partnerId.value) {
    series.push({
      name: opts.partnerLabel,
      type: 'line',
      data: opts.partnerData,
      smooth: true,
      symbolSize: 6,
      lineStyle: { color: opts.partnerColor, width: 2 },
      itemStyle: { color: opts.partnerColor },
    })
  }

  chart.setOption({
    tooltip: {
      trigger: 'axis',
      formatter: (params: unknown) => {
        const list = params as Array<{ dataIndex?: number }>
        if (!list?.length)
          return ''
        const i = list[0]?.dataIndex
        if (i == null)
          return ''
        const self = sleepData.value[i]
        if (!self)
          return ''
        const lines = [`<b>${dates[i]}</b>`, opts.tooltipMine(self)]
        if (partnerId.value)
          lines.push(opts.tooltipPartner(self))
        return lines.join('<br/>')
      },
    },
    legend: {
      data: legend,
      bottom: 0,
      textStyle: { fontSize: 10, color: text2 },
      itemWidth: 12,
      itemHeight: 8,
    },
    grid: { left: 50, right: 16, top: 16, bottom: 48 },
    xAxis: {
      type: 'category',
      data: dates,
      axisLabel: {
        fontSize: 10,
        color: text2,
        interval: labelInterval,
      },
      axisLine: { show: false },
    },
    yAxis: {
      type: 'value',
      min: opts.yMin,
      max: opts.yMax,
      axisLabel: {
        fontSize: 10,
        color: text2,
        formatter: formatAxisTime,
      },
      splitLine: { lineStyle: { color: border } },
    },
    series,
    animation: false,
  })
  return chart
}

function initCharts() {
  nextTick(() => {
    const primary = cssVar('--van-primary-color', '#e8905e')
    const danger = cssVar('--van-tag-danger-color', '#d97a6e')
    const success = cssVar('--van-tag-success-color', '#7ec8a0')
    const warning = cssVar('--van-tag-warning-color', '#e8b05e')

    wakeChart = initOneChart('wake-trend-chart', wakeChart, {
      mineLabel: labelMineWake.value,
      partnerLabel: labelPartnerWake.value,
      mineData: sleepData.value.map(d => toMinutes(d.wake, 'wake')),
      partnerData: sleepData.value.map(d => toMinutes(d.partnerWake, 'wake')),
      mineColor: primary,
      partnerColor: success,
      yMin: 300,
      yMax: 720,
      tooltipMine: d => `${labelMineWake.value}: ${d.wake ?? '-'}`,
      tooltipPartner: d => `${labelPartnerWake.value}: ${d.partnerWake ?? '-'}`,
    })

    sleepChart = initOneChart('sleep-trend-chart', sleepChart, {
      mineLabel: labelMineSleep.value,
      partnerLabel: labelPartnerSleep.value,
      mineData: sleepData.value.map(d => toMinutes(d.sleep, 'sleep')),
      partnerData: sleepData.value.map(d => toMinutes(d.partnerSleep, 'sleep')),
      mineColor: danger,
      partnerColor: warning,
      yMin: 1080,
      yMax: 1620,
      tooltipMine: d => `${labelMineSleep.value}: ${d.sleep ?? '-'}`,
      tooltipPartner: d => `${labelPartnerSleep.value}: ${d.partnerSleep ?? '-'}`,
    })
  })
}

async function loadTrend() {
  const days: RangeDays = Number(rangeDays.value) === 30 ? 30 : 7
  try {
    const res = await getWeeklyCheckin(days)
    sleepData.value = buildChartFromCheckins(res.data ?? [], days)
  }
  catch {
    const labels = getDateLabels(days)
    dateLabels.value = labels
    sleepData.value = emptyDays(labels)
  }
  initCharts()
}

async function loadData() {
  try {
    const res = await getTodayCheckin()
    todayCheckins.value = res.data ?? []
  }
  catch {
    todayCheckins.value = []
  }
  await loadTrend()
}

async function handleCheckin(type: 'wake' | 'sleep') {
  try {
    await doCheckin(type)
    showToast(type === 'wake' ? '已打卡起床' : '已打卡睡觉')
    emitShibaGreet()
    await loadData()
  }
  catch { /* notify handled by interceptor */ }
}

function formatTime(value: string | number[]): string {
  return extractTime(value)
}

watch(rangeDays, () => {
  loadTrend()
})

onMounted(loadData)
onUnmounted(() => {
  wakeChart?.dispose()
  sleepChart?.dispose()
})
</script>

<template>
  <div class="sleep-page">
    <div class="section-title">
      {{ $t('dashboard.todayCheckin') }}
    </div>
    <div class="checkin-row">
      <div class="checkin-card" :class="{ done: wakeDone }">
        <div class="checkin-icon">
          🌅
        </div>
        <div class="checkin-label">
          {{ $t('dashboard.wakeUp') }}
        </div>
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
        <div class="checkin-icon">
          🌙
        </div>
        <div class="checkin-label">
          {{ $t('dashboard.goToSleep') }}
        </div>
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

    <van-tabs v-model:active="rangeDays" shrink class="range-tabs">
      <van-tab :title="$t('dashboard.range7Days')" :name="7" />
      <van-tab :title="$t('dashboard.range30Days')" :name="30" />
    </van-tabs>

    <div class="chart-card">
      <div class="chart-title">
        {{ $t('dashboard.wakeTrend') }}
      </div>
      <div id="wake-trend-chart" class="sleep-chart" />
    </div>

    <div class="chart-card">
      <div class="chart-title">
        {{ $t('dashboard.sleepTrendDetail') }}
      </div>
      <div id="sleep-trend-chart" class="sleep-chart" />
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

.range-tabs {
  margin-bottom: 12px;
}

.chart-card {
  background: var(--van-background-2);
  border-radius: 12px;
  padding: 16px;
  border: 1px solid var(--van-border-color);
  margin-bottom: 12px;
}

.chart-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--van-text-color);
  margin-bottom: 12px;
}

.sleep-chart {
  width: 100%;
  height: 240px;
}
</style>

<route lang="json5">
{
  name: 'PartnerDashboardSleep'
}
</route>
