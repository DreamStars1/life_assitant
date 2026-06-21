<script setup lang="ts">
const props = defineProps<{
  year: number
  month: number
  events?: Array<{ id: string, title: string, start_time: string, status: string, color?: string | null }>
  lifelogs?: Array<{ id: string, log_type: string, content: string, logged_at?: string | null }>
  today?: string
  showPlan?: boolean
  typeIcons?: Record<string, string>
}>()
const emit = defineEmits<{ clickDay: [date: string] }>()

const weekdays = ['一', '二', '三', '四', '五', '六', '日']

const days = computed(() => {
  const firstDay = new Date(props.year, props.month - 1, 1)
  const lastDay = new Date(props.year, props.month, 0)
  const startWeekday = (firstDay.getDay() + 6) % 7
  const totalDays = lastDay.getDate()
  const result: Array<{ day: number, dateStr: string, isCurrentMonth: boolean }> = []
  for (let i = 0; i < startWeekday; i++) {
    result.push({ day: 0, dateStr: '', isCurrentMonth: false })
  }
  for (let d = 1; d <= totalDays; d++) {
    const ds = `${props.year}-${String(props.month).padStart(2, '0')}-${String(d).padStart(2, '0')}`
    result.push({ day: d, dateStr: ds, isCurrentMonth: true })
  }
  return result
})

function eventsForDate(dateStr: string) {
  if (!props.events)
    return []
  return props.events.filter((e) => {
    const d = new Date(e.start_time)
    const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    return key === dateStr
  })
}

function lifelogsForDate(dateStr: string) {
  if (!props.lifelogs)
    return []
  return props.lifelogs.filter((l) => {
    const d = new Date(l.logged_at || Date.now())
    const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    return key === dateStr
  })
}
</script>

<template>
  <div class="month-grid">
    <div class="weekday-header">
      <span v-for="d in weekdays" :key="d">{{ d }}</span>
    </div>
    <div class="days-grid">
      <div
        v-for="(cell, idx) in days" :key="idx"
        class="day-cell"
        :class="{ 'other-month': !cell.isCurrentMonth, 'is-today': cell.dateStr === today }"
        @click="cell.dateStr && emit('clickDay', cell.dateStr)"
      >
        <span v-if="cell.day" class="day-num">{{ cell.day }}</span>

        <!-- Plan mode: colored dots -->
        <div v-if="showPlan && cell.day" class="event-dots">
          <div
            v-for="ev in eventsForDate(cell.dateStr).slice(0, 3)" :key="ev.id"
            class="event-dot"
            :style="{ background: ev.color || (ev.status === 'completed' ? '#7EC8A0' : ev.status === 'missed' ? '#D97A6E' : '#E8905E') }"
            :title="ev.title"
          />
          <span v-if="eventsForDate(cell.dateStr).length > 3" class="more">+{{ eventsForDate(cell.dateStr).length - 3 }}</span>
        </div>

        <!-- Actual mode: type icons -->
        <div v-if="!showPlan && cell.day" class="log-icons">
          <span v-for="log in lifelogsForDate(cell.dateStr).slice(0, 2)" :key="log.id" class="log-icon" :title="log.content">
            {{ typeIcons?.[log.log_type] || '📝' }}
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.month-grid {
  width: 100%;
  user-select: none;
}
.weekday-header {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  text-align: center;
  font-weight: 600;
  padding: 8px 0;
  color: var(--van-text-color-2);
  font-size: 13px;
}
.days-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 2px;
}
.day-cell {
  padding: 4px;
  min-height: 48px;
  border-radius: 8px;
  background: var(--van-cell-background);
  cursor: pointer;
  transition: background 0.2s;
}
.day-cell:hover {
  background: #fff5ee;
}
.day-cell.other-month {
  opacity: 0.3;
}
.day-cell.is-today {
  background: #fff0e6;
}
.day-cell.is-today .day-num {
  color: #e8905e;
  font-weight: 700;
}
.day-num {
  font-size: 13px;
  color: var(--van-text-color);
  font-weight: 500;
}
.event-dots {
  display: flex;
  flex-wrap: wrap;
  gap: 3px;
  margin-top: 2px;
}
.event-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}
.more {
  font-size: 10px;
  color: var(--van-text-color-3);
}
.log-icons {
  display: flex;
  flex-wrap: wrap;
  gap: 2px;
  margin-top: 2px;
  font-size: 14px;
}
.log-icon {
  line-height: 1;
}
</style>
