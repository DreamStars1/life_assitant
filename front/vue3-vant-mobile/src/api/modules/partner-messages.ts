import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'

export interface PartnerMessage {
  id: string
  createdBy: string
  content?: string | null
  imageUrls: string[]
  createdAt: string
  sharedRecordId?: string | null
  todoId?: string | null
  pointsId?: string | null
}

export interface PartnerMessageCreateBody {
  content?: string
  imageUrls?: string[]
  publishSharedRecord?: boolean
  sharedRecordTitle?: string
  sharedRecordContent?: string
  publishTodo?: boolean
  todoAssignedTo?: 'self' | 'partner' | 'none'
  publishPoints?: boolean
  pointsChange?: number
  pointsReason?: string
}

export function fetchPartnerMessages(params?: { page?: number, size?: number }) {
  return request.get<ApiResponse<PartnerMessage[]>>('/partner/messages', { params })
}

export function uploadPartnerMessageImages(files: File[]) {
  const formData = new FormData()
  for (const file of files)
    formData.append('files', file)
  return request.post<ApiResponse<{ urls: string[] }>>('/partner/messages/images', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function createPartnerMessage(data: PartnerMessageCreateBody) {
  return request.post<ApiResponse<PartnerMessage>>('/partner/messages', data)
}

export function deletePartnerMessage(id: string) {
  return request.delete<ApiResponse<void>>(`/partner/messages/${id}`)
}
