import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'

export interface AckTemplate {
  id: string
  content: string
  sort_order: number
}

export function fetchTemplates() {
  return request.get<ApiResponse<AckTemplate[]>>('/ack-templates')
}

export function createTemplate(content: string) {
  return request.post<ApiResponse<AckTemplate>>('/ack-templates', { content })
}

export function updateTemplate(id: string, content: string) {
  return request.put<ApiResponse<AckTemplate>>(`/ack-templates/${id}`, { content })
}

export function deleteTemplate(id: string) {
  return request.delete<ApiResponse<void>>(`/ack-templates/${id}`)
}

export function reorderTemplates(ids: string[]) {
  return request.put<ApiResponse<void>>('/ack-templates/reorder', { ids })
}
