/**
 * 工具项节点组件
 * 展示单个工具的详细信息
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { computed } from 'vue'
import { Handle, Position } from '@vue-flow/core'
import { ToolOutlined } from '@ant-design/icons-vue'
import type { ToolItemNodeData } from '../types'

/**
 * Props定义
 */
const props = defineProps<{
  data: ToolItemNodeData
}>()

/**
 * 工具类型文本
 */
const toolTypeText = computed(() => {
  return props.data.tool.toolType === 'BUILTIN' ? '内置' : '自定义'
})

/**
 * 描述文本（截断）
 */
const descriptionText = computed(() => {
  const desc = props.data.tool.description || '暂无描述'
  return desc.length > 40 ? desc.slice(0, 40) + '...' : desc
})
</script>

<template>
  <div class="tool-item-node" :class="{ disabled: !data.tool.enabled }">
    <Handle type="target" :position="Position.Left" id="left" />

    <div class="node-header">
      <div class="node-avatar">
        <ToolOutlined />
      </div>
      <div class="node-name" :title="data.tool.name">
        {{ data.tool.name }}
      </div>
    </div>

    <div class="node-desc" :title="data.tool.description">
      {{ descriptionText }}
    </div>

    <div class="node-footer">
      <ATag size="small" :color="data.tool.toolType === 'BUILTIN' ? 'blue' : 'green'">
        {{ toolTypeText }}
      </ATag>
      <ATag size="small" color="default" v-if="data.tool.category">
        {{ data.tool.category }}
      </ATag>
    </div>
  </div>
</template>

<style scoped lang="scss">
.tool-item-node {
  width: 220px;
  padding: 12px;
  background: white;
  border: 1px solid #e6f7ff;
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.08);
  transition: all 0.2s ease;

  &:hover {
    transform: translateX(4px);
    border-color: #1890ff;
    box-shadow: 0 4px 12px rgba(24, 144, 255, 0.15);
  }

  &.disabled {
    opacity: 0.6;
    border-color: #d9d9d9;

    .node-avatar {
      background: #f5f5f5 !important;
      color: #999 !important;
    }
  }

  .node-header {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 8px;

    .node-avatar {
      width: 32px;
      height: 32px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: #e6f7ff;
      color: #1890ff;
      border-radius: 8px;
      font-size: 16px;
      flex-shrink: 0;
    }

    .node-name {
      flex: 1;
      font-size: 13px;
      font-weight: 600;
      color: #262626;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  .node-desc {
    font-size: 11px;
    color: #8c8c8c;
    line-height: 1.5;
    margin-bottom: 8px;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    min-height: 33px;
  }

  .node-footer {
    display: flex;
    gap: 4px;
    flex-wrap: wrap;

    :deep(.ant-tag) {
      margin: 0;
      font-size: 10px;
      line-height: 18px;
      padding: 0 6px;
    }
  }

  :deep(.vue-flow__handle) {
    width: 8px;
    height: 8px;
    background: #91d5ff;
    border: 2px solid #1890ff;
  }
}
</style>
