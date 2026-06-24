<script setup lang="ts">
import { showConfirmDialog, showNotify, showToast } from 'vant'
import router from '@/router'
import { useUserStore } from '@/stores'
import { version } from '~root/package.json'
import { createTemplate, deleteTemplate, fetchTemplates, updateTemplate } from '@/api/modules/ack-templates'
import type { AckTemplate } from '@/api/modules/ack-templates'

const { t } = useI18n()
const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)

function Logout() {
  showConfirmDialog({
    title: t('settings.confirmTitle'),
  })
    .then(() => {
      userStore.logout()
      router.replace({ name: 'Login' })
    })
    .catch(() => {})
}

// 确认回复模板管理
const ACTIVE_TEMPLATE_KEY = 'life_assistant_active_ack_template'
const showTemplates = ref(false)
const templates = ref<AckTemplate[]>([])
const templatesLoading = ref(false)
const showAddDialog = ref(false)
const newContent = ref('')
const showEditDialog = ref(false)
const editContent = ref('')
const editingId = ref<string | null>(null)
const activeTemplateId = ref(localStorage.getItem(ACTIVE_TEMPLATE_KEY) || '')

function getActiveTemplate(): AckTemplate | undefined {
  return templates.value.find(t => t.id === activeTemplateId.value)
}

function setActiveTemplate(id: string) {
  activeTemplateId.value = id
  localStorage.setItem(ACTIVE_TEMPLATE_KEY, id)
}

async function loadTemplates() {
  templatesLoading.value = true
  try {
    const res = await fetchTemplates()
    templates.value = res.data ?? []
    if (activeTemplateId.value && !templates.value.some(t => t.id === activeTemplateId.value)) {
      setActiveTemplate('')
    }
    if (!activeTemplateId.value && templates.value.length > 0) {
      setActiveTemplate(templates.value[0]!.id)
    }
  }
  finally {
    templatesLoading.value = false
  }
}

function openTemplateManager() {
  loadTemplates()
  showTemplates.value = true
}

function openAdd() {
  newContent.value = ''
  showAddDialog.value = true
}

async function onSaveNew() {
  if (!newContent.value.trim())
    return
  try {
    await createTemplate(newContent.value.trim())
    showAddDialog.value = false
    await loadTemplates()
    if (!activeTemplateId.value && templates.value.length > 0) {
      setActiveTemplate(templates.value[0]!.id)
    }
  }
  catch {
    showNotify({ type: 'danger', message: '添加失败，最多 5 条' })
  }
}

function openEdit(t: AckTemplate) {
  editingId.value = t.id
  editContent.value = t.content
  showEditDialog.value = true
}

async function onSaveEdit() {
  if (!editContent.value.trim() || !editingId.value)
    return
  await updateTemplate(editingId.value, editContent.value.trim())
  showEditDialog.value = false
  await loadTemplates()
}

async function onDelete(t: AckTemplate) {
  try {
    await showConfirmDialog({ title: '删除模板', message: `确定删除「${t.content}」吗？` })
    await deleteTemplate(t.id)
    showToast('已删除')
    if (activeTemplateId.value === t.id)
      setActiveTemplate('')
    await loadTemplates()
  }
  catch { /* cancelled */ }
}
</script>

<template>
  <div>
    <van-cell-group :inset="true" class="!mt-4">
      <van-cell title="确认回复模板" is-link clickable @click="openTemplateManager" />
    </van-cell-group>

    <van-cell-group :inset="true" class="!mt-4">
      <van-cell v-if="userInfo.id" :title="$t('settings.logout')" clickable class="danger-text" @click="Logout" />
    </van-cell-group>

    <div class="text-gray mt-2 text-center">
      {{ $t("settings.currentVersion") }}: v{{ version }}
    </div>

    <!-- 模板管理弹窗 -->
    <van-popup v-model:show="showTemplates" position="bottom" round title="确认回复模板" style="max-height: 70vh;">
      <div class="template-popup">
        <div class="template-popup-header">
          <span class="template-popup-title">确认回复模板</span>
          <van-button v-if="templates.length < 5" size="small" round icon="plus" type="primary" @click="openAdd">
            添加
          </van-button>
        </div>
        <div v-if="templatesLoading" class="template-popup-loading">
          <van-loading />
        </div>
        <div v-else-if="templates.length === 0" class="template-popup-empty">
          暂无模板，点击右上角添加
        </div>
        <div v-else class="template-popup-list">
          <div v-for="(t, i) in templates" :key="t.id" class="template-popup-item" :class="[{ active: t.id === activeTemplateId }]">
            <div class="template-popup-num">
              {{ i + 1 }}
            </div>
            <span class="template-popup-content">{{ t.content }}</span>
            <div class="template-popup-actions">
              <van-icon
                :name="t.id === activeTemplateId ? 'success' : 'circle'"
                :color="t.id === activeTemplateId ? 'var(--van-green)' : 'var(--van-gray-4)'"
                @click="setActiveTemplate(t.id)"
              />
              <van-icon name="edit" @click="openEdit(t)" />
              <van-icon name="delete" @click="onDelete(t)" />
            </div>
          </div>
          <div v-if="templates.length >= 5" class="template-popup-hint">
            已达到最大数量（5 条）
          </div>
        </div>
      </div>
    </van-popup>

    <!-- 添加弹窗 -->
    <van-dialog v-model:show="showAddDialog" title="添加模板" show-cancel-button @confirm="onSaveNew">
      <van-field v-model="newContent" placeholder="请输入确认文案" maxlength="100" autofocus clearable />
    </van-dialog>

    <!-- 编辑弹窗 -->
    <van-dialog v-model:show="showEditDialog" title="编辑模板" show-cancel-button @confirm="onSaveEdit">
      <van-field v-model="editContent" placeholder="请输入确认文案" maxlength="100" autofocus clearable />
    </van-dialog>
  </div>
</template>

<style scoped>
.danger-text {
  --van-cell-text-color: var(--van-red);
}
.template-popup {
  padding: 16px;
  min-height: 200px;
}
.template-popup-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.template-popup-title {
  font-size: 16px;
  font-weight: 600;
}
.template-popup-loading,
.template-popup-empty {
  text-align: center;
  padding: 40px 0;
  color: var(--van-gray-5);
  font-size: 14px;
}
.template-popup-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.template-popup-item {
  display: flex;
  align-items: center;
  border-radius: 8px;
  padding: 10px 12px;
  gap: 10px;
}
.template-popup-item.active {
  background: #e8f5e9;
}
.template-popup-num {
  font-size: 12px;
  font-weight: 600;
  color: var(--van-gray-5);
  min-width: 14px;
  text-align: center;
}
.template-popup-content {
  flex: 1;
  font-size: 14px;
}
.template-popup-actions {
  display: flex;
  gap: 12px;
  color: var(--van-gray-6);
  font-size: 18px;
  align-items: center;
}
.template-popup-hint {
  text-align: center;
  color: var(--van-gray-5);
  font-size: 12px;
  padding: 8px 0;
}
</style>

<route lang="json5">
{
  name: 'Settings'
}
</route>
