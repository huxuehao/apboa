/**
 * 系统提示词模板状态管理
 *
 * @author huxuehao
 */

import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { SystemPromptTemplateVO, SystemPromptTemplate } from '@/types'
import * as promptApi from '@/api/prompt'
import { message } from 'ant-design-vue'

export const usePromptStore = defineStore('prompt', () => {
  const list = ref<SystemPromptTemplateVO[]>([])
  const categories = ref<string[]>([])
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
      const response = await promptApi.listCategories()
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
      const query = {
        page,
        size: pageSize.value,
        category: selectedCategory.value || undefined,
        name: keyword.value || undefined
      }

      const response = await promptApi.page(query)
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
   * 删除模板
   *
   * @param id 模板ID
   */
  async function deleteTemplate(id: string) {
    await promptApi.remove([id])
    message.success('删除成功')
  }

  /**
   * 检查是否被智能体使用
   *
   * @param id 模板ID
   * @returns 是否被使用
   */
  async function checkUsedWithAgent(id: string): Promise<string[]> {
    const response = await promptApi.usedWithAgent([id])
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

    const entity: SystemPromptTemplate = {
      id: item.id,
      enabled,
    } as SystemPromptTemplate

    await promptApi.update(entity)
    item.enabled = enabled
    message.success('操作成功')
  }

  return {
    list,
    categories,
    selectedCategory,
    keyword,
    loading,
    hasMore,
    fetchCategories,
    fetchPage,
    loadMore,
    setCategory,
    setKeyword,
    resetAndFetch,
    resetPagination,
    deleteTemplate,
    checkUsedWithAgent,
    toggleEnabled
  }
})
