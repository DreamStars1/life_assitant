<script setup lang="ts">
import { showNotify, showToast } from 'vant'
import { useUserStore } from '@/stores'
import request from '@/utils/request'

const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)

const inviteToken = ref('')
const loading = ref(false)
const bindCode = ref('')

// ---- 伴侣邀请/绑定 ----
async function generateInvite() {
  loading.value = true
  try {
    const res = await request.post('/identity/invite')
    inviteToken.value = res.data?.invite_token || ''
    showNotify({ type: 'success', message: '邀请码已生成' })
  }
  finally {
    loading.value = false
  }
}

async function copyInvite() {
  // ponytail: navigator.clipboard 需要 HTTPS，HTTP 下用 textarea + execCommand 兜底
  const ta = document.createElement('textarea')
  ta.value = inviteToken.value
  ta.style.position = 'fixed'
  ta.style.left = '-9999px'
  document.body.appendChild(ta)
  ta.select()
  try {
    document.execCommand('copy')
    showToast('已复制邀请码，发给你的伴侣吧')
  }
  catch {
    showNotify({ type: 'danger', message: '复制失败，请长按选中后手动复制' })
  }
  finally {
    document.body.removeChild(ta)
  }
}

async function bindPartner() {
  if (!bindCode.value)
    return
  loading.value = true
  try {
    await request.post(`/identity/bind-partner?inviteToken=${encodeURIComponent(bindCode.value)}`)
    showNotify({ type: 'success', message: '伴侣已绑定！' })
    await userStore.info()
  }
  finally {
    loading.value = false
  }
}

function toLocalDateStr(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function apiDateToLocalStr(iso: string | null | undefined): string {
  if (!iso)
    return ''
  return iso.slice(0, 10)
}

// ---- ★一起做过的事 ----
const records = ref<any[]>([])
const showForm = ref(false)
const editingId = ref<string | null>(null)
const form = reactive({ title: '', content: '', occurredAt: '' })
const showCalendar = ref(false)
const searchKeyword = ref('')
const filteredRecords = computed(() => {
  const kw = searchKeyword.value.trim().toLowerCase()
  if (!kw)
    return records.value
  return records.value.filter(r =>
    (r.title && r.title.toLowerCase().includes(kw))
    || (r.content && r.content.toLowerCase().includes(kw)),
  )
})

// ---- 时间筛选 ----
const filterStart = ref('')
const filterEnd = ref('')
const showFilterStart = ref(false)
const showFilterEnd = ref(false)

function onFilterStartConfirm(d: Date) {
  filterStart.value = toLocalDateStr(d)
  showFilterStart.value = false
  loadRecords()
}
function onFilterEndConfirm(d: Date) {
  filterEnd.value = toLocalDateStr(d)
  showFilterEnd.value = false
  loadRecords()
}
function clearFilter() {
  filterStart.value = ''
  filterEnd.value = ''
  loadRecords()
}

function onDateConfirm(d: Date) {
  form.occurredAt = toLocalDateStr(d)
  showCalendar.value = false
}

async function loadRecords() {
  const params: any = {}
  if (filterStart.value)
    params.start = filterStart.value
  if (filterEnd.value)
    params.end = filterEnd.value
  const res = await request.get('/shared-records', { params })
  records.value = res.data || []
}

function openCreate() {
  editingId.value = null
  form.title = ''
  form.content = ''
  form.occurredAt = toLocalDateStr(new Date())
  showForm.value = true
}

function openEdit(r: any) {
  editingId.value = r.id
  form.title = r.title
  form.content = r.content || ''
  form.occurredAt = apiDateToLocalStr(r.occurredAt)
  showForm.value = true
}

function cancelForm() {
  showForm.value = false
  editingId.value = null
}

async function saveRecord() {
  if (!form.title)
    return
  if (editingId.value) {
    await request.patch(`/shared-records/${editingId.value}`, {
      title: form.title,
      content: form.content || undefined,
      occurredAt: form.occurredAt ? `${form.occurredAt}T00:00:00` : undefined,
    })
    showToast('已更新')
  }
  else {
    await request.post('/shared-records', {
      title: form.title,
      content: form.content || undefined,
      occurredAt: form.occurredAt ? `${form.occurredAt}T00:00:00` : undefined,
    })
    showToast('记录已添加')
  }
  form.title = ''
  form.content = ''
  form.occurredAt = ''
  showForm.value = false
  editingId.value = null
  await loadRecords()
}

async function deleteRecord(id: string) {
  await request.delete(`/shared-records/${id}`)
  showToast('已删除')
  await loadRecords()
}

function formatDate(d: string) {
  const dateStr = apiDateToLocalStr(d)
  if (!dateStr)
    return ''
  const [y, m, day] = dateStr.split('-').map(Number)
  return new Date(y, m - 1, day).toLocaleDateString('zh-CN')
}

const partnerId = computed(() => userInfo.value?.partnerId || (userInfo.value as any)?.partner_id)
const hasRecords = computed(() => records.value.length > 0)
const hasFilteredRecords = computed(() => filteredRecords.value.length > 0)

onMounted(async () => {
  await userStore.info()
  if (partnerId.value)
    loadRecords()
})

watch(partnerId, (val) => {
  if (val)
    loadRecords()
})
</script>

<template>
  <div>
    <!-- 未绑定伴侣时显示引导 -->
    <div v-if="!partnerId" class="p-4 space-y-3">
      <van-empty description="绑定伴侣后，一起记录做过的事" />

      <van-cell-group :inset="true" title="生成邀请码">
        <van-field v-model="inviteToken" placeholder="点击下方按钮生成" readonly :rows="2" autosize type="textarea" />
        <van-cell v-if="inviteToken" title="复制邀请码" icon="share-o" is-link @click="copyInvite" />
        <van-cell title="生成邀请码" icon="add-circle-o" is-link @click="generateInvite" />
      </van-cell-group>
      <van-cell-group :inset="true" title="输入伴侣邀请码">
        <van-field v-model="bindCode" placeholder="粘贴伴侣的邀请码">
          <template #button>
            <van-button size="small" type="success" @click="bindPartner">
              绑定
            </van-button>
          </template>
        </van-field>
      </van-cell-group>
    </div>

    <!-- ★一起做过的事（绑定后显示） -->
    <template v-if="partnerId">
      <!-- 搜索 + 时间筛选 -->
      <div class="px-4 pb-1 pt-3 space-y-2">
        <van-field
          v-model="searchKeyword"
          placeholder="搜索标题或内容"
          clearable
          left-icon="search"
          class="!p-0"
        />
        <div class="flex gap-2 items-center">
          <van-field v-model="filterStart" is-link readonly placeholder="开始日期" class="!p-0 !flex-1" @click="showFilterStart = true" />
          <span class="text-gray-400">—</span>
          <van-field v-model="filterEnd" is-link readonly placeholder="结束日期" class="!p-0 !flex-1" @click="showFilterEnd = true" />
          <van-button v-if="filterStart || filterEnd" size="small" plain @click="clearFilter">
            清除
          </van-button>
        </div>
      </div>
      <van-calendar v-model:show="showFilterStart" :min-date="new Date('2020-01-01')" @confirm="onFilterStartConfirm" />
      <van-calendar v-model:show="showFilterEnd" :min-date="new Date('2020-01-01')" @confirm="onFilterEndConfirm" />

      <van-cell-group :inset="true" title="一起做过的事">
        <van-empty v-if="!hasRecords" description="还没有记录，添加第一条吧" />
        <van-empty v-else-if="!hasFilteredRecords" description="没有匹配的记录" />
        <van-swipe-cell v-for="r in filteredRecords" :key="r.id">
          <van-cell :title="r.title" :label="r.content || ''" :value="formatDate(r.occurredAt)" is-link @click="openEdit(r)" />
          <template #right>
            <van-button square type="danger" text="删除" @click="deleteRecord(r.id)" />
          </template>
        </van-swipe-cell>
      </van-cell-group>

      <div class="px-4 !mt-3">
        <van-button v-if="!showForm" type="primary" round block @click="openCreate">
          添加一起做过的事
        </van-button>
        <div v-else class="p-3 rounded-lg bg-white">
          <van-field v-model="form.title" :placeholder="editingId ? '修改标题' : '标题（如：一起看了电影）'" class="!mb-2" />
          <van-field v-model="form.content" placeholder="详细描述（可选）" class="!mb-2" />
          <van-field v-model="form.occurredAt" is-link readonly placeholder="日期（可选）" @click="showCalendar = true" />
          <van-calendar v-model:show="showCalendar" :min-date="new Date('2020-01-01')" @confirm="onDateConfirm" />
          <div class="flex gap-2">
            <van-button type="primary" size="small" @click="saveRecord">
              {{ editingId ? '更新' : '保存' }}
            </van-button>
            <van-button size="small" @click="cancelForm">
              取消
            </van-button>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<route lang="json5">
{
  name: 'Share'
}
</route>
