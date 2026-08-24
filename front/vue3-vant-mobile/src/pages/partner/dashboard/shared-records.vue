<script setup lang="ts">
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { createSharedRecord, deleteSharedRecord, fetchSharedRecords, updateSharedRecord } from '@/api/modules/shared-records'
import type { SharedRecordItem } from '@/api/modules/shared-records'

const router = useRouter()

function toLocalDateStr(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

const records = ref<SharedRecordItem[]>([])
const currentPage = ref(1)
const totalPages = ref(0)
const pageSize = ref(5)
const showPageSize = ref(false)
const searchKeyword = ref('')

const filterStart = ref('')
const filterEnd = ref('')
const showFilterStart = ref(false)
const showFilterEnd = ref(false)

const editingRecordId = ref<string | null>(null)
const editForm = reactive({ title: '', content: '', occurredAt: '' })
const showCalendar = ref(false)

const showCreateForm = ref(false)
const createForm = reactive({ title: '', content: '', occurredAt: '' })
const showCreateCalendar = ref(false)

async function loadRecords() {
  try {
    const params: Record<string, unknown> = { page: currentPage.value, size: pageSize.value }
    if (searchKeyword.value.trim())
      params.keyword = searchKeyword.value.trim()
    if (filterStart.value)
      params.start = filterStart.value
    if (filterEnd.value)
      params.end = filterEnd.value
    const res = await fetchSharedRecords(params)
    const data = res.data ?? { records: [] as SharedRecordItem[], pages: 0 }
    records.value = data.records
    totalPages.value = data.pages
  }
  catch {
    showToast('加载失败')
  }
}

async function goToPage(page: number) {
  currentPage.value = page
  await loadRecords()
}

async function changePageSize(size: number) {
  showPageSize.value = false
  pageSize.value = size
  await goToPage(1)
}

async function onSearch() {
  await goToPage(1)
}

async function onFilterStartConfirm(d: Date) {
  filterStart.value = toLocalDateStr(d)
  showFilterStart.value = false
  await goToPage(1)
}

async function onFilterEndConfirm(d: Date) {
  filterEnd.value = toLocalDateStr(d)
  showFilterEnd.value = false
  await goToPage(1)
}

async function clearFilter() {
  filterStart.value = ''
  filterEnd.value = ''
  await goToPage(1)
}

function onCalendarConfirm(d: Date) {
  editForm.occurredAt = toLocalDateStr(d)
  showCalendar.value = false
}

function onCreateCalendarConfirm(d: Date) {
  createForm.occurredAt = toLocalDateStr(d)
  showCreateCalendar.value = false
}

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
  if (!editForm.title.trim()) {
    showToast('请输入标题')
    return
  }
  try {
    await updateSharedRecord(r.id, {
      title: editForm.title,
      content: editForm.content || undefined,
      occurredAt: editForm.occurredAt ? `${editForm.occurredAt}T00:00:00` : undefined,
    })
    showToast('已更新')
    await goToPage(1)
  }
  catch {
    showToast('更新失败')
  }
  finally {
    editingRecordId.value = null
  }
}

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
  if (!createForm.title.trim()) {
    showToast('请输入标题')
    return
  }
  try {
    await createSharedRecord({
      title: createForm.title,
      content: createForm.content || undefined,
      occurredAt: createForm.occurredAt ? `${createForm.occurredAt}T00:00:00` : undefined,
    })
    showToast('记录已添加')
    showCreateForm.value = false
    await goToPage(1)
  }
  catch {
    showToast('添加失败')
  }
}

async function deleteRecord(id: string) {
  try {
    await deleteSharedRecord(id)
    showToast('已删除')
    await goToPage(1)
  }
  catch {
    showToast('删除失败')
  }
}

function formatDate(d: string): string {
  if (!d)
    return ''
  const parts = d.slice(0, 10).split('-').map(Number)
  if (parts.length !== 3 || parts.some(Number.isNaN))
    return d.slice(0, 10)
  const [y, m, day] = parts
  return new Date(y, m - 1, day).toLocaleDateString('zh-CN')
}

const hasRecords = computed(() => records.value.length > 0)

onMounted(() => loadRecords())
</script>

<template>
  <div>
    <van-nav-bar title="一起做过的事" left-arrow @click-left="router.back()" />

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

    <van-cell-group :inset="true" title="一起做过的事">
      <van-empty v-if="!hasRecords" description="还没有记录，添加第一条吧" />
      <van-swipe-cell v-for="r in records" :key="r.id">
        <template v-if="editingRecordId !== r.id">
          <van-cell
            :title="r.title"
            :label="r.content || ''"
            :value="formatDate(r.occurredAt)"
            is-link
            @click="openEdit(r)"
          />
        </template>
        <template v-else>
          <div class="edit-card p-3">
            <van-field v-model="editForm.title" placeholder="标题" class="!mb-2" />
            <van-field v-model="editForm.content" placeholder="详细描述（可选）" class="!mb-2" />
            <van-field v-model="editForm.occurredAt" is-link readonly placeholder="日期（可选）" @click="showCalendar = true" />
            <div class="flex gap-2">
              <van-button size="small" type="primary" @click="saveEdit(r)">
                保存
              </van-button>
              <van-button size="small" @click="cancelEdit">
                取消
              </van-button>
            </div>
          </div>
        </template>
        <template #right>
          <van-button square type="danger" text="删除" @click="deleteRecord(r.id)" />
        </template>
      </van-swipe-cell>
    </van-cell-group>

    <div v-if="totalPages > 0" class="pagination">
      <div class="pagination-inner">
        <van-button :disabled="currentPage <= 1" size="small" plain @click="goToPage(currentPage - 1)">
          上一页
        </van-button>
        <span class="page-info">第 {{ currentPage }}/{{ totalPages }} 页</span>
        <van-button :disabled="currentPage >= totalPages" size="small" plain @click="goToPage(currentPage + 1)">
          下一页
        </van-button>
        <span class="page-size-trigger" @click="showPageSize = true">每页 {{ pageSize }} 条 <van-icon name="arrow-down" /></span>
      </div>
    </div>

    <van-calendar v-model:show="showCalendar" :min-date="new Date('2020-01-01')" @confirm="onCalendarConfirm" />

    <div class="mt-3 px-4">
      <van-button v-if="!showCreateForm" type="primary" round block @click="openCreate">
        添加一起做过的事
      </van-button>
      <div v-else class="p-3 rounded-lg bg-white">
        <van-field v-model="createForm.title" placeholder="标题（如：一起看了电影）" class="!mb-2" />
        <van-field v-model="createForm.content" placeholder="详细描述（可选）" class="!mb-2" />
        <van-field v-model="createForm.occurredAt" is-link readonly placeholder="日期（可选）" @click="showCreateCalendar = true" />
        <van-calendar v-model:show="showCreateCalendar" :min-date="new Date('2020-01-01')" @confirm="onCreateCalendarConfirm" />
        <div class="flex gap-2">
          <van-button type="primary" size="small" @click="saveCreate">
            保存
          </van-button>
          <van-button size="small" @click="cancelCreate">
            取消
          </van-button>
        </div>
      </div>
    </div>

    <van-action-sheet v-model:show="showPageSize" title="每页显示">
      <van-cell v-for="s in [3, 5, 10]" :key="s" :title="`${s} 条`" :label="s === 5 ? '推荐' : ''" is-link @click="changePageSize(s)" />
    </van-action-sheet>
  </div>
</template>

<style scoped>
.pagination {
  display: flex;
  justify-content: center;
  padding: 16px;
}
.pagination-inner {
  display: flex;
  align-items: center;
  gap: 8px;
}
.page-info {
  font-size: 13px;
  color: var(--van-gray-6);
  white-space: nowrap;
}
.page-size-trigger {
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: 12px;
  color: var(--van-blue);
  cursor: pointer;
}
</style>

<route lang="json5">
{
  name: 'PartnerDashboardSharedRecords'
}
</route>
