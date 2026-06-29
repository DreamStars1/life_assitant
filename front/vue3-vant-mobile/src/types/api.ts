export interface FieldError {
  field: string
  message: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
  pages: number
}

export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data?: T
  errors?: FieldError[]
  traceId?: string
}
