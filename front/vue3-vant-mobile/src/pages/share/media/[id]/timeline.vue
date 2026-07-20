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

    <div v-else class="event-list">
      <div
        v-for="event in events"
        :key="event.id"
        class="event-item"
      >
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

.event-list {
  padding: 12px 16px;
}

.event-item {
  padding: 12px 0;
  border-bottom: 1px solid var(--van-border-color);
}

.event-item:last-child {
  border-bottom: none;
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
}
</style>

<route lang="json5">
{
  name: 'MediaProgressTimeline'
}
</route>
