<script setup lang="ts">
import { showConfirmDialog, showNotify } from 'vant'
import { useUserStore } from '@/stores'
import request from '@/utils/request'
import router from '@/router'

const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)
const unbinding = ref(false)

const partnerId = computed(() => userInfo.value?.partnerId || (userInfo.value as any)?.partner_id)
const partnerName = ref('')

async function loadPartnerInfo() {
  if (!partnerId.value) return
  try {
    const res = await request.get(`/users/${partnerId.value}`)
    partnerName.value = res.data?.fullName || res.data?.full_name || '已绑定伴侣'
  } catch {
    partnerName.value = '已绑定伴侣'
  }
}

async function handleUnbind() {
  try {
    await showConfirmDialog({
      title: '确认解除伴侣？',
      message: '解除后将删除双方所有的共同记录，此操作不可恢复。对方不会收到通知。',
      confirmButtonText: '确认解除',
      confirmButtonColor: 'red',
    })
  } catch {
    return // 取消
  }

  unbinding.value = true
  try {
    await request.post('/identity/unbind-partner')
    showNotify({ type: 'success', message: '已解除伴侣绑定' })
    await userStore.info()
    router.push({ name: 'Profile' })
  } catch {
    showNotify({ type: 'danger', message: '解除失败' })
  } finally {
    unbinding.value = false
  }
}

onMounted(async () => {
  await userStore.info()
  await loadPartnerInfo()
})
</script>

<template>
  <div class="p-4">
    <!-- 未绑定 -->
    <div v-if="!partnerId" class="flex flex-col items-center gap-4 pt-10">
      <van-empty description="尚未绑定伴侣" />
      <van-button type="primary" round @click="router.push('/share')">
        去绑定伴侣
      </van-button>
    </div>

    <!-- 已绑定 -->
    <template v-else>
      <div class="flex flex-col items-center gap-3 pt-8 pb-6">
        <van-icon name="friends-o" size="64" color="var(--van-primary-color)" />
        <div class="text-lg font-semibold">{{ partnerName }}</div>
        <div class="text-sm text-gray-400">已绑定伴侣</div>
      </div>

      <van-button
        type="danger"
        plain
        round
        block
        :loading="unbinding"
        @click="handleUnbind"
      >
        解除伴侣绑定
      </van-button>
    </template>
  </div>
</template>

<route lang="json5">
{
  name: 'PartnerDetail'
}
</route>
