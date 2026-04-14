/**
 * 工具配置状态管理
 *
 * @author huxuehao
 */

import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { ToolVO, ToolConfig, ToolDTO } from '@/types'
import { CodeLanguage, ToolType } from '@/types'
import * as toolApi from '@/api/tool'
import { message } from 'ant-design-vue'

export const useToolStore = defineStore('tool', () => {
  const list = ref<ToolVO[]>([])
  const categories = ref<string[]>([])
  const selectedToolType = ref<string | null>(null)
  const selectedCategory = ref<string | null>(null)
  const keyword = ref<string>('')
  const loading = ref<boolean>(false)
  const hasMore = ref<boolean>(true)
  const currentPage = ref<number>(1)
  const pageSize = ref<number>(50)

  /**
   * 获取分类列表
   */
  async function fetchCategories() {
    try {
      const response = await toolApi.listCategories()
      categories.value = response.data.data || []
    } catch (error) {
      console.error('获取分类列表失败:', error)
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
      const query: ToolDTO = {
        page,
        size: pageSize.value,
        toolType: selectedToolType.value as ToolType | undefined,
        category: selectedCategory.value || undefined,
        name: keyword.value || undefined
      }

      const response = await toolApi.page(query)
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
   * 设置工具类型（仅更新状态，不触发加载）
   *
   * @param toolType 工具类型
   */
  function setToolType(toolType: string | null) {
    selectedToolType.value = toolType
  }

  /**
   * 设置当前分类（仅更新状态，不触发加载）
   *
   * @param category 分类名称
   */
  function setCategory(category: string | null) {
    selectedCategory.value = category
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
   * 切换启用状态
   *
   * @param id 配置ID
   * @param enabled 启用状态
   */
  async function toggleEnabled(id: string, enabled: boolean) {
    const item = list.value.find((x) => x.id === id)
    if (!item) return

    const entity: ToolConfig = {
      id: item.id,
      enabled,
    } as ToolConfig

    await toolApi.update(entity)
    item.enabled = enabled
    message.success('操作成功')
  }

  /**
   * 删除配置
   *
   * @param id 配置ID
   */
  async function deleteConfig(id: string) {
    await toolApi.remove([id])
    message.success('删除成功')
  }

  /**
   * 检查是否被智能体使用
   *
   * @param id 配置ID
   * @returns 是否被使用
   */
  async function checkUsedWithAgent(id: string): Promise<string[]> {
    const response = await toolApi.usedWithAgent([id])
    return response.data.data as string[] || []
  }

  return {
    list,
    categories,
    selectedToolType,
    selectedCategory,
    keyword,
    loading,
    hasMore,
    fetchCategories,
    fetchPage,
    loadMore,
    setToolType,
    setCategory,
    setKeyword,
    resetAndFetch,
    resetPagination,
    toggleEnabled,
    deleteConfig,
    checkUsedWithAgent
  }
})
