<script setup lang="ts">
import router from '@/router'
import { useUserStore } from '@/stores'
import request from '@/utils/request'

const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)
const isLogin = computed(() => !!userInfo.value.id)
const displayName = computed(() => {
  const fn = userInfo.value.fullName
  const email = userInfo.value.email
  if (fn && fn !== email)
    return fn
  return null
})

const partnerName = ref('')

watch(() => userInfo.value.partnerId, async (pid) => {
  if (pid) {
    try {
      const res = await request.get(`/users/${pid}`)
      partnerName.value = res.data?.fullName || res.data?.full_name || '已绑定伴侣'
    }
    catch {
      partnerName.value = '已绑定伴侣'
    }
  }
  else {
    partnerName.value = ''
  }
}, { immediate: true })

function login() {
  if (isLogin.value)
    return
  router.push({ name: 'Login', query: { redirect: 'Profile' } })
}
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

    <VanCellGroup :inset="true" class="!mt-4">
      <van-cell v-if="isLogin" title="伴侣" icon="friends-o" is-link to="/partner/detail">
        <template #icon>
          <van-icon name="friends-o" class="icon" />
        </template>
        <template v-if="userInfo.partnerId" #value>
          <span class="text-sm text-gray-400">{{ partnerName }}</span>
        </template>
      </van-cell>
      <van-cell v-if="isLogin" title="昵称" icon="contact" is-link to="/profile/edit">
        <template #icon>
          <van-icon name="contact" class="icon" />
        </template>
        <template #value>
          <span class="text-sm text-gray-400">{{ displayName || userInfo.email }}</span>
        </template>
      </van-cell>
      <van-cell title="设置" icon="setting-o" is-link to="/settings">
        <template #icon>
          <van-icon name="setting-o" class="icon" />
        </template>
      </van-cell>
    </VanCellGroup>
  </div>
</template>

<style scoped>
.icon {
  margin-right: 6px;
}
</style>

<route lang="json5">
{
  name: 'Profile'
}
</route>
