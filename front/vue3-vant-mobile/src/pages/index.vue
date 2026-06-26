<script setup lang="ts">
import { onMounted } from 'vue'
import { showNotify } from 'vant'
import { fetchTemplates } from '@/api/modules/ack-templates'
import { acknowledgeTodo } from '@/api/modules/todos'
import { useUserStore } from '@/stores'
import { useTodoStore } from '@/stores/modules/todo'
import { useRouter } from 'vue-router'

const userStore = useUserStore()
const todoStore = useTodoStore()
const router = useRouter()

const userInfo = computed(() => userStore.userInfo)
const displayName = computed(() => {
  const fn = userInfo.value.fullName
  const email = userInfo.value.email
  if (fn && fn !== email)
    return fn
  return null
})

const today = new Date()
const dateLabel = computed(() =>
  today.toLocaleDateString('zh-CN', { month: 'long', day: 'numeric', weekday: 'long' }),
)

function priorityColor(p: string): string {
  const map: Record<string, string> = { low: '#999', medium: '#1989fa', high: '#ff976a', urgent: '#ee0a24' }
  return map[p] || '#999'
}

function dateDiff(iso: string | null | undefined): number | null {
  if (!iso)
    return null
  const d = new Date(`${iso.slice(0, 10)}T00:00:00`)
  const today = new Date(new Date().getFullYear(), new Date().getMonth(), new Date().getDate())
  return Math.round((d.getTime() - today.getTime()) / 86400000)
}

function labelDate(iso: string | null | undefined): string {
  const diff = dateDiff(iso)
  if (diff == null)
    return ''
  if (diff < 0)
    return `已逾期 ${Math.abs(diff)} 天`
  if (diff === 0)
    return '今天'
  if (diff === 1)
    return '明天'
  if (diff === 2)
    return '后天'
  const d = new Date(`${iso!.slice(0, 10)}T00:00:00`)
  return `${d.getMonth() + 1}/${d.getDate()}`
}

function labelClass(iso: string | null | undefined): string {
  const diff = dateDiff(iso)
  if (diff == null)
    return 'label-none'
  if (diff < 0)
    return 'label-overdue'
  if (diff === 0)
    return 'label-today'
  return 'label-future'
}

function dotClass(iso: string | null | undefined): string {
  const diff = dateDiff(iso)
  if (diff == null)
    return 'dot-none'
  if (diff < 0)
    return 'dot-overdue'
  if (diff === 0)
    return 'dot-today'
  return 'dot-future'
}

onMounted(() => {
  loadUpcoming()
})

watch(() => userInfo.value.id, (id) => {
  if (id)
    loadUpcoming()
})

const ACTIVE_TEMPLATE_KEY = 'life_assistant_active_ack_template'

async function onAcknowledge(todo: any) {
  try {
    const res = await fetchTemplates()
    const templates = res.data ?? []
    const activeId = localStorage.getItem(ACTIVE_TEMPLATE_KEY)
    const active = activeId ? templates.find(t => t.id === activeId) : undefined
    const message = active?.content ?? templates[0]?.content ?? '收到'
    await acknowledgeTodo(todo.id, message)
    showNotify({ type: 'success', message: `已确认：${message}` })
    loadUpcoming()
  }
  catch { showNotify({ type: 'danger', message: '确认失败' }) }
}

function loadUpcoming() {
  if (userInfo.value.id)
    todoStore.loadUpcoming()
}
</script>

<template>
  <div class="timeline-page">
    <div class="timeline-header">
      <div class="date-row">
        <span class="date-label">{{ dateLabel }}</span>
      </div>
      <div class="greeting">
        {{ displayName || userInfo.email }}
      </div>
    </div>

    <div v-if="todoStore.upcoming.length > 0" class="tl">
      <div class="tl-title">
        <span>待办</span>
        <span class="tl-more" @click="router.push('/todos')">查看全部 →</span>
      </div>
      <div class="tl-list">
        <div v-for="todo in todoStore.upcoming.slice(0, 5)" :key="todo.id" class="tl-item">
          <div class="tl-line">
            <div class="tl-dot" :class="[dotClass(todo.dueDate)]" />
            <div class="tl-bar" />
          </div>
          <div class="tl-card">
            <div class="tl-card-top">
              <div v-if="todo.dueDate" class="tl-label-wrap">
                <span class="tl-label" :class="[labelClass(todo.dueDate)]">{{ labelDate(todo.dueDate) }}</span>
              </div>
              <span class="tl-prio" :style="{ background: priorityColor(todo.priority) }" />
            </div>
            <div class="tl-card-body">
              <div class="tl-check" @click="todoStore.toggleComplete(todo.id)">
                <div class="tl-ck" :class="[{ checked: todo.isCompleted }]">
                  <van-icon v-if="todo.isCompleted" name="success" />
                </div>
              </div>
              <span class="tl-text" :class="[{ done: todo.isCompleted }]">{{ todo.title }}</span>
            </div>
            <div class="tl-card-meta">
              <span v-if="todo.assignedTo" class="tl-meta-item">{{ todo.assignedTo === userInfo.id ? `← 来自 ${userStore.partnerName}` : `→ 交给 ${userStore.partnerName}` }}</span>
              <span v-if="todo.ackStatus === 'unconfirmed' && todo.assignedTo === userInfo.id" class="tl-meta-item warn clickable" @click.stop="onAcknowledge(todo)">确认收到</span>
              <span v-else-if="todo.ackStatus === 'unconfirmed'" class="tl-meta-item warn">待确认</span>
              <span v-if="todo.ackStatus === 'confirmed'" class="tl-meta-item ok">✓ {{ todo.ackMessage }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <van-empty v-else description="暂无内容" />
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
.greeting {
  font-size: 13px;
  color: var(--van-text-color-2);
  margin-top: 4px;
}

/* timeline */
.tl {
  margin: 8px 16px 0;
}
.tl-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 8px;
  padding: 0 4px;
}
.tl-more {
  font-size: 12px;
  color: var(--van-blue);
  font-weight: 400;
  cursor: pointer;
}
.tl-list {
  position: relative;
}
.tl-item {
  display: flex;
  gap: 10px;
  min-height: 64px;
}

/* 竖直时间线 */
.tl-line {
  width: 14px;
  display: flex;
  flex-direction: column;
  align-items: center;
  flex-shrink: 0;
  padding-top: 6px;
}
.tl-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
  z-index: 1;
}
.dot-none {
  background: var(--van-gray-4);
}
.dot-overdue {
  background: #ee0a24;
}
.dot-today {
  background: #1989fa;
}
.dot-future {
  background: var(--van-gray-5);
}
.tl-bar {
  width: 2px;
  flex: 1;
  background: var(--van-gray-2);
  margin-top: 4px;
}

/* 卡片 */
.tl-card {
  flex: 1;
  background: white;
  border-radius: 10px;
  padding: 10px 12px;
  margin-bottom: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}
.tl-card-top {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
}
.tl-label {
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 4px;
}
.label-none {
  color: var(--van-gray-5);
  background: var(--van-gray-1);
}
.label-overdue {
  color: white;
  background: #ee0a24;
}
.label-today {
  color: white;
  background: #1989fa;
}
.label-future {
  color: var(--van-gray-6);
  background: var(--van-gray-1);
}
.tl-prio {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.tl-card-body {
  display: flex;
  align-items: center;
  gap: 8px;
}
.tl-check {
  flex-shrink: 0;
  cursor: pointer;
}
.tl-ck {
  width: 20px;
  height: 20px;
  border: 2px solid var(--van-gray-4);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  color: white;
}
.tl-ck.checked {
  background: var(--van-green);
  border-color: var(--van-green);
}
.tl-text {
  font-size: 14px;
  font-weight: 500;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.tl-text.done {
  text-decoration: line-through;
  color: var(--van-gray-5);
}

.tl-card-meta {
  display: flex;
  gap: 10px;
  margin-top: 6px;
  font-size: 11px;
  flex-wrap: wrap;
}
.tl-meta-item {
  color: var(--van-gray-5);
}
.tl-meta-item.warn {
  color: var(--van-orange);
}
.tl-meta-item.warn.clickable {
  cursor: pointer;
  font-weight: 500;
}
.tl-meta-item.ok {
  color: var(--van-green);
}
</style>

<route lang="json5">
{
  name: 'Today'
}
</route>
