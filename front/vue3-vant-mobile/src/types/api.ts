export interface FieldError {
  field: string
  message: string
}

export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data?: T
  errors?: FieldError[]
  traceId?: string
}
