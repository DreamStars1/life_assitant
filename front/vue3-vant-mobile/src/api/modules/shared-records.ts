import request from '@/utils/request'
import type { ApiResponse, PageResult } from '@/types/api'

export interface SharedRecordItem {
  id: string
  createdBy: string
  title: string
  content?: string | null
  occurredAt: string
  createdAt: string
  updateTime?: string | null
}

export function fetchSharedRecords(params?: {
  page?: number
  size?: number
  keyword?: string
  start?: string
  end?: string
}) {
  return request.get<ApiResponse<PageResult<SharedRecordItem>>>('/shared-records', { params })
}

export function createSharedRecord(data: { title: string, content?: string, occurredAt?: string }) {
  return request.post<ApiResponse<SharedRecordItem>>('/shared-records', data)
}

export function updateSharedRecord(id: string, data: { title?: string, content?: string, occurredAt?: string }) {
  return request.patch<ApiResponse<SharedRecordItem>>(`/shared-records/${id}`, data)
}

export function deleteSharedRecord(id: string) {
  return request.delete<ApiResponse<void>>(`/shared-records/${id}`)
}
