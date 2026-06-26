import request from '@/utils/request'

export interface ApiToken {
  id: string
  name: string
  tokenPrefix: string
  fullToken?: string
  lastUsedAt: string | null
  expiresAt: string | null
  isActive: boolean
  createdAt: string
}

export interface ApiTokenCreateReq {
  name: string
  expiresAt?: string
}

/** 创建 API Token */
export function createApiToken(data: ApiTokenCreateReq) {
  return request.post<ApiToken>('/api-tokens', data)
}

/** 获取 API Token 列表 */
export function fetchApiTokens() {
  return request.get<ApiToken[]>('/api-tokens')
}

/** 撤销 API Token */
export function deleteApiToken(id: string) {
  return request.delete(`/api-tokens/${id}`)
}
