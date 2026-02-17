/**
 * 模型供应商状态管理
 *
 * @author huxuehao
 */

import { defineStore } from 'pinia'
import { ref } from 'vue'
import type {ModelProviderVO, ModelProviderDTO, ModelProvider, ModelConfig} from '@/types'
import * as modelApi from '@/api/model'
import { message } from 'ant-design-vue'
import {providerUpdate} from "@/api/model";

export const useModelStore = defineStore('model', () => {
  const providerList = ref<ModelProviderVO[]>([])
  const selectedProviderType = ref<string | null>(null)
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
  async function fetchProviderPage(page: number) {
    if (loading.value) return

    loading.value = true
    try {
      const query: ModelProviderDTO = {
        page,
        size: pageSize.value,
        type: selectedProviderType.value || undefined,
        name: keyword.value || undefined
      }

      const response = await modelApi.providerPage(query)
      const result = response.data.data

      if (page === 1) {
        providerList.value = result.records || []
      } else {
        providerList.value.push(...(result.records || []))
      }

      hasMore.value = providerList.value.length < result.total
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
    await fetchProviderPage(currentPage.value + 1)
  }

  /**
   * 设置供应商类型
   *
   * @param type 供应商类型
   */
  function setProviderType(type: string | null) {
    selectedProviderType.value = type
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
    providerList.value = []
    currentPage.value = 1
    hasMore.value = true
    fetchProviderPage(1)
  }

  /**
   * 删除供应商
   *
   * @param id 供应商ID
   */
  async function deleteProvider(id: string) {
    await modelApi.providerRemove([id])
    message.success('删除成功')
    resetAndFetch()
  }

  /**
   * 检查是否被模型使用
   *
   * @param id 供应商ID
   * @returns 是否被使用
   */
  async function checkUsedWithModel(id: string): Promise<boolean> {
    try {
      const response = await modelApi.providerUsedWithModel([id])
      return (response.data.data || []).length > 0
    } catch (error) {
      console.error('检查使用情况失败:', error)
      return false
    }
  }

  /**
   * 切换启用状态
   *
   * @param id 配置ID
   * @param enabled 启用状态
   */
  async function toggleEnabled(id: string, enabled: boolean) {
    const item = providerList.value.find((x) => x.id === id)
    if (!item) return

    const entity: ModelProvider = {
      id: item.id,
      enabled,
    } as ModelProvider

    await modelApi.providerUpdate(entity)
    item.enabled = enabled
    message.success('操作成功')
  }

  return {
    providerList,
    selectedProviderType,
    keyword,
    loading,
    hasMore,
    fetchProviderPage,
    loadMore,
    setProviderType,
    setKeyword,
    resetAndFetch,
    deleteProvider,
    checkUsedWithModel,
    toggleEnabled
  }
})
