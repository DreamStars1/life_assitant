<script setup lang="ts">
import { showConfirmDialog, showNotify } from 'vant'
import { useUserStore } from '@/stores'
import { updatePartnerSince } from '@/api/user'
import request from '@/utils/request'
import router from '@/router'

const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)
const unbinding = ref(false)
const showSinceCalendar = ref(false)
const sinceCalendarMaxDate = new Date()

const partnerId = computed(() => userInfo.value?.partnerId || (userInfo.value as any)?.partner_id)
const partnerName = ref('')

const daysTogether = computed(() => {
  const since = userInfo.value.partnerSince
  if (!since)
    return 0
  const start = new Date(`${since.slice(0, 10)}T00:00:00`)
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const diff = Math.floor((today.getTime() - start.getTime()) / 86400000)
  return diff < 0 ? 0 : diff + 1
})

const partnerSinceLabel = computed(() => {
  const since = userInfo.value.partnerSince
  if (!since)
    return '未设置'
  return since.slice(0, 10)
})

const partnerSinceDefaultDate = computed(() => {
  const since = userInfo.value.partnerSince
  if (since)
    return new Date(`${since.slice(0, 10)}T12:00:00`)
  return new Date()
})

function toLocalDateStr(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

async function loadPartnerInfo() {
  if (!partnerId.value)
    return
  try {
    const res = await request.get(`/users/${partnerId.value}`)
    partnerName.value = res.data?.fullName || res.data?.full_name || '已绑定伴侣'
  }
  catch {
    partnerName.value = '已绑定伴侣'
  }
}

async function onPartnerSinceConfirm(d: Date) {
  showSinceCalendar.value = false
  try {
    await updatePartnerSince(toLocalDateStr(d))
    showNotify({ type: 'success', message: '已更新在一起日期' })
    await userStore.info()
  }
  catch {
    // request 拦截器已提示错误
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
  }
  catch {
    return // 取消
  }

  unbinding.value = true
  try {
    await request.post('/identity/unbind-partner')
    showNotify({ type: 'success', message: '已解除伴侣绑定' })
    await userStore.info()
    router.push({ name: 'Profile' })
  }
  catch {
    showNotify({ type: 'danger', message: '解除失败' })
  }
  finally {
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
    <div v-if="!partnerId" class="pt-10 flex flex-col gap-4 items-center">
      <van-empty description="尚未绑定伴侣" />
      <van-button type="primary" round @click="router.push('/share')">
        去绑定伴侣
      </van-button>
    </div>

    <!-- 已绑定 -->
    <template v-else>
      <div class="pb-6 pt-8 flex flex-col gap-3 items-center">
        <van-icon name="friends-o" size="64" color="var(--van-primary-color)" />
        <div class="text-lg font-semibold">
          {{ partnerName }}
        </div>
        <div class="text-sm text-gray-400">
          已绑定伴侣
        </div>
        <div class="text-sm text-gray-500">
          在一起 {{ daysTogether }} 天
        </div>
        <div class="text-sm text-gray-400">
          起始日期：{{ partnerSinceLabel }}
        </div>
      </div>

      <van-button
        type="primary"
        plain
        round
        block
        class="!mb-3"
        @click="showSinceCalendar = true"
      >
        修改在一起日期
      </van-button>

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

      <van-calendar
        v-model:show="showSinceCalendar"
        :min-date="new Date('2020-01-01')"
        :max-date="sinceCalendarMaxDate"
        :default-date="partnerSinceDefaultDate"
        @confirm="onPartnerSinceConfirm"
      />
    </template>
  </div>
</template>

<route lang="json5">
{
  name: 'PartnerDetail'
}
</route>
