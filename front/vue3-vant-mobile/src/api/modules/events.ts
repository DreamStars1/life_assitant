import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'

export interface EventItem {
  id: string
  user_id: string
  title: string
  description?: string | null
  start_time: string
  end_time?: string | null
  status: string
  category?: string | null
  color?: string | null
  location?: string | null
  remind_before?: number | null
  repeat_rule?: object | null
  shared_with_partner: boolean
  created_at: string
}

export interface TimelineEntry {
  id: string
  user_id: string
  source_type: string
  source_id?: string | null
  content: string
  image_url?: string | null
  logged_at?: string | null
  created_at: string
}

export function fetchEvents(params?: { start?: string; end?: string; status?: string }) {
  return request.get<ApiResponse<EventItem[]>>('/events', { params })
}

export function fetchEvent(id: string) {
  return request.get<ApiResponse<EventItem>>(`/events/${id}`)
}

export function createEvent(data: Partial<EventItem>) {
  return request.post<ApiResponse<EventItem>>('/events', data)
}

export function updateEvent(id: string, data: Partial<EventItem>) {
  return request.patch<ApiResponse<EventItem>>(`/events/${id}`, data)
}

export function deleteEvent(id: string) {
  return request.delete<ApiResponse<void>>(`/events/${id}`)
}

export function fetchTimeline(params?: { start?: string; end?: string; source_type?: string }) {
  return request.get<ApiResponse<TimelineEntry[]>>('/timeline', { params })
}

export function createTimelineEntry(data: Partial<TimelineEntry>) {
  return request.post<ApiResponse<TimelineEntry>>('/timeline', data)
}

export function fetchPartnerEvents(params?: { start?: string; end?: string }) {
  return request.get<ApiResponse<EventItem[]>>('/partner/events', { params })
}
