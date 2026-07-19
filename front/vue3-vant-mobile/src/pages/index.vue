<script setup lang="ts">
import { onMounted } from 'vue'
import { showConfirmDialog, showNotify, showToast } from 'vant'
import { useRouter } from 'vue-router'
import { fetchTemplates } from '@/api/modules/ack-templates'
import { acknowledgeTodo } from '@/api/modules/todos'
import type { ScheduleEventItem } from '@/api/modules/schedule'
import {
  acknowledgeScheduleInvite,
  createScheduleEvent,
  deleteScheduleEvent,
  fetchMyScheduleEvents,
  fetchPartnerScheduleEvents,
  inviteScheduleEvent,
  updateScheduleEvent,
} from '@/api/modules/schedule'
import { useUserStore } from '@/stores'
import { useTodoStore } from '@/stores/modules/todo'
import DayTimeline from '@/components/schedule/DayTimeline.vue'
import ScheduleEventForm from '@/components/schedule/ScheduleEventForm.vue'

const { t } = useI18n()
const userStore = useUserStore()
const todoStore = useTodoStore()
const router = useRouter()

const primaryTab = ref(0)
const scheduleModeTab = ref(0)
const today = formatDate(new Date())
const mineEvents = ref<ScheduleEventItem[]>([])
const partnerEvents = ref<ScheduleEventItem[]>([])
const scheduleLoading = ref(false)
const showScheduleForm = ref(false)
const showScheduleEdit = ref(false)
const scheduleFormInitial = ref<Partial<ScheduleEventItem> | undefined>(undefined)
const editingEventId = ref('')
const formLoading = ref(false)
const showEventDetail = ref(false)
const selectedEvent = ref<ScheduleEventItem | null>(null)

const userInfo = computed(() => userStore.userInfo)
const partnerId = computed(() => userStore.userInfo.partnerId)
const scheduleMode = computed<'self' | 'dual'>(() => (scheduleModeTab.value === 0 ? 'self' : 'dual'))
const displayName = computed(() => {
  const fn = userInfo.value.fullName
  const email = userInfo.value.email
  if (fn && fn !== email)
    return fn
  return null
})
const dateLabel = computed(() =>
  new Date().toLocaleDateString('zh-CN', { month: 'long', day: 'numeric', weekday: 'long' }),
)
const isOwnEvent = computed(() => selectedEvent.value?.userId === userStore.userInfo.id)

function formatDate(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function toApiDateTime(iso: string): string {
  return iso.replace('T', ' ').slice(0, 19)
}

function formatDateTime(d: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:00`
}

function priorityColor(p: string): string {
  const map: Record<string, string> = { low: '#999', medium: '#1989fa', high: '#ff976a', urgent: '#ee0a24' }
  return map[p] || '#999'
}

function dateDiff(iso: string | null | undefined): number | null {
  if (!iso)
    return null
  const d = new Date(`${iso.slice(0, 10)}T00:00:00`)
  const now = new Date(new Date().getFullYear(), new Date().getMonth(), new Date().getDate())
  return Math.round((d.getTime() - now.getTime()) / 86400000)
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

async function loadSchedule() {
  scheduleLoading.value = true
  try {
    const range = { from: `${today} 00:00:00`, to: `${today} 23:59:59` }
    const mineRes = await fetchMyScheduleEvents(range)
    mineEvents.value = mineRes.data ?? []
    if (scheduleMode.value === 'dual' && partnerId.value) {
      const partnerRes = await fetchPartnerScheduleEvents(range)
      partnerEvents.value = partnerRes.data ?? []
    }
    else {
      partnerEvents.value = []
    }
  }
  catch {
    showToast('加载日程失败')
  }
  finally {
    scheduleLoading.value = false
  }
}

watch([primaryTab, scheduleModeTab], () => {
  if (primaryTab.value === 1)
    loadSchedule()
})

watch(partnerId, (id) => {
  if (!id && scheduleModeTab.value === 1)
    scheduleModeTab.value = 0
})

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
    const active = activeId ? templates.find(tpl => tpl.id === activeId) : undefined
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

function openCreate() {
  scheduleFormInitial.value = undefined
  editingEventId.value = ''
  showScheduleForm.value = true
}

function onCreateAt(iso: string) {
  const start = new Date(iso)
  const end = new Date(start)
  end.setHours(end.getHours() + 1)
  scheduleFormInitial.value = {
    startAt: formatDateTime(start),
    endAt: formatDateTime(end),
  }
  editingEventId.value = ''
  showScheduleForm.value = true
}

function onOpenEvent(event: ScheduleEventItem) {
  selectedEvent.value = event
  showEventDetail.value = true
}

function openScheduleEdit() {
  if (!selectedEvent.value)
    return
  editingEventId.value = selectedEvent.value.id
  scheduleFormInitial.value = { ...selectedEvent.value }
  showEventDetail.value = false
  showScheduleEdit.value = true
}

async function onScheduleSave(data: {
  title: string
  startAt: string
  endAt: string
  note?: string
  recurrence: 'none' | 'daily' | 'weekly'
  recurrenceEndDate?: string
}) {
  formLoading.value = true
  try {
    const payload = {
      ...data,
      startAt: toApiDateTime(data.startAt),
      endAt: toApiDateTime(data.endAt),
    }
    if (editingEventId.value) {
      await updateScheduleEvent(editingEventId.value, payload)
      showToast(t('schedule.updated'))
      showScheduleEdit.value = false
    }
    else {
      await createScheduleEvent(payload)
      showToast(t('schedule.created'))
      showScheduleForm.value = false
    }
    await loadSchedule()
  }
  catch {
    showToast('保存失败')
  }
  finally {
    formLoading.value = false
  }
}

async function onScheduleDelete() {
  if (!selectedEvent.value)
    return
  try {
    await showConfirmDialog({ title: t('schedule.delete'), message: t('schedule.deleteConfirm') })
    await deleteScheduleEvent(selectedEvent.value.id)
    showToast(t('schedule.deleted'))
    showEventDetail.value = false
    await loadSchedule()
  }
  catch { /* cancelled */ }
}

async function onScheduleInvite() {
  if (!selectedEvent.value)
    return
  try {
    await inviteScheduleEvent(selectedEvent.value.id)
    showToast(t('schedule.inviteSent'))
    showEventDetail.value = false
    await loadSchedule()
  }
  catch {
    showToast('邀约失败')
  }
}

async function onInviteAck(action: 'accept' | 'reject') {
  if (!selectedEvent.value?.pendingInviteId)
    return
  try {
    await acknowledgeScheduleInvite(selectedEvent.value.pendingInviteId, action)
    showToast(action === 'accept' ? t('schedule.accept') : t('schedule.reject'))
    showEventDetail.value = false
    await loadSchedule()
  }
  catch {
    showToast('操作失败')
  }
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

    <van-tabs v-model:active="primaryTab" class="primary-tabs">
      <van-tab :title="t('navbar.Todos')" />
      <van-tab :title="t('events.title')" />
    </van-tabs>

    <template v-if="primaryTab === 0">
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
    </template>

    <template v-else>
      <div class="schedule-controls">
        <van-tabs v-if="partnerId" v-model:active="scheduleModeTab" shrink>
          <van-tab :title="t('schedule.viewSelf')" />
          <van-tab :title="t('schedule.viewDual')" />
        </van-tabs>
        <span class="week-link" @click="router.push('/todos?view=schedule')">{{ t('schedule.viewWeek') }} →</span>
      </div>

      <van-loading v-if="scheduleLoading" class="schedule-loading" />
      <DayTimeline
        v-else
        :date="today"
        :mode="scheduleMode"
        :mine="mineEvents"
        :partner="partnerEvents"
        @create-at="onCreateAt"
        @open-event="onOpenEvent"
      />

      <div class="fab" @click="openCreate">
        <van-button round type="primary" icon="plus" />
      </div>
    </template>

    <van-action-sheet v-model:show="showScheduleForm" :title="t('events.add')" close-on-popup-safe>
      <ScheduleEventForm :key="`today-create-${showScheduleForm}`" :initial="scheduleFormInitial" :loading="formLoading" @save="onScheduleSave" />
    </van-action-sheet>

    <van-action-sheet v-model:show="showScheduleEdit" :title="t('events.edit')" close-on-popup-safe>
      <ScheduleEventForm v-if="scheduleFormInitial" :key="`today-edit-${editingEventId}`" :initial="scheduleFormInitial" :loading="formLoading" @save="onScheduleSave" />
    </van-action-sheet>

    <van-action-sheet v-model:show="showEventDetail" :title="selectedEvent?.title" close-on-popup-safe>
      <div v-if="selectedEvent" class="event-detail">
        <p class="event-detail-time">
          {{ selectedEvent.startAt }} — {{ selectedEvent.endAt }}
        </p>
        <p v-if="selectedEvent.note" class="event-detail-note">
          {{ selectedEvent.note }}
        </p>
        <p v-if="!isOwnEvent && !selectedEvent.pendingInviteId" class="event-detail-readonly">
          {{ t('schedule.partnerReadonly') }}
        </p>
        <div class="event-detail-actions">
          <template v-if="selectedEvent.pendingInviteId">
            <van-button block type="primary" @click="onInviteAck('accept')">
              {{ t('schedule.accept') }}
            </van-button>
            <van-button block plain type="danger" @click="onInviteAck('reject')">
              {{ t('schedule.reject') }}
            </van-button>
          </template>
          <template v-else-if="isOwnEvent">
            <van-button block type="primary" @click="openScheduleEdit">
              {{ t('events.edit') }}
            </van-button>
            <van-button v-if="partnerId" block plain type="primary" @click="onScheduleInvite">
              {{ t('schedule.invite') }}
            </van-button>
            <van-button block plain type="danger" @click="onScheduleDelete">
              {{ t('schedule.delete') }}
            </van-button>
          </template>
        </div>
      </div>
    </van-action-sheet>
  </div>
</template>

<style scoped>
.timeline-page {
  padding-bottom: 16px;
}
.timeline-header {
  padding: 12px 16px 8px;
}
.primary-tabs {
  margin-bottom: 8px;
}
.schedule-controls {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px 8px;
}
.schedule-controls .van-tabs {
  flex: 1;
}
.week-link {
  font-size: 12px;
  color: var(--van-blue);
  white-space: nowrap;
  cursor: pointer;
}
.schedule-loading {
  display: flex;
  justify-content: center;
  padding: 48px;
}
.event-detail {
  padding: 16px;
}
.event-detail-time {
  font-size: 14px;
  color: var(--van-text-color-2);
  margin-bottom: 8px;
}
.event-detail-note {
  font-size: 13px;
  color: var(--van-text-color-3);
  margin-bottom: 16px;
}
.event-detail-readonly {
  font-size: 12px;
  color: var(--van-text-color-3);
  margin-bottom: 16px;
}
.event-detail-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.fab {
  position: fixed;
  right: 20px;
  bottom: 70px;
  z-index: 100;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  border-radius: 50%;
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
