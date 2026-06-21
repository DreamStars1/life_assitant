<script setup lang="ts">
import { showToast } from 'vant'
import { useUserStore } from '@/stores'
import request from '@/utils/request'

const router = useRouter()
const userStore = useUserStore()

const fullName = ref(userStore.userInfo.fullName || '')

async function save() {
  if (!fullName.value.trim())
    return
  await request.patch('/users/me', { fullName: fullName.value })
  await userStore.info()
  showToast('昵称已更新')
  router.back()
}
</script>

<template>
  <div class="p-4">
    <van-form @submit="save">
      <van-cell-group :inset="true">
        <van-field
          v-model="fullName"
          label="昵称"
          placeholder="请输入昵称"
          clearable
          maxlength="50"
        />
      </van-cell-group>
      <div class="mt-6 px-4">
        <van-button type="primary" round block native-type="submit">
          保存
        </van-button>
      </div>
    </van-form>
  </div>
</template>

<route lang="json5">
{
  name: 'ProfileEdit'
}
</route>
