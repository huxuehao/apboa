<script setup lang="ts">
import { ref } from 'vue'
import { FolderOutlined, FolderOpenOutlined } from '@ant-design/icons-vue'
import WorkspaceFileItem from './WorkspaceFileItem.vue'
import type { WorkspaceFileNode } from '@/types'

/**
 * 工作空间文件夹节点组件（递归）
 */
const props = defineProps<{
  /** 文件夹节点数据 */
  node: WorkspaceFileNode
  /** 缩进层级 */
  depth: number
  /** 已选中的文件路径集合 */
  selectedPaths: Set<string>
  /** 是否处于多选模式 */
  multiSelect: boolean
  /** 正在被操作的文件路径集合 */
  operatingPaths: Set<string>
}>()

const emit = defineEmits<{
  (e: 'select', path: string): void
  (e: 'download-file', node: WorkspaceFileNode): void
  (e: 'delete-file', node: WorkspaceFileNode): void
}>()

/** 文件夹展开状态，默认展开 */
const expanded = ref(true)

/** 文件夹行 padding */
const paddingLeft = `${props.depth * 16 + 8}px`

/** 子节点数量（含文件和子文件夹） */
const childCount = props.node.children?.length ?? 0

/**
 * 双击切换展开/折叠
 */
const handleDblClick = () => {
  expanded.value = !expanded.value
}
</script>

<template>
  <div class="workspace-folder-item">
    <!-- 文件夹行 -->
    <div
      class="workspace-item-row"
      :style="{ paddingLeft }"
      @dblclick="handleDblClick"
    >
      <!-- 文件夹图标 -->
      <span class="workspace-item-icon workspace-folder-icon">
        <FolderOpenOutlined v-if="expanded" />
        <FolderOutlined v-else />
      </span>

      <!-- 文件夹名称 -->
      <span class="workspace-item-name" :title="node.name">
        {{ node.name }}
        <span v-if="childCount > 0" class="workspace-folder-count">
          ({{ childCount }})
        </span>
      </span>

      <!-- 子节点数量 -->

      <!-- 双击提示（hover 时淡入，不喧宾夺主） -->
      <span class="workspace-item-hint">双击{{ expanded ? '折叠' : '展开' }}</span>
    </div>

    <!-- 子节点列表 -->
    <div v-show="expanded" class="workspace-folder-children">
      <template v-if="node.children && node.children.length">
        <template v-for="child in node.children" :key="child.path">
          <!-- 子文件夹：递归 -->
          <WorkspaceFolderItem
            v-if="child.directory"
            :node="child"
            :depth="depth + 1"
            :selected-paths="selectedPaths"
            :multi-select="multiSelect"
            :operating-paths="operatingPaths"
            @select="emit('select', $event)"
            @download-file="emit('download-file', $event)"
            @delete-file="emit('delete-file', $event)"
          />
          <!-- 子文件 -->
          <WorkspaceFileItem
            v-else
            :node="child"
            :depth="depth + 1"
            :selected="selectedPaths.has(child.path)"
            :multi-select="multiSelect"
            :operating="operatingPaths.has(child.path)"
            @select="emit('select', $event)"
            @download="emit('download-file', $event)"
            @delete="emit('delete-file', $event)"
          />
        </template>
      </template>
    </div>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/chat/workspace.scss' as *;
</style>
