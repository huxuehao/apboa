import { ref, computed, watch } from 'vue'
import { message } from 'ant-design-vue'
import * as chatSessionApi from '@/api/chatSession'
import type { ChatSessionVO } from '@/types'

export function useSessions(agentId: import('vue').Ref<string>) {
  const sessions = ref<ChatSessionVO[]>([])

  const pinnedSessions = computed(() => sessions.value.filter((s) => s.isPinned))
  const otherSessions = computed(() => sessions.value.filter((s) => !s.isPinned))

  const loadSessions = async () => {
    if (!agentId.value) return
    try {
      const res = await chatSessionApi.listSessions({ agentId: agentId.value })
      sessions.value = (res.data?.data ?? []) as ChatSessionVO[]
    } catch {
      sessions.value = []
    }
  }

  const createSession = async (title: string = '新对话') => {
    try {
      const res = await chatSessionApi.createSession({ agentId: agentId.value, title })
      const session = res.data?.data as ChatSessionVO
      if (session) {
        sessions.value = [session, ...sessions.value]
        return session
      }
    } catch {
      message.error('创建会话失败')
    }
    return null
  }

  const updateSessionTitle = async (sessionId: string | number, title: string) => {
    try {
      await chatSessionApi.updateSessionTitle(String(sessionId), title)
      await loadSessions()
    } catch {
      message.error('更新标题失败')
    }
  }

  const pinSession = async (sessionId: string | number) => {
    try {
      await chatSessionApi.pinSession(String(sessionId))
      await loadSessions()
    } catch {
      message.error('操作失败')
    }
  }

  const unpinSession = async (sessionId: string | number) => {
    try {
      await chatSessionApi.unpinSession(String(sessionId))
      await loadSessions()
    } catch {
      message.error('操作失败')
    }
  }

  const deleteSession = async (sessionId: string | number) => {
    try {
      await chatSessionApi.deleteSession(String(sessionId))
      await loadSessions()
    } catch {
      message.error('删除失败')
    }
  }

  watch(agentId, loadSessions, { immediate: true })

  return {
    sessions,
    pinnedSessions,
    otherSessions,
    loadSessions,
    createSession,
    updateSessionTitle,
    pinSession,
    unpinSession,
    deleteSession,
  }
}
