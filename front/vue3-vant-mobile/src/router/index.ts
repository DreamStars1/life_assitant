import { createRouter, createWebHistory } from 'vue-router'
import { handleHotUpdate, routes } from 'vue-router/auto-routes'

import NProgress from 'nprogress'
import 'nprogress/nprogress.css'

import type { EnhancedRouteLocation } from './types'
import { useRouteCacheStore, useUserStore } from '@/stores'

import { isLogin } from '@/utils/auth'
import setPageTitle from '@/utils/set-page-title'

NProgress.configure({ showSpinner: true, parent: '#app' })

const router = createRouter({
  history: createWebHistory(import.meta.env.VITE_APP_PUBLIC_PATH),
  routes,
})

if (import.meta.hot)
  handleHotUpdate(router)

const PUBLIC_ROUTES = ['Login', 'Register', 'ForgotPassword']

router.beforeEach(async (to: EnhancedRouteLocation) => {
  NProgress.start()

  const routeCacheStore = useRouteCacheStore()
  routeCacheStore.addRoute(to)

  setPageTitle(to.name)

  const isPublic = PUBLIC_ROUTES.includes(to.name as string)

  if (!isLogin() && !isPublic) {
    // 未登录 → 跳转到登录页，带上原始目标路由
    NProgress.done()
    return { name: 'Login', query: { redirect: to.name as string } }
  }

  if (isLogin() && isPublic) {
    // 已登录 → 登录/注册页直接跳到首页
    NProgress.done()
    return { name: 'Today' }
  }

  // 已登录 → 预加载用户信息（仅首次）
  if (isLogin()) {
    const userStore = useUserStore()
    if (!userStore.userInfo?.id) {
      try {
        await userStore.info()
      }
      catch {
        // token 失效，后续请求会 401 自动跳转
      }
    }
  }
})

router.afterEach(() => {
  NProgress.done()
})

export default router
