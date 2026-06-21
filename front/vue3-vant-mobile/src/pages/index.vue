<script setup lang="ts">
import { useUserStore } from '@/stores'
import { fetchTodos, updateTodo } from '@/api/modules/todos'
import { fetchEvents } from '@/api/modules/events'
import { createLifeLog } from '@/api/modules/lifelogs'
import type { TodoItem } from '@/api/modules/todos'
import type { EventItem } from '@/api/modules/events'
import { showNotify } from 'vant'

const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)
const displayName = computed(() => {
  const fn = userInfo.value.fullName
  const email = userInfo.value.email
  if (fn && fn !== email)
    return fn
  return null
})
const router = useRouter()

const todayTodos = ref<TodoItem[]>([])
const todayEvents = ref<EventItem[]>([])
const timelineMode = ref<'mine' | 'ours'>('mine')
const loading = ref(false)
const showQuickLog = ref(false)
const quickLogText = ref('')

const today = new Date()
const dateLabel = computed(() =>
  today.toLocaleDateString('zh-CN', { month: 'long', day: 'numeric', weekday: 'long' }),
)

function getTodayRange() {
  const start = new Date(today.getFullYear(), today.getMonth(), today.getDate())
  const end = new Date(start.getTime() + 86400000)
  return { start: start.toISOString(), end: end.toISOString() }
}

async function loadToday() {
  loading.value = true
  try {
    const { start, end } = getTodayRange()
    const [todosRes, eventsRes] = await Promise.all([
      fetchTodos({ is_completed: false }),
      fetchEvents({ start, end }),
    ])
    todayTodos.value = Array.isArray(todosRes) ? todosRes : []
    todayEvents.value = Array.isArray(eventsRes) ? eventsRes : []
  }
  finally {
    loading.value = false
  }
}

async function toggleTodoDone(todo: TodoItem) {
  await updateTodo(todo.id, { is_completed: !todo.is_completed })
  await loadToday()
}

async function submitQuickLog() {
  if (!quickLogText.value.trim())
    return
  await createLifeLog({ content: quickLogText.value, log_type: 'diet' })
  quickLogText.value = ''
  showQuickLog.value = false
  showNotify({ type: 'success', message: '已记录' })
  await loadToday()
}

function goTo(path: string) {
  router.push(path)
}

function statusColor(status: string) {
  return status === 'completed'
    ? '#7EC8A0'
    : status === 'missed'
      ? '#D97A6E'
      : status === 'cancelled'
        ? '#B8A99A'
        : '#E8905E'
}

function formatHour(iso: string) {
  return new Date(iso).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

onMounted(loadToday)
</script>

<template>
  <div class="timeline-page">
    <!-- Header -->
    <div class="timeline-header">
      <div class="date-row">
        <span class="date-label">{{ dateLabel }}</span>
        <div class="mode-switch">
          <van-button
            :type="timelineMode === 'mine' ? 'primary' : 'default'"
            size="mini" plain @click="timelineMode = 'mine'"
          >
            我的
          </van-button>
          <van-button
            :type="timelineMode === 'ours' ? 'primary' : 'default'"
            size="mini" plain @click="timelineMode = 'ours'"
          >
            我们的
          </van-button>
        </div>
      </div>
      <div class="greeting">
        {{ displayName || userInfo.email }}
      </div>
    </div>

    <!-- Timeline Axis -->
    <div class="timeline-axis">
      <div v-for="hour in 12" :key="hour" class="timeline-hour">
        <span class="hour-tag">{{ hour + 6 }}:00</span>
        <div class="hour-track">
          <!-- Event blocks (blue) -->
          <div
            v-for="ev in todayEvents.filter(e => {
              const h = new Date(e.start_time).getHours()
              return h >= hour + 6 && h < hour + 7
            })" :key="ev.id"
            class="event-block"
            :style="{ background: statusColor(ev.status), left: '40px', right: '16px' }"
          >
            <span class="event-title">{{ ev.title }}</span>
            <span class="event-time">{{ formatHour(ev.start_time) }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Today Todos -->
    <van-cell-group :title="`待办 (${todayTodos.length})`" :border="false" class="section">
      <template v-if="todayTodos.length === 0">
        <van-cell title="今天没有待办事项" />
      </template>
      <van-cell v-for="todo in todayTodos" :key="todo.id">
        <template #title>
          <span :class="{ 'line-through': todo.is_completed }">{{ todo.title }}</span>
        </template>
        <template #right-icon>
          <van-checkbox :model-value="todo.is_completed" @click="toggleTodoDone(todo)" />
        </template>
      </van-cell>
    </van-cell-group>

    <!-- Quick Add + Nav -->
    <div class="quick-actions">
      <van-button icon="edit" type="primary" size="small" @click="showQuickLog = true">
        记一笔
      </van-button>
      <van-button icon="plus" type="default" size="small" @click="goTo('/todos')">
        加待办
      </van-button>
    </div>

    <!-- Quick Log Dialog -->
    <van-dialog v-model:show="showQuickLog" title="记一笔" show-cancel-button @confirm="submitQuickLog">
      <van-field
        v-model="quickLogText"
        type="textarea"
        :autosize="{ minHeight: 60 }"
        placeholder="吃了什么？做了什么？心情如何？"
      />
    </van-dialog>
  </div>
</template>

<style scoped>
.timeline-page {
  padding-bottom: 16px;
}
.timeline-header {
  padding: 12px 16px 8px;
}
.date-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.date-label {
  font-size: 16px;
  font-weight: 600;
  color: var(--van-text-color);
}
.mode-switch {
  display: flex;
  gap: 4px;
}
.greeting {
  font-size: 13px;
  color: var(--van-text-color-2);
  margin-top: 4px;
}
.timeline-axis {
  padding: 0 16px;
}
.timeline-hour {
  display: flex;
  min-height: 44px;
  border-bottom: 1px solid #f0e6d8;
  position: relative;
}
.hour-tag {
  width: 36px;
  font-size: 11px;
  color: var(--van-text-color-3);
  padding-top: 2px;
  flex-shrink: 0;
}
.hour-track {
  flex: 1;
  position: relative;
}
.event-block {
  position: absolute;
  top: 2px;
  height: 28px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  padding: 0 8px;
  gap: 6px;
  z-index: 1;
  opacity: 0.9;
  cursor: pointer;
}
.event-title {
  font-size: 12px;
  font-weight: 500;
  color: #fff;
  white-space: nowrap;
  overflow: hidden;
}
.event-time {
  font-size: 10px;
  color: rgba(255, 255, 255, 0.8);
}
.section {
  margin-top: 8px;
}
.line-through {
  text-decoration: line-through;
  opacity: 0.5;
}
.quick-actions {
  display: flex;
  gap: 8px;
  padding: 12px 16px;
}
</style>

<route lang="json5">
{
  name: 'Today'
}
</route>
