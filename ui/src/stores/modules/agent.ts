/**
 * 智能体状态管理
 *
 * @author huxuehao
 */

import { defineStore } from 'pinia'
import { ref } from 'vue'
import type {AgentDefinitionVO, AgentDefinitionDTO, AgentDefinition} from '@/types'
import * as agentApi from '@/api/agent'
import { message } from 'ant-design-vue'

export const useAgentStore = defineStore('agent', () => {
  const list = ref<AgentDefinitionVO[]>([])
  const tags = ref<string[]>([])
  const selectedAgentType = ref<string | null>('AGENT')
  const selectedTag = ref<string | null>(null)
  const keyword = ref<string>('')
  const loading = ref<boolean>(false)
  const hasMore = ref<boolean>(true)
  const currentPage = ref<number>(1)
  const pageSize = ref<number>(20)

  /**
   * 获取标签列表
   */
  async function fetchTags() {
    try {
      const response = await agentApi.listTags()
      tags.value = response.data.data || []
    } catch (error) {
      console.error('获取标签列表失败:', error)
    }
  }

  /**
   * 加载分页数据
   *
   * @param page 页码
   */
  async function fetchPage(page: number) {
    if (loading.value) return

    loading.value = true
    try {
      const query: AgentDefinitionDTO = {
        page,
        size: pageSize.value,
        name: keyword.value || undefined,
        tag: selectedTag.value || undefined
      }

      const response = await agentApi.page(query)
      const result = response.data.data

      if (page === 1) {
        list.value = result.records || []
      } else {
        list.value.push(...(result.records || []))
      }

      hasMore.value = list.value.length < result.total
      currentPage.value = page
    } catch (error) {
      console.error('加载数据失败:', error)
    } finally {
      loading.value = false
    }
  }

  /**
   * 加载更多数据
   */
  async function loadMore() {
    if (!hasMore.value || loading.value) return
    await fetchPage(currentPage.value + 1)
  }

  /**
   * 设置智能体类型
   *
   * @param agentType 智能体类型
   */
  function setAgentType(agentType: string | null) {
    selectedAgentType.value = agentType
    resetAndFetch()
  }

  /**
   * 设置当前标签
   *
   * @param tag 标签名称
   */
  function setTag(tag: string | null) {
    selectedTag.value = tag
    resetAndFetch()
  }

  /**
   * 设置搜索关键词
   *
   * @param value 关键词
   */
  function setKeyword(value: string) {
    keyword.value = value
    resetAndFetch()
  }

  /**
   * 重置并重新加载
   */
  function resetAndFetch() {
    list.value = []
    currentPage.value = 1
    hasMore.value = true
    fetchPage(1)
  }

  /**
   * 删除配置
   *
   * @param id 配置ID
   */
  async function deleteConfig(id: string) {
    await agentApi.remove([id])
    message.success('删除成功')
  }

  /**
   * 检查是否被智能体使用
   *
   * @param id 配置ID
   * @returns 是否被使用
   */
  async function checkUsedWithAgent(id: string): Promise<string[]> {
    const response = await agentApi.usedWithAgent([id])
    return response.data.data as string[] || []
  }

  /**
   * 切换启用状态
   *
   * @param id 配置ID
   * @param enabled 启用状态
   */
  async function toggleEnabled(id: string, enabled: boolean) {
    const item = list.value.find((x) => x.id === id)
    if (!item) return

    const entity: AgentDefinitionVO = {
      id: item.id,
      enabled,
    } as AgentDefinitionVO

    await agentApi.update(entity)
    item.enabled = enabled
    message.success('操作成功')
  }

  return {
    list,
    tags,
    selectedAgentType,
    selectedTag,
    keyword,
    loading,
    hasMore,
    fetchTags,
    fetchPage,
    loadMore,
    setAgentType,
    setTag,
    setKeyword,
    resetAndFetch,
    deleteConfig,
    checkUsedWithAgent,
    toggleEnabled
  }
})
