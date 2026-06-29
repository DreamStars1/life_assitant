<script setup lang="ts">
import { showConfirmDialog, showNotify, showToast } from 'vant'
import { useTodoStore } from '@/stores/modules/todo'
import { useUserStore } from '@/stores'
import { fetchTemplates } from '@/api/modules/ack-templates'
import { acknowledgeTodo, updateTodo } from '@/api/modules/todos'
import TodoForm from '@/components/TodoForm.vue'

const todoStore = useTodoStore()
const userStore = useUserStore()

const activeFilter = ref(0)
const showFilterCalendar = ref(false)
const dateRange = ref<[string, string]>(['', ''])
const showForm = ref(false)
const showEdit = ref(false)
const editId = ref('')
const expandedId = ref<string | null>(null)
const editInitial = ref<{ title: string, description?: string, priority: string, dueDate?: string } | undefined>(undefined)

const listLoading = ref(true)
const showPageSize = ref(false)

const filters = ['全部', '进行中', '已完成']

watch(activeFilter, async (v) => {
  listLoading.value = true
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
  listLoading.value = false
})

function onSelectDateRange() { showFilterCalendar.value = true }

function onDateConfirm(dates: [Date, Date]) {
  dateRange.value = [
    `${dates[0]!.getFullYear()}-${String(dates[0]!.getMonth() + 1).padStart(2, '0')}-${String(dates[0]!.getDate()).padStart(2, '0')}`,
    `${dates[1]!.getFullYear()}-${String(dates[1]!.getMonth() + 1).padStart(2, '0')}-${String(dates[1]!.getDate()).padStart(2, '0')}`,
  ]
  showFilterCalendar.value = false
  activeFilter.value = activeFilter.value
}

async function onToggle(todo: { id: string }) { await todoStore.toggleComplete(todo.id) }

async function onDelete(todo: { id: string, title: string }) {
  try {
    await showConfirmDialog({ title: '删除待办', message: `确定删除「${todo.title}」吗？` })
    await todoStore.remove(todo.id)
    showToast('已删除')
  }
  catch { /* cancelled */ }
}

function openCreate() {
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
  listLoading.value = true
  try {
    await updateTodo(editId.value, data)
    showToast('已更新')
    showEdit.value = false
    await todoStore.loadTodos(true)
  } catch {
    showToast('更新失败')
  } finally {
    listLoading.value = false
  }
}

const ACTIVE_TEMPLATE_KEY = 'life_assistant_active_ack_template'

async function onAcknowledge(todo: any) {
  listLoading.value = true
  try {
    const res = await fetchTemplates()
    const templates = res.data ?? []
    const activeId = localStorage.getItem(ACTIVE_TEMPLATE_KEY)
    const active = activeId ? templates.find(t => t.id === activeId) : undefined
    const message = active?.content ?? templates[0]?.content ?? '收到'
    await acknowledgeTodo(todo.id, message)
    showNotify({ type: 'success', message: `已确认：${message}` })
    await todoStore.loadTodos(true)
  }
  catch { showNotify({ type: 'danger', message: '确认失败' }) }
  finally { listLoading.value = false }
}

function formatDate(iso: string | null | undefined): string { return iso ? iso.slice(0, 10) : '' }

function priorityColor(p: string): string {
  const map: Record<string, string> = { low: '#999', medium: '#1989fa', high: '#ff976a', urgent: '#ee0a24' }
  return map[p] || '#999'
}

const partnerId = computed(() => userStore.userInfo.partnerId)

function toggleExpand(id: string) { expandedId.value = expandedId.value === id ? null : id }

async function onRefresh() {
  listLoading.value = true
  await todoStore.loadTodos(true)
  listLoading.value = false
}

async function onLoadMore() {
  listLoading.value = true
  await todoStore.loadTodos(false)
  listLoading.value = false
}

function onPageSizeChange(size: number) {
  showPageSize.value = false
  todoStore.changePageSize(size)
}

async function loadInitial() {
  await todoStore.loadTodos(true)
  listLoading.value = false
}
loadInitial()
</script>

<template>
  <div class="todos-page">
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

    <van-pull-refresh v-model="todoStore.loading" @refresh="onRefresh">
      <van-list
        v-model:loading="listLoading"
        :finished="!todoStore.hasMore"
        @load="onLoadMore"
      >
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
                    <span v-if="todo.dueDate" class="meta-item"><van-icon name="clock-o" /> {{ formatDate(todo.dueDate) }}</span>
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

        <template #finished>
          <div class="list-footer">
            <span>没有更多了</span>
            <span class="page-size-trigger" @click="showPageSize = true">每页 {{ todoStore.pageSize }} 条 <van-icon name="arrow-down" /></span>
          </div>
        </template>
      </van-list>
    </van-pull-refresh>

    <div class="fab" @click="openCreate">
      <van-button round type="primary" icon="plus" />
    </div>

    <van-calendar v-model:show="showFilterCalendar" type="range" @confirm="onDateConfirm" />

    <van-action-sheet v-model:show="showForm" title="添加待办" close-on-popup-safe>
      <TodoForm :key="`create-${showForm}`" :show-assign="partnerId" @save="onCreate" />
    </van-action-sheet>

    <van-action-sheet v-model:show="showEdit" title="编辑待办" close-on-popup-safe>
      <TodoForm v-if="editInitial" :key="`edit-${editId}`" :initial="editInitial" @save="onEdit" />
    </van-action-sheet>

    <van-action-sheet v-model:show="showPageSize" title="每页显示">
      <van-cell v-for="s in [3, 5, 10]" :key="s" :title="`${s} 条`" :label="s === 5 ? '推荐' : ''" is-link @click="onPageSizeChange(s)" />
    </van-action-sheet>
  </div>
</template>

<style scoped>
.todos-page {
  min-height: 100vh;
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
.fab {
  position: fixed;
  right: 20px;
  bottom: 70px;
  z-index: 100;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  border-radius: 50%;
}
.page-size-trigger {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 12px;
  font-size: 12px;
  color: var(--van-gray-5);
  cursor: pointer;
}
.list-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px;
  font-size: 12px;
  color: var(--van-gray-5);
}
.list-footer .page-size-trigger {
  padding: 0;
  color: var(--van-blue);
}
</style>

<route lang="json5">
{
  name: 'Todos'
}
</route>
