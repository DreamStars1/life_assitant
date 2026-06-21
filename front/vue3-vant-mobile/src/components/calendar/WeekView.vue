<script setup lang="ts">
defineProps<{
  date: Date
  events?: Array<{ id: string, title: string, planned_start_time: string, color?: string | null }>
}>()
defineEmits<{ clickEvent: [event: any] }>()

const weekdays = ['一', '二', '三', '四', '五', '六', '日']
</script>

<template>
  <div class="week-view">
    <div class="week-header">
      <span v-for="(wd, i) in weekdays" :key="i" class="day-label">
        <span class="wd">{{ wd }}</span>
        <span class="date-num">15</span>
      </span>
    </div>
    <div class="week-body">
      <div v-for="col in 7" :key="col" class="day-column">
        <div class="hour-markers">
          <div v-for="h in 12" :key="h" class="hour-slot">
            <span class="hour-tag">{{ h + 7 }}:00</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.week-view {
  display: flex;
  flex-direction: column;
}
.week-header {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  text-align: center;
  padding: 8px 0;
  border-bottom: 1px solid var(--van-border-color);
}
.day-label {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.wd {
  font-size: 12px;
  color: var(--van-text-color-2);
}
.date-num {
  font-size: 16px;
  font-weight: 600;
  color: var(--van-text-color);
}
.week-body {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  overflow-x: auto;
}
.day-column {
  min-height: 400px;
  border-left: 1px solid var(--van-border-color);
}
.hour-markers {
  position: relative;
}
.hour-slot {
  height: 48px;
  border-bottom: 1px solid #f5ede4;
  position: relative;
}
.hour-tag {
  position: absolute;
  top: -8px;
  left: 2px;
  font-size: 10px;
  color: var(--van-text-color-3);
}
</style>
