<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { Modal } from 'ant-design-vue'
import { useAgentDetail } from '@/views/Chat/composables/useAgentDetail'
import { useSessions } from '@/views/Chat/composables/useSessions'
import { useCurrentSession } from '@/views/Chat/composables/useCurrentSession'
import ChatSidebar from './components/ChatSidebar.vue'
import ChatMain from './components/ChatMain.vue'
import type { DisplayMessage } from './types'

const route = useRoute()

const agentId = computed(() => (route.params.agentId as string) || '')

// 智能体详情
const { agentDetail } = useAgentDetail(agentId)

// 会话列表管理
const {
  sessions,
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
} = useCurrentSession(agentId)

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
  return list
})

// 选择会话
const handleSelectSession = async (session: any) => {
  await selectSession(session)
}

// 会话菜单操作
const handleDeleteSession = async (session: any) => {
  const id = String(session.id)
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
}


// 初始化加载会话列表
onMounted(() => {
  loadSessions()
})
</script>

<template>
  <div class="chat-page">
    <ChatSidebar
      :agent-name="agentDetail?.name"
      :sessions="sessions"
      :current-session-id="currentSessionId"
      @select-session="handleSelectSession"
      @delete-session="handleDeleteSession"
    />

    <ChatMain
      ref="chatMainRef"
      :title="currentSessionTitle || agentDetail?.name || '对话'"
      :messages="displayMessages"
    />
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/chat/index.scss' as *;
</style>
