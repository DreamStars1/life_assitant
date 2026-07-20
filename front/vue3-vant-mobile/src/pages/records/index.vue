<script setup lang="ts">
import { showConfirmDialog, showToast } from 'vant'
import { useUserStore } from '@/stores'
import { createSharedMedia, deleteSharedMedia, fetchSharedMediaList, updateSharedMedia } from '@/api/modules/shared-media'
import type { SharedMediaItem } from '@/api/modules/shared-media'
import { useRouter } from 'vue-router'

const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)
const router = useRouter()

const partnerId = computed(() => (userInfo.value as any)?.partnerId || (userInfo.value as any)?.partner_id)

function toLocalDateStr(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

const mediaRecords = ref<SharedMediaItem[]>([])
const mediaPage = ref(1)
const mediaTotalPages = ref(0)
const mediaTypeFilter = ref('')
const mediaStatusFilter = ref('')

const showAddMedia = ref(false)
const addMediaForm = reactive({ title: '', mediaType: 'movie', description: '', lastWatchedAt: '', isPrivate: false })
const addMediaCoverList = ref<{ file?: File }[]>([])
const showAddLastWatchedCalendar = ref(false)
const showEditMedia = ref(false)
const editingMediaId = ref('')
const editMediaForm = reactive({ title: '', mediaType: 'movie', description: '', lastWatchedAt: '', isPrivate: false })
const showEditLastWatchedCalendar = ref(false)
const editMediaCoverList = ref<{ file?: File }[]>([])
const showMediaTypePicker = ref(false)
const showEditMediaTypePicker = ref(false)
const mediaTypeColumns = [
  { text: '电影', value: 'movie' },
  { text: '书籍', value: 'book' },
  { text: '漫剧', value: 'tv' },
]

const visibleMedia = computed(() =>
  mediaRecords.value.filter(
    item => !(item.isPrivate && item.createdBy !== userStore.userInfo.id),
  ),
)

function formatMediaTimeLine(item: SharedMediaItem): string {
  if (item.isPrivate) {
    if (item.lastWatchedAt)
      return `上次观看：${item.lastWatchedAt.slice(0, 10)}`
    return `更新于：${item.updateTime.slice(0, 10)}`
  }
  if (item.lastWatchedAt)
    return `上次一起看：${item.lastWatchedAt.slice(0, 10)}`
  return ''
}

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
    fd.append('lastWatchedAt', addMediaForm.lastWatchedAt || '')
    fd.append('isPrivate', String(addMediaForm.isPrivate))
    await createSharedMedia(fd)
    showToast('已添加')
    showAddMedia.value = false
    addMediaForm.title = ''
    addMediaForm.mediaType = 'movie'
    addMediaForm.description = ''
    addMediaForm.lastWatchedAt = toLocalDateStr(new Date())
    addMediaForm.isPrivate = false
    addMediaCoverList.value = []
    mediaPage.value = 1
    await loadMedia()
  }
  catch {
    showToast('添加失败')
  }
}

function openAddMedia() {
  addMediaForm.lastWatchedAt = toLocalDateStr(new Date())
  showAddMedia.value = true
}

function onAddLastWatchedConfirm(d: Date) {
  addMediaForm.lastWatchedAt = toLocalDateStr(d)
  showAddLastWatchedCalendar.value = false
}

function onEditLastWatchedConfirm(d: Date) {
  editMediaForm.lastWatchedAt = toLocalDateStr(d)
  showEditLastWatchedCalendar.value = false
}

function openEditMedia(item: SharedMediaItem) {
  editingMediaId.value = item.id
  editMediaForm.title = item.title
  editMediaForm.mediaType = item.mediaType
  editMediaForm.description = item.description || ''
  editMediaForm.lastWatchedAt = item.lastWatchedAt ? item.lastWatchedAt.slice(0, 10) : ''
  editMediaForm.isPrivate = item.isPrivate
  editMediaCoverList.value = []
  showEditMedia.value = true
}

async function onEditMedia() {
  if (!editMediaForm.title.trim()) {
    showToast('请输入名称')
    return
  }
  try {
    const fd = new FormData()
    fd.append('title', editMediaForm.title)
    fd.append('mediaType', editMediaForm.mediaType)
    fd.append('description', editMediaForm.description)
    if (editMediaCoverList.value[0]?.file)
      fd.append('cover', editMediaCoverList.value[0].file)
    fd.append('lastWatchedAt', editMediaForm.lastWatchedAt || '')
    fd.append('isPrivate', String(editMediaForm.isPrivate))
    await updateSharedMedia(editingMediaId.value, fd)
    showToast('已更新')
    showEditMedia.value = false
    await loadMedia()
  }
  catch {
    showToast('更新失败')
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
  return path
}

onMounted(async () => {
  await userStore.info()
  if (partnerId.value)
    await loadMedia()
})

watch(partnerId, async (val) => {
  if (val)
    await loadMedia()
})
</script>

<template>
  <div>
    <van-empty v-if="!partnerId" description="请先在「伴侣」页绑定伴侣后使用记录" />

    <template v-else>
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

      <div class="px-4 pt-3">
        <van-empty v-if="visibleMedia.length === 0" description="还没有一起看过的内容，点右下角 + 添加吧" />
        <van-swipe-cell v-for="item in visibleMedia" :key="item.id">
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
              <div v-if="formatMediaTimeLine(item)" class="text-xs text-gray-400 mt-1">
                {{ formatMediaTimeLine(item) }}
              </div>
            </div>
            <van-icon
              name="edit"
              size="18"
              class="media-edit-btn flex-shrink-0"
              @click.stop="openEditMedia(item)"
            />
          </div>
          <template #right>
            <van-button square type="primary" text="编辑" @click="openEditMedia(item)" />
            <van-button square type="danger" text="删除" @click="deleteMedia(item.id)" />
          </template>
        </van-swipe-cell>
      </div>

      <div v-if="mediaTotalPages > 0" class="px-4 py-3 flex gap-2 items-center justify-center">
        <van-button :disabled="mediaPage <= 1" size="small" plain @click="goToMediaPage(mediaPage - 1)">
          上一页
        </van-button>
        <span class="text-sm text-gray-500">第 {{ mediaPage }}/{{ mediaTotalPages }} 页</span>
        <van-button :disabled="mediaPage >= mediaTotalPages" size="small" plain @click="goToMediaPage(mediaPage + 1)">
          下一页
        </van-button>
      </div>

      <div class="mt-3 px-4">
        <van-button type="primary" round block icon="plus" @click="openAddMedia">
          添加看过的
        </van-button>
      </div>

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
          <van-field
            v-model="addMediaForm.lastWatchedAt"
            is-link
            readonly
            clearable
            label="上次一起看"
            placeholder="可选日期"
            @click="showAddLastWatchedCalendar = true"
            @clear="addMediaForm.lastWatchedAt = ''"
          />
          <van-calendar
            v-model:show="showAddLastWatchedCalendar"
            :min-date="new Date('2020-01-01')"
            @confirm="onAddLastWatchedConfirm"
          />
          <van-field label="仅自己可见">
            <template #input>
              <van-switch v-model="addMediaForm.isPrivate" size="20" />
            </template>
          </van-field>
          <div class="text-sm text-gray-500 mb-1">
            封面图
          </div>
          <van-uploader v-model="addMediaCoverList" accept="image/*" max-count="1" />
        </div>
      </van-dialog>

      <van-popup v-model:show="showMediaTypePicker" position="bottom">
        <van-picker
          :columns="mediaTypeColumns"
          @confirm="({ selectedOptions }: any) => { addMediaForm.mediaType = selectedOptions[0]?.value ?? 'movie'; showMediaTypePicker = false }"
          @cancel="showMediaTypePicker = false"
        />
      </van-popup>

      <van-dialog
        v-model:show="showEditMedia"
        title="编辑一起看过的"
        show-cancel-button
        @confirm="onEditMedia"
      >
        <div class="px-4 py-3 space-y-3">
          <van-field v-model="editMediaForm.title" placeholder="名称" clearable />
          <van-field
            :model-value="formatMediaType(editMediaForm.mediaType)"
            is-link
            readonly
            placeholder="选择类型"
            label="类型"
            @click="showEditMediaTypePicker = true"
          />
          <van-field v-model="editMediaForm.description" placeholder="简介（可选）" type="textarea" :rows="2" autosize />
          <van-field
            v-model="editMediaForm.lastWatchedAt"
            is-link
            readonly
            clearable
            label="上次一起看"
            placeholder="可选日期"
            @click="showEditLastWatchedCalendar = true"
            @clear="editMediaForm.lastWatchedAt = ''"
          />
          <van-calendar
            v-model:show="showEditLastWatchedCalendar"
            :min-date="new Date('2020-01-01')"
            @confirm="onEditLastWatchedConfirm"
          />
          <van-field label="仅自己可见">
            <template #input>
              <van-switch v-model="editMediaForm.isPrivate" size="20" />
            </template>
          </van-field>
          <div class="text-sm text-gray-500 mb-1">
            封面图（不选则保留原图）
          </div>
          <van-uploader v-model="editMediaCoverList" accept="image/*" max-count="1" />
        </div>
      </van-dialog>

      <van-popup v-model:show="showEditMediaTypePicker" position="bottom">
        <van-picker
          :columns="mediaTypeColumns"
          @confirm="({ selectedOptions }: any) => { editMediaForm.mediaType = selectedOptions[0]?.value ?? 'movie'; showEditMediaTypePicker = false }"
          @cancel="showEditMediaTypePicker = false"
        />
      </van-popup>
    </template>
  </div>
</template>

<style scoped>
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

.media-edit-btn {
  color: var(--van-gray-5);
  padding: 8px;
  margin-right: -8px;
}
</style>

<route lang="json5">
{
  name: 'Records'
}
</route>
