<script setup lang="ts">
import type { ScheduleEventItem } from '@/api/modules/schedule'

const props = defineProps<{
  date: string
  mode: 'self' | 'dual'
  mine: ScheduleEventItem[]
  partner: ScheduleEventItem[]
}>()

const emit = defineEmits<{
  'create-at': [iso: string]
  'open-event': [event: ScheduleEventItem]
}>()

const { t } = useI18n()

const HOUR_HEIGHT = 52
const TOTAL_HEIGHT = 24 * HOUR_HEIGHT
const scrollRef = ref<HTMLElement | null>(null)

interface PositionedEvent {
  event: ScheduleEventItem
  top: number
  height: number
}

function pad(n: number) {
  return String(n).padStart(2, '0')
}

function positionEvents(events: ScheduleEventItem[]): PositionedEvent[] {
  const dayStart = new Date(`${props.date}T00:00:00`)
  const dayEnd = new Date(`${props.date}T23:59:59`)

  return events
    .map((event) => {
      const start = new Date(event.startAt.replace(' ', 'T'))
      const end = new Date(event.endAt.replace(' ', 'T'))
      if (end <= dayStart || start > dayEnd)
        return null

      const clipStart = start < dayStart ? dayStart : start
      const clipEnd = end > dayEnd ? dayEnd : end
      const startMinutes = clipStart.getHours() * 60 + clipStart.getMinutes()
      let endMinutes = clipEnd.getHours() * 60 + clipEnd.getMinutes()
      if (end > dayEnd)
        endMinutes = 24 * 60
      endMinutes = Math.max(endMinutes, startMinutes + 15)

      const top = (startMinutes / 1440) * TOTAL_HEIGHT
      const height = Math.max(((endMinutes - startMinutes) / 1440) * TOTAL_HEIGHT, 20)
      return { event, top, height }
    })
    .filter((x): x is PositionedEvent => x !== null)
}

const mineBlocks = computed(() => positionEvents(props.mine))
const partnerBlocks = computed(() => positionEvents(props.partner))

function onSlotClick(hour: number) {
  emit('create-at', `${props.date}T${pad(hour)}:00:00`)
}

function onEventClick(event: ScheduleEventItem) {
  emit('open-event', event)
}

onMounted(() => {
  scrollRef.value?.scrollTo({ top: 8 * HOUR_HEIGHT, behavior: 'auto' })
})
</script>

<template>
  <div ref="scrollRef" class="day-timeline">
    <div v-if="mode === 'dual'" class="column-header">
      <span class="axis-spacer" />
      <span class="col-label">{{ t('schedule.mine') }}</span>
      <span class="col-label">{{ t('schedule.partner') }}</span>
    </div>
    <div class="timeline-body">
      <div class="hour-axis">
        <div v-for="h in 24" :key="h" class="hour-label" :style="{ height: `${HOUR_HEIGHT}px` }">
          {{ pad(h - 1) }}:00
        </div>
      </div>
      <div class="columns" :class="{ dual: mode === 'dual' }">
        <div class="event-column">
          <div class="grid-layer">
            <div
              v-for="h in 24"
              :key="h"
              class="hour-slot"
              :style="{ height: `${HOUR_HEIGHT}px` }"
              @click="onSlotClick(h - 1)"
            />
          </div>
          <div class="blocks-layer" :style="{ height: `${TOTAL_HEIGHT}px` }">
            <button
              v-for="block in mineBlocks"
              :key="block.event.instanceKey"
              type="button"
              class="event-block mine"
              :style="{ top: `${block.top}px`, height: `${block.height}px` }"
              @click.stop="onEventClick(block.event)"
            >
              <span class="event-title">{{ block.event.title }}</span>
              <span v-if="block.event.pendingInviteId" class="pending-badge">{{ t('schedule.pendingInvite') }}</span>
            </button>
          </div>
        </div>
        <div v-if="mode === 'dual'" class="event-column partner">
          <div class="grid-layer">
            <div
              v-for="h in 24"
              :key="h"
              class="hour-slot"
              :style="{ height: `${HOUR_HEIGHT}px` }"
            />
          </div>
          <div class="blocks-layer" :style="{ height: `${TOTAL_HEIGHT}px` }">
            <button
              v-for="block in partnerBlocks"
              :key="block.event.instanceKey"
              type="button"
              class="event-block partner-block"
              :style="{ top: `${block.top}px`, height: `${block.height}px` }"
              @click.stop="onEventClick(block.event)"
            >
              <span class="event-title">{{ block.event.title }}</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.day-timeline {
  overflow-y: auto;
  max-height: calc(100vh - 200px);
}
.column-header {
  display: flex;
  position: sticky;
  top: 0;
  z-index: 2;
  background: var(--van-background-2, #fff);
  border-bottom: 1px solid #f5ede4;
}
.axis-spacer {
  width: 48px;
  flex-shrink: 0;
}
.col-label {
  flex: 1;
  text-align: center;
  font-size: 13px;
  font-weight: 600;
  padding: 8px 4px;
  color: var(--van-text-color);
}
.timeline-body {
  display: flex;
}
.hour-axis {
  width: 48px;
  flex-shrink: 0;
}
.hour-label {
  font-size: 11px;
  color: var(--van-text-color-3);
  text-align: right;
  padding: 2px 6px 0 0;
  box-sizing: border-box;
}
.columns {
  flex: 1;
  display: flex;
  min-width: 0;
}
.columns.dual .event-column {
  border-left: 1px solid #f5ede4;
}
.event-column {
  flex: 1;
  position: relative;
  min-width: 0;
}
.grid-layer {
  position: relative;
  z-index: 0;
}
.hour-slot {
  border-bottom: 1px solid #f5ede4;
  box-sizing: border-box;
}
.blocks-layer {
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
}
.event-block {
  position: absolute;
  left: 4px;
  right: 4px;
  border: none;
  border-radius: 6px;
  padding: 4px 6px;
  text-align: left;
  overflow: hidden;
  pointer-events: auto;
  cursor: pointer;
}
.event-block.mine {
  background: rgba(25, 137, 250, 0.15);
  border-left: 3px solid var(--van-primary-color, #1989fa);
}
.event-block.partner-block {
  background: rgba(255, 151, 106, 0.15);
  border-left: 3px solid #ff976a;
}
.event-title {
  display: block;
  font-size: 12px;
  line-height: 1.3;
  color: var(--van-text-color);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.pending-badge {
  display: inline-block;
  margin-top: 2px;
  font-size: 10px;
  color: var(--van-warning-color, #ff976a);
}
</style>
