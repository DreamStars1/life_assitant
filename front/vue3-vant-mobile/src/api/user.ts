import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'

export interface LoginData {
  email: string
  password: string
}

export interface LoginRes {
  accessToken?: string
  access_token?: string
  tokenType?: string
  token_type?: string
  refreshToken?: string
  refresh_token?: string
}

export interface UserState {
  id?: string
  fullName?: string
  full_name?: string
  email?: string
  partnerId?: string | null
  partner_id?: string | null
  isActive?: boolean
  is_active?: boolean
  isSuperuser?: boolean
  is_superuser?: boolean
  createdAt?: string
  created_at?: string
}

export function login(data: LoginData) {
  return request.post<ApiResponse<LoginRes>>('/auth/login', {
    email: data.email,
    password: data.password,
  })
}

export function getUserInfo() {
  return request.get<ApiResponse<UserState>>('/users/me')
}

export function register(data: { email: string, password: string, fullName?: string }) {
  return request.post<ApiResponse<UserState>>('/users/signup', {
    email: data.email,
    password: data.password,
    full_name: data.fullName,
  })
}
