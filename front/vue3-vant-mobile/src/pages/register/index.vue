<script setup lang="ts">
import { useRouter } from 'vue-router'
import type { FieldRule } from 'vant'
import { showNotify } from 'vant'
import { useUserStore } from '@/stores'
import vw from '@/utils/inline-px-to-vw'

const { t } = useI18n()
const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)

const postData = reactive({
  email: '',
  fullName: '',
  password: '',
  confirmPassword: '',
})

const validatorPassword = (val: string) => val === postData.password

const rules = reactive({
  email: [
    { required: true, message: t('register.pleaseEnterUsername') },
  ],
  fullName: [
    { required: true, message: t('register.pleaseEnterFullName') },
  ],
  password: [
    { required: true, message: t('register.pleaseEnterPassword') },
  ],
  confirmPassword: [
    { required: true, message: t('register.pleaseEnterConfirmPassword') },
    { required: true, validator: validatorPassword, message: t('register.passwordsDoNotMatch') },
  ] as FieldRule[],
})

async function register() {
  try {
    loading.value = true
    await userStore.register({
      email: postData.email,
      password: postData.password,
      fullName: postData.fullName,
    })
    showNotify({ type: 'success', message: t('register.registerSuccess') })
    router.push({ name: 'Login' })
  }
  finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="mx-auto p-3 text-center w-full">
    <van-form :model="postData" :rules="rules" validate-trigger="onSubmit" @submit="register">
      <div class="rounded-md overflow-hidden">
        <van-field
          v-model.trim="postData.email"
          :rules="rules.email"
          name="email"
          :placeholder="$t('register.username')"
        />
      </div>

      <div class="mt-4 rounded-md overflow-hidden">
        <van-field
          v-model.trim="postData.fullName"
          :rules="rules.fullName"
          name="fullName"
          :placeholder="$t('register.fullName')"
        />
      </div>

      <div class="mt-4 rounded-md overflow-hidden">
        <van-field
          v-model.trim="postData.password"
          type="password"
          :rules="rules.password"
          name="password"
          :placeholder="$t('register.password')"
        />
      </div>

      <div class="mt-4 rounded-md overflow-hidden">
        <van-field
          v-model.trim="postData.confirmPassword"
          type="password"
          :rules="rules.confirmPassword"
          name="confirmPassword"
          :placeholder="$t('register.confirmPassword')"
        />
      </div>

      <div class="mt-4">
        <van-button
          :loading="loading"
          type="primary"
          native-type="submit"
          round block
        >
          {{ $t('register.confirm') }}
        </van-button>
      </div>
    </van-form>

    <GhostButton to="login" block :style="{ 'margin-top': vw(8) }">
      {{ $t('register.backToLogin') }}
    </GhostButton>
  </div>
</template>

<route lang="json5">
{
  name: 'Register'
}
</route>
