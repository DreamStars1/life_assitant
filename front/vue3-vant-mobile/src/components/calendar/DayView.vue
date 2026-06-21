<script setup lang="ts">
defineProps<{
  date: Date
  events?: Array<{ id: string, title: string, planned_start_time: string, planned_end_time?: string | null, color?: string | null }>
}>()
defineEmits<{ clickEvent: [event: any] }>()
</script>

<template>
  <div class="day-view">
    <div class="day-date-header">
      {{ date.toLocaleDateString('zh-CN', { month: 'long', day: 'numeric', weekday: 'long' }) }}
    </div>
    <div v-for="hour in 24" :key="hour" class="hour-row">
      <span class="hour-label">{{ String(hour).padStart(2, '0') }}:00</span>
      <div class="hour-content" />
    </div>
  </div>
</template>

<style scoped>
.day-view {
  display: flex;
  flex-direction: column;
}
.day-date-header {
  text-align: center;
  padding: 12px;
  font-size: 16px;
  font-weight: 600;
  color: var(--van-text-color);
}
.hour-row {
  display: flex;
  min-height: 52px;
  border-bottom: 1px solid #f5ede4;
}
.hour-label {
  width: 56px;
  font-size: 12px;
  color: var(--van-text-color-3);
  padding: 4px 8px;
  text-align: right;
  flex-shrink: 0;
}
.hour-content {
  flex: 1;
  position: relative;
}
</style>
