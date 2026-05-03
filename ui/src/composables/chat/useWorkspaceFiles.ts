/**
 * 工作空间文件数据管理 Composable
 * 负责获取文件树、扁平化、缓存
 *
 * @author huxuehao
 */

import { ref, computed, watch, type Ref } from 'vue'
import * as workspaceApi from '@/api/workspace'
import type { WorkspaceFileNode } from '@/types'

/**
 * 扁平化后的文件项
 */
export interface FlatFileItem {
  /** 文件名称 */
  name: string
  /** 文件全名（含后缀） */
  fullName: string
  /** 文件在工作空间中的相对路径 */
  path: string
  /** 文件后缀（不含点号） */
  extension?: string
  /** 所属文件夹路径（如 "folder/subfolder"） */
  folderPath: string
  /** 文件大小可读格式 */
  readableSize?: string
}

/**
 * 递归扁平化文件树，只保留文件节点
 *
 * @param nodes 文件树节点列表
 * @param parentPath 父文件夹路径
 * @returns 扁平化文件列表
 */
export function flattenFileTree(
  nodes: WorkspaceFileNode[],
  parentPath: string = ''
): FlatFileItem[] {
  const result: FlatFileItem[] = []

  for (const node of nodes) {
    if (node.directory) {
      // 递归处理子目录
      const children = flattenFileTree(node.children ?? [], node.path)
      result.push(...children)
    } else {
      result.push({
        name: node.name,
        fullName: node.fullName || node.name,
        path: node.path,
        extension: node.extension,
        folderPath: parentPath,
        readableSize: node.readableSize
      })
    }
  }

  return result
}

/**
 * 使用工作空间文件
 *
 * @param sessionId 当前会话 ID
 * @returns 文件列表相关状态和方法
 */
export function useWorkspaceFiles(sessionId: Ref<string | null>) {
  /** 原始文件树 */
  const nodes = ref<WorkspaceFileNode[]>([])
  /** 是否加载中 */
  const loading = ref(false)

  /** 扁平化后的文件列表 */
  const flatFiles = computed<FlatFileItem[]>(() => flattenFileTree(nodes.value))

  /**
   * 获取文件列表
   */
  const fetchFiles = async () => {
    if (!sessionId.value) {
      nodes.value = []
      return
    }
    loading.value = true
    try {
      const res = await workspaceApi.listFiles(sessionId.value)
      nodes.value = res.data.data ?? []
    } finally {
      loading.value = false
    }
  }

  /**
   * 刷新文件列表
   */
  const refresh = () => fetchFiles()

  // 监听 sessionId 变化，自动刷新
  watch(
    () => sessionId.value,
    (newId) => {
      if (newId) {
        fetchFiles()
      } else {
        nodes.value = []
      }
    },
    { immediate: true }
  )

  return {
    nodes,
    flatFiles,
    loading,
    fetchFiles,
    refresh
  }
}
