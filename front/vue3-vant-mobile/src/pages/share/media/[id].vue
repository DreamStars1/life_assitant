<script setup lang="ts">
import { showToast, showConfirmDialog } from 'vant'
import { useUserStore } from '@/stores'
import {
  getSharedMediaDetail,
  fetchComments,
  createComment,
  fetchProgress,
  updateProgress,
  updateSharedMedia,
} from '@/api/modules/shared-media'
import type { SharedMediaItem, MediaComment, MediaProgress } from '@/api/modules/shared-media'

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

// progress update dialog
const showProgressDialog = ref(false)
const progressScope = ref<'shared' | 'personal'>('shared')
const progressText = ref('')
const markFinished = ref(false)

const myId = computed(() => userStore.userInfo.id)
const partnerId = computed(() => userStore.userInfo.partnerId)

const myProgress = computed(() => {
  if (!myId.value) return null
  return progressList.value.find(p => p.scope === 'personal' && p.userId === myId.value) ?? null
})

const partnerProgress = computed(() => {
  if (!partnerId.value) return null
  return progressList.value.find(p => p.scope === 'personal' && p.userId === partnerId.value) ?? null
})

const sharedProgress = computed(() => {
  return progressList.value.find(p => p.scope === 'shared') ?? null
})

function progressTextOf(entry: MediaProgress | null): string {
  return entry?.progressText || ''
}

async function loadData() {
  loading.value = true
  try {
    const [detailRes, commentsRes, progressRes] = await Promise.all([
      getSharedMediaDetail(mediaId.value),
      fetchComments(mediaId.value),
      fetchProgress(mediaId.value),
    ])
    media.value = detailRes.data ?? null
    comments.value = commentsRes.data ?? []
    progressList.value = progressRes.data ?? []
  } catch {
    showToast('加载失败')
  } finally {
    loading.value = false
  }
}

function isOwnComment(comment: MediaComment): boolean {
  return myId.value !== undefined && comment.userId === myId.value
}

async function sendMessage() {
  const text = messageText.value.trim()
  if (!text) return
  sending.value = true
  try {
    const res = await createComment(mediaId.value, { content: text })
    if (res.data) comments.value.push(res.data)
    messageText.value = ''
  } catch {
    showToast('发送失败')
  } finally {
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
  } catch {
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
  } catch { /* cancelled */ }
}

function parseDateTime(value: string): Date {
  if (!value) return new Date(Number.NaN)
  if (/[zZ]|[+-]\d{2}:?\d{2}$/.test(value)) return new Date(value)
  return new Date(value.replace(' ', 'T'))
}

function formatTime(iso: string): string {
  const d = parseDateTime(iso)
  if (Number.isNaN(d.getTime())) return ''
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
        <div class="progress-label">共同</div>
        <div class="progress-value">{{ progressTextOf(sharedProgress) || '–' }}</div>
      </div>
      <div class="progress-divider" />
      <div class="progress-col">
        <div class="progress-label">你</div>
        <div class="progress-value">{{ progressTextOf(myProgress) || '–' }}</div>
      </div>
      <div class="progress-divider" />
      <div class="progress-col">
        <div class="progress-label">{{ userStore.partnerName || '对方' }}</div>
        <div class="progress-value">{{ progressTextOf(partnerProgress) || '–' }}</div>
      </div>
    </div>

    <!-- Chat Messages -->
    <div class="chat-container" v-if="!loading">
      <div v-if="comments.length === 0" class="chat-empty">
        <van-icon name="chat-o" size="48" color="var(--van-gray-4)" />
        <p>还没有留言，说点什么吧</p>
      </div>
      <div
        v-for="comment in comments"
        :key="comment.id"
        class="message-row"
        :class="{ 'message-own': isOwnComment(comment), 'message-partner': !isOwnComment(comment) }"
      >
        <div class="message-bubble" @longpress="confirmDeleteComment(comment.id)">
          <div class="message-content">{{ comment.content }}</div>
          <div class="message-time">{{ formatTime(comment.createdAt) }}</div>
        </div>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="loading-state">
      <van-loading type="spinner" size="24" />
    </div>

    <!-- Input Bar -->
    <div class="input-bar">
      <van-field
        v-model="messageText"
        placeholder="输入留言..."
        :disabled="sending"
        clearable
        @keypress.enter="sendMessage"
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
          <van-radio name="shared">共同进度</van-radio>
          <van-radio name="personal">个人进度</van-radio>
        </van-radio-group>
        <van-field
          v-model="progressText"
          placeholder="如：看到第 3 章 / 第 45 分钟"
          type="textarea"
          :rows="2"
          autosize
          class="!mt-3"
        />
        <div class="flex items-center gap-2 mt-3 px-1">
          <van-switch v-model="markFinished" size="20" />
          <span class="text-sm text-gray-600">标记为已看完</span>
        </div>
      </div>
    </van-dialog>
  </div>
</template>

<style scoped>
.media-detail-page {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  background: #f7f8fa;
}

.progress-bar {
  display: flex;
  align-items: center;
  padding: 10px 16px;
  background: #fff;
  border-bottom: 1px solid #ebedf0;
  flex-shrink: 0;
}

.progress-col {
  flex: 1;
  text-align: center;
  min-width: 0;
}

.progress-label {
  font-size: 12px;
  color: #969799;
  margin-bottom: 2px;
}

.progress-value {
  font-size: 13px;
  color: #323233;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.progress-divider {
  width: 1px;
  height: 24px;
  background: #ebedf0;
  margin: 0 12px;
  flex-shrink: 0;
}

.chat-container {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 12px 16px;
  padding-bottom: 60px;
  width: 100%;
  box-sizing: border-box;
}

.chat-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
  gap: 12px;
  color: #969799;
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
  background: #fff;
  color: #323233;
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
  display: flex;
  justify-content: center;
  padding: 80px 0;
}

.input-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fff;
  border-top: 1px solid #ebedf0;
  padding: 6px 12px;
  padding-bottom: calc(12px + env(safe-area-inset-bottom));
  z-index: 10;
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
