<script setup lang="ts">
import { showConfirmDialog, showImagePreview, showNotify, showToast } from 'vant'
import type { UploaderFileListItem } from 'vant'
import { useUserStore } from '@/stores'
import request from '@/utils/request'
import {
  createPartnerMessage,
  deletePartnerMessage,
  fetchPartnerMessages,
  uploadPartnerMessageImages,
} from '@/api/modules/partner-messages'
import type { PartnerMessage } from '@/api/modules/partner-messages'
import { buildCommentImageGallery } from '@/pages/share/media/commentImageGallery'

const MAX_MESSAGE_IMAGES = 9
const MAX_IMAGE_SIZE = 5 * 1024 * 1024
const ALLOWED_IMAGE_EXTENSIONS = new Set(['jpg', 'jpeg', 'png', 'webp', 'gif'])

const { t } = useI18n()
const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)
const myId = computed(() => userInfo.value.id)

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

const partnerId = computed(() => userInfo.value.partnerId)

// ---- 留言板 ----
const messages = ref<PartnerMessage[]>([])
const messagesLoading = ref(false)
const messageText = ref('')
const sending = ref(false)
const composeMode = ref<'text' | 'image'>('text')
const pendingFileList = ref<UploaderFileListItem[]>([])
const chatContainer = ref<HTMLElement | null>(null)

const publishSharedRecord = ref(false)
const publishTodo = ref(false)
const todoAssignedTo = ref<'self' | 'partner' | 'none'>('none')
const publishPoints = ref(false)
const pointsSign = ref<1 | -1>(1)
const pointsValue = ref('')
const pointsReason = ref('')

interface PublishFlagsSnapshot {
  publishSharedRecord: boolean
  publishTodo: boolean
  todoAssignedTo: 'self' | 'partner' | 'none'
  publishPoints: boolean
  pointsSign: 1 | -1
  pointsValue: string
  pointsReason: string
}

function snapshotPublishFlags(): PublishFlagsSnapshot {
  return {
    publishSharedRecord: publishSharedRecord.value,
    publishTodo: publishTodo.value,
    todoAssignedTo: todoAssignedTo.value,
    publishPoints: publishPoints.value,
    pointsSign: pointsSign.value,
    pointsValue: pointsValue.value,
    pointsReason: pointsReason.value,
  }
}

function restorePublishFlags(flags: PublishFlagsSnapshot) {
  publishSharedRecord.value = flags.publishSharedRecord
  publishTodo.value = flags.publishTodo
  todoAssignedTo.value = flags.todoAssignedTo
  publishPoints.value = flags.publishPoints
  pointsSign.value = flags.pointsSign
  pointsValue.value = flags.pointsValue
  pointsReason.value = flags.pointsReason
}

function resetPublishFlags() {
  publishSharedRecord.value = false
  publishTodo.value = false
  todoAssignedTo.value = 'none'
  publishPoints.value = false
  pointsSign.value = 1
  pointsValue.value = ''
  pointsReason.value = ''
}

function computePointsChange(flags: PublishFlagsSnapshot): number | undefined {
  if (!flags.publishPoints)
    return undefined
  const n = Number(flags.pointsValue)
  if (!Number.isFinite(n) || n === 0)
    return 0
  return flags.pointsSign * Math.abs(Math.trunc(n))
}

const canSendText = computed(() => {
  if (!messageText.value.trim() || sending.value)
    return false
  if (publishPoints.value && computePointsChange(snapshotPublishFlags()) === 0)
    return false
  return true
})

function messageListFromResponse(res: unknown): PartnerMessage[] | null {
  if (Array.isArray(res))
    return res as PartnerMessage[]
  if (res && typeof res === 'object' && 'data' in res) {
    const data = (res as { data?: unknown }).data
    return Array.isArray(data) ? data as PartnerMessage[] : null
  }
  return null
}

async function loadMessages() {
  if (!partnerId.value)
    return
  messagesLoading.value = true
  try {
    const res = await fetchPartnerMessages({ page: 1, size: 50 })
    const list = messageListFromResponse(res)
    if (list)
      messages.value = list
    await nextTick()
    scrollChatToBottom()
  }
  catch {
    showToast('加载留言失败')
  }
  finally {
    messagesLoading.value = false
  }
}

function scrollChatToBottom() {
  if (chatContainer.value)
    chatContainer.value.scrollTop = chatContainer.value.scrollHeight
}

function isOwnMessage(msg: PartnerMessage): boolean {
  return !!myId.value && msg.createdBy === myId.value
}

function displayImages(urls: string[] | null | undefined): string[] {
  return (urls ?? []).filter((u): u is string => !!u)
}

function imageGridClass(count: number): string {
  if (count === 1)
    return 'image-grid-1'
  if (count === 2)
    return 'image-grid-2'
  return 'image-grid-3'
}

function previewMessageImages(msg: PartnerMessage, localIndex: number) {
  const { images, startPosition } = buildCommentImageGallery(messages.value, msg.id, localIndex)
  if (!images.length)
    return
  showImagePreview({
    images: [...images],
    startPosition,
    showIndicators: images.length > 1,
    closeable: true,
    teleport: 'body',
  })
}

function switchComposeMode(mode: 'text' | 'image') {
  if (composeMode.value === mode)
    return
  composeMode.value = mode
  if (mode === 'image') {
    messageText.value = ''
    resetPublishFlags()
  }
  else {
    pendingFileList.value = []
  }
}

function beforeReadImage(file: File | File[]) {
  const files = Array.isArray(file) ? file : [file]
  if (pendingFileList.value.length + files.length > MAX_MESSAGE_IMAGES) {
    showToast(`最多选择 ${MAX_MESSAGE_IMAGES} 张图片`)
    return false
  }
  for (const f of files) {
    const dot = f.name.lastIndexOf('.')
    const ext = dot >= 0 ? f.name.slice(dot + 1).toLowerCase() : ''
    if (!ALLOWED_IMAGE_EXTENSIONS.has(ext)) {
      showToast('仅支持 jpg/png/webp/gif')
      return false
    }
    if (f.size > MAX_IMAGE_SIZE) {
      showToast('单张图片不能超过 5MB')
      return false
    }
  }
  return true
}

async function sendTextMessage() {
  const text = messageText.value.trim()
  if (!canSendText.value)
    return

  const flags = snapshotPublishFlags()
  const pointsChange = computePointsChange(flags)
  if (flags.publishPoints && pointsChange === 0) {
    showToast(t('share.pointsValueRequired'))
    return
  }

  sending.value = true
  const draft = text
  messageText.value = ''
  resetPublishFlags()
  const previous = messages.value
  try {
    const created = await createPartnerMessage({
      content: draft,
      publishSharedRecord: flags.publishSharedRecord,
      publishTodo: flags.publishTodo,
      todoAssignedTo: flags.publishTodo ? flags.todoAssignedTo : undefined,
      publishPoints: flags.publishPoints,
      pointsChange,
      pointsReason: flags.publishPoints ? (flags.pointsReason.trim() || draft) : undefined,
    })
    if (created.data)
      messages.value = [...previous, created.data]
    else
      await loadMessages()
    await nextTick()
    scrollChatToBottom()
  }
  catch {
    messageText.value = draft
    restorePublishFlags(flags)
    showToast('发送失败')
  }
  finally {
    sending.value = false
  }
}

async function sendImageMessage() {
  const files = pendingFileList.value
    .map(item => item.file)
    .filter((f): f is File => !!f)
  if (!files.length || sending.value)
    return

  sending.value = true
  pendingFileList.value = []
  const previous = messages.value
  try {
    const up = await uploadPartnerMessageImages(files)
    const urls = up.data?.urls ?? []
    if (!urls.length)
      throw new Error('upload empty')
    const created = await createPartnerMessage({ imageUrls: urls })
    if (created.data)
      messages.value = [...previous, created.data]
    else
      await loadMessages()
    await nextTick()
    scrollChatToBottom()
  }
  catch {
    showToast('发送失败')
  }
  finally {
    sending.value = false
  }
}

async function confirmDeleteMessage(msg: PartnerMessage) {
  if (!isOwnMessage(msg))
    return
  try {
    await showConfirmDialog({ title: '删除', message: '确定删除这条留言吗？' })
    await deletePartnerMessage(msg.id)
    messages.value = messages.value.filter(m => m.id !== msg.id)
    showToast('已删除')
  }
  catch { /* cancelled or failed */ }
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

onMounted(async () => {
  await userStore.info()
  if (partnerId.value)
    await loadMessages()
})

watch(partnerId, async (val) => {
  if (val)
    await loadMessages()
})
</script>

<template>
  <div>
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
            <van-button size="small" type="success" :loading="loading" @click="bindPartner">
              绑定
            </van-button>
          </template>
        </van-field>
      </van-cell-group>
    </div>

    <div v-else class="board-page">
      <div v-if="!messagesLoading" ref="chatContainer" class="chat-container">
        <div v-if="messages.length === 0" class="chat-empty">
          <van-icon name="chat-o" size="48" color="var(--van-gray-4)" />
          <p>还没有留言，说点什么吧</p>
        </div>
        <div
          v-for="(msg, index) in messages"
          :key="msg.id || `${msg.createdAt}-${index}`"
          class="message-row"
          :class="{ 'message-own': isOwnMessage(msg), 'message-partner': !isOwnMessage(msg) }"
        >
          <button
            v-if="isOwnMessage(msg)"
            type="button"
            class="message-delete-btn"
            aria-label="删除留言"
            @click.stop="confirmDeleteMessage(msg)"
          >
            <van-icon name="delete-o" size="16" />
          </button>
          <div
            class="message-bubble"
            :class="{ 'has-images': displayImages(msg.imageUrls).length }"
          >
            <div
              v-if="displayImages(msg.imageUrls).length"
              class="image-grid"
              :class="imageGridClass(displayImages(msg.imageUrls).length)"
            >
              <img
                v-for="(url, imgIndex) in displayImages(msg.imageUrls)"
                :key="`${msg.id}-${imgIndex}`"
                :src="url"
                class="comment-image"
                @click.stop="previewMessageImages(msg, imgIndex)"
              >
            </div>
            <div v-else-if="msg.content" class="message-content">
              {{ msg.content }}
            </div>
            <div class="message-time">
              {{ formatTime(msg.createdAt) }}
            </div>
          </div>
        </div>
      </div>

      <div v-else class="loading-state">
        <van-loading type="spinner" size="24" />
      </div>

      <div class="input-bar">
        <div class="mode-toggle">
          <button
            type="button"
            class="mode-btn"
            :class="{ active: composeMode === 'text' }"
            @click="switchComposeMode('text')"
          >
            文字
          </button>
          <button
            type="button"
            class="mode-btn"
            :class="{ active: composeMode === 'image' }"
            @click="switchComposeMode('image')"
          >
            图片
          </button>
        </div>

        <div v-if="composeMode === 'text'" class="compose-text">
          <div class="publish-toggles">
            <van-checkbox v-model="publishSharedRecord" shape="square" icon-size="16">
              {{ t('share.publishRecord') }}
            </van-checkbox>
            <van-checkbox v-model="publishTodo" shape="square" icon-size="16">
              {{ t('share.publishTodo') }}
            </van-checkbox>
            <van-radio-group
              v-if="publishTodo"
              v-model="todoAssignedTo"
              direction="horizontal"
              class="todo-assign-group"
            >
              <van-radio name="self">
                {{ t('share.todoAssignSelf') }}
              </van-radio>
              <van-radio name="partner">
                {{ t('share.todoAssignPartner') }}
              </van-radio>
              <van-radio name="none">
                {{ t('share.todoAssignNone') }}
              </van-radio>
            </van-radio-group>
            <van-checkbox v-model="publishPoints" shape="square" icon-size="16">
              {{ t('share.publishPoints') }}
            </van-checkbox>
            <div v-if="publishPoints" class="points-fields">
              <div class="points-sign-row">
                <button
                  type="button"
                  class="sign-btn"
                  :class="{ active: pointsSign === 1 }"
                  @click="pointsSign = 1"
                >
                  {{ t('share.pointsAdd') }}
                </button>
                <button
                  type="button"
                  class="sign-btn"
                  :class="{ active: pointsSign === -1 }"
                  @click="pointsSign = -1"
                >
                  {{ t('share.pointsSub') }}
                </button>
              </div>
              <van-field
                v-model="pointsValue"
                type="digit"
                :label="t('share.pointsValue')"
                :placeholder="t('share.pointsValuePlaceholder')"
                input-align="right"
              />
              <van-field
                v-model="pointsReason"
                :label="t('share.pointsReason')"
                :placeholder="t('share.pointsReasonPlaceholder')"
              />
            </div>
          </div>

          <div class="input-row">
            <van-field
              v-model="messageText"
              class="input-field"
              type="textarea"
              rows="1"
              :autosize="{ maxHeight: 96 }"
              placeholder="输入留言..."
              :disabled="sending || messagesLoading"
              clearable
            >
              <template #button>
                <van-button
                  size="small"
                  type="primary"
                  :loading="sending"
                  :disabled="!canSendText"
                  @click="sendTextMessage"
                >
                  发送
                </van-button>
              </template>
            </van-field>
          </div>
        </div>

        <div v-else class="input-row input-row-image">
          <van-uploader
            v-model="pendingFileList"
            :max-count="MAX_MESSAGE_IMAGES"
            multiple
            accept="image/*"
            :before-read="beforeReadImage"
            :disabled="sending || messagesLoading"
            class="image-uploader"
          />
          <van-button
            size="small"
            type="primary"
            :loading="sending"
            :disabled="!pendingFileList.length || sending"
            @click="sendImageMessage"
          >
            发送
          </van-button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.board-page {
  display: flex;
  flex-direction: column;
  /* ponytail: 伴侣 tab 内留言板；nav/tab placeholder 由外层占位 */
  height: calc(100dvh - var(--van-nav-bar-height, 46px) - var(--van-tabbar-height, 50px) - 16px);
  max-height: calc(100dvh - var(--van-nav-bar-height, 46px) - var(--van-tabbar-height, 50px) - 16px);
  overflow: hidden;
  background: var(--van-background);
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
  align-items: flex-end;
  gap: 6px;
  margin-bottom: 14px;
}

.message-delete-btn {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  margin: 0;
  padding: 0;
  border: none;
  border-radius: 50%;
  background: transparent;
  color: var(--van-text-color-3);
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
}

.message-delete-btn:active {
  color: var(--van-danger-color);
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

.message-bubble.has-images {
  padding: 4px;
  background: transparent;
  box-shadow: none;
}

.message-own .message-bubble.has-images {
  background: transparent;
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
  white-space: pre-wrap;
}

.message-time {
  font-size: 11px;
  margin-top: 4px;
  opacity: 0.7;
  text-align: right;
}

.has-images .message-time {
  padding: 0 6px 2px;
  color: var(--van-text-color-3);
  opacity: 1;
}

.image-grid {
  display: grid;
  gap: 4px;
}

.image-grid-1 {
  grid-template-columns: 1fr;
}

.image-grid-1 .comment-image {
  max-width: 200px;
  aspect-ratio: auto;
  max-height: 240px;
}

.image-grid-2 {
  grid-template-columns: repeat(2, 1fr);
  max-width: 200px;
}

.image-grid-3 {
  grid-template-columns: repeat(3, 1fr);
  max-width: 240px;
}

.comment-image {
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
  border-radius: 6px;
  display: block;
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

.mode-toggle {
  display: flex;
  gap: 8px;
  margin-bottom: 6px;
}

.mode-btn {
  padding: 4px 12px;
  font-size: 13px;
  border: 1px solid var(--van-border-color);
  border-radius: 16px;
  background: var(--van-background-2);
  color: var(--van-text-color-2);
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
}

.mode-btn.active {
  border-color: var(--van-primary-color);
  color: var(--van-primary-color);
  background: color-mix(in srgb, var(--van-primary-color) 12%, transparent);
}

.compose-text {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.publish-toggles {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 2px 0;
}

.publish-toggles :deep(.van-checkbox) {
  align-items: center;
}

.publish-toggles :deep(.van-checkbox__label) {
  font-size: 13px;
  line-height: 1.4;
}

.todo-assign-group {
  margin-left: 24px;
  gap: 8px;
}

.todo-assign-group :deep(.van-radio) {
  margin-right: 8px;
}

.todo-assign-group :deep(.van-radio__label) {
  font-size: 12px;
}

.points-fields {
  margin-left: 24px;
  border: 1px solid var(--van-border-color);
  border-radius: 8px;
  overflow: hidden;
}

.points-sign-row {
  display: flex;
  gap: 8px;
  padding: 8px 12px 0;
}

.sign-btn {
  padding: 2px 10px;
  font-size: 12px;
  border: 1px solid var(--van-border-color);
  border-radius: 12px;
  background: var(--van-background-2);
  color: var(--van-text-color-2);
  cursor: pointer;
}

.sign-btn.active {
  border-color: var(--van-primary-color);
  color: var(--van-primary-color);
  background: color-mix(in srgb, var(--van-primary-color) 12%, transparent);
}

.points-fields :deep(.van-field) {
  padding-top: 4px;
  padding-bottom: 4px;
}

.input-row {
  display: flex;
  align-items: flex-end;
  gap: 8px;
}

.input-row-image {
  align-items: center;
}

.image-uploader {
  flex: 1;
  min-width: 0;
}

.input-field {
  flex: 1;
  padding: 0;
}

.input-field :deep(.van-field__body) {
  align-items: flex-end;
}

.input-field :deep(.van-field__control) {
  max-height: 96px;
  overflow-y: auto;
}
</style>

<route lang="json5">
{
  name: 'Share'
}
</route>
