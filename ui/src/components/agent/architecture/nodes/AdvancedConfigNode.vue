/**
 * 高级配置节点组件
 * 展示智能体的高级配置开关状态
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { Handle, Position } from '@vue-flow/core'
import {
  SettingOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  ScheduleOutlined,
  CloudOutlined,
  CompressOutlined,
  ProfileOutlined
} from '@ant-design/icons-vue'
import type { AdvancedConfigNodeData } from '../types'

/**
 * Props定义
 */
defineProps<{
  data: AdvancedConfigNodeData
}>()
</script>

<template>
  <div class="advanced-config-node">
    <Handle type="target" :position="Position.Left" id="left" />

    <div class="node-header">
      <div class="node-avatar">
        <SettingOutlined />
      </div>
      <div class="node-title">高级配置</div>
    </div>

    <div class="config-list">
      <div class="config-item">
        <div class="config-icon">
          <ScheduleOutlined />
        </div>
        <div class="config-info">
          <div class="config-name">启用计划</div>
          <div class="config-detail" v-if="data.enablePlanning">
            最大子任务: {{ data.maxSubtasks }}
          </div>
        </div>
        <div class="config-status">
          <CheckCircleOutlined v-if="data.enablePlanning" class="status-icon enabled" />
          <CloseCircleOutlined v-else class="status-icon disabled" />
        </div>
      </div>

      <div class="config-item">
        <div class="config-icon">
          <CloudOutlined />
        </div>
        <div class="config-info">
          <div class="config-name">启用记忆</div>
        </div>
        <div class="config-status">
          <CheckCircleOutlined v-if="data.enableMemory" class="status-icon enabled" />
          <CloseCircleOutlined v-else class="status-icon disabled" />
        </div>
      </div>

      <div class="config-item">
        <div class="config-icon">
          <CompressOutlined />
        </div>
        <div class="config-info">
          <div class="config-name">记忆压缩</div>
        </div>
        <div class="config-status">
          <CheckCircleOutlined v-if="data.enableMemoryCompression" class="status-icon enabled" />
          <CloseCircleOutlined v-else class="status-icon disabled" />
        </div>
      </div>

      <div class="config-item">
        <div class="config-icon">
          <ProfileOutlined />
        </div>
        <div class="config-info">
          <div class="config-name">结构化输出</div>
        </div>
        <div class="config-status">
          <CheckCircleOutlined v-if="data.structuredOutputEnabled" class="status-icon enabled" />
          <CloseCircleOutlined v-else class="status-icon disabled" />
        </div>
      </div>
    </div>

    <div class="node-footer">
      <div class="iteration-info">
        <span class="info-label">最大迭代:</span>
        <span class="info-value">{{ data.maxIterations }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.advanced-config-node {
  width: 280px;
  padding: 14px;
  background: white;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  transition: all 0.2s ease;
  &:hover {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  }

  .node-header {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 12px;
    padding-bottom: 10px;
    border-bottom: 1px solid #f0f0f0;

    .node-avatar {
      width: 36px;
      height: 36px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: #f5f5f5;
      color: #595959;
      border-radius: 10px;
      font-size: 18px;
    }

    .node-title {
      font-size: 14px;
      font-weight: 600;
      color: #262626;
    }
  }

  .config-list {
    display: flex;
    flex-direction: column;
    gap: 8px;

    .config-item {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 8px 10px;
      background: #fafafa;
      border-radius: 8px;
      transition: background 0.2s ease;

      &:hover {
        background: #f5f5f5;
      }

      .config-icon {
        width: 28px;
        height: 28px;
        display: flex;
        align-items: center;
        justify-content: center;
        background: white;
        border-radius: 6px;
        color: #8c8c8c;
        font-size: 14px;
        flex-shrink: 0;
      }

      .config-info {
        flex: 1;
        min-width: 0;

        .config-name {
          font-size: 12px;
          color: #262626;
        }

        .config-detail {
          font-size: 10px;
          color: #8c8c8c;
          margin-top: 1px;
        }
      }

      .config-status {
        .status-icon {
          font-size: 16px;

          &.enabled {
            color: #52c41a;
          }

          &.disabled {
            color: #d9d9d9;
          }
        }
      }
    }
  }

  .node-footer {
    margin-top: 12px;
    padding-top: 10px;
    border-top: 1px solid #f0f0f0;

    .iteration-info {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 11px;

      .info-label {
        color: #8c8c8c;
      }

      .info-value {
        color: #262626;
        font-weight: 500;
        font-family: 'Monaco', 'Menlo', monospace;
      }
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
