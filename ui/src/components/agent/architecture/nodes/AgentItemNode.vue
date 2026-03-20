/**
 * 子智能体项节点组件
 * 展示作为工具使用的子智能体的详细信息
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { computed } from 'vue'
import { Handle, Position } from '@vue-flow/core'
import { RobotOutlined } from '@ant-design/icons-vue'
import type { AgentItemNodeData } from '../types'

/**
 * Props定义
 */
const props = defineProps<{
  data: AgentItemNodeData
}>()

/**
 * 智能体类型文本
 */
const agentTypeText = computed(() => {
  return props.data.agent.agentType === 'CUSTOM' ? '自定义' : 'A2A'
})

/**
 * 描述文本（截断）
 */
const descriptionText = computed(() => {
  const desc = props.data.agent.description || '暂无描述'
  return desc.length > 40 ? desc.slice(0, 40) + '...' : desc
})
</script>

<template>
  <div class="agent-item-node" :class="{ disabled: !data.agent.enabled }">
    <Handle type="target" :position="Position.Left" id="left" />

    <div class="node-header">
      <div class="node-avatar">
        <RobotOutlined />
      </div>
      <div class="node-title">
        <div class="node-name" :title="data.agent.name">
          {{ data.agent.name }}
        </div>
        <div class="node-code">{{ data.agent.agentCode }}</div>
      </div>
    </div>

    <div class="node-desc" :title="data.agent.description">
      {{ descriptionText }}
    </div>

    <div class="node-footer">
      <ATag size="small" :bordered="false" :color="data.agent.agentType === 'CUSTOM' ? 'magenta' : 'blue'">
        {{ agentTypeText }}
      </ATag>
      <ATag v-if="data.agent.studioConfigId" color="purple" :bordered="false">
        Studio
      </ATag>
      <ATag size="small" :bordered="false" color="default" v-if="data.agent.tag">
        {{ data.agent.tag }}
      </ATag>
    </div>
  </div>
</template>

<style scoped lang="scss">
.agent-item-node {
  width: 220px;
  padding: 12px;
  background: white;
  border: 1px solid #f0f5ff;
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(91, 105, 214, 0.08);
  transition: all 0.2s ease;

  &:hover {
    box-shadow: 0 4px 12px rgba(91, 105, 214, 0.3);
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
      background: #f0f5ff;
      color: #5b69d6;
      border-radius: 8px;
      font-size: 16px;
      flex-shrink: 0;
    }

    .node-title {
      flex: 1;
      min-width: 0;

      .node-name {
        font-size: 13px;
        font-weight: 600;
        color: #262626;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .node-code {
        font-size: 10px;
        color: #8c8c8c;
        font-family: 'Monaco', 'Menlo', monospace;
        margin-top: 1px;
      }
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
