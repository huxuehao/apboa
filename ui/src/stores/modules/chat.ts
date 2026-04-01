/**
 * 对话页 UI 状态 Store
 *
 * 管理记忆/规划开关、侧边栏折叠等按 agent+account 隔离的 UI 偏好，
 * 使用 Pinia 持久化到 localStorage。
 *
 * @author huxuehao
 */

import { defineStore } from 'pinia'
import { ref } from 'vue'

const STORAGE_KEY = 'chat_agent_preferences'

/** 单个 agent 的 UI 偏好 */
export interface AgentPreference {
  memoryActive?: boolean
  planActive?: boolean
  toolProcessActive?: boolean
  sidebarCollapsed?: boolean
}

/** 偏好映射：key 为 `${agentId}_${accountId}` */
type PreferenceMap = Record<string, AgentPreference>

function preferenceKey(agentId: string, accountId: string | undefined): string {
  return `${agentId}_${accountId ?? 'anonymous'}`
}

export const useChatStore = defineStore(
  'chat',
  () => {
    const preferences = ref<PreferenceMap>({})

    /**
     * 获取当前 agent 的有效记忆开关状态
     * @param agentId 智能体 ID
     * @param accountId 账号 ID
     * @param enableMemory 智能体是否支持记忆
     */
    function getMemoryActive(
      agentId: string,
      accountId: string | undefined,
      enableMemory: boolean
    ): boolean {
      if (!agentId || !enableMemory) return false
      const key = preferenceKey(agentId, accountId)
      const stored = preferences.value[key]?.memoryActive
      return enableMemory && (stored ?? true)
    }

    /**
     * 设置记忆开关
     */
    function setMemoryActive(
      agentId: string,
      accountId: string | undefined,
      value: boolean
    ): void {
      if (!agentId) return
      const key = preferenceKey(agentId, accountId)
      if (!preferences.value[key]) preferences.value[key] = {}
      preferences.value[key].memoryActive = value
    }

    /**
     * 获取当前 agent 的有效规划开关状态
     */
    function getPlanActive(
      agentId: string,
      accountId: string | undefined,
      enablePlanning: boolean
    ): boolean {
      if (!agentId || !enablePlanning) return false
      const key = preferenceKey(agentId, accountId)
      const stored = preferences.value[key]?.planActive
      return enablePlanning && (stored ?? true)
    }

    /**
     * 设置规划开关
     */
    function setPlanActive(
      agentId: string,
      accountId: string | undefined,
      value: boolean
    ): void {
      if (!agentId) return
      const key = preferenceKey(agentId, accountId)
      if (!preferences.value[key]) preferences.value[key] = {}
      preferences.value[key].planActive = value
    }

    /**
     * 获取当前 agent 的有效规划开关状态
     */
    function getToolProcessActive(
      agentId: string,
      accountId: string | undefined,
      showToolProcess: boolean
    ): boolean {
      if (!agentId || !showToolProcess) return false
      const key = preferenceKey(agentId, accountId)
      const stored = preferences.value[key]?.toolProcessActive
      return showToolProcess && (stored ?? true)
    }

    /**
     * 设置规划开关
     */
    function setToolProcessActive(
      agentId: string,
      accountId: string | undefined,
      value: boolean
    ): void {
      if (!agentId) return
      const key = preferenceKey(agentId, accountId)
      if (!preferences.value[key]) preferences.value[key] = {}
      preferences.value[key].toolProcessActive = value
    }

    /**
     * 获取侧边栏折叠状态
     */
    function getSidebarCollapsed(
      agentId: string,
      accountId: string | undefined
    ): boolean {
      if (!agentId) return false
      const key = preferenceKey(agentId, accountId)
      return preferences.value[key]?.sidebarCollapsed ?? false
    }

    /**
     * 设置侧边栏折叠状态
     */
    function setSidebarCollapsed(
      agentId: string,
      accountId: string | undefined,
      value: boolean
    ): void {
      if (!agentId) return
      const key = preferenceKey(agentId, accountId)
      if (!preferences.value[key]) preferences.value[key] = {}
      preferences.value[key].sidebarCollapsed = value
    }

    return {
      preferences,
      getMemoryActive,
      setMemoryActive,
      getPlanActive,
      setPlanActive,
      getSidebarCollapsed,
      setSidebarCollapsed,
      getToolProcessActive,
      setToolProcessActive
    }
  },
  {
    persist: {
      key: STORAGE_KEY,
      storage: localStorage,
      pick: ['preferences'],
      debug: import.meta.env.DEV,
    },
  }
)
