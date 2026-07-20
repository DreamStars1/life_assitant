import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'

export interface MediaCategory {
  id: string
  name: string
  createdAt: string
  updateTime: string
}

export function fetchCategories() {
  return request.get<ApiResponse<MediaCategory[]>>('/media-categories')
}

export function createCategory(name: string) {
  return request.post<ApiResponse<MediaCategory>>('/media-categories', { name })
}

export function updateCategory(id: string, name: string) {
  return request.patch<ApiResponse<MediaCategory>>(`/media-categories/${id}`, { name })
}

export function deleteCategory(id: string) {
  return request.delete<ApiResponse<void>>(`/media-categories/${id}`)
}
