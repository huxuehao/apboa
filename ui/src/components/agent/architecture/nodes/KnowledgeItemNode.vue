/**
 * 知识库项节点组件
 * 展示单个知识库的详细信息
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { computed } from 'vue'
import { Handle, Position } from '@vue-flow/core'
import { DatabaseOutlined } from '@ant-design/icons-vue'
import type { KnowledgeItemNodeData } from '../types'

/**
 * Props定义
 */
const props = defineProps<{
  data: KnowledgeItemNodeData
}>()

/**
 * 知识库类型文本
 */
const kbTypeText = computed(() => {
  const typeMap: Record<string, string> = {
    BAILIAN: '百炼',
    DIFY: 'Dify',
    RAGFLOW: 'RagFlow'
  }
  return typeMap[props.data.knowledge.kbType] || props.data.knowledge.kbType
})

/**
 * 描述文本（截断）
 */
const descriptionText = computed(() => {
  const desc = props.data.knowledge.description || '暂无描述'
  return desc.length > 40 ? desc.slice(0, 40) + '...' : desc
})
</script>

<template>
  <div class="knowledge-item-node" :class="{ disabled: !data.knowledge.enabled }">
    <Handle type="target" :position="Position.Left" id="left" />

    <div class="node-header">
      <div class="node-avatar">
        <DatabaseOutlined />
      </div>
      <div class="node-name" :title="data.knowledge.name">
        {{ data.knowledge.name }}
      </div>
    </div>

    <div class="node-desc" :title="data.knowledge.description">
      {{ descriptionText }}
    </div>

    <div class="node-footer">
      <ATag size="small" :bordered="false" color="cyan">
        {{ kbTypeText }}
      </ATag>
      <ATag size="small" :bordered="false" color="default" v-if="data.knowledge.ragMode">
        {{ data.knowledge.ragMode }}
      </ATag>
    </div>
  </div>
</template>

<style scoped lang="scss">
.knowledge-item-node {
  width: 220px;
  padding: 12px;
  background: white;
  border: 1px solid #e6fffb;
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(19, 194, 194, 0.08);
  transition: all 0.2s ease;

  &:hover {
    box-shadow: 0 4px 12px rgba(19, 194, 194, 0.3);
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
      background: #e6fffb;
      color: #13c2c2;
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
    background: transparent;
    border: none;
    opacity: 0;
  }
}
</style>
