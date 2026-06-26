import { defineStore } from 'pinia'
import { clearToken, setToken } from '@/utils/auth'
import { getUserInfo, login as userLogin } from '@/api/user'
import request from '@/utils/request'

export interface UserState {
  id?: string
  fullName?: string
  email?: string
  partnerId?: string | null
  isActive?: boolean
  isSuperuser?: boolean
  createdAt?: string
  pushEnabled?: boolean
}

const InitUserInfo: UserState = {}

export const useUserStore = defineStore('user', () => {
  const userInfo = ref<UserState>({ ...InitUserInfo })
  const partnerName = ref('')

  const setInfo = (raw: Record<string, unknown>) => {
    const pid = (raw.partnerId || raw.partner_id) as string | null | undefined
    userInfo.value = {
      id: raw.id as string | undefined,
      fullName: (raw.fullName || raw.full_name) as string | undefined,
      email: raw.email as string | undefined,
      partnerId: pid,
      isActive: (raw.isActive ?? raw.is_active) as boolean | undefined,
      isSuperuser: (raw.isSuperuser ?? raw.is_superuser) as boolean | undefined,
      createdAt: (raw.createdAt || raw.created_at) as string | undefined,
      pushEnabled: (raw.pushEnabled ?? raw.push_enabled) as boolean | undefined,
    }
    if (pid)
      loadPartnerName(pid)
    else partnerName.value = ''
  }

  const loadPartnerName = async (pid: string) => {
    try {
      const res = await request.get(`/users/${pid}`)
      partnerName.value = res.data?.fullName || res.data?.full_name || '对方'
    }
    catch {
      partnerName.value = '对方'
    }
  }

  const info = async () => {
    try {
      const res = await getUserInfo()
      if (res.data)
        setInfo(res.data as unknown as Record<string, unknown>)
    }
    catch (error) {
      clearToken()
      throw error
    }
  }

  const login = async (loginForm: { email: string, password: string }) => {
    try {
      const res = await userLogin(loginForm)
      const data = res.data
      const token = data?.accessToken || data?.access_token
      if (token && typeof token === 'string')
        setToken(token)
      await info()
    }
    catch (error) {
      clearToken()
      throw error
    }
  }

  const logout = async () => {
    clearToken()
    setInfo({ ...InitUserInfo })
    partnerName.value = ''
  }

  const register = async (form: { email: string, password: string, fullName?: string }) => {
    return request.post('/users/signup', {
      email: form.email,
      password: form.password,
      fullName: form.fullName,
    })
  }

  const resetPassword = async (email: string) => {
    return request.post(`/password-recovery/${email}`)
  }

  const sendResetCode = async (email: string) => {
    return request.post('/reset-password', { email })
  }

  // ponytail: 持久化恢复后/绑定伴侣后自动拉取对方昵称
  watch(() => userInfo.value.partnerId, (pid) => {
    if (pid)
      loadPartnerName(pid)
    else partnerName.value = ''
  }, { immediate: true })

  return {
    userInfo,
    partnerName,
    info,
    login,
    logout,
    register,
    resetPassword,
    sendResetCode,
  }
}, {
  persist: true,
})

export default useUserStore
