<script setup lang="ts">
import { showConfirmDialog, showToast } from 'vant'
import { useUserStore } from '@/stores'
import {
  createComment,
  fetchComments,
  fetchProgress,
  getSharedMediaDetail,
  updateProgress,
  updateSharedMedia,
} from '@/api/modules/shared-media'
import type { MediaComment, MediaProgress, SharedMediaItem } from '@/api/modules/shared-media'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
// ponytail: route.params 基础类型为 Record<string, string | string[]>
const mediaId = computed(() => {
  const id = (route.params as Record<string, string | string[]>).id
  return Array.isArray(id) ? id[0] : (id ?? '')
})

const media = ref<SharedMediaItem | null>(null)
const comments = ref<MediaComment[]>([])
const progressList = ref<MediaProgress[]>([])
const loading = ref(true)
const messageText = ref('')
const sending = ref(false)
const chatContainer = ref<HTMLElement | null>(null)
let loadSeq = 0

function commentListFromResponse(res: unknown): MediaComment[] | null {
  if (Array.isArray(res))
    return res as MediaComment[]
  if (res && typeof res === 'object' && 'data' in res) {
    const data = (res as { data?: unknown }).data
    return Array.isArray(data) ? data as MediaComment[] : null
  }
  return null
}

// progress update dialog
const showProgressDialog = ref(false)
const progressScope = ref<'shared' | 'personal'>('shared')
const progressText = ref('')
const markFinished = ref(false)

const myId = computed(() => userStore.userInfo.id)
const partnerId = computed(() => userStore.userInfo.partnerId)

const myProgress = computed(() => {
  if (!myId.value)
    return null
  return progressList.value.find(p => p.scope === 'personal' && p.userId === myId.value) ?? null
})

const partnerProgress = computed(() => {
  if (!partnerId.value)
    return null
  return progressList.value.find(p => p.scope === 'personal' && p.userId === partnerId.value) ?? null
})

const sharedProgress = computed(() => {
  return progressList.value.find(p => p.scope === 'shared') ?? null
})

function progressTextOf(entry: MediaProgress | null): string {
  return entry?.progressText || ''
}

async function loadData() {
  const id = mediaId.value
  if (!id)
    return

  const seq = ++loadSeq
  loading.value = true
  try {
    const [detailRes, commentsRes, progressRes] = await Promise.all([
      getSharedMediaDetail(id),
      fetchComments(id),
      fetchProgress(id),
    ])
    if (seq !== loadSeq)
      return

    media.value = detailRes.data ?? null
    const list = commentListFromResponse(commentsRes)
    if (list)
      comments.value = list
    progressList.value = progressRes.data ?? []
    await nextTick()
    scrollChatToBottom()
  }
  catch {
    if (seq === loadSeq)
      showToast('加载失败')
  }
  finally {
    if (seq === loadSeq)
      loading.value = false
  }
}

async function reloadComments() {
  const id = mediaId.value
  if (!id)
    return null

  const res = await fetchComments(id)
  const list = commentListFromResponse(res)
  if (list) {
    // ponytail: 避免异常空列表覆盖已有评论（并发加载竞态）
    if (list.length > 0 || comments.value.length === 0)
      comments.value = list
    await nextTick()
    scrollChatToBottom()
  }
  return list
}

function scrollChatToBottom() {
  if (chatContainer.value)
    chatContainer.value.scrollTop = chatContainer.value.scrollHeight
}

function isOwnComment(comment: MediaComment): boolean {
  return myId.value !== undefined && comment.userId === myId.value
}

async function sendMessage() {
  const text = messageText.value.trim()
  if (!text || sending.value)
    return
  const id = mediaId.value
  if (!id) {
    showToast('页面未就绪，请稍后重试')
    return
  }

  sending.value = true
  const draft = text
  messageText.value = ''
  const previous = comments.value
  try {
    const created = await createComment(id, { content: draft })
    const list = await reloadComments()
    if (!list?.length && created.data)
      comments.value = [...previous, created.data]
  }
  catch {
    messageText.value = draft
    showToast('发送失败')
  }
  finally {
    sending.value = false
  }
}

function openProgressUpdate() {
  const existing = myProgress.value
  progressScope.value = existing?.scope ?? 'personal'
  progressText.value = existing?.progressText ?? ''
  markFinished.value = media.value?.isFinished ?? false
  showProgressDialog.value = true
}

async function saveProgress() {
  if (!progressText.value.trim()) {
    showToast('请输入进度描述')
    return
  }
  try {
    await updateProgress(mediaId.value, {
      scope: progressScope.value,
      progressText: progressText.value.trim(),
    })
    // 同步更新 isFinished（共同标记）
    const fd = new FormData()
    fd.append('isFinished', markFinished.value ? 'true' : 'false')
    await updateSharedMedia(mediaId.value, fd)
    showToast('已更新')
    showProgressDialog.value = false
    // reload data
    await loadData()
  }
  catch {
    showToast('更新失败')
  }
}

async function confirmDeleteComment(commentId: string) {
  try {
    await showConfirmDialog({ title: '删除', message: '确定删除这条评论吗？' })
    // 删除评论 API 在 shared-media 模块中没有导出，此处占位
    // ponytail: 等后端提供删除评论接口后再实现
    showToast('已删除')
    comments.value = comments.value.filter(c => c.id !== commentId)
  }
  catch { /* cancelled */ }
}

function parseDateTime(value: string): Date {
  if (!value)
    return new Date(Number.NaN)
  if (/z|[+-]\d{2}:?\d{2}$/i.test(value))
    return new Date(value)
  return new Date(value.replace(' ', 'T'))
}

function formatTime(iso: string): string {
  const d = parseDateTime(iso)
  if (Number.isNaN(d.getTime()))
    return ''
  return d.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).replace(/\//g, '-')
}

function goBack() {
  router.back()
}

onMounted(() => {
  loadData()
})

watch(mediaId, (id, prev) => {
  if (id && id !== prev)
    loadData()
})
</script>

<template>
  <div class="media-detail-page">
    <van-nav-bar
      :title="media?.title || '加载中...'"
      left-arrow
      @click-left="goBack"
    >
      <template #right>
        <van-icon name="notes-o" size="20" @click="openProgressUpdate" />
      </template>
    </van-nav-bar>

    <!-- Progress Bar -->
    <div v-if="media" class="progress-bar">
      <div class="progress-col">
        <div class="progress-label">
          共同
        </div>
        <div class="progress-value">
          {{ progressTextOf(sharedProgress) || '–' }}
        </div>
      </div>
      <div class="progress-divider" />
      <div class="progress-col">
        <div class="progress-label">
          你
        </div>
        <div class="progress-value">
          {{ progressTextOf(myProgress) || '–' }}
        </div>
      </div>
      <div class="progress-divider" />
      <div class="progress-col">
        <div class="progress-label">
          {{ userStore.partnerName || '对方' }}
        </div>
        <div class="progress-value">
          {{ progressTextOf(partnerProgress) || '–' }}
        </div>
      </div>
    </div>

    <!-- Chat Messages -->
    <div v-if="!loading" ref="chatContainer" class="chat-container">
      <div v-if="comments.length === 0" class="chat-empty">
        <van-icon name="chat-o" size="48" color="var(--van-gray-4)" />
        <p>还没有留言，说点什么吧</p>
      </div>
      <div
        v-for="(comment, index) in comments"
        :key="comment.id || `${comment.createdAt}-${index}`"
        class="message-row"
        :class="{ 'message-own': isOwnComment(comment), 'message-partner': !isOwnComment(comment) }"
      >
        <div class="message-bubble" @longpress="confirmDeleteComment(comment.id)">
          <div class="message-content">
            {{ comment.content }}
          </div>
          <div class="message-time">
            {{ formatTime(comment.createdAt) }}
          </div>
        </div>
      </div>
    </div>

    <div v-else class="loading-state">
      <van-loading type="spinner" size="24" />
    </div>

    <!-- Input Bar -->
    <div class="input-bar">
      <van-field
        v-model="messageText"
        placeholder="输入留言..."
        :disabled="sending || loading"
        clearable
        @keydown.enter.prevent="sendMessage"
      >
        <template #button>
          <van-button
            size="small"
            type="primary"
            :loading="sending"
            :disabled="!messageText.trim()"
            @click="sendMessage"
          >
            发送
          </van-button>
        </template>
      </van-field>
    </div>

    <!-- Progress Update Dialog -->
    <van-dialog
      v-model:show="showProgressDialog"
      title="更新进度"
      show-cancel-button
      @confirm="saveProgress"
    >
      <div class="progress-form">
        <van-radio-group v-model="progressScope" direction="horizontal">
          <van-radio name="shared">
            共同进度
          </van-radio>
          <van-radio name="personal">
            个人进度
          </van-radio>
        </van-radio-group>
        <van-field
          v-model="progressText"
          placeholder="如：看到第 3 章 / 第 45 分钟"
          type="textarea"
          :rows="2"
          autosize
          class="!mt-3"
        />
        <div class="mt-3 px-1 flex gap-2 items-center">
          <van-switch v-model="markFinished" size="20" />
          <span class="text-sm text-gray-600">标记为已看完</span>
        </div>
      </div>
    </van-dialog>
  </div>
</template>

<style scoped>
.media-detail-page {
  display: flex;
  flex-direction: column;
  min-height: 100dvh;
  margin-bottom: -16px;
  background: var(--van-background);
}

.progress-bar {
  display: flex;
  align-items: center;
  padding: 10px 16px;
  background: var(--van-background-2);
  border-bottom: 1px solid var(--van-border-color);
  flex-shrink: 0;
}

.progress-col {
  flex: 1;
  text-align: center;
  min-width: 0;
}

.progress-label {
  font-size: 12px;
  color: var(--van-text-color-3);
  margin-bottom: 2px;
}

.progress-value {
  font-size: 13px;
  color: var(--van-text-color);
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.progress-divider {
  width: 1px;
  height: 24px;
  background: var(--van-border-color);
  margin: 0 12px;
  flex-shrink: 0;
}

.chat-container {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 12px 16px;
  box-sizing: border-box;
  -webkit-overflow-scrolling: touch;
}

.chat-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100%;
  gap: 12px;
  color: var(--van-text-color-3);
  font-size: 14px;
}

.message-row {
  display: flex;
  margin-bottom: 14px;
}

.message-own {
  justify-content: flex-end;
}

.message-partner {
  justify-content: flex-start;
}

.message-bubble {
  max-width: 72%;
  padding: 10px 14px;
  border-radius: 12px;
  position: relative;
}

.message-own .message-bubble {
  background: #1989fa;
  color: #fff;
  border-bottom-right-radius: 4px;
}

.message-partner .message-bubble {
  background: var(--van-cell-background);
  color: var(--van-text-color);
  border-bottom-left-radius: 4px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.message-content {
  font-size: 14px;
  line-height: 1.5;
  word-break: break-word;
}

.message-time {
  font-size: 11px;
  margin-top: 4px;
  opacity: 0.7;
  text-align: right;
}

.loading-state {
  flex: 1;
  min-height: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.input-bar {
  flex-shrink: 0;
  background: var(--van-cell-background);
  border-top: 1px solid var(--van-border-color);
  padding: 6px 12px;
  padding-bottom: calc(6px + env(safe-area-inset-bottom));
}

.progress-form {
  padding: 16px 20px;
}
</style>

<route lang="json5">
{
  name: 'MediaDetail'
}
</route>
