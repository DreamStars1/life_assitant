import type { AxiosError, AxiosInstance, AxiosRequestConfig, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import axios from 'axios'
import { showDialog, showNotify } from 'vant'
import type { FieldError } from '@/types/api'

const TOKEN_KEY = 'access_token'

export const REQUEST_TOKEN_KEY = 'Authorization'

const request = axios.create({
  baseURL: import.meta.env.VITE_APP_API_BASE_URL,
  timeout: 15000,
})

export type RequestError = AxiosError<{
  code?: number
  message?: string
  result?: any
  errorMessage?: string
  detail?: string
  traceId?: string
  errors?: FieldError[]
}>

function errorHandler(error: RequestError): Promise<any> {
  if (error.response) {
    const { data = {}, status } = error.response
    if (status === 401 || status === 403) {
      const msg = (data as any)?.message || (data as any)?.detail || '未认证'
      localStorage.removeItem(TOKEN_KEY)

      if (msg.includes('账号已在其他设备登录')) {
        showDialog({ title: '登录失效', message: msg }).then(() => {
          window.location.href = `${import.meta.env.VITE_APP_PUBLIC_PATH || '/'}login`
        })
      }
      else {
        showNotify({ type: 'danger', message: msg })
        if (window.location.hash !== '#/login' && window.location.pathname !== '/login')
          window.location.href = `${import.meta.env.VITE_APP_PUBLIC_PATH || '/'}login`
      }
    }
    if (status === 422) {
      const errors: FieldError[] | undefined = (data as any)?.errors
      if (errors && errors.length > 0) {
        const msg = errors.map(e => `${e.field}: ${e.message}`).join('\n')
        showNotify({ type: 'danger', message: msg })
      }
      else {
        const msg = (data as any)?.message || '参数校验失败'
        showNotify({ type: 'danger', message: msg })
      }
    }
  }
  return Promise.reject(error)
}

function requestHandler(config: InternalAxiosRequestConfig): InternalAxiosRequestConfig | Promise<InternalAxiosRequestConfig> {
  const savedToken = localStorage.getItem(TOKEN_KEY)
  if (savedToken && savedToken !== 'null' && savedToken !== 'undefined')
    config.headers[REQUEST_TOKEN_KEY] = `Bearer ${savedToken}`
  return config
}

request.interceptors.request.use(requestHandler, errorHandler)

function responseHandler(response: AxiosResponse) {
  return response.data
}

request.interceptors.response.use(responseHandler, errorHandler)

interface RequestInstance extends AxiosInstance {
  <T = any>(url: string, config?: AxiosRequestConfig): Promise<T>
  <T = any>(config: AxiosRequestConfig): Promise<T>
  get: <T = any>(url: string, config?: AxiosRequestConfig) => Promise<T>
  post: <T = any>(url: string, data?: any, config?: AxiosRequestConfig) => Promise<T>
  put: <T = any>(url: string, data?: any, config?: AxiosRequestConfig) => Promise<T>
  delete: <T = any>(url: string, config?: AxiosRequestConfig) => Promise<T>
}

export default request as unknown as RequestInstance
