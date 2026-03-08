/**
 * 中心智能体节点组件
 * 作为架构图的核心节点，展示智能体基本信息
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { computed } from 'vue'
import { Handle, Position } from '@vue-flow/core'
import { RobotOutlined } from '@ant-design/icons-vue'
import type { CenterAgentNodeData } from '../types'

/**
 * Props定义
 */
const props = defineProps<{
  data: CenterAgentNodeData
}>()

/**
 * 智能体类型文本
 */
const agentTypeText = computed(() => {
  return props.data.agent.agentType === 'CUSTOM' ? '自定义智能体' : 'A2A智能体'
})

/**
 * 描述文本（截断）
 */
const descriptionText = computed(() => {
  const desc = props.data.agent.description || '暂无描述'
  return desc.length > 50 ? desc.slice(0, 50) + '...' : desc
})
</script>

<template>
  <div class="center-agent-node">
    <!-- 连接点 -->
    <Handle type="source" :position="Position.Top" id="top" />
    <Handle type="source" :position="Position.Right" id="right" />
    <Handle type="source" :position="Position.Bottom" id="bottom" />
    <Handle type="source" :position="Position.Left" id="left" />

    <div class="node-header">
      <div class="node-avatar">
        <RobotOutlined />
      </div>
      <div class="node-title">
        <div class="node-name">{{ data.agent.name }}</div>
        <div class="node-code">{{ data.agent.agentCode }}</div>
      </div>
    </div>

    <div class="node-content">
      <div class="node-type">
        <ATag :color="data.agent.agentType === 'CUSTOM' ? 'purple' : 'blue'">
          {{ agentTypeText }}
        </ATag>
        <ATag v-if="data.agent.tag" color="default">{{ data.agent.tag }}</ATag>
      </div>
      <div class="node-desc" :title="data.agent.description">
        {{ descriptionText }}
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.center-agent-node {
  width: 280px;
  min-height: 160px;
  padding: 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 16px;
  color: white;
  box-shadow:
    0 10px 40px rgba(102, 126, 234, 0.4),
    0 0 0 1px rgba(255, 255, 255, 0.1) inset;

  .node-header {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 12px;

    .node-avatar {
      width: 48px;
      height: 48px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: rgba(255, 255, 255, 0.2);
      border-radius: 12px;
      font-size: 24px;
      backdrop-filter: blur(10px);
    }

    .node-title {
      flex: 1;
      min-width: 0;

      .node-name {
        font-size: 16px;
        font-weight: 600;
        line-height: 1.4;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .node-code {
        font-size: 12px;
        opacity: 0.8;
        font-family: 'Monaco', 'Menlo', monospace;
        margin-top: 2px;
      }
    }
  }

  .node-content {
    .node-type {
      display: flex;
      gap: 6px;
      flex-wrap: wrap;
      margin-bottom: 8px;

      :deep(.ant-tag) {
        margin: 0;
        border: none;
        font-size: 11px;
      }
    }

    .node-desc {
      font-size: 12px;
      line-height: 1.5;
      opacity: 0.9;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }
  }

  :deep(.vue-flow__handle) {
    width: 10px;
    height: 10px;
    background: rgba(255, 255, 255, 0.8);
    border: 2px solid #667eea;
  }
}
</style>
