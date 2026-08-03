import request from '@/utils/request'
import type { ApiResponse, PageResult } from '@/types/api'

export interface HealthProfile {
  displayName?: string
  motto?: string
  heightCm?: number | null
  targetKg?: number | null
  restingKcal?: number | null
}

export interface HealthMeal {
  id: string
  date: string
  mealType: '早餐' | '午餐' | '晚餐'
  food: string
  proteinG?: number | null
  feedback?: string | null
}

export interface HealthDaily {
  date: string
  stomachStatus?: string | null
  stomachNote?: string | null
  cyclePhase?: string | null
  cycleDay?: number | null
}

export interface HealthWeight {
  id: string
  date: string
  weightType: '晨重' | '晚重'
  kg: number
  standard: boolean
}

export interface HealthSummary {
  latestMorningKg?: number | null
  avg7MorningKg?: number | null
  targetKg?: number | null
  gapToTargetKg?: number | null
  avg30ProteinG?: number | null
}

export interface HealthMemory {
  id: string
  title: string
  detail?: string | null
}

export interface HealthTolerance {
  id: string
  name: string
  level: string
}

export interface HealthTrigger {
  id: string
  name: string
  note?: string | null
  stars: number
}

export function getHealthProfile() {
  return request.get<ApiResponse<HealthProfile>>('/health/profile')
}

export function putHealthProfile(body: HealthProfile) {
  return request.put<ApiResponse<HealthProfile>>('/health/profile', body)
}

export function listHealthMeals(date: string) {
  return request.get<ApiResponse<HealthMeal[]>>('/health/meals', { params: { date } })
}

export function upsertHealthMeal(body: Omit<HealthMeal, 'id'> & { id?: string }) {
  return request.put<ApiResponse<HealthMeal>>('/health/meals', body)
}

export function deleteHealthMeal(id: string) {
  return request.delete<ApiResponse<void>>(`/health/meals/${id}`)
}

export function getHealthDaily(date: string) {
  return request.get<ApiResponse<HealthDaily>>('/health/daily', { params: { date } })
}

export function putHealthDaily(body: HealthDaily) {
  return request.put<ApiResponse<HealthDaily>>('/health/daily', body)
}

export function listHealthWeights(params?: { from?: string, to?: string, type?: string }) {
  return request.get<ApiResponse<HealthWeight[]>>('/health/weights', { params })
}

export function createHealthWeight(body: { date: string, weightType: string, kg: number, standard?: boolean }) {
  return request.post<ApiResponse<HealthWeight>>('/health/weights', body)
}

export function deleteHealthWeight(id: string) {
  return request.delete<ApiResponse<void>>(`/health/weights/${id}`)
}

export function getHealthSummary() {
  return request.get<ApiResponse<HealthSummary>>('/health/summary')
}

export function listHealthMemories(page = 1, pageSize = 5) {
  return request.get<ApiResponse<PageResult<HealthMemory>>>('/health/memories', { params: { page, pageSize } })
}

export function createHealthMemory(body: { title: string, detail?: string }) {
  return request.post<ApiResponse<HealthMemory>>('/health/memories', body)
}

export function patchHealthMemory(id: string, body: { title?: string, detail?: string }) {
  return request.patch<ApiResponse<HealthMemory>>(`/health/memories/${id}`, body)
}

export function deleteHealthMemory(id: string) {
  return request.delete<ApiResponse<void>>(`/health/memories/${id}`)
}

export function listHealthTolerances(page = 1, pageSize = 4) {
  return request.get<ApiResponse<PageResult<HealthTolerance>>>('/health/tolerances', { params: { page, pageSize } })
}

export function createHealthTolerance(body: { name: string, level: string }) {
  return request.post<ApiResponse<HealthTolerance>>('/health/tolerances', body)
}

export function patchHealthTolerance(id: string, body: { name?: string, level?: string }) {
  return request.patch<ApiResponse<HealthTolerance>>(`/health/tolerances/${id}`, body)
}

export function deleteHealthTolerance(id: string) {
  return request.delete<ApiResponse<void>>(`/health/tolerances/${id}`)
}

export function listHealthTriggers(page = 1, pageSize = 3) {
  return request.get<ApiResponse<PageResult<HealthTrigger>>>('/health/triggers', { params: { page, pageSize } })
}

export function createHealthTrigger(body: { name: string, note?: string, stars: number }) {
  return request.post<ApiResponse<HealthTrigger>>('/health/triggers', body)
}

export function patchHealthTrigger(id: string, body: { name?: string, note?: string, stars?: number }) {
  return request.patch<ApiResponse<HealthTrigger>>(`/health/triggers/${id}`, body)
}

export function deleteHealthTrigger(id: string) {
  return request.delete<ApiResponse<void>>(`/health/triggers/${id}`)
}
