<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

onMounted(() => {
  router.replace('/todos')
})
</script>

<template>
  <div class="redirect-page">
    <van-loading />
  </div>
</template>

<style scoped>
.redirect-page {
  display: flex;
  justify-content: center;
  padding: 48px;
}
</style>

<route lang="json5">
{
  name: 'Today'
}
</route>
