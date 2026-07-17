<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { showToast } from 'vant'
import { getPointsBalance, getPointsHistory, addPoints } from '@/api/modules/partner-points'
import type { PointsRecord } from '@/api/modules/partner-points'

useI18n()

const balance = ref(0)
const amount = ref(1)
const reason = ref('')
const mode = ref<'add' | 'sub'>('add')
const submitting = ref(false)
const history = ref<PointsRecord[]>([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)
const finished = ref(false)

async function loadBalance() {
  try {
    const res = await getPointsBalance()
    balance.value = res.data ?? 0
  }
  catch {
    balance.value = 0
  }
}

async function loadHistory(append = false) {
  loading.value = true
  try {
    const res = await getPointsHistory(page.value, 20)
    const records = res.data?.records ?? []
    total.value = res.data?.total ?? 0
    if (append)
      history.value.push(...records)
    else
      history.value = records
    finished.value = history.value.length >= total.value
  }
  catch {
    if (!append) {
      history.value = []
      total.value = 0
    }
    finished.value = true
  }
  finally {
    loading.value = false
  }
}

function onLoad() {
  if (finished.value)
    return
  page.value++
  loadHistory(true)
}

function setQuickAmount(val: number) {
  mode.value = val > 0 ? 'add' : 'sub'
  amount.value = Math.abs(val)
}

async function confirmPoints() {
  if (!reason.value.trim())
    return
  submitting.value = true
  try {
    const change = mode.value === 'add' ? amount.value : -amount.value
    await addPoints(change, reason.value.trim())
    showToast(mode.value === 'add' ? '已加分' : '已扣分')
    amount.value = 1
    reason.value = ''
    mode.value = 'add'
    page.value = 1
    finished.value = false
    await loadBalance()
    await loadHistory()
  }
  catch { /* notify handled by interceptor */ }
  finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadBalance()
  loadHistory()
})
</script>

<template>
  <div class="points-page">
    <div class="balance-section">
      <div class="balance-number" :class="{ negative: balance < 0 }">
        {{ balance }}
      </div>
      <div class="balance-label">{{ $t('dashboard.points') }}</div>
    </div>

    <div class="action-card">
      <div class="action-group">
        <span class="action-label">{{ $t('dashboard.addPoints') }}</span>
        <van-button size="small" round type="primary" @click="setQuickAmount(1)">+1</van-button>
        <van-button size="small" round type="primary" @click="setQuickAmount(3)">+3</van-button>
        <van-button size="small" round type="primary" @click="setQuickAmount(5)">+5</van-button>
      </div>
      <div class="action-divider" />
      <div class="action-group">
        <span class="action-label">{{ $t('dashboard.subPoints') }}</span>
        <van-button size="small" round type="danger" @click="setQuickAmount(-1)">-1</van-button>
        <van-button size="small" round type="danger" @click="setQuickAmount(-3)">-3</van-button>
        <van-button size="small" round type="danger" @click="setQuickAmount(-5)">-5</van-button>
      </div>
    </div>

    <div class="input-card">
      <div class="input-row">
        <span class="input-label">{{ mode === 'add' ? $t('dashboard.addPoints') : $t('dashboard.subPoints') }}</span>
        <van-stepper v-model="amount" :min="1" :max="100" />
      </div>
      <van-field
        v-model="reason"
        :placeholder="$t('dashboard.reasonPlaceholder')"
        clearable
        :maxlength="50"
        class="reason-field"
      />
      <div class="input-preview" :class="mode">
        {{ mode === 'add' ? '+' : '-' }}{{ amount }}
      </div>
      <van-button
        :type="mode === 'add' ? 'primary' : 'danger'"
        block
        round
        :disabled="!reason.trim()"
        :loading="submitting"
        @click="confirmPoints"
      >
        {{ mode === 'add' ? $t('dashboard.confirmAdd') : $t('dashboard.confirmSub') }}
      </van-button>
    </div>

    <div class="history-section">
      <div class="history-title">{{ $t('dashboard.pointsHistory') }}</div>
      <div v-if="history.length === 0 && !loading" class="empty-hint">
        {{ $t('common.noData') }}
      </div>
      <van-list
        v-else
        v-model:loading="loading"
        :finished="finished"
        finished-text=""
        @load="onLoad"
      >
        <div v-for="item in history" :key="item.id" class="history-item">
          <div class="hi-left">
            <div class="hi-reason">{{ item.reason }}</div>
            <div class="hi-time">{{ new Date(item.createdAt).toLocaleString('zh-CN') }}</div>
          </div>
          <div class="hi-change" :class="{ add: item.pointsChange > 0, sub: item.pointsChange < 0 }">
            {{ item.pointsChange > 0 ? '+' : '' }}{{ item.pointsChange }}
          </div>
        </div>
      </van-list>
    </div>
  </div>
</template>

<style scoped>
.points-page {
  padding: 16px;
  min-height: 100vh;
  background: var(--van-background);
  color: var(--van-text-color);
  padding-bottom: 80px;
}

.balance-section {
  text-align: center;
  padding: 24px 0;
  background: var(--van-background-2);
  border-radius: 12px;
  border: 1px solid var(--van-border-color);
  margin-bottom: 12px;
}

.balance-number {
  font-size: 56px;
  font-weight: 700;
  color: var(--van-tag-success-color);
  line-height: 1;
}

.balance-number.negative {
  color: var(--van-tag-danger-color);
}

.balance-label {
  font-size: 13px;
  color: var(--van-text-color-3);
  margin-top: 4px;
}

.action-card {
  background: var(--van-background-2);
  border-radius: 12px;
  padding: 16px;
  border: 1px solid var(--van-border-color);
  margin-bottom: 12px;
}

.action-group {
  display: flex;
  align-items: center;
  gap: 10px;
}

.action-divider {
  height: 1px;
  background: var(--van-border-color);
  margin: 12px 0;
}

.action-label {
  font-size: 13px;
  color: var(--van-text-color-2);
  min-width: 32px;
}

.input-card {
  background: var(--van-background-2);
  border-radius: 12px;
  padding: 16px;
  border: 1px solid var(--van-border-color);
  margin-bottom: 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.input-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.input-label {
  font-size: 14px;
  color: var(--van-text-color);
  font-weight: 500;
}

.reason-field {
  background: var(--van-tag-default-color);
  border-radius: 8px;
  padding: 4px 12px;
}

.input-preview {
  font-size: 32px;
  font-weight: 700;
  text-align: center;
}

.input-preview.add {
  color: var(--van-tag-success-color);
}

.input-preview.sub {
  color: var(--van-tag-danger-color);
}

.history-section {
  background: var(--van-background-2);
  border-radius: 12px;
  padding: 16px;
  border: 1px solid var(--van-border-color);
}

.history-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--van-text-color);
  margin-bottom: 8px;
}

.empty-hint {
  color: var(--van-text-color-3);
  font-size: 12px;
  text-align: center;
  padding: 16px 0;
}

.history-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid var(--van-border-color);
}

.history-item:last-child {
  border-bottom: none;
}

.hi-left {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.hi-reason {
  font-size: 14px;
  color: var(--van-text-color);
}

.hi-time {
  font-size: 12px;
  color: var(--van-text-color-3);
}

.hi-change {
  font-size: 18px;
  font-weight: 700;
}

.hi-change.add {
  color: var(--van-tag-success-color);
}

.hi-change.sub {
  color: var(--van-tag-danger-color);
}
</style>

<route lang="json5">
{
  name: 'PartnerDashboardPoints'
}
</route>
