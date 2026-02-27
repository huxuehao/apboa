/**
 * 钩子配置状态管理
 *
 * @author huxuehao
 */

import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { HookConfigVO, HookConfig, HookConfigDTO, HookType } from '@/types'
import * as hookApi from '@/api/hook'
import { message } from 'ant-design-vue'

export const useHookStore = defineStore('hook', () => {
  const list = ref<HookConfigVO[]>([])
  const selectedHookType = ref<HookType | null>(null)
  const keyword = ref<string>('')
  const loading = ref<boolean>(false)
  const hasMore = ref<boolean>(true)
  const currentPage = ref<number>(1)
  const pageSize = ref<number>(20)

  /**
   * 加载分页数据
   *
   * @param page 页码
   */
  async function fetchPage(page: number) {
    if (loading.value) return

    loading.value = true
    try {
      const query: HookConfigDTO = {
        page,
        size: pageSize.value,
        hookType: selectedHookType.value ?? undefined,
        name: keyword.value || undefined
      }

      const response = await hookApi.page(query)
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
   * 设置钩子类型
   *
   * @param hookType 钩子类型
   */
  function setHookType(hookType: HookType | null) {
    selectedHookType.value = hookType
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
   * 切换启用状态
   *
   * @param id 配置ID
   * @param enabled 启用状态
   */
  async function toggleEnabled(id: string, enabled: boolean) {
    const item = list.value.find((x) => x.id === id)
    if (!item) return

    const entity: HookConfig = {
      ...item,
      enabled
    }

    await hookApi.update(entity)
    item.enabled = enabled
    message.success('操作成功')
  }

  /**
   * 删除配置
   *
   * @param id 配置ID
   */
  async function deleteConfig(id: string) {
    await hookApi.remove([id])
    message.success('删除成功')
    resetAndFetch()
  }

  /**
   * 检查是否被智能体使用
   *
   * @param id 配置ID
   * @returns 使用该钩子的智能体名称列表
   */
  async function checkUsedWithAgent(id: string): Promise<string[]> {
    const response = await hookApi.usedWithAgent([id])
    return (response.data.data as string[]) || []
  }

  return {
    list,
    selectedHookType,
    keyword,
    loading,
    hasMore,
    fetchPage,
    loadMore,
    setHookType,
    setKeyword,
    resetAndFetch,
    toggleEnabled,
    deleteConfig,
    checkUsedWithAgent
  }
})
