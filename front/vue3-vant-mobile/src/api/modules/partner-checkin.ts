import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'

export interface CheckinRecord {
  id: string
  userId: string
  checkinType: 'wake' | 'sleep'
  checkinTime: string | number[]
  checkinDate: string | number[]
}

export function doCheckin(checkinType: 'wake' | 'sleep') {
  return request.post<ApiResponse<void>>('/partner/checkin', { checkinType })
}

export function getTodayCheckin() {
  return request.get<ApiResponse<CheckinRecord[]>>('/partner/checkin/today')
}

export function getWeeklyCheckin() {
  return request.get<ApiResponse<CheckinRecord[]>>('/partner/checkin/weekly')
}
