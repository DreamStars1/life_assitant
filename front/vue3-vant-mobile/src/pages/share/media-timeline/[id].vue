<script setup lang="ts">
import { showToast } from 'vant'
import { useUserStore } from '@/stores'
import { fetchProgressEvents } from '@/api/modules/shared-media'
import type { MediaProgressEvent } from '@/api/modules/shared-media'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const mediaId = computed(() => {
  const id = (route.params as Record<string, string | string[]>).id
  return Array.isArray(id) ? id[0] : (id ?? '')
})

const events = ref<MediaProgressEvent[]>([])
const loading = ref(true)

const myId = computed(() => userStore.userInfo.id)
const partnerName = computed(() => userStore.partnerName || '对方')

function operatorLabel(userId: string): string {
  if (userId === myId.value)
    return '你'
  return partnerName.value
}

function scopeLabel(scope: MediaProgressEvent['scope']): string {
  return scope === 'shared' ? '共同' : '个人'
}

function parseDateTime(value: string): Date {
  if (!value)
    return new Date(Number.NaN)
  if (/z|[+-]\d{2}:?\d{2}$/i.test(value))
    return new Date(value)
  return new Date(value.replace(' ', 'T'))
}

function formatTime(iso: string): string {
  const d = parseDateTime(iso)
  if (Number.isNaN(d.getTime()))
    return ''
  return d.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).replace(/\//g, '-')
}

async function loadEvents() {
  const id = mediaId.value
  if (!id)
    return

  loading.value = true
  try {
    const res = await fetchProgressEvents(id)
    events.value = res.data ?? []
  }
  catch {
    showToast('加载失败')
  }
  finally {
    loading.value = false
  }
}

function goBack() {
  router.back()
}

onMounted(() => {
  loadEvents()
})

watch(mediaId, (id, prev) => {
  if (id && id !== prev)
    loadEvents()
})
</script>

<template>
  <div class="timeline-page">
    <van-nav-bar title="进度时间轴" left-arrow @click-left="goBack" />

    <div v-if="loading" class="loading-state">
      <van-loading type="spinner" size="24" />
    </div>

    <div v-else-if="events.length === 0" class="empty-state">
      <van-icon name="clock-o" size="48" color="var(--van-gray-4)" />
      <p>还没有进度记录</p>
    </div>

    <div v-else class="timeline">
      <div
        v-for="(event, index) in events"
        :key="event.id"
        class="timeline-item"
        :class="{ first: index === 0, last: index === events.length - 1 }"
      >
        <div class="timeline-axis">
          <span
            class="timeline-dot"
            :class="event.scope === 'shared' ? 'dot-shared' : 'dot-personal'"
          />
        </div>
        <div class="timeline-card">
          <div class="event-time">
            {{ formatTime(event.createdAt) }}
          </div>
          <div class="event-meta">
            <van-tag :type="event.scope === 'shared' ? 'primary' : 'default'" plain>
              {{ scopeLabel(event.scope) }}
            </van-tag>
            <span class="event-operator">{{ operatorLabel(event.userId) }}</span>
          </div>
          <div class="event-text">
            {{ event.progressText }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.timeline-page {
  min-height: 100dvh;
  background: var(--van-background);
}

.loading-state,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 16px;
  gap: 12px;
  color: var(--van-text-color-3);
  font-size: 14px;
}

.timeline {
  padding: 16px 16px 24px 12px;
}

.timeline-item {
  display: flex;
  align-items: stretch;
  gap: 12px;
  position: relative;
}

.timeline-axis {
  position: relative;
  width: 16px;
  flex-shrink: 0;
  display: flex;
  justify-content: center;
}

.timeline-axis::before {
  content: '';
  position: absolute;
  top: 0;
  bottom: 0;
  left: 50%;
  width: 2px;
  transform: translateX(-50%);
  background: var(--van-border-color);
}

.timeline-item.first .timeline-axis::before {
  top: 10px;
}

.timeline-item.last .timeline-axis::before {
  bottom: calc(100% - 14px);
}

.timeline-dot {
  position: relative;
  z-index: 1;
  margin-top: 6px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  border: 2px solid #1989fa;
  background: #fff;
  box-sizing: border-box;
  flex-shrink: 0;
}

.timeline-dot.dot-shared {
  border-color: #1989fa;
  background: #1989fa;
}

.timeline-dot.dot-personal {
  border-color: #969799;
  background: #fff;
}

.timeline-card {
  flex: 1;
  min-width: 0;
  margin-bottom: 16px;
  padding: 12px 14px;
  background: var(--van-background-2, #fff);
  border-radius: 10px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.timeline-item.last .timeline-card {
  margin-bottom: 0;
}

.event-time {
  font-size: 12px;
  color: var(--van-text-color-3);
  margin-bottom: 6px;
}

.event-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.event-operator {
  font-size: 13px;
  color: var(--van-text-color-2);
}

.event-text {
  font-size: 15px;
  color: var(--van-text-color);
  line-height: 1.5;
  word-break: break-word;
  font-weight: 500;
}
</style>

<route lang="json5">
{
  name: 'MediaProgressTimeline'
}
</route>
