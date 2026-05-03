<script setup lang="ts">
import {
  EyeOutlined,
  DownloadOutlined,
  DeleteOutlined
} from '@ant-design/icons-vue'
import type { WorkspaceFileNode } from '@/types'
import FileIcon from "@/components/workspace/FileIcon.vue";

/**
 * 工作空间文件节点组件
 */
const props = defineProps<{
  /** 文件节点数据 */
  node: WorkspaceFileNode
  /** 是否被选中 */
  selected: boolean
  /** 是否处于多选模式 */
  multiSelect: boolean
  /** 是否正在被操作中（操作动画） */
  operating: boolean
  /** 缩进层级 */
  depth?: number
}>()

const emit = defineEmits<{
  (e: 'select', path: string): void
  (e: 'preview', node: WorkspaceFileNode): void
  (e: 'download', node: WorkspaceFileNode): void
  (e: 'delete', node: WorkspaceFileNode): void
}>()

const paddingLeft = `${(props.depth ?? 0) * 16 + 8}px`

/**
 * 处理行点击（多选模式下切换选中）
 */
const handleRowClick = () => {
  if (props.multiSelect) {
    emit('select', props.node.path)
  }
}
</script>

<template>
  <div
    class="workspace-item-row"
    :class="{
      'is-selected': selected,
      'is-operating': operating
    }"
    :style="{ paddingLeft }"
    @click="handleRowClick"
  >
    <!-- 操作中 loading -->
    <div v-if="operating" class="workspace-operating-spin" />

    <!-- 多选 checkbox -->
    <div v-else class="workspace-item-checkbox">
      <ACheckbox :disabled="operating" :checked="selected" @click.stop="emit('select', node.path)" />
    </div>

    <!-- 文件图标 -->
    <span class="workspace-item-icon workspace-file-icon">
      <FileIcon :file-name="node.fullName" width="17" />
    </span>

    <!-- 文件名 -->
    <span
      class="workspace-item-name"
      @click.stop="emit('preview', node)"
      :class="{ 'workspace-item-name--operating': operating }"
      :title="node.fullName || node.name"
    >
      {{ node.fullName || node.name }}
    </span>

    <!-- 文件大小 -->
    <span v-if="node.readableSize" class="workspace-item-meta">
      {{ node.readableSize }}
    </span>

    <!-- 行内操作按钮（始终显示，operating 时禁用） -->
    <div class="workspace-file-actions">
      <AButton
        type="text"
        size="small"
        class="workspace-file-action-btn workspace-file-action-btn--primary"
        title="预览"
        @click.stop="emit('preview', node)"
        :disabled="operating"
      >
        <EyeOutlined />
      </AButton>
      <AButton
        type="text"
        size="small"
        class="workspace-file-action-btn workspace-file-action-btn--primary"
        title="下载"
        :disabled="operating || multiSelect"
        @click.stop="emit('download', node)"
      >
        <DownloadOutlined />
      </AButton>
      <AButton
        type="text"
        size="small"
        class="workspace-file-action-btn workspace-file-action-btn--danger"
        title="删除"
        :disabled="operating || multiSelect"
        @click.stop="emit('delete', node)"
      >
        <DeleteOutlined />
      </AButton>
    </div>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/chat/workspace.scss' as *;
</style>
