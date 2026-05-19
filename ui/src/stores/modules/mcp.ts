/**
 * MCP 服务配置状态管理
 *
 * @author huxuehao
 */

import { defineStore } from 'pinia'
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import type { McpServer, McpServerDTO, McpServerVO } from '@/types'
import { McpActivationStatus, McpProtocol } from '@/types'
import * as mcpApi from '@/api/mcp'

export const useMcpStore = defineStore('mcp', () => {
  const list = ref<McpServerVO[]>([])
  const selectedProtocol = ref<string | null>(null)
  const keyword = ref('')
  const loading = ref(false)
  const hasMore = ref(true)
  const currentPage = ref(1)
  const pageSize = ref(50)

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
      throw error
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
   * 设置协议类型
   *
   * @param protocol 协议类型
   */
  function setProtocol(protocol: string | null) {
    selectedProtocol.value = protocol
  }

  /**
   * 设置搜索关键字
   *
   * @param value 关键字
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
   * 重置分页状态
   */
  function resetPagination() {
    currentPage.value = 1
    hasMore.value = true
  }

  /**
   * 删除配置
   *
   * @param id 配置 ID
   */
  async function deleteConfig(id: string) {
    await mcpApi.remove([id])
    message.success('删除成功')
  }

  /**
   * 检查是否被智能体使用
   *
   * @param id 配置 ID
   */
  async function checkUsedWithAgent(id: string): Promise<string[]> {
    const response = await mcpApi.usedWithAgent([id])
    return response.data.data as string[] || []
  }

  /**
   * 切换启用状态
   *
   * @param id 配置 ID
   * @param enabled 启用状态
   */
  async function toggleEnabled(id: string, enabled: boolean) {
    const item = list.value.find((x) => x.id === id)
    if (!item) return

    const entity: McpServer = {
      id: item.id,
      enabled
    } as McpServer

    const response = await mcpApi.update(entity)
    patchItem(response.data.data)
    message.success(enabled ? '已启用' : '已停用')
  }

  /**
   * 手动连接
   *
   * @param id MCP ID
   * @param action 操作文案
   */
  async function activateServer(id: string, action = '连接') {
    // 乐观更新：先将状态置为激活中，让卡片立即显示加载动画
    const item = list.value.find((x) => x.id === id)
    if (item) {
      item.activationStatus = McpActivationStatus.ACTIVATING
      item.activationMessage = '正在连接 MCP 并刷新工具目录'
    }

    const response = await mcpApi.activate(id)
    patchItem(response.data.data)
    showConnectionMessage(response.data.data, action)
    return response.data.data
  }

  /**
   * 手动刷新工具目录
   *
   * @param id MCP ID
   * @param action 操作文案
   */
  async function syncServerTools(id: string, action = '刷新工具') {
    // 乐观更新：先将状态置为激活中，让卡片立即显示加载动画
    const item = list.value.find((x) => x.id === id)
    if (item) {
      item.activationStatus = McpActivationStatus.ACTIVATING
      item.activationMessage = '正在刷新工具目录'
    }

    const response = await mcpApi.syncTools(id)
    patchItem(response.data.data)
    showConnectionMessage(response.data.data, action)
    return response.data.data
  }

  function patchItem(next: McpServerVO) {
    const index = list.value.findIndex(item => item.id === next.id)
    if (index >= 0) {
      list.value[index] = next
    }
  }

  function showConnectionMessage(server: McpServerVO, action: string) {
    if (server.activationStatus === McpActivationStatus.FAILED) {
      message.error(`${action}失败，${server.activationMessage || '请检查 MCP 配置'}`)
      return
    }

    if (server.toolCount === 0) {
      message.warning(`${action}完成：连接成功，但未发现可用工具`)
      return
    }

    message.success(`${action}成功`)
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
    toggleEnabled,
    activateServer,
    syncServerTools
  }
})
