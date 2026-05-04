import { ref, watch } from 'vue'
import * as chatSessionApi from '@/api/chatSession'
import type { ChatMessageVO } from '@/types'

/** 每次向上加载的消息条数 */
const PAGE_SIZE = 30

export function useCurrentSession(agentId: import('vue').Ref<string>) {
  const currentSessionId = ref<string | null>(null)
  const currentSessionTitle = ref<string>('')
  const messagesList = ref<ChatMessageVO[]>([])

  /** 是否还有更早的历史消息 */
  const hasMoreHistory = ref(false)
  /** 加载中状态 */
  const historyLoading = ref(false)
  /** 下一页游标（本批最小 depth） */
  const nextBeforeDepth = ref<number | null>(null)

  /**
   * 首次加载：分页获取最新 PAGE_SIZE 条消息
   */
  const loadCurrentMessages = async () => {
    if (!currentSessionId.value) {
      messagesList.value = []
      hasMoreHistory.value = false
      nextBeforeDepth.value = null
      return
    }
    try {
      const res = await chatSessionApi.getCurrentMessagesPaged(
        currentSessionId.value,
        { beforeDepth: null, size: PAGE_SIZE }
      )
      const page = res.data?.data
      messagesList.value = (page?.messages ?? []) as ChatMessageVO[]
      hasMoreHistory.value = page?.hasMore ?? false
      nextBeforeDepth.value = page?.nextBeforeDepth ?? null
    } catch {
      messagesList.value = []
      hasMoreHistory.value = false
      nextBeforeDepth.value = null
    }
  }

  /**
   * 加载更早的历史消息，返回新加载的条数
   */
  const loadMoreHistory = async (): Promise<number> => {
    if (!currentSessionId.value || !hasMoreHistory.value || historyLoading.value) {
      return 0
    }
    historyLoading.value = true
    try {
      const res = await chatSessionApi.getCurrentMessagesPaged(
        currentSessionId.value,
        { beforeDepth: nextBeforeDepth.value, size: PAGE_SIZE }
      )
      const page = res.data?.data
      const older = (page?.messages ?? []) as ChatMessageVO[]
      if (older.length > 0) {
        // 追加到列表头部，保持顺序不变
        messagesList.value = [...older, ...messagesList.value]
      }
      hasMoreHistory.value = page?.hasMore ?? false
      nextBeforeDepth.value = page?.nextBeforeDepth ?? null
      return older.length
    } catch {
      return 0
    } finally {
      historyLoading.value = false
    }
  }

  const selectSession = async (session: { id: string | number; title?: string }) => {
    currentSessionId.value = String(session.id)
    currentSessionTitle.value = session.title || '新对话'
  }

  const resetSession = () => {
    currentSessionId.value = null
    currentSessionTitle.value = ''
    messagesList.value = []
    hasMoreHistory.value = false
    nextBeforeDepth.value = null
  }

  watch(currentSessionId, async () => {
    if (currentSessionId.value) await loadCurrentMessages()
    else {
      messagesList.value = []
      hasMoreHistory.value = false
      nextBeforeDepth.value = null
    }
  })

  return {
    currentSessionId,
    currentSessionTitle,
    messagesList,
    hasMoreHistory,
    historyLoading,
    loadCurrentMessages,
    loadMoreHistory,
    selectSession,
    resetSession,
  }
}
