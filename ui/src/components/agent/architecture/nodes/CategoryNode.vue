/**
 * 分类节点组件
 * 作为工具、钩子、技能、MCP、知识库、子智能体等的父节点
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { Handle, Position } from '@vue-flow/core'
import {
  ToolOutlined,
  LoginOutlined,
  AppstoreOutlined,
  CloudServerOutlined,
  DatabaseOutlined,
  RobotOutlined,
  ThunderboltOutlined,
  FileTextOutlined,
  SettingOutlined,
  SafetyCertificateOutlined
} from '@ant-design/icons-vue'
import type { CategoryNodeData } from '../types'

/**
 * Props定义
 */
defineProps<{
  data: CategoryNodeData
}>()

/**
 * 图标映射
 */
const iconMap: Record<string, typeof ToolOutlined> = {
  ToolOutlined,
  LoginOutlined,
  AppstoreOutlined,
  CloudServerOutlined,
  DatabaseOutlined,
  RobotOutlined,
  ThunderboltOutlined,
  FileTextOutlined,
  SettingOutlined,
  SafetyCertificateOutlined
}
</script>

<template>
  <div
    class="category-node"
    :style="{
      '--node-color': data.color,
      '--node-bg-color': data.bgColor,
      '--node-border-color': data.borderColor
    }"
  >
    <!-- 连接点 -->
    <Handle type="target" :position="Position.Left" id="left" />
    <Handle type="source" :position="Position.Right" id="right" />

    <div class="node-icon">
      <component :is="iconMap[data.icon]" />
    </div>
    <div class="node-info">
      <div class="node-label">{{ data.label }}</div>
      <div class="node-count" v-if="data.count > 0">{{ data.count }} 项</div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.category-node {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 140px;
  padding: 12px 16px;
  background: var(--node-bg-color);
  border: 2px solid var(--node-border-color);
  border-radius: 12px;
  transition: all 0.2s ease;

  &:hover {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    // border-color: var(--node-color);
  }

  .node-icon {
    width: 32px;
    height: 32px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: white;
    border-radius: 8px;
    color: var(--node-color);
    font-size: 16px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.06);
  }

  .node-info {
    flex: 1;
    min-width: 0;

    .node-label {
      font-size: 13px;
      font-weight: 600;
      color: #474747;
      white-space: nowrap;
    }

    .node-count {
      font-size: 11px;
      color: #999;
      margin-top: 2px;
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
