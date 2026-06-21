import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'

export interface TodoItem {
  id: string
  user_id: string
  title: string
  description?: string | null
  is_completed: boolean
  priority: string
  category?: string | null
  due_date?: string | null
  assigned_to?: string | null
  assigned_by?: string | null
  completed_at?: string | null
  cancelled_at?: string | null
  created_at: string
}

export function fetchTodos(params?: { is_completed?: boolean; priority?: string; category?: string }) {
  return request.get<ApiResponse<TodoItem[]>>('/todos', { params })
}

export function fetchTodo(id: string) {
  return request.get<ApiResponse<TodoItem>>(`/todos/${id}`)
}

export function createTodo(data: Partial<TodoItem>) {
  return request.post<ApiResponse<TodoItem>>('/todos', data)
}

export function updateTodo(id: string, data: Partial<TodoItem>) {
  return request.patch<ApiResponse<TodoItem>>(`/todos/${id}`, data)
}

export function deleteTodo(id: string) {
  return request.delete<ApiResponse<void>>(`/todos/${id}`)
}

export function assignTodo(id: string, assigned_to_id: string) {
  return request.post<ApiResponse<TodoItem>>(`/todos/${id}/assign`, { assigned_to_id })
}

export function fetchPartnerTodos() {
  return request.get<ApiResponse<TodoItem[]>>('/partner/todos')
}
