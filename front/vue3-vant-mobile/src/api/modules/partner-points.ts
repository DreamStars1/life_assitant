import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'

export interface PointsRecord {
  id: string
  createdBy: string
  pointsChange: number
  reason: string
  createdAt: string
  pendingRecordDate?: string | null
  pendingRequestedBy?: string | null
  pendingRequestedAt?: string | null
}

export interface PointsHistoryResult {
  records: PointsRecord[]
  total: number
  size: number
  current: number
  pages: number
  pendingConfirmCount: number
}

export type PointsDateChangeStatus = 'APPLIED' | 'PENDING'

export function getPointsBalance() {
  return request.get<ApiResponse<number>>('/partner/points')
}

export function getPointsHistory(page = 1, size = 20) {
  return request.get<ApiResponse<PointsHistoryResult>>('/partner/points/history', {
    params: { page, size },
  })
}

export function addPoints(pointsChange: number, reason: string, recordDate?: string) {
  return request.post<ApiResponse<void>>('/partner/points', {
    pointsChange,
    reason,
    ...(recordDate ? { recordDate } : {}),
  })
}

export function updatePointsRecordDate(id: string, recordDate: string) {
  return request.patch<ApiResponse<{ status: PointsDateChangeStatus }>>(`/partner/points/${id}`, { recordDate })
}

export function approvePointsDateChange(id: string) {
  return request.post<ApiResponse<void>>(`/partner/points/${id}/date-change/approve`)
}

export function rejectPointsDateChange(id: string) {
  return request.post<ApiResponse<void>>(`/partner/points/${id}/date-change/reject`)
}

export function approveAllPointsDateChanges() {
  return request.post<ApiResponse<{ approvedCount: number }>>('/partner/points/date-change/approve-all')
}
