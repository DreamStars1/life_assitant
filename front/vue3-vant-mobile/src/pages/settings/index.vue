<script setup lang="ts">
import { showConfirmDialog, showNotify, showToast } from 'vant'
import router from '@/router'
import { useUserStore } from '@/stores'
import { version } from '~root/package.json'
import { createTemplate, deleteTemplate, fetchTemplates, updateTemplate } from '@/api/modules/ack-templates'
import type { AckTemplate } from '@/api/modules/ack-templates'
import { createApiToken, deleteApiToken, fetchApiTokens } from '@/api/modules/api-tokens'
import type { ApiToken } from '@/api/modules/api-tokens'

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

// API 令牌管理
const showApiTokens = ref(false)
const apiTokens = ref<ApiToken[]>([])
const apiTokensLoading = ref(false)
const showAddTokenDialog = ref(false)
const newTokenName = ref('')
const apiTokenTabActive = ref(0)

const configTemplates = [
  {
    title: 'Cursor (.cursor/mcp.json)',
    code: `{
  "mcpServers": {
    "life-assistant": {
      "url": "https://mcp.life-assitant.top",
      "headers": {
        "Authorization": "Bearer la_xxx"
      }
    }
  }
}`,
  },
  {
    title: 'Claude Desktop (via mcp-remote)',
    code: `{
  "mcpServers": {
    "life-assistant": {
      "command": "npx",
      "args": [
        "mcp-remote@latest",
        "https://mcp.life-assitant.top",
        "--header", "Authorization: Bearer la_xxx"
      ]
    }
  }
}`,
  },
  {
    title: 'Claude Code CLI',
    code: `claude mcp add life-assistant --transport http https://mcp.life-assitant.top --header "Authorization: Bearer la_xxx"`,
  },
  {
    title: 'Workbuddy (CodeBuddy)',
    code: `{
  "mcpServers": {
    "life-assistant": {
      "url": "https://mcp.life-assitant.top",
      "headers": {
        "Authorization": "Bearer la_xxx"
      }
    }
  }
}`,
  },
]

async function copyTemplate(code: string, title: string) {
  try {
    await navigator.clipboard.writeText(code)
    showToast(`已复制: ${title}`)
  }
  catch {
    showNotify({ type: 'danger', message: '复制失败，请手动复制' })
  }
}

async function loadApiTokens() {
  apiTokensLoading.value = true
  try {
    const res = await fetchApiTokens()
    apiTokens.value = res.data ?? []
  }
  finally {
    apiTokensLoading.value = false
  }
}

function openApiTokenManager() {
  loadApiTokens()
  showApiTokens.value = true
}

async function onCreateApiToken() {
  if (!newTokenName.value.trim())
    return
  try {
    const res = await createApiToken({ name: newTokenName.value.trim() })
    const token = res.data!
    showAddTokenDialog.value = false
    newTokenName.value = ''
    // 用 showDialog 展示完整 token（仅一次）
    const { showDialog } = await import('vant')
    showDialog({
      title: 'API 令牌创建成功',
      message: `请立即复制此令牌，关闭后将无法再次查看完整令牌：\n\n${token.fullToken}`,
      confirmButtonText: '已复制，关闭',
    }).then(() => {
      loadApiTokens()
    })
  }
  catch {
    showNotify({ type: 'danger', message: '创建失败' })
  }
}

async function onRevokeApiToken(t: ApiToken) {
  try {
    await showConfirmDialog({ title: '撤销令牌', message: `确定撤销「${t.name}」吗？撤销后该令牌将立即失效。` })
    await deleteApiToken(t.id)
    showToast('已撤销')
    await loadApiTokens()
  }
  catch { /* cancelled */ }
}

function formatDate(dateStr: string) {
  return dateStr ? dateStr.substring(0, 10) : ''
}
</script>

<template>
  <div>
    <van-cell-group :inset="true" class="!mt-4">
      <van-cell title="确认回复模板" is-link clickable @click="openTemplateManager" />
    </van-cell-group>

    <van-cell-group :inset="true" class="!mt-4">
      <van-cell title="API 令牌" is-link clickable @click="openApiTokenManager" />
    </van-cell-group>

    <van-cell-group :inset="true" class="!mt-4">
      <van-cell v-if="userInfo.id" :title="$t('settings.logout')" clickable class="danger-text" @click="Logout" />
    </van-cell-group>

    <div class="text-gray mt-2 text-center">
      {{ $t("settings.currentVersion") }}: v{{ version }}
    </div>
    <div class="beian-footer">
      <a href="https://beian.miit.gov.cn/" target="_blank">苏ICP备2026047415号-1</a>
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
          <div v-for="(template, i) in templates" :key="template.id" class="template-popup-item" :class="[{ active: template.id === activeTemplateId }]">
            <div class="template-popup-num">
              {{ i + 1 }}
            </div>
            <span class="template-popup-content">{{ template.content }}</span>
            <div class="template-popup-actions">
              <van-icon
                :name="template.id === activeTemplateId ? 'success' : 'circle'"
                :color="template.id === activeTemplateId ? 'var(--van-green)' : 'var(--van-gray-4)'"
                @click="setActiveTemplate(template.id)"
              />
              <van-icon name="edit" @click="openEdit(template)" />
              <van-icon name="delete" @click="onDelete(template)" />
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
    <!-- API 令牌管理弹窗 -->
    <van-popup v-model:show="showApiTokens" position="bottom" round title="API 令牌" style="max-height: 75vh;">
      <van-tabs v-model:active="apiTokenTabActive">
        <van-tab title="令牌管理">
          <div class="template-popup">
            <div class="template-popup-header">
              <span class="template-popup-title">API 令牌</span>
              <van-button size="small" round icon="plus" type="primary" @click="showAddTokenDialog = true">
                新建
              </van-button>
            </div>
            <div v-if="apiTokensLoading" class="template-popup-loading">
              <van-loading />
            </div>
            <div v-else-if="apiTokens.length === 0" class="template-popup-empty">
              暂无 API 令牌，点击右上角新建
            </div>
            <div v-else class="template-popup-list">
              <div v-for="token in apiTokens" :key="token.id" class="template-popup-item">
                <div style="flex: 1">
                  <div style="font-size: 14px; font-weight: 500;">
                    {{ token.name }}
                  </div>
                  <div style="font-size: 12px; color: var(--van-gray-5); margin-top: 2px;">
                    {{ token.tokenPrefix }}
                    <span v-if="token.lastUsedAt"> · 最后使用: {{ formatDate(token.lastUsedAt) }}</span>
                  </div>
                </div>
                <van-icon name="delete" @click="onRevokeApiToken(token)" />
              </div>
            </div>
          </div>
        </van-tab>
        <van-tab title="使用指引">
          <div class="guide-popup">
            <p class="guide-desc">
              API 令牌用于通过 MCP 协议在外部 AI 客户端中调用待办和共享记录功能。
            </p>
            <p class="guide-desc" style="margin-bottom: 16px;">
              MCP 服务地址：<code class="guide-code">https://mcp.life-assitant.top</code>
            </p>

            <div v-for="(tmpl, idx) in configTemplates" :key="idx" class="guide-block">
              <div class="guide-block-header">
                <span class="guide-block-title">{{ tmpl.title }}</span>
                <van-button size="mini" plain type="primary" @click="copyTemplate(tmpl.code, tmpl.title)">
                  复制
                </van-button>
              </div>
              <pre class="guide-block-code">{{ tmpl.code }}</pre>
            </div>

            <p class="guide-note">
              注意：模板中的 <code class="guide-code">la_xxx</code> 为占位符，请替换为你在「令牌管理」中创建的完整 API 令牌。
            </p>
          </div>
        </van-tab>
      </van-tabs>
    </van-popup>

    <!-- 新建 Token 弹窗 -->
    <van-dialog v-model:show="showAddTokenDialog" title="新建 API 令牌" show-cancel-button @confirm="onCreateApiToken">
      <van-field v-model="newTokenName" placeholder="请输入令牌名称（如: Claude 桌面端）" maxlength="50" autofocus clearable />
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
.guide-popup {
  padding: 16px;
}
.guide-desc {
  font-size: 13px;
  color: var(--van-gray-6);
  line-height: 1.6;
  margin: 0 0 8px 0;
}
.guide-code {
  background: #f5f5f5;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
}
.guide-block {
  margin-bottom: 14px;
  border: 1px solid var(--van-gray-3);
  border-radius: 8px;
  overflow: hidden;
}
.guide-block-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: var(--van-gray-1);
  border-bottom: 1px solid var(--van-gray-3);
}
.guide-block-title {
  font-size: 13px;
  font-weight: 600;
}
.guide-block-code {
  font-size: 12px;
  line-height: 1.5;
  padding: 12px;
  margin: 0;
  overflow-x: auto;
  white-space: pre;
  background: #fafafa;
}
.guide-note {
  font-size: 12px;
  color: var(--van-orange);
  line-height: 1.5;
  margin: 16px 0 0 0;
  padding: 8px 12px;
  background: #fff8e1;
  border-radius: 6px;
}
.beian-footer {
  margin-top: 8px;
  padding-bottom: 16px;
  text-align: center;
}
.beian-footer a {
  font-size: 12px;
  color: var(--van-gray-5);
  text-decoration: none;
}
</style>

<route lang="json5">
{
  name: 'Settings'
}
</route>
