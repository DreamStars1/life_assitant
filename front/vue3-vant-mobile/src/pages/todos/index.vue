<script setup lang="ts">
import { showConfirmDialog, showNotify } from 'vant'
import { fetchTodos, createTodo, updateTodo, deleteTodo } from '@/api/modules/todos'
import type { TodoItem } from '@/api/modules/todos'

const { t } = useI18n()

const todos = ref<TodoItem[]>([])
const loading = ref(false)
const showAdd = ref(false)
const editingTodo = ref<Partial<TodoItem>>({})
const filterCompleted = ref<boolean | undefined>(undefined)

async function loadTodos() {
  loading.value = true
  try {
    const res = await fetchTodos({ is_completed: filterCompleted.value })
    todos.value = Array.isArray(res) ? res : []
  } finally {
    loading.value = false
  }
}

async function onSave() {
  if (editingTodo.value.id) {
    await updateTodo(editingTodo.value.id, editingTodo.value)
  } else {
    await createTodo(editingTodo.value)
  }
  showAdd.value = false
  editingTodo.value = {}
  showNotify({ type: 'success', message: '已保存' })
  await loadTodos()
}

async function toggleComplete(todo: TodoItem) {
  await updateTodo(todo.id, { is_completed: !todo.is_completed })
  await loadTodos()
}

async function onDelete(id: string) {
  try {
    await showConfirmDialog({ title: t('todos.confirmDelete') })
    await deleteTodo(id)
    await loadTodos()
  } catch {}
}

function openNew() {
  editingTodo.value = { is_completed: false, priority: 'medium' }
  showAdd.value = true
}

const todayTodos = computed(() => todos.value.filter(t => !t.is_completed && t.due_date && new Date(t.due_date) <= new Date()))
const upcomingTodos = computed(() => todos.value.filter(t => !t.is_completed && (!t.due_date || new Date(t.due_date) > new Date())))
const doneTodos = computed(() => todos.value.filter(t => t.is_completed))

onMounted(loadTodos)
</script>

<template>
  <div class="todos-page">
    <van-sticky>
      <div class="header-bar">
        <span class="title">待办</span>
        <van-button icon="plus" type="primary" size="small" @click="openNew">新建</van-button>
      </div>
    </van-sticky>

    <van-pull-refresh v-model="loading" @refresh="loadTodos">
      <!-- Today -->
      <van-cell-group title="今天" :border="false">
        <van-cell v-for="todo in todayTodos" :key="todo.id">
          <template #title>
            <span>{{ todo.title }}</span>
            <van-tag v-if="todo.priority === 'urgent'" type="danger" :size="'mini' as any" class="tag">紧急</van-tag>
          </template>
          <template #right-icon>
            <van-checkbox :model-value="todo.is_completed" @click="toggleComplete(todo)" />
          </template>
        </van-cell>
        <van-cell v-if="todayTodos.length === 0" title="暂无待办" />
      </van-cell-group>

      <!-- Upcoming -->
      <van-cell-group title="即将" :border="false">
        <van-cell v-for="todo in upcomingTodos" :key="todo.id">
          <template #title>
            <span>{{ todo.title }}</span>
            <span v-if="todo.due_date" class="due">{{ new Date(todo.due_date).toLocaleDateString('zh-CN') }}</span>
          </template>
          <template #right-icon>
            <van-checkbox :model-value="todo.is_completed" @click="toggleComplete(todo)" />
          </template>
        </van-cell>
        <van-cell v-if="upcomingTodos.length === 0" title="暂无待办" />
      </van-cell-group>

      <!-- Done -->
      <van-cell-group title="已完成" :border="false">
        <van-cell v-for="todo in doneTodos" :key="todo.id">
          <template #title>
            <span class="done-text">{{ todo.title }}</span>
          </template>
          <template #right-icon>
            <van-icon name="delete" @click="onDelete(todo.id)" />
          </template>
        </van-cell>
      </van-cell-group>
    </van-pull-refresh>

    <!-- Add Dialog -->
    <van-dialog v-model:show="showAdd" title="新建待办" show-cancel-button @confirm="onSave">
      <van-form>
        <van-field v-model="editingTodo.title" label="标题" required />
        <van-field name="priority" label="优先级">
          <template #input>
            <van-radio-group v-model="editingTodo.priority" direction="horizontal">
              <van-radio name="low">低</van-radio>
              <van-radio name="medium">中</van-radio>
              <van-radio name="high">高</van-radio>
              <van-radio name="urgent">紧急</van-radio>
            </van-radio-group>
          </template>
        </van-field>
      </van-form>
    </van-dialog>
  </div>
</template>

<style scoped>
.todos-page { min-height: 100vh; }
.header-bar { display: flex; justify-content: space-between; align-items: center; padding: 8px 16px; background: var(--van-background-2); }
.title { font-size: 16px; font-weight: 600; }
.tag { margin-left: 6px; }
.done-text { text-decoration: line-through; opacity: 0.5; }
.due { margin-left: 8px; font-size: 12px; color: var(--van-gray-5); }
</style>

<route lang="json5">
{
  name: 'Todos'
}
</route>
