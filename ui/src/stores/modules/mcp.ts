/**
 * MCP服务器配置状态管理
 *
 * @author huxuehao
 */

import { defineStore } from 'pinia'
import { ref } from 'vue'
import type {McpServerVO, McpServerDTO, McpServer} from '@/types'
import { McpProtocol } from '@/types'
import * as mcpApi from '@/api/mcp'
import { message } from 'ant-design-vue'

export const useMcpStore = defineStore('mcp', () => {
  const list = ref<McpServerVO[]>([])
  const selectedProtocol = ref<string | null>(null)
  const keyword = ref<string>('')
  const loading = ref<boolean>(false)
  const hasMore = ref<boolean>(true)
  const currentPage = ref<number>(1)
  const pageSize = ref<number>(50)

  /**
   * 加载分页数据
   *
   * @param page 页码
   */
  async function fetchPage(page: number) {
    if (loading.value) return

    loading.value = true
    try {
      const query: McpServerDTO = {
        page,
        size: pageSize.value,
        protocol: selectedProtocol.value as McpProtocol | undefined,
        name: keyword.value || undefined
      }

      const response = await mcpApi.page(query)
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
   * 设置协议类型（仅更新状态，不触发加载）
   *
   * @param protocol 协议类型
   */
  function setProtocol(protocol: string | null) {
    selectedProtocol.value = protocol
  }

  /**
   * 设置搜索关键词（仅更新状态，不触发加载）
   *
   * @param value 关键词
   */
  function setKeyword(value: string) {
    keyword.value = value
  }

  /**
   * 重置并重新加载
   */
  async function resetAndFetch() {
    list.value = []
    currentPage.value = 1
    hasMore.value = true
    await fetchPage(1)
  }

  /**
   * 重置分页状态（不加载数据）
   */
  function resetPagination() {
    currentPage.value = 1
    hasMore.value = true
  }

  /**
   * 删除配置
   *
   * @param id 配置ID
   */
  async function deleteConfig(id: string) {
    await mcpApi.remove([id])
    message.success('删除成功')
  }

  /**
   * 检查是否被智能体使用
   *
   * @param id 配置ID
   * @returns 是否被使用
   */
  async function checkUsedWithAgent(id: string): Promise<string[]> {
    const response = await mcpApi.usedWithAgent([id])
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

    const entity: McpServer = {
      id: item.id,
      enabled,
    } as McpServer

    await mcpApi.update(entity)
    item.enabled = enabled
    message.success('操作成功')
  }

  return {
    list,
    selectedProtocol,
    keyword,
    loading,
    hasMore,
    fetchPage,
    loadMore,
    setProtocol,
    setKeyword,
    resetAndFetch,
    resetPagination,
    deleteConfig,
    checkUsedWithAgent,
    toggleEnabled
  }
})
