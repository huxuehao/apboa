/**
 * 中心智能体节点组件
 * 作为架构图的核心节点，展示智能体基本信息
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { computed } from 'vue'
import { Handle, Position } from '@vue-flow/core'
import {ClockCircleOutlined, RobotOutlined} from '@ant-design/icons-vue'
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
  return props.data.agent.agentType === 'CUSTOM' ? '自定义' : 'A2A'
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
        <ClockCircleOutlined
          v-if="data.agent?.jobInfo"
          class="timing"
          :class="{'timing_start' : data.agent?.jobInfo?.enabled && data.agent.enabled}" />
      </div>
      <div class="node-title">
        <div class="node-name">{{ data.agent.name }}</div>
        <div class="node-code">{{ data.agent.agentCode }}</div>
      </div>
    </div>

    <div class="node-content">
      <div class="node-type">
        <ATag :color="data.agent.agentType === 'CUSTOM' ? 'magenta' : 'blue'" :bordered="false">
          {{ agentTypeText }}
        </ATag>
        <ATag v-if="data.agent.studioConfigId" color="purple" :bordered="false">
          Studio
        </ATag>
        <ATag v-if="data.agent.tag" color="default" :bordered="false">{{ data.agent.tag }}</ATag>
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
  background: #ffffff;
  border-radius: 16px;
  color: #333333;
  box-shadow:0 4px 12px rgba(0, 0, 0, 0.08);
  transition: all 0.2s ease;
  &:hover {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  }

  .node-header {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 12px;

    .node-avatar {
      position: relative;
      width: 48px;
      height: 48px;
      display: flex;
      align-items: center;
      justify-content: center;
      background-color: #E8EAF6;
      border-radius: 12px;
      font-size: 24px;
      color: #4449D0;

      .timing {
        position: absolute;
        right: -7px;
        bottom:-0px;
        font-size: 16px;
        color: #8a8a8a;

        &_start {
          color: #4449d0;
        }
      }
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
        color: #1a1a1a;
      }

      .node-code {
        font-size: 12px;
        color: #8c8c8c;
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
        font-size: 11px;
      }
    }

    .node-desc {
      font-size: 12px;
      line-height: 1.5;
      color: #666666;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }
  }

  :deep(.vue-flow__handle) {
    width: 1px;
    height: 1px;
    background: transparent;
    border: none;
    opacity: 0;
  }
}
</style>
<style>
.vue-flow__node.selected {
  outline: none !important;
  box-shadow: none !important;
  border: none !important;
}
</style>
