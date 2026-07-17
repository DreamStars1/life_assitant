import request from '@/utils/request'
import type { ApiResponse, PageResult } from '@/types/api'

export interface PointsRecord {
  id: string
  createdBy: string
  pointsChange: number
  reason: string
  createdAt: string
}

export function getPointsBalance() {
  return request.get<ApiResponse<number>>('/partner/points')
}

export function getPointsHistory(page = 1, size = 20) {
  return request.get<ApiResponse<PageResult<PointsRecord>>>('/partner/points/history', {
    params: { page, size },
  })
}

export function addPoints(pointsChange: number, reason: string) {
  return request.post<ApiResponse<void>>('/partner/points', { pointsChange, reason })
}
