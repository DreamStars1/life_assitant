<script setup lang="ts">
import router from '@/router'
import { useUserStore } from '@/stores'
import { fetchTodos } from '@/api/modules/todos'
import { fetchLifeLogStats } from '@/api/modules/lifelogs'
import request from '@/utils/request'

const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)
const isLogin = computed(() => !!userInfo.value.id)
const displayName = computed(() => {
  const fn = userInfo.value.fullName
  const email = userInfo.value.email
  if (fn && fn !== email) return fn
  return null // no custom nickname
})

const partnerName = ref('')

watch(() => userInfo.value.partnerId, async (pid) => {
  if (pid) {
    try {
      const res = await request.get(`/users/${pid}`)
      partnerName.value = res.data?.fullName || res.data?.full_name || '已绑定伴侣'
    } catch {
      partnerName.value = '已绑定伴侣'
    }
  } else {
    partnerName.value = ''
  }
}, { immediate: true })

const stats = ref<{ total_todos: number; done_todos: number; total_events: number; log_counts: Record<string, number> }>({
  total_todos: 0, done_todos: 0, total_events: 0, log_counts: {},
})

async function loadStats() {
  if (!isLogin.value) return
  try {
    const [todosRes, doneRes, statsRes] = await Promise.all([
      fetchTodos(),
      fetchTodos({ is_completed: true }),
      fetchLifeLogStats('weekly'),
    ])
    const allTodos = Array.isArray(todosRes.data) ? todosRes.data : []
    const doneTodos = Array.isArray(doneRes.data) ? doneRes.data : []
    const s = Array.isArray(statsRes.data) ? statsRes.data[0] : (statsRes.data as any) || {}
    stats.value = {
      total_todos: allTodos.length,
      done_todos: doneTodos.length,
      total_events: 0,
      log_counts: s?.by_type || {},
    }
  } catch {}
}

function login() {
  if (isLogin.value) return
  router.push({ name: 'Login', query: { redirect: 'Profile' } })
}

onMounted(loadStats)
</script>

<template>
  <div>
    <VanCellGroup :inset="true">
      <van-cell center :is-link="!isLogin" @click="login">
        <template #value>
          <span v-if="isLogin && displayName" class="text-lg font-semibold">{{ displayName }}</span>
          <span v-else-if="isLogin" class="text-sm text-gray-400 cursor-pointer" @click="router.push('/profile/edit')">点击设置昵称</span>
          <span v-else>{{ $t('profile.login') }}</span>
        </template>
      </van-cell>
      <van-cell v-if="isLogin" title="伴侣" :value="partnerName || '未绑定伴侣'" />
    </VanCellGroup>

    <VanCellGroup v-if="isLogin" :inset="true" title="本周统计" class="!mt-4">
      <van-cell title="待办完成率">
        <template #value>
          <span class="stat-num">{{ stats.done_todos }}/{{ stats.total_todos }}</span>
          <span class="stat-pct">{{ stats.total_todos ? Math.round(stats.done_todos / stats.total_todos * 100) : 0 }}%</span>
        </template>
      </van-cell>
      <van-cell v-for="(count, type) in stats.log_counts" :key="type" :title="'记录: ' + type">
        <template #value>{{ count }} 条</template>
      </van-cell>
    </VanCellGroup>

    <VanCellGroup :inset="true" class="!mt-4">
      <van-cell title="伴侣" icon="friends-o" is-link to="/partner/detail">
        <template #icon><van-icon name="friends-o" class="icon" /></template>
        <template #value v-if="userInfo.partnerId">
          <span class="text-sm text-gray-400">{{ partnerName }}</span>
        </template>
      </van-cell>
      <van-cell title="设置" icon="setting-o" is-link to="/settings">
        <template #icon><van-icon name="setting-o" class="icon" /></template>
      </van-cell>
    </VanCellGroup>
  </div>
</template>

<style scoped>
.icon { margin-right: 6px; }
.stat-num { font-weight: 600; color: var(--van-primary-color); margin-right: 8px; }
.stat-pct { color: var(--van-text-color-2); font-size: 12px; }
</style>

<route lang="json5">
{
  name: 'Profile'
}
</route>
