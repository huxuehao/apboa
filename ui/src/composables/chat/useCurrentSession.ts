import { ref, watch } from 'vue'
import * as chatSessionApi from '@/api/chatSession'
import type { ChatMessageVO } from '@/types'

export function useCurrentSession(agentId: import('vue').Ref<string>) {
  const currentSessionId = ref<string | null>(null)
  const currentSessionTitle = ref<string>('')
  const messagesList = ref<ChatMessageVO[]>([])

  const loadCurrentMessages = async () => {
    if (!currentSessionId.value) {
      messagesList.value = []
      return
    }
    try {
      const res = await chatSessionApi.getCurrentMessages(currentSessionId.value)
      messagesList.value = (res.data?.data ?? []) as ChatMessageVO[]
    } catch {
      messagesList.value = []
    }
  }

  const selectSession = async (session: { id: string | number; title?: string }) => {
    currentSessionId.value = String(session.id)
    currentSessionTitle.value = session.title || '新对话'
    await loadCurrentMessages()
  }

  const resetSession = () => {
    currentSessionId.value = null
    currentSessionTitle.value = ''
    messagesList.value = []
  }

  watch(currentSessionId, () => {
    if (currentSessionId.value) loadCurrentMessages()
    else messagesList.value = []
  })

  return {
    currentSessionId,
    currentSessionTitle,
    messagesList,
    loadCurrentMessages,
    selectSession,
    resetSession,
  }
}
