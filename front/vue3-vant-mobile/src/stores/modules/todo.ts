import { defineStore } from 'pinia'
import { acknowledgeTodo, createTodo, deleteTodo, fetchTodos, fetchUpcomingTodos, toggleTodo, updateTodo } from '@/api/modules/todos'
import { fetchTemplates } from '@/api/modules/ack-templates'
import type { TodoItem } from '@/api/modules/todos'
import { showNotify } from 'vant'

export const useTodoStore = defineStore('todo', () => {
  const todos = ref<TodoItem[]>([])
  const upcoming = ref<TodoItem[]>([])
  const loading = ref(false)
  const currentPage = ref(1)
  const hasMore = ref(true)
  const pageSize = ref(5)
  const filter = ref<{ isCompleted?: boolean, startDueDate?: string, endDueDate?: string }>({})

  async function loadTodos(reset = false) {
    if (reset) {
      currentPage.value = 1
      todos.value = []
      hasMore.value = true
    }
    if (!hasMore.value || loading.value) return
    loading.value = true
    try {
      const params: Record<string, unknown> = { page: currentPage.value, size: pageSize.value }
      if (filter.value.isCompleted !== undefined)
        params.isCompleted = filter.value.isCompleted
      if (filter.value.startDueDate)
        params.startDueDate = filter.value.startDueDate
      if (filter.value.endDueDate)
        params.endDueDate = filter.value.endDueDate
      const res = await fetchTodos(params)
      const data = res.data ?? { records: [] as TodoItem[], pages: 0 }
      todos.value.push(...data.records)
      hasMore.value = currentPage.value < data.pages
      currentPage.value++
    } finally {
      loading.value = false
    }
  }

  async function loadUpcoming() {
    try {
      const res = await fetchUpcomingTodos()
      upcoming.value = res.data ?? []
    } catch {
      // silent
    }
  }

  async function create(data: { title: string, description?: string, priority: string, dueDate?: string, assignedTo?: string }) {
    const res = await createTodo(data)
    await loadTodos(true)
    return res.data
  }

  async function update(id: string, data: { title?: string, description?: string, priority?: string, dueDate?: string }) {
    const res = await updateTodo(id, data)
    await loadTodos(true)
    return res.data
  }

  async function remove(id: string) {
    await deleteTodo(id)
    await loadTodos(true)
  }

  async function toggleComplete(id: string) {
    const item = upcoming.value.find(t => t.id === id)
    if (item)
      item.isCompleted = !item.isCompleted
    await toggleTodo(id)
    loadUpcoming()
    loadTodos(true)
  }

  async function acknowledge(id: string) {
    const templatesRes = await fetchTemplates()
    const templates = templatesRes.data ?? []
    const message = templates.length > 0 ? templates[0]!.content : '收到'
    await acknowledgeTodo(id, message)
    showNotify({ type: 'success', message: '已确认收到' })
    await loadTodos(true)
  }

  async function setFilter(f: { isCompleted?: boolean, startDueDate?: string, endDueDate?: string }) {
    filter.value = f
    await loadTodos(true)
  }

  async function changePageSize(size: number) {
    pageSize.value = size
    await loadTodos(true)
  }

  return { todos, upcoming, loading, currentPage, hasMore, pageSize, loadTodos, loadUpcoming, create, update, remove, toggleComplete, acknowledge, setFilter, changePageSize }
})
