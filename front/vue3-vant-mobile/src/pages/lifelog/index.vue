<script setup lang="ts">
import { showNotify } from 'vant'
import { fetchLifeLogs, createLifeLog } from '@/api/modules/lifelogs'
import type { LifeLogItem } from '@/api/modules/lifelogs'

const { t } = useI18n()
const logs = ref<LifeLogItem[]>([])
const loading = ref(false)
const showAdd = ref(false)
const logTypes = ['diet', 'exercise', 'work', 'mood', 'sleep']
const activeType = ref<string | undefined>(undefined)
const newLog = reactive({
  log_type: 'diet',
  content: '',
})

async function loadLogs() {
  loading.value = true
  try {
    const res = await fetchLifeLogs({ log_type: activeType.value })
    logs.value = Array.isArray(res) ? res : []
  } finally {
    loading.value = false
  }
}

async function onSave() {
  await createLifeLog({ log_type: newLog.log_type, content: newLog.content })
  showAdd.value = false
  newLog.log_type = 'diet'
  newLog.content = ''
  showNotify({ type: 'success', message: '已保存' })
  await loadLogs()
}

function formatTime(iso?: string | null) {
  if (!iso) return ''
  return new Date(iso).toLocaleString('zh-CN', {
    month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit',
  })
}

const typeLabels: Record<string, string> = {
  diet: '饮食',
  exercise: '运动',
  work: '工作',
  mood: '心情',
  sleep: '睡眠',
}

watch(activeType, loadLogs)
onMounted(loadLogs)
</script>

<template>
  <div>
    <!-- Type tabs -->
    <van-tabs v-model:active="activeType" @change="loadLogs">
      <van-tab v-for="t in logTypes" :key="t" :title="typeLabels[t] || t" :name="t" />
      <van-tab :title="t('lifelog.total')" :name="undefined" />
    </van-tabs>

    <div class="header-bar">
      <van-button icon="plus" type="primary" size="small" @click="showAdd = true">
        {{ $t('lifelog.add') }}
      </van-button>
    </div>

    <van-pull-refresh v-model="loading" @refresh="loadLogs">
      <van-cell-group :border="false">
        <van-cell v-for="log in logs" :key="log.id">
          <template #title>
            <van-tag :size="'mini' as any">{{ log.log_type }}</van-tag>
            {{ log.content }}
          </template>
          <template #label>
            {{ formatTime(log.logged_at || log.created_at) }}
          </template>
        </van-cell>
      </van-cell-group>
    </van-pull-refresh>

    <van-dialog v-model:show="showAdd" :title="$t('lifelog.add')" show-cancel-button @confirm="onSave">
      <van-form>
        <van-field name="log_type" label="类型">
          <template #input>
            <van-radio-group v-model="newLog.log_type" direction="horizontal">
              <van-radio v-for="t in logTypes" :key="t" :name="t">{{ typeLabels[t] || t }}</van-radio>
            </van-radio-group>
          </template>
        </van-field>
        <van-field v-model="newLog.content" label="内容" type="textarea" :autosize="{ minHeight: 60 }" />
      </van-form>
    </van-dialog>
  </div>
</template>

<style scoped>
.header-bar {
  display: flex;
  justify-content: flex-end;
  padding: 8px 16px;
}
</style>

<route lang="json5">
{
  name: 'LifeLog'
}
</route>
