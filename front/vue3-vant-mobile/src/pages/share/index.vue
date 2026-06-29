<script setup lang="ts">
import { showNotify, showToast } from 'vant'
import { useUserStore } from '@/stores'
import request from '@/utils/request'
import { fetchSharedRecords, createSharedRecord, updateSharedRecord, deleteSharedRecord } from '@/api/modules/shared-records'
import type { SharedRecordItem } from '@/api/modules/shared-records'

const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)

// ---- 伴侣邀请/绑定 ----
const inviteToken = ref('')
const loading = ref(false)
const bindCode = ref('')

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
  try {
    await navigator.clipboard.writeText(inviteToken.value)
    showToast('已复制邀请码，发给你的伴侣吧')
  } catch {
    const ta = document.createElement('textarea')
    ta.value = inviteToken.value
    ta.style.position = 'fixed'
    ta.style.left = '-9999px'
    document.body.appendChild(ta)
    ta.select()
    try {
      document.execCommand('copy')
      showToast('已复制邀请码，发给你的伴侣吧')
    } catch {
      showNotify({ type: 'danger', message: '复制失败，请长按选中后手动复制' })
    } finally {
      document.body.removeChild(ta)
    }
  }
}

async function bindPartner() {
  if (!bindCode.value)
    return
  loading.value = true
  try {
    await request.post('/identity/bind-partner', { inviteToken: bindCode.value })
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

// ---- 分页状态 ----
const records = ref<SharedRecordItem[]>([])
const currentPage = ref(1)
const hasMore = ref(true)
const pageSize = ref(5)
const listLoading = ref(false)
const showPageSize = ref(false)
const searchKeyword = ref('')

// ---- 时间筛选 ----
const filterStart = ref('')
const filterEnd = ref('')
const showFilterStart = ref(false)
const showFilterEnd = ref(false)

// ---- 卡片编辑 ----
const editingRecordId = ref<string | null>(null)
const editForm = reactive({ title: '', content: '', occurredAt: '' })
const showCalendar = ref(false)

// ---- 新建表单 ----
const showCreateForm = ref(false)
const createForm = reactive({ title: '', content: '', occurredAt: '' })
const showCreateCalendar = ref(false)

async function loadRecords(reset = false) {
  if (reset) {
    listLoading.value = true
    currentPage.value = 1
    records.value = []
    hasMore.value = true
  }
  if (!hasMore.value) {
    if (reset) listLoading.value = false
    return
  }
  try {
    const params: Record<string, unknown> = { page: currentPage.value, size: pageSize.value }
    if (searchKeyword.value.trim()) params.keyword = searchKeyword.value.trim()
    if (filterStart.value) params.start = filterStart.value
    if (filterEnd.value) params.end = filterEnd.value
    const res = await fetchSharedRecords(params)
    const data = res.data ?? { records: [] as SharedRecordItem[], pages: 0 }
    records.value.push(...data.records)
    hasMore.value = currentPage.value < data.pages
    currentPage.value++
  } catch {
    showToast('加载失败')
  } finally {
    if (reset) listLoading.value = false
  }
}

function onLoadMore() {
  listLoading.value = true
  loadRecords(false).finally(() => { listLoading.value = false })
}

async function changePageSize(size: number) {
  showPageSize.value = false
  pageSize.value = size
  await loadRecords(true)
}

async function onSearch() {
  await loadRecords(true)
}

async function onFilterStartConfirm(d: Date) {
  filterStart.value = toLocalDateStr(d)
  showFilterStart.value = false
  await loadRecords(true)
}

async function onFilterEndConfirm(d: Date) {
  filterEnd.value = toLocalDateStr(d)
  showFilterEnd.value = false
  await loadRecords(true)
}

async function clearFilter() {
  filterStart.value = ''
  filterEnd.value = ''
  await loadRecords(true)
}

function onCalendarConfirm(d: Date) {
  editForm.occurredAt = toLocalDateStr(d)
  showCalendar.value = false
}

function onCreateCalendarConfirm(d: Date) {
  createForm.occurredAt = toLocalDateStr(d)
  showCreateCalendar.value = false
}

// ---- 编辑 ----
function openEdit(r: SharedRecordItem) {
  cancelCreate()
  editingRecordId.value = r.id
  editForm.title = r.title
  editForm.content = r.content || ''
  editForm.occurredAt = r.occurredAt ? r.occurredAt.slice(0, 10) : ''
}

function cancelEdit() {
  editingRecordId.value = null
}

async function saveEdit(r: SharedRecordItem) {
  if (!editForm.title.trim()) { showToast('请输入标题'); return }
  try {
    await updateSharedRecord(r.id, {
      title: editForm.title,
      content: editForm.content || undefined,
      occurredAt: editForm.occurredAt ? `${editForm.occurredAt}T00:00:00` : undefined,
    })
    showToast('已更新')
    await loadRecords(true)
  } catch {
    showToast('更新失败')
  } finally {
    editingRecordId.value = null
  }
}

// ---- 新增 ----
function openCreate() {
  cancelEdit()
  showCreateForm.value = true
  createForm.title = ''
  createForm.content = ''
  createForm.occurredAt = toLocalDateStr(new Date())
}

function cancelCreate() {
  showCreateForm.value = false
}

async function saveCreate() {
  if (!createForm.title.trim()) { showToast('请输入标题'); return }
  try {
    await createSharedRecord({
      title: createForm.title,
      content: createForm.content || undefined,
      occurredAt: createForm.occurredAt ? `${createForm.occurredAt}T00:00:00` : undefined,
    })
    showToast('记录已添加')
    showCreateForm.value = false
    await loadRecords(true)
  } catch {
    showToast('添加失败')
  }
}

// ---- 删除 ----
async function deleteRecord(id: string) {
  try {
    await deleteSharedRecord(id)
    showToast('已删除')
    await loadRecords(true)
  } catch {
    showToast('删除失败')
  }
}

function formatDate(d: string): string {
  if (!d) return ''
  const parts = d.slice(0, 10).split('-').map(Number)
  if (parts.length !== 3 || parts.some(isNaN)) return d.slice(0, 10)
  const [y, m, day] = parts
  return new Date(y, m - 1, day).toLocaleDateString('zh-CN')
}

const partnerId = computed(() => (userInfo.value as any)?.partnerId || (userInfo.value as any)?.partner_id)
const hasRecords = computed(() => records.value.length > 0)

onMounted(async () => {
  await userStore.info()
  if (partnerId.value) {
    listLoading.value = true
    await loadRecords(true)
    listLoading.value = false
  }
})

watch(partnerId, async (val) => {
  if (val) {
    listLoading.value = true
    await loadRecords(true)
    listLoading.value = false
  }
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
          @clear="onSearch"
          @search="onSearch"
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

      <!-- 分页列表 -->
      <van-list
        v-model:loading="listLoading"
        :finished="!hasMore"
        @load="onLoadMore"
      >
        <van-cell-group :inset="true" title="一起做过的事">
          <van-empty v-if="!hasRecords" description="还没有记录，添加第一条吧" />
          <van-swipe-cell v-for="r in records" :key="r.id">
            <!-- 查看模式 -->
            <template v-if="editingRecordId !== r.id">
              <van-cell
                :title="r.title"
                :label="r.content || ''"
                :value="formatDate(r.occurredAt)"
                is-link
                @click="openEdit(r)"
              />
            </template>
            <!-- 编辑模式：卡片原地展开 -->
            <template v-else>
              <div class="edit-card p-3">
                <van-field v-model="editForm.title" placeholder="标题" class="!mb-2" />
                <van-field v-model="editForm.content" placeholder="详细描述（可选）" class="!mb-2" />
                <van-field v-model="editForm.occurredAt" is-link readonly placeholder="日期（可选）" @click="showCalendar = true" />
                <div class="flex gap-2">
                  <van-button size="small" type="primary" @click="saveEdit(r)">保存</van-button>
                  <van-button size="small" @click="cancelEdit">取消</van-button>
                </div>
              </div>
            </template>
            <template #right>
              <van-button square type="danger" text="删除" @click="deleteRecord(r.id)" />
            </template>
          </van-swipe-cell>
        </van-cell-group>

        <template #finished>
          <div class="list-footer">
            <span>没有更多了</span>
            <span class="page-size-trigger" @click="showPageSize = true">每页 {{ pageSize }} 条 <van-icon name="arrow-down" /></span>
          </div>
        </template>
      </van-list>

      <van-calendar v-model:show="showCalendar" :min-date="new Date('2020-01-01')" @confirm="onCalendarConfirm" />

      <!-- 新建表单 -->
      <div class="px-4 mt-3">
        <van-button v-if="!showCreateForm" type="primary" round block @click="openCreate">
          添加一起做过的事
        </van-button>
        <div v-else class="p-3 rounded-lg bg-white">
          <van-field v-model="createForm.title" placeholder="标题（如：一起看了电影）" class="!mb-2" />
          <van-field v-model="createForm.content" placeholder="详细描述（可选）" class="!mb-2" />
          <van-field v-model="createForm.occurredAt" is-link readonly placeholder="日期（可选）" @click="showCreateCalendar = true" />
          <van-calendar v-model:show="showCreateCalendar" :min-date="new Date('2020-01-01')" @confirm="onCreateCalendarConfirm" />
          <div class="flex gap-2">
            <van-button type="primary" size="small" @click="saveCreate">保存</van-button>
            <van-button size="small" @click="cancelCreate">取消</van-button>
          </div>
        </div>
      </div>

      <!-- 页面大小切换 ActionSheet -->
      <van-action-sheet v-model:show="showPageSize" title="每页显示">
        <van-cell v-for="s in [3, 5, 10]" :key="s" :title="`${s} 条`" :label="s === 5 ? '推荐' : ''" is-link @click="changePageSize(s)" />
      </van-action-sheet>
    </template>
  </div>
</template>

<style scoped>
.page-size-trigger {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 12px;
  font-size: 12px;
  color: var(--van-gray-5);
  cursor: pointer;
}
.list-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px;
  font-size: 12px;
  color: var(--van-gray-5);
}
.list-footer .page-size-trigger {
  padding: 0;
  color: var(--van-blue);
}
</style>

<route lang="json5">
{
  name: 'Share'
}
</route>
