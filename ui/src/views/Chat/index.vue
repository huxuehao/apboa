<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { Modal } from 'ant-design-vue'
import { useAccountStore } from '@/stores'
import { formatSessionTitle } from './utils/format'
import { useAgentDetail } from './composables/useAgentDetail'
import { useSessions } from './composables/useSessions'
import { useCurrentSession } from './composables/useCurrentSession'
import { useChatStream } from './composables/useChatStream'
import ChatSidebar from './components/ChatSidebar.vue'
import ChatMain from './components/ChatMain.vue'
import RenameModal from './components/RenameModal.vue'
import type { DisplayMessage } from './types'
import * as chatSessionApi from '@/api/chatSession'
import type { ChatMessageVO } from "@/types";

const route = useRoute()
const accountStore = useAccountStore()
const userInfo = computed(() => accountStore.userInfo)

const agentId = computed(() => (route.params.agentId as string) || '')

// 智能体详情
const { agentDetail } = useAgentDetail(agentId)

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

// 流式对话及工具调用
const {
  streamingContent,
  streamingMessageId,
  toolCallsInProgress,
  isRunning,
  sendMessage,
  sendToolContent,
  abortRun
} = useChatStream({
  agentId,
  agentDetail,
  currentSessionId,
  onMessageSaved: () => {
    loadCurrentMessages()
  },
})

// 输入框内容
const inputText = ref('')

// 构建展示消息
const displayMessages = computed<DisplayMessage[]>(() => {
  const list: DisplayMessage[] = []
  for (const m of messagesList.value) {
    if (m.role === 'system') continue
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
  }
  return list
})

const isWelcomeMode = computed(() => messagesList.value.length === 0 && !streamingMessageId.value)

// 侧边栏折叠状态
const sidebarCollapsed = ref(false)

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
  if (!text || !agentId.value || isRunning.value) return

  // 如果没有当前会话，先创建
  if (!currentSessionId.value) {
    const newSession = await createSession(formatSessionTitleFromInput(text))
    if (!newSession) return
    currentSessionId.value = String(newSession.id)
    currentSessionTitle.value = newSession.title || '新对话'
    await loadCurrentMessages()
  }

  // 保存用户消息
  await chatSessionApi.appendMessage(currentSessionId.value, { role: 'user', content: text })
  // 如果是新会话，更新标题
  if (messagesList.value.length === 0) {
    const title = formatSessionTitle(text)
    await updateSessionTitle(currentSessionId.value, title)
    currentSessionTitle.value = title
  }
  inputText.value = ''
  await loadCurrentMessages()

  // 触发流式回复
  await sendMessage(text, [{ role: 'user', content: text }] as ChatMessageVO[])
  // await sendMessage(text, messagesList.value)
}

// 切换侧边栏
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
      :isRunning="isRunning"
      @update:input-value="inputText = $event"
      @toolContent="handelToolContent"
      @send="handleSend"
      @abort="abortRun"
    />
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/chat/index.scss' as *;
</style>
