import { defineMock } from 'vite-plugin-mock-dev-server'
import { builder } from '../util'

export default defineMock([
  {
    url: '/api/auth/login',
    delay: 200,
    body: () => {
      // ponytail: mimic real backend LoginResp response format (flat, no code/data/msg wrapper)
      return {
        accessToken: 'eyJhbGciOiJIUzI1NiJ9.eyJsb2dpblR5cGUiOiJsb2dpbiIsImxvZ2luSWQiOiJtb2NrLXVzZXItaWQifQ.mock-jwt-token',
        tokenType: 'bearer',
        refreshToken: 'eyJhbGciOiJIUzI1NiJ9.eyJsb2dpblR5cGUiOiJyZWZyZXNoIn0.mock-refresh-token',
      }
    },
  },
  {
    url: '/api/users/me',
    delay: 100,
    body: () => {
      return {
        id: 'mock-user-id',
        fullName: 'Admin',
        email: 'admin@mock.local',
        isActive: true,
        isSuperuser: false,
        partnerId: null,
        createdAt: new Date().toISOString(),
      }
    },
  },
  {
    url: '/api/auth/logout',
    delay: 200,
    body: () => {
      return { message: 'Logged out successfully' }
    },
  },
  {
    url: '/api/password-recovery/mock@test.com',
    delay: 500,
    body: () => {
      return { message: 'If that email is registered, we sent a password recovery link' }
    },
  },
  {
    url: '/api/reset-password',
    delay: 500,
    body: () => {
      return { message: 'Password reset successfully' }
    },
  },
  {
    url: '/api/users/signup',
    delay: 500,
    body: () => {
      const res = true
      return builder(res)
    },
  },
])
