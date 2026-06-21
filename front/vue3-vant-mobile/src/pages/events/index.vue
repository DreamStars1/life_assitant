<script setup lang="ts">
import { showNotify } from 'vant'
import { fetchEvents, createEvent, updateEvent } from '@/api/modules/events'
import { fetchLifeLogs } from '@/api/modules/lifelogs'
import type { EventItem } from '@/api/modules/events'
import type { LifeLogItem } from '@/api/modules/lifelogs'
import MonthGrid from '@/components/calendar/MonthGrid.vue'

const { t } = useI18n()
const events = ref<EventItem[]>([])
const lifelogs = ref<LifeLogItem[]>([])
const loading = ref(false)
const showAdd = ref(false)
const editingEvent = ref<Partial<EventItem>>({})
const viewMode = ref<'plan' | 'actual'>('plan')
const currentDate = ref(new Date())

const year = computed(() => currentDate.value.getFullYear())
const month = computed(() => currentDate.value.getMonth() + 1)
const monthLabel = computed(() => `${year.value}年${month.value}月`)

async function loadData() {
  loading.value = true
  try {
    if (viewMode.value === 'plan') {
      const res = await fetchEvents()
      events.value = Array.isArray(res) ? res : []
    } else {
      const res = await fetchLifeLogs()
      lifelogs.value = Array.isArray(res) ? res : []
    }
  } finally {
    loading.value = false
  }
}

function prev() { currentDate.value = new Date(year.value, month.value - 2, 1) }
function next() { currentDate.value = new Date(year.value, month.value, 1) }
async function goToday() { currentDate.value = new Date() }

async function onSave() {
  if (editingEvent.value.id) {
    await updateEvent(editingEvent.value.id, editingEvent.value)
  } else {
    await createEvent(editingEvent.value)
  }
  showAdd.value = false
  editingEvent.value = {}
  showNotify({ type: 'success', message: '已保存' })
  await loadData()
}

function openNew() {
  editingEvent.value = { shared_with_partner: false }
  showAdd.value = true
}

function onDayClick(date: string) {
  editingEvent.value = { shared_with_partner: false, start_time: date }
  showAdd.value = true
}

const typeIcons: Record<string, string> = {
  diet: '👨‍🍳', exercise: '🏃', work: '💼', mood: '😊', sleep: '😴',
}

watch(viewMode, loadData)
onMounted(loadData)
</script>

<template>
  <div>
    <!-- Header -->
    <div class="cal-header">
      <van-icon name="arrow-left" @click="prev" />
      <span class="month-title" @click="goToday">{{ monthLabel }}</span>
      <van-icon name="arrow" @click="next" />
    </div>

    <!-- View Mode + Toggle -->
    <div class="view-bar">
      <div class="view-toggle">
        <van-button
          :type="viewMode === 'plan' ? 'primary' : 'default'"
          size="mini" plain @click="viewMode = 'plan'"
        >{{ t('events.month') }}</van-button>
        <van-button
          :type="viewMode === 'actual' ? 'primary' : 'default'"
          size="mini" plain @click="viewMode = 'actual'"
        >实际</van-button>
      </div>
      <van-button icon="plus" type="primary" size="small" @click="openNew">
        {{ t('events.add') }}
      </van-button>
    </div>

    <!-- Calendar Body -->
    <div class="cal-body">
      <MonthGrid
        :year="year" :month="month"
        :events="viewMode === 'plan' ? events : []"
        :lifelogs="viewMode === 'actual' ? lifelogs : []"
        :today="`${year}-${String(month).padStart(2,'0')}-${String(new Date().getDate()).padStart(2,'0')}`"
        :show-plan="viewMode === 'plan'"
        :type-icons="typeIcons"
        @click-day="onDayClick"
      />
    </div>

    <!-- Add/Edit Dialog -->
    <van-dialog v-if="viewMode === 'plan'"
      v-model:show="showAdd"
      :title="editingEvent.id ? '编辑日程' : '新建日程'"
      show-cancel-button @confirm="onSave"
    >
      <van-form>
        <van-field v-model="editingEvent.title" label="标题" required />
        <van-field v-model="editingEvent.category" label="分类" />
        <van-field name="shared_with_partner" label="共享给伴侣">
          <template #input>
            <van-switch v-model="editingEvent.shared_with_partner" />
          </template>
        </van-field>
      </van-form>
    </van-dialog>
  </div>
</template>

<style scoped>
.cal-header { display: flex; align-items: center; justify-content: center; gap: 16px; padding: 12px 16px; font-size: 16px; font-weight: 600; color: var(--van-text-color); }
.month-title { cursor: pointer; min-width: 120px; text-align: center; }
.view-bar { display: flex; gap: 8px; padding: 0 16px 8px; align-items: center; }
.view-toggle { display: flex; gap: 4px; }
.view-bar .van-button:last-child { margin-left: auto; }
.cal-body { padding: 0 8px 16px; }
</style>

<route lang="json5">
{
  name: 'Events'
}
</route>
