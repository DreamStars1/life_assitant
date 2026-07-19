<script setup lang="ts">
import type { ScheduleEventItem } from '@/api/modules/schedule'
import { showToast } from 'vant'

const props = defineProps<{
  initial?: Partial<Pick<ScheduleEventItem, 'title' | 'startAt' | 'endAt' | 'note' | 'recurrence' | 'recurrenceEndDate'>>
  loading?: boolean
}>()

const emit = defineEmits<{
  save: [data: {
    title: string
    startAt: string
    endAt: string
    note?: string
    recurrence: 'none' | 'daily' | 'weekly'
    recurrenceEndDate?: string
  }]
}>()

const { t } = useI18n()

const title = ref(props.initial?.title ?? '')
const startAt = ref(props.initial?.startAt?.replace(' ', 'T') ?? '')
const endAt = ref(props.initial?.endAt?.replace(' ', 'T') ?? '')
const note = ref(props.initial?.note ?? '')
const recurrence = ref<'none' | 'daily' | 'weekly'>(props.initial?.recurrence ?? 'none')
const recurrenceEndDate = ref(props.initial?.recurrenceEndDate ?? '')

const showStartCalendar = ref(false)
const showStartTime = ref(false)
const showEndCalendar = ref(false)
const showEndTime = ref(false)
const showRecurrenceEnd = ref(false)
const editingField = ref<'start' | 'end'>('start')

const timeColumns = [
  Array.from({ length: 24 }, (_, i) => ({ text: String(i).padStart(2, '0'), value: String(i).padStart(2, '0') })),
  Array.from({ length: 12 }, (_, i) => ({ text: String(i * 5).padStart(2, '0'), value: String(i * 5).padStart(2, '0') })),
]

function splitDateTime(v: string) {
  const sep = v.includes('T') ? 'T' : v.includes(' ') ? ' ' : null
  if (!sep)
    return { date: v, time: '' }
  const [date, rest] = v.split(sep)
  return { date: date!, time: rest?.slice(0, 5) ?? '' }
}

function formatDateTimeText(v: string) {
  if (!v)
    return ''
  const { date, time } = splitDateTime(v)
  return time ? `${date} ${time}` : date
}

function onDateConfirm(d: Date) {
  const pad = (n: number) => String(n).padStart(2, '0')
  const field = editingField.value
  const current = field === 'start' ? startAt.value : endAt.value
  const { time } = splitDateTime(current)
  const next = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${time || '09:00'}:00`
  if (field === 'start') {
    startAt.value = next
    showStartCalendar.value = false
    showStartTime.value = true
  }
  else {
    endAt.value = next
    showEndCalendar.value = false
    showEndTime.value = true
  }
}

function onTimeConfirm({ selectedValues }: { selectedValues: (string | number)[] }) {
  const h = String(selectedValues[0]!).padStart(2, '0')
  const m = String(selectedValues[1]!).padStart(2, '0')
  const field = editingField.value
  const current = field === 'start' ? startAt.value : endAt.value
  const { date } = splitDateTime(current)
  const d = date || new Date().toISOString().slice(0, 10)
  const next = `${d}T${h}:${m}:00`
  if (field === 'start') {
    startAt.value = next
    showStartTime.value = false
  }
  else {
    endAt.value = next
    showEndTime.value = false
  }
}

function openStartPicker() {
  editingField.value = 'start'
  showStartCalendar.value = true
}

function openEndPicker() {
  editingField.value = 'end'
  showEndCalendar.value = true
}

function onRecurrenceEndConfirm(d: Date) {
  const pad = (n: number) => String(n).padStart(2, '0')
  recurrenceEndDate.value = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
  showRecurrenceEnd.value = false
}

function onSave() {
  if (!title.value.trim()) {
    showToast(t('schedule.validationTitle'))
    return
  }
  if (!startAt.value || !endAt.value || new Date(endAt.value) <= new Date(startAt.value)) {
    showToast(t('schedule.validationTime'))
    return
  }
  emit('save', {
    title: title.value.trim(),
    startAt: startAt.value.replace(' ', 'T'),
    endAt: endAt.value.replace(' ', 'T'),
    note: note.value.trim() || undefined,
    recurrence: recurrence.value,
    recurrenceEndDate: recurrence.value !== 'none' && recurrenceEndDate.value ? recurrenceEndDate.value : undefined,
  })
}
</script>

<template>
  <div class="schedule-event-form">
    <van-field
      v-model="title"
      :label="t('schedule.titleLabel')"
      :placeholder="t('schedule.titlePlaceholder')"
      maxlength="255"
      required
      clearable
    />
    <van-field
      :label="t('schedule.startAt')"
      :model-value="formatDateTimeText(startAt)"
      is-link
      :placeholder="t('schedule.selectDateTime')"
      @click="openStartPicker"
    />
    <van-field
      :label="t('schedule.endAt')"
      :model-value="formatDateTimeText(endAt)"
      is-link
      :placeholder="t('schedule.selectDateTime')"
      @click="openEndPicker"
    />
    <van-field
      v-model="note"
      :label="t('schedule.note')"
      type="textarea"
      :placeholder="t('schedule.notePlaceholder')"
      autosize
      clearable
    />
    <van-field :label="t('schedule.recurrence')">
      <template #input>
        <van-radio-group v-model="recurrence" direction="horizontal">
          <van-radio name="none">
            {{ t('schedule.recurrenceNone') }}
          </van-radio>
          <van-radio name="daily">
            {{ t('schedule.recurrenceDaily') }}
          </van-radio>
          <van-radio name="weekly">
            {{ t('schedule.recurrenceWeekly') }}
          </van-radio>
        </van-radio-group>
      </template>
    </van-field>
    <van-field
      v-if="recurrence !== 'none'"
      :label="t('schedule.recurrenceEndDate')"
      :model-value="recurrenceEndDate"
      is-link
      :placeholder="t('schedule.selectDate')"
      @click="showRecurrenceEnd = true"
    />
    <van-calendar v-model:show="showStartCalendar" @confirm="onDateConfirm" />
    <van-calendar v-model:show="showEndCalendar" @confirm="onDateConfirm" />
    <van-calendar v-model:show="showRecurrenceEnd" @confirm="onRecurrenceEndConfirm" />
    <van-popup v-model:show="showStartTime" position="bottom" round>
      <van-picker
        :title="t('schedule.selectTime')"
        :columns="timeColumns"
        @confirm="onTimeConfirm"
        @cancel="showStartTime = false"
      />
    </van-popup>
    <van-popup v-model:show="showEndTime" position="bottom" round>
      <van-picker
        :title="t('schedule.selectTime')"
        :columns="timeColumns"
        @confirm="onTimeConfirm"
        @cancel="showEndTime = false"
      />
    </van-popup>
    <div class="form-actions">
      <van-button round block type="primary" :loading="loading" @click="onSave">
        {{ t('schedule.save') }}
      </van-button>
    </div>
  </div>
</template>

<style scoped>
.schedule-event-form {
  padding: 16px 0;
}
.form-actions {
  padding: 16px;
}
</style>
