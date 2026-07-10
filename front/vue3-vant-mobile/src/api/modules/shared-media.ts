import request from '@/utils/request'
import type { ApiResponse, PageResult } from '@/types/api'

export interface SharedMediaItem {
  id: string
  createdBy: string
  title: string
  mediaType: string
  coverPath: string | null
  description: string | null
  isFinished: boolean
  finishedAt: string | null
  createdAt: string
  updateTime: string
}

export interface MediaComment {
  id: string
  mediaId: string
  userId: string
  content: string
  createdAt: string
}

export interface MediaProgress {
  id: string
  mediaId: string
  userId: string | null
  scope: 'shared' | 'personal'
  progressText: string
  createdAt: string
}

export function fetchSharedMediaList(params?: {
  page?: number
  size?: number
  mediaType?: string
  status?: string
}) {
  return request.get<ApiResponse<PageResult<SharedMediaItem>>>('/shared-media', { params })
}

export function getSharedMediaDetail(id: string) {
  return request.get<ApiResponse<SharedMediaItem>>(`/shared-media/${id}`)
}

export function createSharedMedia(formData: FormData) {
  return request.post<ApiResponse<SharedMediaItem>>('/shared-media', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function updateSharedMedia(id: string, formData: FormData) {
  return request.patch<ApiResponse<SharedMediaItem>>(`/shared-media/${id}`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function deleteSharedMedia(id: string) {
  return request.delete<ApiResponse<void>>(`/shared-media/${id}`)
}

export function fetchComments(mediaId: string) {
  return request.get<ApiResponse<MediaComment[]>>(`/shared-media/${mediaId}/comments`)
}

export function createComment(mediaId: string, data: { content: string }) {
  return request.post<ApiResponse<MediaComment>>(`/shared-media/${mediaId}/comments`, data)
}

export function fetchProgress(mediaId: string) {
  return request.get<ApiResponse<MediaProgress[]>>(`/shared-media/${mediaId}/progress`)
}

export function updateProgress(mediaId: string, data: { scope: 'shared' | 'personal'; progressText: string }) {
  return request.put<ApiResponse<MediaProgress>>(`/shared-media/${mediaId}/progress`, data)
}
