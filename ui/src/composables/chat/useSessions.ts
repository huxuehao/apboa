import { ref, computed, watch } from 'vue'
import * as chatSessionApi from '@/api/chatSession'
import type { ChatSessionVO } from '@/types'

const PAGE_SIZE = 50

/**
 * 会话列表管理（支持分页加载）
 *
 * @param agentId 智能体ID
 */
export function useSessions(agentId: import('vue').Ref<string>) {
  const pinnedSessions = ref<ChatSessionVO[]>([])
  const otherSessions = ref<ChatSessionVO[]>([])

  // 合并所有会话（兼容 chatHistory 页面使用）
  const sessions = computed(() => [...pinnedSessions.value, ...otherSessions.value])

  // 分页状态
  const currentPage = ref(1)
  const hasMore = ref(true)
  const loading = ref(false)

  /**
   * 加载所有置顶会话
   */
  const loadPinnedSessions = async () => {
    if (!agentId.value) return
    try {
      const res = await chatSessionApi.pageSessions({
        agentId: agentId.value,
        isPinned: true,
        current: 1,
        size: 999,
      })
      pinnedSessions.value = (res.data?.data?.records ?? []) as ChatSessionVO[]
    } catch {
      pinnedSessions.value = []
    }
  }

  /**
   * 分页加载非置顶会话（追加模式）
   */
  const loadMoreSessions = async () => {
    if (!agentId.value || loading.value || !hasMore.value) return
    loading.value = true
    try {
      const res = await chatSessionApi.pageSessions({
        agentId: agentId.value,
        isPinned: false,
        current: currentPage.value,
        size: PAGE_SIZE,
      })
      const pageData = res.data?.data
      if (pageData) {
        const records = (pageData.records ?? []) as ChatSessionVO[]
        if (currentPage.value === 1) {
          otherSessions.value = records
        } else {
          otherSessions.value = [...otherSessions.value, ...records]
        }
        hasMore.value = currentPage.value < pageData.pages
        currentPage.value++
      }
    } catch {
      // 加载失败不清空已有数据
    } finally {
      loading.value = false
    }
  }

  /**
   * 分页加载所有会话（不区分置顶，用于 chatHistory 页面）
   */
  const loadMoreAllSessions = async () => {
    if (!agentId.value || loading.value || !hasMore.value) return
    loading.value = true
    try {
      const res = await chatSessionApi.pageSessions({
        agentId: agentId.value,
        current: currentPage.value,
        size: PAGE_SIZE,
      })
      const pageData = res.data?.data
      if (pageData) {
        const records = (pageData.records ?? []) as ChatSessionVO[]
        if (currentPage.value === 1) {
          pinnedSessions.value = []
          otherSessions.value = records
        } else {
          otherSessions.value = [...otherSessions.value, ...records]
        }
        hasMore.value = currentPage.value < pageData.pages
        currentPage.value++
      }
    } catch {
      // 加载失败不清空已有数据
    } finally {
      loading.value = false
    }
  }

  /**
   * 重置分页状态并重新加载（置顶 + 非置顶）
   */
  const resetAndReload = async () => {
    currentPage.value = 1
    hasMore.value = true
    otherSessions.value = []
    await loadPinnedSessions()
    await loadMoreSessions()
  }

  /**
   * 重置分页状态并重新加载所有会话（chatHistory 页面）
   */
  const resetAndReloadAll = async () => {
    currentPage.value = 1
    hasMore.value = true
    pinnedSessions.value = []
    otherSessions.value = []
    await loadMoreAllSessions()
  }

  /**
   * 兼容原有 loadSessions 方法
   */
  const loadSessions = async () => {
    await resetAndReload()
  }

  const createSession = async (title: string = '新对话') => {
    const res = await chatSessionApi.createSession({ agentId: agentId.value, title })
    const session = res.data?.data as ChatSessionVO
    if (session) {
      otherSessions.value = [session, ...otherSessions.value]
      return session
    }
    return null
  }

  const updateSessionTitle = async (sessionId: string | number, title: string) => {
    await chatSessionApi.updateSessionTitle(String(sessionId), title)
    // 更新本地数据中的标题
    const updateTitle = (list: ChatSessionVO[]) => {
      const item = list.find((s) => String(s.id) === String(sessionId))
      if (item) item.title = title
    }
    updateTitle(pinnedSessions.value)
    updateTitle(otherSessions.value)
  }

  const pinSession = async (sessionId: string | number) => {
    await chatSessionApi.pinSession(String(sessionId))
    await resetAndReload()
  }

  const unpinSession = async (sessionId: string | number) => {
    await chatSessionApi.unpinSession(String(sessionId))
    await resetAndReload()
  }

  const deleteSession = async (sessionId: string | number) => {
    await chatSessionApi.deleteSession(String(sessionId))
    await resetAndReload()
  }

  const deleteSessionForAll = async (sessionId: string | number) => {
    await chatSessionApi.deleteSession(String(sessionId))
    await resetAndReloadAll()
  }

  watch(agentId, resetAndReload)

  return {
    sessions,
    pinnedSessions,
    otherSessions,
    loading,
    hasMore,
    loadSessions,
    loadPinnedSessions,
    loadMoreSessions,
    loadMoreAllSessions,
    resetAndReload,
    resetAndReloadAll,
    createSession,
    updateSessionTitle,
    pinSession,
    unpinSession,
    deleteSession,
    deleteSessionForAll,
  }
}
