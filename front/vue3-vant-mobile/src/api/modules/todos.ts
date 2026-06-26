import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'

export interface TodoItem {
  id: string
  userId: string
  title: string
  description?: string | null
  isCompleted: boolean
  priority: string
  category?: string | null
  dueDate?: string | null
  assignedTo?: string | null
  assignedBy?: string | null
  ackStatus: string
  ackMessage?: string | null
  completedAt?: string | null
  cancelledAt?: string | null
  createdAt: string
  updateTime?: string | null
}

export function fetchTodos(params?: { isCompleted?: boolean, priority?: string, startDueDate?: string, endDueDate?: string }) {
  return request.get<ApiResponse<TodoItem[]>>('/todos', { params })
}

export function fetchTodo(id: string) {
  return request.get<ApiResponse<TodoItem>>(`/todos/${id}`)
}

export function createTodo(data: { title: string, description?: string, priority: string, dueDate?: string, assignedTo?: string }) {
  return request.post<ApiResponse<TodoItem>>('/todos', data)
}

export function updateTodo(id: string, data: { title?: string, description?: string, priority?: string, dueDate?: string }) {
  return request.patch<ApiResponse<TodoItem>>(`/todos/${id}`, data)
}

export function deleteTodo(id: string) {
  return request.delete<ApiResponse<void>>(`/todos/${id}`)
}

export function toggleTodo(id: string) {
  return request.post<ApiResponse<TodoItem>>(`/todos/${id}/toggle`)
}

export function acknowledgeTodo(id: string, message: string) {
  return request.post<ApiResponse<TodoItem>>(`/todos/${id}/acknowledge`, { message })
}

export function fetchUpcomingTodos() {
  return request.get<ApiResponse<TodoItem[]>>('/todos/upcoming')
}
