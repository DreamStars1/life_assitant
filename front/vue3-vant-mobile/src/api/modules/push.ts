import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'

export interface PushSubscription {
  id: string
  user_id: string
  endpoint: string
  p256dh: string
  auth: string
  is_active: boolean
}

export function subscribePush(data: { endpoint: string, p256dh: string, auth: string }) {
  return request.post<ApiResponse<PushSubscription>>('/push/subscribe', data)
}

export function unsubscribePush(data: { endpoint: string }) {
  return request.delete<ApiResponse<void>>('/push/subscription', { data })
}

export function updatePushPreferences(data: {
  push_enabled?: boolean
  quiet_hours_start?: string
  quiet_hours_end?: string
}) {
  return request.put<ApiResponse<void>>('/push/preferences', data)
}

export function testPush(data?: { title?: string, body?: string }) {
  return request.post<ApiResponse<void>>('/push/test', data || { title: 'Test', body: 'Hello!' })
}
