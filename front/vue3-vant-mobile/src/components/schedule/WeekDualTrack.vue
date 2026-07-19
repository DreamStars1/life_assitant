<script setup lang="ts">
import type { ScheduleEventItem } from '@/api/modules/schedule'

const props = defineProps<{
  weekStart: string
  mode: 'self' | 'dual'
  mine: ScheduleEventItem[]
  partner: ScheduleEventItem[]
}>()

const emit = defineEmits<{
  'select-day': [date: string]
  'open-event': [event: ScheduleEventItem]
}>()

const { t, locale } = useI18n()

function addDays(dateStr: string, days: number): string {
  const d = new Date(`${dateStr}T00:00:00`)
  d.setDate(d.getDate() + days)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

const weekDays = computed(() =>
  Array.from({ length: 7 }, (_, i) => addDays(props.weekStart, i)),
)

function eventsForDay(events: ScheduleEventItem[], date: string): ScheduleEventItem[] {
  const dayStart = new Date(`${date}T00:00:00`)
  const dayEnd = new Date(`${date}T23:59:59`)

  return events
    .filter((event) => {
      const start = new Date(event.startAt.replace(' ', 'T'))
      const end = new Date(event.endAt.replace(' ', 'T'))
      return end > dayStart && start <= dayEnd
    })
    .sort((a, b) => a.startAt.localeCompare(b.startAt))
}

const mineByDay = computed(() =>
  weekDays.value.map(date => ({ date, events: eventsForDay(props.mine, date) })),
)

const partnerByDay = computed(() =>
  weekDays.value.map(date => ({ date, events: eventsForDay(props.partner, date) })),
)

function weekdayLabel(dateStr: string): string {
  return new Date(`${dateStr}T00:00:00`).toLocaleDateString(locale.value, { weekday: 'short' })
}

function dayNumber(dateStr: string): number {
  return new Date(`${dateStr}T00:00:00`).getDate()
}

function onSelectDay(date: string) {
  emit('select-day', date)
}

function onEventClick(event: ScheduleEventItem) {
  emit('open-event', event)
}
</script>

<template>
  <div class="week-dual-track">
    <div class="day-header">
      <span class="track-spacer" />
      <button
        v-for="date in weekDays"
        :key="date"
        type="button"
        class="day-head"
        @click="onSelectDay(date)"
      >
        <span class="weekday">{{ weekdayLabel(date) }}</span>
        <span class="date-num">{{ dayNumber(date) }}</span>
      </button>
    </div>

    <div class="track-row mine-row">
      <span class="track-label">{{ t('schedule.mine') }}</span>
      <div
        v-for="day in mineByDay"
        :key="day.date"
        class="day-cell mine"
        role="button"
        tabindex="0"
        @click="onSelectDay(day.date)"
        @keydown.enter="onSelectDay(day.date)"
      >
        <div
          v-for="event in day.events"
          :key="event.instanceKey"
          class="event-chip"
          role="button"
          tabindex="0"
          @click.stop="onEventClick(event)"
          @keydown.enter.stop="onEventClick(event)"
        >
          <span class="event-title">{{ event.title }}</span>
          <span v-if="event.pendingInviteId" class="pending-badge">{{ t('schedule.pendingInvite') }}</span>
        </div>
      </div>
    </div>

    <div v-if="mode === 'dual'" class="track-row partner-row">
      <span class="track-label">{{ t('schedule.partner') }}</span>
      <div
        v-for="day in partnerByDay"
        :key="day.date"
        class="day-cell partner"
        role="button"
        tabindex="0"
        @click="onSelectDay(day.date)"
        @keydown.enter="onSelectDay(day.date)"
      >
        <div
          v-for="event in day.events"
          :key="event.instanceKey"
          class="event-chip"
          role="button"
          tabindex="0"
          @click.stop="onEventClick(event)"
          @keydown.enter.stop="onEventClick(event)"
        >
          <span class="event-title">{{ event.title }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.week-dual-track {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.day-header {
  display: flex;
  border-bottom: 1px solid #f5ede4;
}
.track-spacer {
  width: 36px;
  flex-shrink: 0;
}
.day-head {
  flex: 1;
  min-width: 0;
  border: none;
  background: transparent;
  padding: 6px 2px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}
.weekday {
  font-size: 11px;
  color: var(--van-text-color-3);
}
.date-num {
  font-size: 14px;
  font-weight: 600;
  color: var(--van-text-color);
}
.track-row {
  display: flex;
  min-height: 72px;
}
.track-label {
  width: 36px;
  flex-shrink: 0;
  font-size: 12px;
  font-weight: 600;
  color: var(--van-text-color-2);
  padding-top: 6px;
  text-align: center;
}
.day-cell {
  flex: 1;
  min-width: 0;
  border-left: 1px solid #f5ede4;
  padding: 4px 2px;
  cursor: pointer;
}
.day-cell.mine {
  background: rgba(25, 137, 250, 0.04);
}
.day-cell.partner {
  background: rgba(255, 151, 106, 0.04);
}
.event-chip {
  margin-bottom: 4px;
  padding: 2px 4px;
  border-radius: 4px;
  overflow: hidden;
}
.mine-row .event-chip {
  background: rgba(25, 137, 250, 0.12);
  border-left: 2px solid var(--van-primary-color, #1989fa);
}
.partner-row .event-chip {
  background: rgba(255, 151, 106, 0.12);
  border-left: 2px solid #ff976a;
}
.event-title {
  display: block;
  font-size: 11px;
  line-height: 1.3;
  color: var(--van-text-color);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.pending-badge {
  display: block;
  font-size: 9px;
  color: var(--van-warning-color, #ff976a);
}
</style>
