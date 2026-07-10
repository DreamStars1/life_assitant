<script setup lang="ts">
import { showConfirmDialog, showNotify, showToast } from 'vant'
import { useUserStore } from '@/stores'
import request from '@/utils/request'
import { createSharedRecord, deleteSharedRecord, fetchSharedRecords, updateSharedRecord } from '@/api/modules/shared-records'
import type { SharedRecordItem } from '@/api/modules/shared-records'
import { createSharedMedia, deleteSharedMedia, fetchSharedMediaList } from '@/api/modules/shared-media'
import type { SharedMediaItem } from '@/api/modules/shared-media'
import { useRouter } from 'vue-router'

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
  }
  catch {
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
const totalPages = ref(0)
const pageSize = ref(5)
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

// ---- 删除 ----
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

const partnerId = computed(() => (userInfo.value as any)?.partnerId || (userInfo.value as any)?.partner_id)
const hasRecords = computed(() => records.value.length > 0)

// ---- 共享媒体（一起看过的） ----
const router = useRouter()
const activeTab = ref<'records' | 'media'>('records')

const mediaRecords = ref<SharedMediaItem[]>([])
const mediaPage = ref(1)
const mediaTotalPages = ref(0)
const mediaTypeFilter = ref('')
const mediaStatusFilter = ref('')

const showAddMedia = ref(false)
const addMediaForm = reactive({ title: '', mediaType: 'movie', description: '' })
const addMediaCoverList = ref<{ file?: File }[]>([])
const showMediaTypePicker = ref(false)
const mediaTypeColumns = [
  { text: '电影', value: 'movie' },
  { text: '书籍', value: 'book' },
  { text: '漫剧', value: 'tv' },
]

async function loadMedia() {
  try {
    const params: Record<string, unknown> = { page: mediaPage.value, size: 5 }
    if (mediaTypeFilter.value)
      params.mediaType = mediaTypeFilter.value
    if (mediaStatusFilter.value === 'finished')
      params.status = 'finished'
    else if (mediaStatusFilter.value === 'unfinished')
      params.status = 'unfinished'
    const res = await fetchSharedMediaList(params)
    const data = res.data ?? ({ records: [] as SharedMediaItem[], pages: 0 } as any)
    mediaRecords.value = data.records
    mediaTotalPages.value = data.pages
  }
  catch {
    showToast('加载媒体失败')
  }
}

function onMediaTypeFilter(type: string) {
  mediaTypeFilter.value = type
  mediaPage.value = 1
  loadMedia()
}

function onMediaStatusFilter(status: string) {
  mediaStatusFilter.value = status
  mediaPage.value = 1
  loadMedia()
}

async function onAddMedia() {
  if (!addMediaForm.title.trim()) {
    showToast('请输入名称')
    return
  }
  try {
    const fd = new FormData()
    fd.append('title', addMediaForm.title)
    fd.append('mediaType', addMediaForm.mediaType)
    if (addMediaForm.description)
      fd.append('description', addMediaForm.description)
    if (addMediaCoverList.value[0]?.file)
      fd.append('cover', addMediaCoverList.value[0].file)
    await createSharedMedia(fd)
    showToast('已添加')
    showAddMedia.value = false
    addMediaForm.title = ''
    addMediaForm.mediaType = 'movie'
    addMediaForm.description = ''
    addMediaCoverList.value = []
    mediaPage.value = 1
    await loadMedia()
  }
  catch {
    showToast('添加失败')
  }
}

async function deleteMedia(id: string) {
  try {
    await showConfirmDialog({ title: '确认删除', message: '删除后将同时删除相关评论和进度记录，确定吗？' })
  }
  catch {
    return
  }
  try {
    await deleteSharedMedia(id)
    showToast('已删除')
    mediaPage.value = 1
    await loadMedia()
  }
  catch {
    showToast('删除失败')
  }
}

function goToMediaPage(page: number) {
  mediaPage.value = page
  loadMedia()
}

function formatMediaType(t: string): string {
  const map: Record<string, string> = { movie: '电影', book: '书籍', tv: '漫剧' }
  return map[t] || t
}

function formatFinishedDate(iso: string | null): string {
  if (!iso)
    return ''
  const d = new Date(iso)
  if (Number.isNaN(d.getTime()))
    return ''
  return d.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
}

function mediaCoverUrl(path: string | null): string {
  if (!path)
    return ''
  if (path.startsWith('http'))
    return path
  // 后端已返回以 / 开头的路径，直接使用
  return path
}

onMounted(async () => {
  await userStore.info()
  if (partnerId.value)
    goToPage(1)
})

watch(partnerId, async (val) => {
  if (val)
    goToPage(1)
})

watch(activeTab, (tab) => {
  if (tab === 'media' && mediaRecords.value.length === 0) {
    loadMedia()
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

    <!-- ★伴侣 Tab 栏（绑定后显示） -->
    <template v-if="partnerId">
      <van-tabs v-model:active="activeTab" sticky>
        <van-tab title="一起做过的事" name="records">
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

          <!-- 记录列表 -->
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

          <!-- 新建表单 -->
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

          <!-- 页面大小切换 ActionSheet -->
          <van-action-sheet v-model:show="showPageSize" title="每页显示">
            <van-cell v-for="s in [3, 5, 10]" :key="s" :title="`${s} 条`" :label="s === 5 ? '推荐' : ''" is-link @click="changePageSize(s)" />
          </van-action-sheet>
        </van-tab>

        <van-tab title="一起看过的" name="media">
          <!-- 媒体类型筛选 -->
          <div class="px-4 pt-3 flex flex-wrap gap-2">
            <van-tag
              v-for="t in [{ label: '全部', value: '' }, { label: '电影', value: 'movie' }, { label: '书籍', value: 'book' }, { label: '漫剧', value: 'tv' }]"
              :key="t.value"
              :type="mediaTypeFilter === t.value ? 'primary' : 'default'"
              size="medium"
              round
              @click="onMediaTypeFilter(t.value)"
            >
              {{ t.label }}
            </van-tag>
          </div>
          <!-- 状态筛选 -->
          <div class="px-4 pt-2 flex flex-wrap gap-2">
            <van-tag
              v-for="s in [{ label: '全部', value: '' }, { label: '没看完', value: 'unfinished' }, { label: '已看完', value: 'finished' }]"
              :key="s.value"
              :type="mediaStatusFilter === s.value ? 'primary' : 'default'"
              size="medium"
              round
              plain
              @click="onMediaStatusFilter(s.value)"
            >
              {{ s.label }}
            </van-tag>
          </div>

          <!-- 媒体列表 -->
          <div class="px-4 pt-3">
            <van-empty v-if="mediaRecords.length === 0" description="还没有一起看过的内容，点右下角 + 添加吧" />
            <van-swipe-cell v-for="item in mediaRecords" :key="item.id">
              <div
                class="media-card mb-2 p-3 rounded-lg bg-white flex items-center"
                @click="router.push(`/share/media/${item.id}`)"
              >
                <img
                  v-if="item.coverPath"
                  :src="mediaCoverUrl(item.coverPath)"
                  alt=""
                  class="media-cover flex-shrink-0"
                >
                <div v-else class="media-cover-placeholder">
                  <van-icon name="photo-o" size="24" />
                </div>
                <div class="ml-3 flex-1 min-w-0">
                  <div class="font-medium truncate">
                    {{ item.title }}
                  </div>
                  <div class="text-xs text-gray-500 mt-1">
                    {{ formatMediaType(item.mediaType) }}
                  </div>
                  <div class="text-xs mt-1">
                    <van-tag :type="item.isFinished ? 'success' : 'warning'">
                      {{ item.isFinished ? '已看完' : '还没看完' }}
                    </van-tag>
                    <span v-if="item.isFinished && item.finishedAt" class="text-xs text-gray-400 ml-1">
                      {{ formatFinishedDate(item.finishedAt) }}
                    </span>
                  </div>
                </div>
              </div>
              <template #right>
                <van-button square type="danger" text="删除" @click="deleteMedia(item.id)" />
              </template>
            </van-swipe-cell>
          </div>

          <!-- 分页 -->
          <div v-if="mediaTotalPages > 0" class="px-4 py-3 flex gap-2 items-center justify-center">
            <van-button :disabled="mediaPage <= 1" size="small" plain @click="goToMediaPage(mediaPage - 1)">
              上一页
            </van-button>
            <span class="text-sm text-gray-500">第 {{ mediaPage }}/{{ mediaTotalPages }} 页</span>
            <van-button :disabled="mediaPage >= mediaTotalPages" size="small" plain @click="goToMediaPage(mediaPage + 1)">
              下一页
            </van-button>
          </div>

          <!-- 添加按钮 -->
          <div class="mt-3 px-4">
            <van-button type="primary" round block icon="plus" @click="showAddMedia = true">
              添加看过的
            </van-button>
          </div>

          <!-- 添加弹窗 -->
          <van-dialog
            v-model:show="showAddMedia"
            title="添加一起看过的"
            show-cancel-button
            @confirm="onAddMedia"
          >
            <div class="px-4 py-3 space-y-3">
              <van-field v-model="addMediaForm.title" placeholder="名称（如：盗梦空间）" clearable />
              <van-field
                :model-value="formatMediaType(addMediaForm.mediaType)"
                is-link
                readonly
                placeholder="选择类型"
                label="类型"
                @click="showMediaTypePicker = true"
              />
              <van-field v-model="addMediaForm.description" placeholder="简介（可选）" type="textarea" :rows="2" autosize />
              <div class="text-sm text-gray-500 mb-1">
                封面图
              </div>
              <van-uploader v-model="addMediaCoverList" accept="image/*" max-count="1" />
            </div>
          </van-dialog>

          <!-- 媒体类型选择器 -->
          <van-popup v-model:show="showMediaTypePicker" position="bottom">
            <van-picker
              :columns="mediaTypeColumns"
              @confirm="({ selectedOptions }: any) => { addMediaForm.mediaType = selectedOptions[0]?.value ?? 'movie'; showMediaTypePicker = false }"
              @cancel="showMediaTypePicker = false"
            />
          </van-popup>
        </van-tab>
      </van-tabs>
    </template>
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

.media-card {
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  cursor: pointer;
}

.media-cover {
  width: 64px;
  height: 80px;
  object-fit: cover;
  border-radius: 6px;
}

.media-cover-placeholder {
  width: 64px;
  height: 80px;
  border-radius: 6px;
  background: var(--van-gray-2);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--van-gray-5);
}
</style>

<route lang="json5">
{
  name: 'Share'
}
</route>
