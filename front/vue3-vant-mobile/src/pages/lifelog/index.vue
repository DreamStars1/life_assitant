<script setup lang="ts">
const ENABLED = false
</script>

<template>
  <div v-if="ENABLED" class="lifelog-page">
    <!-- 原生活记录内容，后端实现后恢复 -->
  </div>
  <div v-else class="placeholder-page">
    <van-icon name="underway-o" size="48" color="var(--van-gray-5)" />
    <p class="placeholder-text">
      功能开发中，敬请期待
    </p>
  </div>
</template>

<style scoped>
.placeholder-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  gap: 12px;
}
.placeholder-text {
  color: var(--van-gray-5);
  font-size: 14px;
}
</style>

<route lang="json5">
{
  name: 'LifeLog'
}
</route>
