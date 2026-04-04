<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { Modal, message } from 'ant-design-vue'
import { useAccountStore, useChatStore } from '@/stores'
import { formatSessionTitle } from '@/utils/chat/format'
import { useAgentDetail } from '@/composables/chat/useAgentDetail'
import { useSessions } from '@/composables/chat/useSessions'
import { useCurrentSession } from '@/composables/chat/useCurrentSession'
import { useChatStream } from '@/composables/chat/useChatStream'
import ChatSidebar from '@/components/chat/ChatSidebar.vue'
import ChatMain from '@/components/chat/ChatMain.vue'
import RenameModal from '@/components/chat/RenameModal.vue'
import type { DisplayMessage, ChatMessageVO, UploadedFileItem } from '@/types'
import * as chatSessionApi from '@/api/chatSession'

const route = useRoute()
const accountStore = useAccountStore()
const chatStore = useChatStore()
const userInfo = computed(() => accountStore.userInfo)

const agentId = computed(() => (route.params.agentId as string) || '')

// 智能体详情
const { agentDetail, allowFileType } = useAgentDetail(agentId)

// 记忆/规划是否可用（由 agentDetail 决定）
const accountId = computed(() => accountStore.userInfo?.id)
const enableMemory = computed(() => agentDetail.value?.enableMemory === true)
const enablePlanning = computed(() => agentDetail.value?.enablePlanning === true)
const showToolProcess = computed(() => agentDetail.value?.showToolProcess === true)

// 记忆/规划/侧边栏状态：从 Pinia store 读取（持久化由 pinia-plugin-persistedstate 处理）
const memoryActive = computed(() => {
  const id = agentDetail.value?.id ?? agentId.value
  chatStore.preferences // 依赖以保持响应性
  return chatStore.getMemoryActive(id, accountId.value, enableMemory.value)
})
const planActive = computed(() => {
  const id = agentDetail.value?.id ?? agentId.value
  chatStore.preferences
  return chatStore.getPlanActive(id, accountId.value, enablePlanning.value)
})
const toolProcessActive = computed(() => {
  const id = agentDetail.value?.id ?? agentId.value
  chatStore.preferences
  return chatStore.getToolProcessActive(id, accountId.value, showToolProcess.value)
})
const sidebarCollapsed = computed({
  get: () => {
    const id = agentDetail.value?.id ?? agentId.value
    chatStore.preferences
    return chatStore.getSidebarCollapsed(id, accountId.value)
  },
  set: (v: boolean) => {
    const id = agentDetail.value?.id ?? agentId.value
    chatStore.setSidebarCollapsed(id, accountId.value, v)
  },
})

const handleMemoryChange = (v: boolean) => {
  if (!v) {
    message.warning("关闭记忆后，所有工具调用将无需人工确认，自动执行")
  }
  const id = agentDetail.value?.id ?? agentId.value
  chatStore.setMemoryActive(id, accountId.value, v)
}

const handlePlanChange = (v: boolean) => {
  const id = agentDetail.value?.id ?? agentId.value
  chatStore.setPlanActive(id, accountId.value, v)
}

const handelToolProcess = (v: boolean) => {
  const id = agentDetail.value?.id ?? agentId.value
  chatStore.setToolProcessActive(id, accountId.value, v)
}

// 会话列表管理
const {
  pinnedSessions,
  otherSessions,
  createSession,
  updateSessionTitle,
  pinSession,
  unpinSession,
  deleteSession,
  loadSessions,
} = useSessions(agentId)

// 当前会话管理
const {
  currentSessionId,
  currentSessionTitle,
  messagesList,
  selectSession,
  resetSession,
  loadCurrentMessages,
} = useCurrentSession(agentId)

// 已上传附件（仅已完成上传的计入 fileIds）
const uploadedFiles = ref<UploadedFileItem[]>([])
const fileIds = computed(() =>
  uploadedFiles.value.filter((f) => !f.uploading).map((f) => f.id)
)

// 流式对话及工具调用
const {
  agentHasResult,
  streamingContent,
  streamingMessageId,
  toolCallsInProgress,
  isRunning,
  sendMessage,
  sendToolContent,
  abortRun
} = useChatStream(
  agentId,
  agentDetail,
  currentSessionId,
  fileIds,
  memoryActive,
  planActive,
  toolProcessActive,
  (chatMsg: ChatMessageVO) => {
    messagesList.value.push(chatMsg)
  })

// 输入框内容
const inputText = ref('')

/** 构建文件前缀字符串 */
function buildFilesPrefix(files: UploadedFileItem[]): string {
  if (!files.length) return ''
  return JSON.stringify({ files }) + '@==##::::##==@'
}

// 构建展示消息
const displayMessages = computed<DisplayMessage[]>(() => {
  const list: DisplayMessage[] = []
  for (const m of messagesList.value) {
    if (m.role === 'system' || !m.content) continue
    list.push({
      id: String(m.id),
      role: m.role as any,
      content: (m.content || '') as string,
      isStreaming: false,
    })
  }
  if (streamingMessageId.value) {
    list.push({
      id: streamingMessageId.value,
      role: 'assistant',
      content: streamingContent.value,
      isStreaming: true,
    })
  } else {
    // 响应加载动画
    if (list[list.length -1]?.role === 'user') {
      list.push({
        id: '',
        role: 'assistant',
        content: '',
        isStreaming: true,
      })
    }
  }
  return list
})

const isWelcomeMode = computed(() => messagesList.value.length === 0 && !streamingMessageId.value)

// 重命名模态框
const renameModalVisible = ref(false)
const renameSessionRef = ref<any>(null)
const renameTitle = ref('')
const renameSubmitting = ref(false)

// 新会话
const handleNewSession = async () => {
  if (isRunning.value) return
  resetSession()
  // 可选：自动创建一个空会话？原逻辑是点击新建重置，不自动创建，发送时创建。这里保持原样。
  // 但也可以在这里创建一个空白会话，原组件是 resetSession 后 currentSessionId 为 null，然后发送时创建。
  // 无需额外操作
}

// 选择会话
const handleSelectSession = async (session: any) => {
  if (isRunning.value) return
  await selectSession(session)
}

// 会话菜单操作
const handleSessionMenu = async (key: string, session: any) => {
  const id = String(session.id)
  if (key === 'rename') {
    renameSessionRef.value = session
    renameTitle.value = session.title || '新对话'
    renameModalVisible.value = true
    return
  }
  if (key === 'pin') {
    await pinSession(id)
    if (currentSessionId.value === id) {
      // 若当前会话被置顶，可能需要更新列表，已自动重新加载
    }
    return
  }
  if (key === 'unpin') {
    await unpinSession(id)
    return
  }
  if (key === 'delete') {
    Modal.confirm({
      title: '确认删除',
      content: '删除后无法恢复，是否继续？',
      onOk: async () => {
        await deleteSession(id)
        if (currentSessionId.value === id) {
          resetSession()
        }
      },
    })
    return
  }
}

// 提交重命名
const submitRename = async () => {
  const session = renameSessionRef.value
  if (!session) return
  const title = renameTitle.value.trim() || '新对话'
  renameSubmitting.value = true
  try {
    await updateSessionTitle(session.id, title)
    renameModalVisible.value = false
  } finally {
    renameSubmitting.value = false
  }
}

function formatSessionTitleFromInput(text: string): string {
  const t = (text || '').trim()
  if (!t) return '新对话'
  return t.length > 50 ? t.slice(0, 50) + '...' : t
}

// 发送工具执行结果
const handelToolContent = async (value: any) => {
  await sendToolContent(value)
}

// 发送消息
const handleSend = async () => {
  const text = inputText.value.trim()
  const filesToSend = uploadedFiles.value.filter((f) => !f.uploading)
  const hasFiles = filesToSend.length > 0
  if ((!text && !hasFiles) || !agentId.value || isRunning.value) return

  const finalText = hasFiles ? buildFilesPrefix(filesToSend) + text : text
  const fileIdsToSend = filesToSend.map((f) => f.id)

  // 立即清空输入框和附件，提升交互体验
  inputText.value = ''
  uploadedFiles.value = []

  // 如果没有当前会话，先创建
  if (!currentSessionId.value) {
    const titleInput = text || (hasFiles ? '附件' : '新对话')
    const newSession = await createSession(formatSessionTitleFromInput(titleInput))
    if (!newSession) return
    currentSessionId.value = String(newSession.id)
    currentSessionTitle.value = newSession.title || '新对话'
  }

  // 保存用户消息
  const userMsg = await chatSessionApi.appendMessage(currentSessionId.value, { role: 'user', content: finalText })
  // 如果是新会话，更新标题
  if (messagesList.value.length === 0) {
    const title = formatSessionTitle(text || (hasFiles ? '附件' : '新对话'))
    await updateSessionTitle(currentSessionId.value, title)
    currentSessionTitle.value = title
  }
  messagesList.value.push(userMsg.data.data)

  // 触发流式回复（传入 fileIdsToSend，因输入框已提前清空）
  await sendMessage(
    finalText,
    [{ role: 'user', content: finalText }] as ChatMessageVO[],
    fileIdsToSend
  )
}

// 切换侧边栏（通过 computed setter 自动持久化到 store）
const toggleSidebar = () => {
  sidebarCollapsed.value = !sidebarCollapsed.value
}

// 初始化加载会话列表
onMounted(() => {
  loadSessions()
})
</script>

<template>
  <div class="chat-page">
    <ChatSidebar
      :collapsed="sidebarCollapsed"
      :agent-name="agentDetail?.name"
      :pinned-sessions="pinnedSessions"
      :other-sessions="otherSessions"
      :current-session-id="currentSessionId"
      :user-nickname="userInfo?.nickname"
      :is-running="isRunning"
      @toggle-collapse="toggleSidebar"
      @new-session="handleNewSession"
      @select-session="handleSelectSession"
      @session-menu="handleSessionMenu"
    />

    <RenameModal
      v-model:visible="renameModalVisible"
      v-model:title="renameTitle"
      :confirm-loading="renameSubmitting"
      @ok="submitRename"
    />

    <ChatMain
      ref="chatMainRef"
      :title="currentSessionTitle || agentDetail?.name || '对话'"
      :is-welcome-mode="isWelcomeMode"
      :welcome-headline="`来和 ${agentDetail?.name || '智能体'} 聊聊吧`"
      :welcome-desc="agentDetail?.description || '有什么想说的，直接发给我就好～'"
      :messages="displayMessages"
      :tool-calls="toolCallsInProgress"
      :input-value="inputText"
      :uploaded-files="uploadedFiles"
      :isRunning="isRunning"
      :memory-active="memoryActive"
      :plan-active="planActive"
      :enable-memory="enableMemory"
      :enable-planning="enablePlanning"
      :allow-upload-file-type="allowFileType"
      :agent-has-result="agentHasResult"
      :show-tool-process="showToolProcess"
      :tool-process-active="toolProcessActive"
      @update:input-value="inputText = $event"
      @update:uploaded-files="uploadedFiles = $event"
      @memory="handleMemoryChange"
      @plan="handlePlanChange"
      @toolProcess="handelToolProcess"
      @toolContent="handelToolContent"
      @send="handleSend"
      @abort="abortRun"
    />
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/chat/index.scss' as *;
</style>
