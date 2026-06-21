<script setup lang="ts">
defineProps<{
  show: boolean
  event?: {
    id: string
    title: string
    description?: string | null
    planned_start_time: string
    planned_end_time?: string | null
    color?: string | null
    category?: string | null
  } | null
}>()
const emit = defineEmits<{ close: [], edit: [event: any] }>()
</script>

<template>
  <van-action-sheet
    :show="show"
    :title="event?.title || ''"
    @close="emit('close')"
  >
    <div class="sheet-content">
      <van-cell-group :border="false">
        <van-cell v-if="event?.category" title="分类" :value="event.category" />
        <van-cell v-if="event?.description" :title="event.description" />
        <van-cell title="开始时间" :value="event?.planned_start_time ? new Date(event.planned_start_time).toLocaleString('zh-CN') : ''" />
        <van-cell title="结束时间" :value="event?.planned_end_time ? new Date(event.planned_end_time).toLocaleString('zh-CN') : '待定'" />
        <van-cell v-if="event?.color" title="颜色">
          <template #value>
            <span class="color-dot" :style="{ background: event.color }" />
          </template>
        </van-cell>
      </van-cell-group>
    </div>
  </van-action-sheet>
</template>

<style scoped>
.sheet-content {
  padding: 16px 16px 32px;
}
.color-dot {
  display: inline-block;
  width: 16px;
  height: 16px;
  border-radius: 50%;
}
</style>
