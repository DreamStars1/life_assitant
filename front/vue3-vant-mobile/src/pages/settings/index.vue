<script setup lang="ts">
import { showConfirmDialog } from 'vant'
import router from '@/router'
import { useUserStore } from '@/stores'
import { updatePushPreferences } from '@/api/modules/push'
import { version } from '~root/package.json'

const { t } = useI18n()
const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)

const pushEnabled = computed({
  get: () => userInfo.value.pushEnabled ?? true,
  set: async (val) => {
    await updatePushPreferences({ push_enabled: val })
    await userStore.info()
  },
})

function Logout() {
  showConfirmDialog({
    title: t('settings.confirmTitle'),
  })
    .then(() => {
      userStore.logout()
      router.push({ name: 'Today' })
    })
    .catch(() => {})
}
</script>

<template>
  <div>
    <van-cell-group :inset="true">
      <van-cell center :title="$t('profile.pushEnabled')">
        <template #right-icon>
          <van-switch v-model="pushEnabled" size="20px" />
        </template>
      </van-cell>
    </van-cell-group>

    <van-cell-group :inset="true" class="!mt-4">
      <van-cell v-if="userInfo.id" :title="$t('settings.logout')" clickable class="danger-text" @click="Logout" />
    </van-cell-group>

    <div class="text-gray mt-2 text-center">
      {{ $t("settings.currentVersion") }}: v{{ version }}
    </div>
  </div>
</template>

<style scoped>
.danger-text {
  --van-cell-text-color: var(--van-red);
}
</style>

<route lang="json5">
{
  name: 'Settings'
}
</route>
