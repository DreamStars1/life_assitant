<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores'

import logo from '~/images/logo.svg'

const { t } = useI18n()
const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const showPassword = ref(false)

const postData = reactive({
  email: '',
  password: '',
})

const rules = reactive({
  email: [
    { required: true, message: t('login.pleaseEnterUsername') },
  ],
  password: [
    { required: true, message: t('login.pleaseEnterPassword') },
  ],
})

async function login() {
  try {
    loading.value = true
    await userStore.login({ email: postData.email, password: postData.password })
    const { redirect, ...othersQuery } = router.currentRoute.value.query
    router.push({
      name: (redirect as any) || 'Today',
      query: { ...othersQuery },
    })
  }
  finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="mx-auto p-3 text-center w-full">
    <div class="mb-8 mt-2">
      <van-image :src="logo" class="h-30 w-30" alt="brand logo" />
    </div>

    <van-form :model="postData" :rules="rules" validate-trigger="onSubmit" @submit="login">
      <div class="rounded-md overflow-hidden">
        <van-field
          v-model="postData.email"
          :rules="rules.email"
          name="email"
          :placeholder="$t('login.username')"
        />
      </div>

      <div class="mt-4 rounded-md overflow-hidden">
        <van-field
          v-model="postData.password"
          :type="showPassword ? 'text' : 'password'"
          :rules="rules.password"
          name="password"
          :placeholder="$t('login.password')"
        >
          <template #button>
            <van-icon
              :name="showPassword ? 'eye-o' : 'closed-eye'"
              class="cursor-pointer"
              @click="showPassword = !showPassword"
            />
          </template>
        </van-field>
      </div>

      <div class="mt-4">
        <van-button
          :loading="loading"
          type="primary"
          native-type="submit"
          round block
        >
          {{ $t('login.login') }}
        </van-button>
      </div>
    </van-form>

    <div class="beian-footer">
      <a href="https://beian.miit.gov.cn/" target="_blank">苏ICP备2026047415号-1</a>
    </div>
  </div>
</template>

<style scoped>
.beian-footer {
  margin-top: 24px;
  text-align: center;
}
.beian-footer a {
  font-size: 12px;
  color: var(--van-gray-5);
  text-decoration: none;
}
</style>

<route lang="json5">
{
  name: 'Login'
}
</route>
