<script setup lang="ts">
import { rootRouteList } from '@/config/routes'

const active = ref(0)
const route = useRoute()

const show = computed(() => {
  if (route.name && rootRouteList.includes(route.name)) {
    return true
  }
  return false
})
</script>

<template>
  <van-tabbar v-if="show" v-model="active" route placeholder>
    <van-tabbar-item replace to="/">
      {{ $t('tabbar.today') }}
      <template #icon>
        <van-icon name="calendar-o" />
      </template>
    </van-tabbar-item>
    <van-tabbar-item replace to="/todos">
      {{ $t('tabbar.todos') }}
      <template #icon>
        <van-icon name="checked" />
      </template>
    </van-tabbar-item>
    <van-tabbar-item replace to="/events">
      {{ $t('tabbar.events') }}
      <template #icon>
        <van-icon name="clock-o" />
      </template>
    </van-tabbar-item>
    <van-tabbar-item replace to="/share">
      伴侣
      <template #icon>
        <van-icon name="like-o" />
      </template>
    </van-tabbar-item>
    <van-tabbar-item replace to="/profile">
      {{ $t('tabbar.profile') }}
      <template #icon>
        <van-icon name="contact" />
      </template>
    </van-tabbar-item>
  </van-tabbar>
</template>
