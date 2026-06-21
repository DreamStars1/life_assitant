import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'

export interface LifeLogItem {
  id: string
  user_id: string
  log_type: string
  content: string
  quantity?: number | null
  unit?: string | null
  mood?: number | null
  tags?: string[] | null
  metadata_json?: object | null
  logged_at?: string | null
  created_at: string
}

export function fetchLifeLogs(params?: { log_type?: string, start?: string, end?: string }) {
  return request.get<ApiResponse<LifeLogItem[]>>('/life-logs', { params })
}

export function createLifeLog(data: Partial<LifeLogItem>) {
  return request.post<ApiResponse<LifeLogItem>>('/life-logs', data)
}

export function fetchLifeLogStats(period: string = 'weekly') {
  return request.get<ApiResponse<any>>('/life-logs/stats', { params: { period } })
}
