<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { CloseOutlined } from '@ant-design/icons-vue'
import WorkspaceToolbar from './WorkspaceToolbar.vue'
import WorkspaceFileTree from './WorkspaceFileTree.vue'
import * as workspaceApi from '@/api/workspace'
import type { WorkspaceFileNode } from '@/types'
import {Modal} from "ant-design-vue";

/**
 * 工作空间面板主组件
 * 负责文件树数据管理及所有文件操作逻辑
 */
const props = defineProps<{
  /** 当前会话 ID */
  sessionId: string | null
}>()

defineEmits(['close'])

/** 文件树节点列表 */
const nodes = ref<WorkspaceFileNode[]>([])
/** 是否加载中 */
const loading = ref(false)
/** 是否正在上传 */
const uploading = ref(false)
/** 是否处于多选模式 */
const multiSelect = ref(false)
/** 已选中的文件路径集合（响应式，使用 reactive Set） */
const selectedPaths = reactive(new Set<string>())
/** 正在被操作的文件路径集合 */
const operatingPaths = reactive(new Set<string>())

/** 是否存在文件 */
const hasFiles = computed(() => nodes.value.length > 0)

/** 已选中文件数量 */
const selectedCount = computed(() => selectedPaths.size)

/**
 * 收集树中所有文件的路径（递归）
 * @param nodeList 节点列表
 */
// eslint-disable-next-line @typescript-eslint/no-unused-vars
const collectFilePaths = (nodeList: WorkspaceFileNode[]): string[] => {
  const paths: string[] = []
  for (const n of nodeList) {
    if (n.directory) {
      paths.push(...collectFilePaths(n.children ?? []))
    } else {
      paths.push(n.path)
    }
  }
  return paths
}

/**
 * 根据路径在树中查找节点（递归）
 */
const findNodeByPath = (nodeList: WorkspaceFileNode[], path: string): WorkspaceFileNode | null => {
  for (const n of nodeList) {
    if (n.path === path) return n
    if (n.directory && n.children) {
      const found = findNodeByPath(n.children, path)
      if (found) return found
    }
  }
  return null
}

/**
 * 刷新文件树
 */
const fetchFiles = async () => {
  if (!props.sessionId) {
    nodes.value = []
    return
  }
  loading.value = true
  try {
    const res = await workspaceApi.listFiles(props.sessionId)
    nodes.value = res.data.data ?? []
  } finally {
    loading.value = false
  }
}

/**
 * 判断文件是否为压缩包
 */
const isArchive = (file: File): boolean => {
  const name = file.name.toLowerCase()
  return /\.(zip|tar|gz|rar|7z|bz2|xz|tgz)$/.test(name)
}

/**
 * 触发浏览器下载 Blob 数据
 */
const triggerBlobDownload = (blob: Blob, filename: string) => {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

/**
 * 处理上传（工具栏或拖拽触发）
 * 区分压缩包与普通文件分别调用不同接口
 */
const handleUpload = async (files: File[]) => {
  if (!props.sessionId || !files.length) return
  uploading.value = true
  try {
    const archives = files.filter(isArchive)
    const normals = files.filter(f => !isArchive(f))

    const tasks: Promise<unknown>[] = []
    // 压缩包逐个上传并解压
    for (const archive of archives) {
      tasks.push(workspaceApi.uploadArchive(props.sessionId, archive))
    }
    // 普通文件批量上传
    if (normals.length > 0) {
      tasks.push(workspaceApi.uploadBatch(props.sessionId, normals))
    }
    await Promise.all(tasks)
    await fetchFiles()
  } finally {
    uploading.value = false
  }
}

/**
 * 处理切换选中状态
 */
const handleSelect = (path: string) => {
  if (selectedPaths.has(path)) {
    selectedPaths.delete(path)
  } else {
    selectedPaths.add(path)
  }
}

/**
 * 处理删除选中文件（串行执行，兼容无批量删除接口）
 */
const handleDelete = async () => {
  Modal.confirm({
    title: '确认删除',
    content: `将删除所选文件，此操作不可恢复，是否继续？`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      if (!props.sessionId || selectedPaths.size === 0) return
      const paths = Array.from(selectedPaths)
      for (const p of paths) {
        operatingPaths.add(p)
      }
      try {
        for (const filePath of paths) {
          await workspaceApi.deleteFile(props.sessionId, filePath)
        }
        selectedPaths.clear()
        // 关闭多选模式
        multiSelect.value = false
        await fetchFiles()
      } finally {
        for (const p of paths) {
          operatingPaths.delete(p)
        }
      }
    },
  })
}

/**
 * 处理删除单个文件（来自右键菜单）
 */
const handleDeleteFile = async (node: WorkspaceFileNode) => {
  Modal.confirm({
    title: '确认删除',
    content: `将删除所选[${node.fullName}]，此操作不可恢复，是否继续？`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      if (!props.sessionId) return
      operatingPaths.add(node.path)
      try {
        await workspaceApi.deleteFile(props.sessionId, node.path)
        selectedPaths.delete(node.path)
        await fetchFiles()
      } finally {
        operatingPaths.delete(node.path)
      }
    },
  })
}

/**
 * 处理下载选中文件
 * 单个文件调用单文件下载接口，多个文件调用批量打包接口
 */
const handleDownload = async () => {
  if (!props.sessionId || selectedPaths.size === 0) return
  const sid = props.sessionId as string
  const paths = Array.from(selectedPaths)
  if (paths.length === 1) {
    const firstPath = paths[0]!
    const node = findNodeByPath(nodes.value, firstPath)
    if (node) {
      const filename = node.fullName ?? node.name
      const res = await workspaceApi.download(sid, filename)
      triggerBlobDownload(res.data as Blob, filename)
    }
  } else {
    const res = await workspaceApi.downloadBatch(sid, paths)
    triggerBlobDownload(res.data as Blob, 'workspace_files.zip')
  }
}

/**
 * 处理下载单个文件（来自右键菜单）
 */
const handleDownloadFile = async (node: WorkspaceFileNode) => {
  if (!props.sessionId) return
  const filename = node.fullName ?? node.name
  const res = await workspaceApi.download(props.sessionId, filename)
  triggerBlobDownload(res.data as Blob, filename)
}

/**
 * 处理清空工作空间
 */
const handleClear = async () => {
  if (!props.sessionId) return
  await workspaceApi.clearWorkspace(props.sessionId)
  selectedPaths.clear()
  multiSelect.value = false
  await fetchFiles()
}

// 监听 sessionId 变化，切换会话时刷新文件树
watch(
  () => props.sessionId,
  (newId) => {
    selectedPaths.clear()
    multiSelect.value = false
    if (newId) {
      fetchFiles()
    } else {
      nodes.value = []
    }
  },
  { immediate: true }
)

/**
 * 标记文件开始被操作（供外部或智能体调用）
 *
 * @param path 文件相对路径
 */
const startFileOperation = (path: string) => {
  operatingPaths.add(path)
}

/**
 * 标记文件操作结束（供外部或智能体调用）
 *
 * @param path 文件相对路径
 */
const stopFileOperation = (path: string) => {
  operatingPaths.delete(path)
}

/**
 * 手动刷新文件树（供外部调用）
 */
const refresh = () => {
  fetchFiles()
}

defineExpose({ startFileOperation, stopFileOperation, refresh })
</script>

<template>
  <div class="workspace-panel">
    <!-- 标题栏 -->
    <div class="workspace-panel-header">
      <span class="workspace-panel-title">工作空间</span>
      <AButton
        type="text"
        size="small"
        class="workspace-panel-close-btn"
        title="关闭"
        @click="$emit('close')"
      >
        <CloseOutlined />
      </AButton>
    </div>

    <!-- 工具栏 -->
    <WorkspaceToolbar
      :multi-select="multiSelect"
      :selected-count="selectedCount"
      :has-files="hasFiles"
      :uploading="uploading"
      @upload="handleUpload"
      @delete="handleDelete"
      @download="handleDownload"
      @clear="handleClear"
      @refresh="fetchFiles"
    />

    <!-- 文件树主体 -->
    <div class="workspace-body">
      <WorkspaceFileTree
        :nodes="nodes"
        :session-id="sessionId"
        :loading="loading"
        :selected-paths="selectedPaths"
        :multi-select="multiSelect"
        :operating-paths="operatingPaths"
        @select="handleSelect"
        @download-file="handleDownloadFile"
        @delete-file="handleDeleteFile"
        @drop-upload="handleUpload"
      />
    </div>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/chat/workspace.scss' as *;
</style>
