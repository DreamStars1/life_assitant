import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'

export interface ScheduleEventItem {
  id: string
  userId: string
  title: string
  startAt: string
  endAt: string
  note?: string | null
  recurrence: 'none' | 'daily' | 'weekly'
  recurrenceEndDate?: string | null
  linkedEventId?: string | null
  instanceKey: string
  pendingInviteId?: string | null
  createdAt?: string
}

export function fetchMyScheduleEvents(params: { from: string, to: string }) {
  return request.get<ApiResponse<ScheduleEventItem[]>>('/schedule/events', { params })
}

export function fetchPartnerScheduleEvents(params: { from: string, to: string }) {
  return request.get<ApiResponse<ScheduleEventItem[]>>('/schedule/events/partner', { params })
}

export function createScheduleEvent(data: {
  title: string
  startAt: string
  endAt: string
  note?: string
  recurrence?: 'none' | 'daily' | 'weekly'
  recurrenceEndDate?: string
}) {
  return request.post<ApiResponse<ScheduleEventItem>>('/schedule/events', data)
}

export function updateScheduleEvent(id: string, data: {
  title?: string
  startAt?: string
  endAt?: string
  note?: string
  recurrence?: 'none' | 'daily' | 'weekly'
  recurrenceEndDate?: string
}) {
  return request.patch<ApiResponse<ScheduleEventItem>>(`/schedule/events/${id}`, data)
}

export function deleteScheduleEvent(id: string) {
  return request.delete<ApiResponse<void>>(`/schedule/events/${id}`)
}

export function inviteScheduleEvent(id: string) {
  return request.post<ApiResponse<void>>(`/schedule/events/${id}/invite`)
}

export function acknowledgeScheduleInvite(id: string, action: 'accept' | 'reject') {
  return request.post<ApiResponse<ScheduleEventItem>>(`/schedule/invites/${id}/acknowledge`, { action })
}
