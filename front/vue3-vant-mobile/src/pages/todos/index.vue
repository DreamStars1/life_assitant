<script setup lang="ts">
import { showConfirmDialog, showNotify, showToast } from 'vant'
import { useRoute } from 'vue-router'
import { useTodoStore } from '@/stores/modules/todo'
import { useUserStore } from '@/stores'
import { fetchTemplates } from '@/api/modules/ack-templates'
import { acknowledgeTodo, updateTodo } from '@/api/modules/todos'
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
import TodoForm from '@/components/TodoForm.vue'
import DayTimeline from '@/components/schedule/DayTimeline.vue'
import WeekDualTrack from '@/components/schedule/WeekDualTrack.vue'
import ScheduleEventForm from '@/components/schedule/ScheduleEventForm.vue'

const { t } = useI18n()
const route = useRoute()
const todoStore = useTodoStore()
const userStore = useUserStore()

const PRIMARY_TAB_KEY = 'todos.primaryTab'

const primaryTab = ref(0)
const activeFilter = ref(0)
const showFilterCalendar = ref(false)
const dateRange = ref<[string, string]>(['', ''])
const showForm = ref(false)
const showEdit = ref(false)
const editId = ref('')
const expandedId = ref<string | null>(null)
const editInitial = ref<{ title: string, description?: string, priority: string, dueDate?: string } | undefined>(undefined)
const showPageSize = ref(false)

const scheduleRangeTab = ref(0)
const scheduleModeTab = ref(0)
const selectedDate = ref(formatDate(new Date()))
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
const showScheduleDateCalendar = ref(false)

const filters = ['全部', '进行中', '已完成']

const scheduleRange = computed(() => (scheduleRangeTab.value === 0 ? 'day' : 'week'))
const scheduleMode = computed<'self' | 'dual'>(() => (scheduleModeTab.value === 0 ? 'self' : 'dual'))
const weekStart = computed(() => getWeekStart(selectedDate.value))
const partnerId = computed(() => userStore.userInfo.partnerId)

function formatDate(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function getWeekStart(dateStr: string): string {
  const d = new Date(`${dateStr}T00:00:00`)
  const day = d.getDay()
  const diff = day === 0 ? -6 : 1 - day
  d.setDate(d.getDate() + diff)
  return formatDate(d)
}

function addDays(dateStr: string, days: number): string {
  const d = new Date(`${dateStr}T00:00:00`)
  d.setDate(d.getDate() + days)
  return formatDate(d)
}

function toApiDateTime(iso: string): string {
  return iso.replace(' ', 'T').slice(0, 19)
}

function formatDateTime(d: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:00`
}

function initPrimaryTab() {
  if (route.query.view === 'schedule') {
    primaryTab.value = 1
    localStorage.setItem(PRIMARY_TAB_KEY, 'schedule')
  }
  else {
    primaryTab.value = localStorage.getItem(PRIMARY_TAB_KEY) === 'schedule' ? 1 : 0
  }
}

watch(() => route.query.view, (view) => {
  if (view === 'schedule')
    primaryTab.value = 1
  else if (view === 'todos')
    primaryTab.value = 0
})

watch(primaryTab, (v) => {
  localStorage.setItem(PRIMARY_TAB_KEY, v === 1 ? 'schedule' : 'todos')
  if (v === 1)
    loadSchedule()
})

watch([scheduleRangeTab, scheduleModeTab, selectedDate], () => {
  if (primaryTab.value === 1)
    loadSchedule()
})

watch(partnerId, (id) => {
  if (!id && scheduleModeTab.value === 1)
    scheduleModeTab.value = 0
})

watch([activeFilter, dateRange], async ([v]) => {
  const params: { isCompleted?: boolean, startDueDate?: string, endDueDate?: string } = {}
  if (v === 1)
    params.isCompleted = false
  else if (v === 2)
    params.isCompleted = true
  if (dateRange.value[0])
    params.startDueDate = dateRange.value[0]
  if (dateRange.value[1])
    params.endDueDate = dateRange.value[1]
  await todoStore.setFilter(params)
}, { deep: true })

async function loadSchedule() {
  scheduleLoading.value = true
  try {
    const range = scheduleRange.value === 'week'
      ? { from: `${weekStart.value}T00:00:00`, to: `${addDays(weekStart.value, 6)}T23:59:59` }
      : { from: `${selectedDate.value}T00:00:00`, to: `${selectedDate.value}T23:59:59` }
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

function onSelectDateRange() {
  showFilterCalendar.value = true
}

function onDateConfirm(dates: [Date, Date]) {
  dateRange.value = [
    `${dates[0]!.getFullYear()}-${String(dates[0]!.getMonth() + 1).padStart(2, '0')}-${String(dates[0]!.getDate()).padStart(2, '0')}`,
    `${dates[1]!.getFullYear()}-${String(dates[1]!.getMonth() + 1).padStart(2, '0')}-${String(dates[1]!.getDate()).padStart(2, '0')}`,
  ]
  showFilterCalendar.value = false
}

function onScheduleDateConfirm(d: Date) {
  selectedDate.value = formatDate(d)
  showScheduleDateCalendar.value = false
}

function shiftDate(days: number) {
  selectedDate.value = addDays(selectedDate.value, days)
}

function shiftWeek(weeks: number) {
  selectedDate.value = addDays(selectedDate.value, weeks * 7)
}

function onWeekSelectDay(date: string) {
  selectedDate.value = date
  scheduleRangeTab.value = 0
}

async function onToggle(todo: { id: string }) {
  await todoStore.toggleComplete(todo.id)
}

async function onDelete(todo: { id: string, title: string }) {
  try {
    await showConfirmDialog({ title: '删除待办', message: `确定删除「${todo.title}」吗？` })
    await todoStore.remove(todo.id)
    showToast('已删除')
  }
  catch { /* cancelled */ }
}

function openCreate() {
  if (primaryTab.value === 1) {
    scheduleFormInitial.value = undefined
    editingEventId.value = ''
    showScheduleForm.value = true
    return
  }
  showForm.value = true
}

function onCreate(data: { title: string, description?: string, priority: string, dueDate?: string, assignedTo?: string }) {
  todoStore.create(data)
  showToast('已创建')
  showForm.value = false
}

function openEdit(todo: any) {
  editId.value = todo.id
  editInitial.value = { title: todo.title, description: todo.description || '', priority: todo.priority, dueDate: todo.dueDate || '' }
  showEdit.value = true
}

async function onEdit(data: { title: string, description?: string, priority: string, dueDate?: string, assignedTo?: string }) {
  try {
    await updateTodo(editId.value, data)
    showToast('已更新')
    showEdit.value = false
    await todoStore.loadTodos()
  }
  catch {
    showToast('更新失败')
  }
}

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
    await todoStore.loadTodos()
  }
  catch { showNotify({ type: 'danger', message: '确认失败' }) }
}

function formatDateLabel(iso: string | null | undefined): string {
  return iso ? iso.slice(0, 10) : ''
}

function priorityColor(p: string): string {
  const map: Record<string, string> = { low: '#999', medium: '#1989fa', high: '#ff976a', urgent: '#ee0a24' }
  return map[p] || '#999'
}

function toggleExpand(id: string) {
  expandedId.value = expandedId.value === id ? null : id
}

function onPageSizeChange(size: number) {
  showPageSize.value = false
  todoStore.changePageSize(size)
}

function onPrevPage() {
  if (todoStore.currentPage > 1)
    todoStore.goToPage(todoStore.currentPage - 1)
}

function onNextPage() {
  if (todoStore.currentPage < todoStore.totalPages)
    todoStore.goToPage(todoStore.currentPage + 1)
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

const isOwnEvent = computed(() => selectedEvent.value?.userId === userStore.userInfo.id)

initPrimaryTab()
todoStore.loadTodos()
if (primaryTab.value === 1)
  loadSchedule()
</script>

<template>
  <div class="todos-page">
    <van-tabs v-model:active="primaryTab" class="primary-tabs">
      <van-tab :title="t('navbar.Todos')" />
      <van-tab :title="t('events.title')" />
    </van-tabs>

    <template v-if="primaryTab === 0">
      <div class="filter-bar">
        <van-tabs v-model:active="activeFilter" shrink>
          <van-tab v-for="(f, i) in filters" :key="i" :title="f" />
        </van-tabs>
        <div class="date-filter" @click="onSelectDateRange">
          <van-icon name="calendar-o" />
          <span v-if="!dateRange[0]">日期</span>
          <span v-else>{{ dateRange[0] }} ~ {{ dateRange[1] }}</span>
        </div>
      </div>

      <div v-if="todoStore.todos.length === 0" class="empty-state">
        <van-icon name="todo-list-o" size="48" color="var(--van-gray-4)" />
        <p>暂无待办</p>
      </div>
      <div
        v-for="todo in todoStore.todos"
        :key="todo.id"
        class="todo-item" :class="[
          { 'todo-to-me': todo.assignedTo === userStore.userInfo.id, 'todo-to-partner': todo.assignedTo && todo.userId === userStore.userInfo.id && todo.assignedTo !== userStore.userInfo.id },
        ]"
      >
        <van-swipe-cell>
          <div class="todo-main">
            <div class="todo-row" @click="toggleExpand(todo.id)">
              <div class="todo-checkbox-wrap" @click.stop="onToggle(todo)">
                <div class="todo-checkbox" :class="[{ checked: todo.isCompleted }]">
                  <van-icon v-if="todo.isCompleted" name="success" />
                </div>
              </div>
              <span class="priority-dot" :style="{ background: priorityColor(todo.priority) }" />
              <div class="todo-body">
                <div class="todo-title-row">
                  <span class="todo-title" :class="[{ completed: todo.isCompleted }]">{{ todo.title }}</span>
                  <span v-if="todo.ackStatus === 'unconfirmed'" class="ack-badge ack-pending">待确认</span>
                  <span v-if="todo.ackStatus === 'confirmed'" class="ack-badge ack-done">✓ {{ todo.ackMessage }}</span>
                </div>
                <div class="todo-meta-row">
                  <span v-if="todo.dueDate" class="meta-item"><van-icon name="clock-o" /> {{ formatDateLabel(todo.dueDate) }}</span>
                  <span v-if="todo.assignedTo && todo.assignedTo === userStore.userInfo.id" class="meta-item">来自 {{ userStore.partnerName }}</span>
                  <span v-if="todo.assignedTo && todo.userId === userStore.userInfo.id && todo.assignedTo !== userStore.userInfo.id" class="meta-item">交给 {{ userStore.partnerName }}</span>
                </div>
              </div>
              <van-icon name="arrow" class="expand-arrow" :class="[{ expanded: expandedId === todo.id }]" />
            </div>

            <div v-if="expandedId === todo.id" class="todo-detail">
              <p v-if="todo.description" class="detail-desc">
                {{ todo.description }}
              </p>
              <div class="detail-meta">
                <span v-if="todo.assignedTo && todo.userId === userStore.userInfo.id && todo.assignedTo !== userStore.userInfo.id">交给 {{ userStore.partnerName }}</span>
                <span v-if="todo.assignedTo && todo.assignedTo === userStore.userInfo.id">来自 {{ userStore.partnerName }}</span>
                <span v-if="todo.ackStatus === 'confirmed' && todo.ackMessage">回复：{{ todo.ackMessage }}</span>
              </div>
              <div class="detail-actions">
                <van-button size="small" plain type="primary" @click="openEdit(todo)">
                  编辑
                </van-button>
                <van-button v-if="todo.ackStatus === 'unconfirmed' && todo.assignedTo === userStore.userInfo.id" size="small" type="primary" @click="onAcknowledge(todo)">
                  确认收到
                </van-button>
                <van-button size="small" plain type="danger" @click="onDelete(todo)">
                  删除
                </van-button>
              </div>
            </div>
          </div>
          <template #right>
            <van-button square type="danger" text="删除" @click="onDelete(todo)" />
          </template>
        </van-swipe-cell>
      </div>

      <div v-if="todoStore.totalPages > 0" class="pagination">
        <div class="pagination-inner">
          <van-button :disabled="todoStore.currentPage <= 1" size="small" plain @click="onPrevPage">
            上一页
          </van-button>
          <span class="page-info">第 {{ todoStore.currentPage }}/{{ todoStore.totalPages }} 页</span>
          <van-button :disabled="todoStore.currentPage >= todoStore.totalPages" size="small" plain @click="onNextPage">
            下一页
          </van-button>
          <span class="page-size-trigger" @click="showPageSize = true">每页 {{ todoStore.pageSize }} 条 <van-icon name="arrow-down" /></span>
        </div>
      </div>
    </template>

    <template v-else>
      <div class="schedule-controls">
        <van-tabs v-model:active="scheduleRangeTab" shrink>
          <van-tab :title="t('events.day')" />
          <van-tab :title="t('events.week')" />
        </van-tabs>
        <van-tabs v-if="partnerId" v-model:active="scheduleModeTab" shrink>
          <van-tab :title="t('schedule.viewSelf')" />
          <van-tab :title="t('schedule.viewDual')" />
        </van-tabs>
      </div>

      <div class="date-nav">
        <van-icon name="arrow-left" @click="scheduleRange === 'day' ? shiftDate(-1) : shiftWeek(-1)" />
        <span class="date-nav-label" @click="showScheduleDateCalendar = true">
          {{ scheduleRange === 'day' ? selectedDate : `${weekStart} ~ ${addDays(weekStart, 6)}` }}
        </span>
        <van-icon name="arrow" @click="scheduleRange === 'day' ? shiftDate(1) : shiftWeek(1)" />
      </div>

      <van-loading v-if="scheduleLoading" class="schedule-loading" />
      <WeekDualTrack
        v-else-if="scheduleRange === 'week'"
        :week-start="weekStart"
        :mode="scheduleMode"
        :mine="mineEvents"
        :partner="partnerEvents"
        @select-day="onWeekSelectDay"
        @open-event="onOpenEvent"
      />
      <DayTimeline
        v-else
        :date="selectedDate"
        :mode="scheduleMode"
        :mine="mineEvents"
        :partner="partnerEvents"
        @create-at="onCreateAt"
        @open-event="onOpenEvent"
      />
    </template>

    <div class="fab" @click="openCreate">
      <van-button round type="primary" icon="plus" />
    </div>

    <van-calendar v-model:show="showFilterCalendar" type="range" @confirm="onDateConfirm" />
    <van-calendar v-model:show="showScheduleDateCalendar" @confirm="onScheduleDateConfirm" />

    <van-action-sheet v-model:show="showForm" title="添加待办" close-on-popup-safe>
      <TodoForm :key="`create-${showForm}`" :show-assign="partnerId" @save="onCreate" />
    </van-action-sheet>

    <van-action-sheet v-model:show="showEdit" title="编辑待办" close-on-popup-safe>
      <TodoForm v-if="editInitial" :key="`edit-${editId}`" :initial="editInitial" @save="onEdit" />
    </van-action-sheet>

    <van-action-sheet v-model:show="showPageSize" title="每页显示">
      <van-cell v-for="s in [3, 5, 10]" :key="s" :title="`${s} 条`" :label="s === 5 ? '推荐' : ''" is-link @click="onPageSizeChange(s)" />
    </van-action-sheet>

    <van-action-sheet v-model:show="showScheduleForm" :title="t('events.add')" close-on-popup-safe>
      <ScheduleEventForm :key="`schedule-create-${showScheduleForm}`" :initial="scheduleFormInitial" :loading="formLoading" @save="onScheduleSave" />
    </van-action-sheet>

    <van-action-sheet v-model:show="showScheduleEdit" :title="t('events.edit')" close-on-popup-safe>
      <ScheduleEventForm v-if="scheduleFormInitial" :key="`schedule-edit-${editingEventId}`" :initial="scheduleFormInitial" :loading="formLoading" @save="onScheduleSave" />
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
.todos-page {
  min-height: 100vh;
}
.primary-tabs {
  background: white;
}
.filter-bar {
  display: flex;
  align-items: center;
  padding: 0 12px;
  background: white;
}
.filter-bar .van-tabs {
  flex: 1;
}
.schedule-controls {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0 8px;
  padding: 0 12px;
  background: white;
}
.schedule-controls .van-tabs {
  flex: 1;
  min-width: 120px;
}
.date-nav {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 10px 16px;
  background: white;
  border-bottom: 1px solid var(--van-gray-2);
}
.date-nav .van-icon {
  font-size: 18px;
  color: var(--van-blue);
  cursor: pointer;
}
.date-nav-label {
  font-size: 14px;
  font-weight: 500;
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
.date-filter {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--van-gray-6);
  padding: 8px 12px;
  white-space: nowrap;
  cursor: pointer;
}
.todo-item {
  background: white;
  margin: 1px 0;
}
.todo-to-me {
  border-left: 4px solid #1989fa;
}
.todo-to-partner {
  border-left: 4px solid #ff976a;
}
.todo-main {
  padding: 0 16px;
}
.todo-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 0;
  cursor: pointer;
  min-height: 44px;
}
.todo-checkbox-wrap {
  flex-shrink: 0;
}
.todo-checkbox {
  width: 20px;
  height: 20px;
  border: 2px solid var(--van-gray-4);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}
.todo-checkbox.checked {
  background: var(--van-green);
  border-color: var(--van-green);
  color: white;
}
.priority-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
  margin-top: 5px;
  align-self: flex-start;
}
.todo-body {
  flex: 1;
  min-width: 0;
}
.todo-title-row {
  display: flex;
  align-items: center;
  gap: 6px;
}
.todo-title {
  font-size: 15px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.todo-title.completed {
  text-decoration: line-through;
  color: var(--van-gray-5);
}
.ack-badge {
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 4px;
  flex-shrink: 0;
  white-space: nowrap;
}
.ack-pending {
  background: #fff3e0;
  color: #e65100;
}
.ack-done {
  background: #e8f5e9;
  color: #2e7d32;
}
.todo-meta-row {
  display: flex;
  gap: 10px;
  margin-top: 3px;
  font-size: 12px;
  color: var(--van-gray-5);
  flex-wrap: wrap;
}
.meta-item {
  display: flex;
  align-items: center;
  gap: 2px;
}
.meta-item .van-icon {
  font-size: 11px;
}
.expand-arrow {
  font-size: 14px;
  color: var(--van-gray-5);
  transition: transform 0.2s;
}
.expand-arrow.expanded {
  transform: rotate(90deg);
}
.todo-detail {
  padding: 0 0 12px 38px;
  border-bottom: 1px solid var(--van-gray-2);
}
.detail-desc {
  font-size: 13px;
  color: var(--van-gray-6);
  line-height: 1.5;
  margin-bottom: 8px;
}
.detail-meta {
  font-size: 12px;
  color: var(--van-gray-5);
  margin-bottom: 10px;
}
.detail-actions {
  display: flex;
  gap: 10px;
}
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
  gap: 12px;
  color: var(--van-gray-5);
  font-size: 14px;
}
.pagination {
  display: flex;
  justify-content: center;
  padding: 16px;
}
.pagination-inner {
  display: flex;
  align-items: center;
  gap: 8px;
}
.page-info {
  font-size: 13px;
  color: var(--van-gray-6);
  white-space: nowrap;
}
.page-size-trigger {
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: 12px;
  color: var(--van-blue);
  cursor: pointer;
}
.fab {
  position: fixed;
  right: 20px;
  bottom: 70px;
  z-index: 100;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  border-radius: 50%;
}
</style>

<route lang="json5">
{
  name: 'Todos'
}
</route>
