<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { useRouteCacheStore } from '@/stores'

const { t } = useI18n()
const route = useRoute()

const PUBLIC_ROUTES = ['Login', 'Register', 'ForgotPassword']
const showLayout = computed(() => !PUBLIC_ROUTES.includes(route.name as string))

useHead({
  title: () => t('app.name'),
  meta: [
    { name: 'description', content: () => t('app.description') },
    { name: 'theme-color', content: () => isDark.value ? '#0B0A0A' : '#ffffff' },
  ],
  link: [
    {
      rel: 'icon',
      type: 'image/svg+xml',
      href: () => preferredDark.value ? '/favicon-dark.svg' : '/favicon.svg',
    },
  ],
})

const routeCacheStore = useRouteCacheStore()
const keepAliveRouteNames = computed(() => routeCacheStore.routeCaches)

const mode = computed<'light' | 'dark'>(() => 'light')
</script>

<template>
  <van-config-provider :theme="mode">
    <nav-bar v-if="showLayout" />
    <router-view v-slot="{ Component }">
      <section class="app-wrapper">
        <keep-alive :include="keepAliveRouteNames">
          <component :is="Component" />
        </keep-alive>
      </section>
    </router-view>
    <tab-bar v-if="showLayout" />
  </van-config-provider>
</template>

<style scoped>
.app-wrapper {
  width: 100%;
  position: relative;
  padding: 0 0 16px;
}
</style>
