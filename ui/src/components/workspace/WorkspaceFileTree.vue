<script setup lang="ts">
import { ref } from 'vue'
import { FolderOpenOutlined } from '@ant-design/icons-vue'
import WorkspaceFolderItem from './WorkspaceFolderItem.vue'
import WorkspaceFileItem from './WorkspaceFileItem.vue'
import type { WorkspaceFileNode } from '@/types'

/**
 * 工作空间文件树组件
 * 支持拖拽上传、空状态占位、递归渲染文件树
 */
defineProps<{
  /** 文件树节点列表 */
  nodes: WorkspaceFileNode[]
  /** 当前会话 ID */
  sessionId: string | null
  /** 是否加载中 */
  loading: boolean
  /** 已选中的文件路径集合 */
  selectedPaths: Set<string>
  /** 是否处于多选模式 */
  multiSelect: boolean
  /** 正在被操作的文件路径集合 */
  operatingPaths: Set<string>
}>()

const emit = defineEmits<{
  (e: 'select', path: string): void
  (e: 'preview', node: WorkspaceFileNode): void
  (e: 'download-file', node: WorkspaceFileNode): void
  (e: 'delete-file', node: WorkspaceFileNode): void
  (e: 'drop-upload', files: File[]): void
}>()

/** 拖拽悬停状态 */
const dropActive = ref(false)

/**
 * 阻止默认拖拽行为并激活高亮
 */
const handleDragOver = (e: DragEvent) => {
  e.preventDefault()
  dropActive.value = true
}

/**
 * 离开拖拽区域时取消高亮
 */
const handleDragLeave = (e: DragEvent) => {
  // 仅当真正离开整个区域时取消（避免进入子元素触发）
  const rect = (e.currentTarget as HTMLElement).getBoundingClientRect()
  const { clientX, clientY } = e
  if (
    clientX < rect.left ||
    clientX >= rect.right ||
    clientY < rect.top ||
    clientY >= rect.bottom
  ) {
    dropActive.value = false
  }
}

/**
 * 处理文件放置，收集文件并向上传递
 */
const handleDrop = (e: DragEvent) => {
  e.preventDefault()
  dropActive.value = false
  const files = Array.from(e.dataTransfer?.files ?? [])
  if (files.length) {
    emit('drop-upload', files)
  }
}
</script>

<template>
  <div
    class="workspace-file-tree"
    :class="{ 'drop-active': dropActive }"
    @dragover="handleDragOver"
    @dragleave="handleDragLeave"
    @drop="handleDrop"
  >
    <!-- 加载中 -->
    <div v-if="loading" class="workspace-empty">
      <ASpin />
    </div>

    <!-- 空状态 -->
    <div v-else-if="!nodes.length" class="workspace-empty">
      <span class="workspace-empty-icon">
        <FolderOpenOutlined />
      </span>
      <span class="workspace-empty-title">工作空间暂无文件</span>
      <span class="workspace-empty-desc">
        智能体操作的文件将在此处展示
      </span>
      <div class="workspace-drop-hint">
        将文件拖放到此处可快速上传
      </div>
    </div>

    <!-- 文件树列表 -->
    <template v-else>
      <template v-for="node in nodes" :key="node.path">
        <!-- 文件夹 -->
        <WorkspaceFolderItem
          v-if="node.directory"
          :node="node"
          :depth="0"
          :selected-paths="selectedPaths"
          :multi-select="multiSelect"
          :operating-paths="operatingPaths"
          @select="emit('select', $event)"
          @download-file="emit('download-file', $event)"
          @delete-file="emit('delete-file', $event)"
        />
        <!-- 文件 -->
        <WorkspaceFileItem
          v-else
          :node="node"
          :depth="0"
          :selected="selectedPaths.has(node.path)"
          :multi-select="multiSelect"
          :operating="operatingPaths.has(node.path)"
          @select="emit('select', $event)"
          @preview="emit('preview', $event)"
          @download="emit('download-file', $event)"
          @delete="emit('delete-file', $event)"
        />
      </template>
    </template>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/chat/workspace.scss' as *;
</style>
