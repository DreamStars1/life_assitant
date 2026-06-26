<script setup lang="ts">
const props = defineProps<{
  initial?: { title?: string, description?: string, priority?: string, dueDate?: string }
  showAssign?: boolean | string
  loading?: boolean
}>()

const emit = defineEmits<{
  save: [data: { title: string, description?: string, priority: string, dueDate?: string, assignedTo?: string }]
}>()

const showCalendar = ref(false)
const showTimePicker = ref(false)
const title = ref(props.initial?.title ?? '')
const description = ref(props.initial?.description ?? '')
const priority = ref(props.initial?.priority ?? 'medium')
const dueDate = ref(props.initial?.dueDate ?? '')
const assignedTo = ref('')

function splitDueDate(v: string) {
  const sep = v.includes('T') ? 'T' : v.includes(' ') ? ' ' : null
  if (!sep)
    return { date: v, time: '' }
  const [date, rest] = v.split(sep)
  return { date: date!, time: rest?.slice(0, 5) ?? '' }
}

function onDateConfirm(d: Date) {
  const pad = (n: number) => String(n).padStart(2, '0')
  const { time } = splitDueDate(dueDate.value)
  const t = time || '12:00'
  dueDate.value = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${t}:00`
  showCalendar.value = false
  showTimePicker.value = true
}

const timeColumns = [
  Array.from({ length: 24 }, (_, i) => ({ text: String(i).padStart(2, '0'), value: String(i).padStart(2, '0') })),
  Array.from({ length: 12 }, (_, i) => ({ text: String(i * 5).padStart(2, '0'), value: String(i * 5).padStart(2, '0') })),
]

function onTimeConfirm({ selectedValues }: { selectedValues: (string | number)[] }) {
  const h = String(selectedValues[0]!).padStart(2, '0')
  const m = String(selectedValues[1]!).padStart(2, '0')
  const { date } = splitDueDate(dueDate.value)
  const d = date || new Date().toISOString().slice(0, 10)
  dueDate.value = `${d}T${h}:${m}:00`
  showTimePicker.value = false
}

function onSave() {
  if (!title.value.trim())
    return
  emit('save', {
    title: title.value,
    description: description.value || undefined,
    priority: priority.value,
    dueDate: dueDate.value ? dueDate.value.replace(' ', 'T') : undefined,
    assignedTo: assignedTo.value || undefined,
  })
}

const dueDateText = computed(() => {
  if (!dueDate.value)
    return ''
  const { date, time } = splitDueDate(dueDate.value)
  return time ? `${date} ${time}:00` : date
})
</script>

<template>
  <div class="todo-form">
    <van-field v-model="title" label="标题" placeholder="请输入标题" maxlength="255" required clearable />
    <van-field v-model="description" label="描述" type="textarea" placeholder="可选描述" autosize clearable />
    <van-field label="优先级">
      <template #input>
        <van-radio-group v-model="priority" direction="horizontal">
          <van-radio name="low">
            低
          </van-radio>
          <van-radio name="medium">
            中
          </van-radio>
          <van-radio name="high">
            高
          </van-radio>
          <van-radio name="urgent">
            紧急
          </van-radio>
        </van-radio-group>
      </template>
    </van-field>
    <van-field label="日期" :model-value="dueDateText" is-link placeholder="选择日期时间" @click="showCalendar = true" />
    <van-calendar v-model:show="showCalendar" :min-date="new Date()" @confirm="onDateConfirm" />
    <van-popup v-model:show="showTimePicker" position="bottom" round>
      <van-picker title="选择时间" :columns="timeColumns" @confirm="onTimeConfirm" @cancel="showTimePicker = false" />
    </van-popup>
    <van-field v-if="showAssign" label="交给 TA">
      <template #input>
        <van-switch v-model="assignedTo" :active-value="showAssign" inactive-value="" />
      </template>
    </van-field>
    <div style="padding: 16px">
      <van-button round block type="primary" :loading="loading" @click="onSave">
        保存
      </van-button>
    </div>
  </div>
</template>

<style scoped>
.todo-form {
  padding: 16px 0;
}
</style>
