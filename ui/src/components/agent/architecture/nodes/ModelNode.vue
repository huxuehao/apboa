/**
 * 模型节点组件
 * 展示模型配置的详细信息
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { computed } from 'vue'
import { Handle, Position } from '@vue-flow/core'
import { ThunderboltOutlined, CheckCircleOutlined, CloseCircleOutlined } from '@ant-design/icons-vue'
import type { ModelNodeData } from '../types'

/**
 * Props定义
 */
const props = defineProps<{
  data: ModelNodeData
}>()

/**
 * 获取模型参数（考虑覆盖）
 */
const modelParams = computed(() => {
  const config = props.data.modelConfig
  const override = props.data.paramsOverride

  if (!config) return null

  // 如果有覆盖参数，使用覆盖的值，否则使用模型默认值
  return {
    temperature: override?.temperature ?? config.temperature,
    topP: override?.topP ?? config.topP,
    topK: override?.topK ?? config.topK,
    maxTokens: override?.maxTokens ?? config.maxTokens,
    streaming: override?.streaming ?? config.streaming,
    thinking: override?.thinking ?? config.thinking
  }
})

/**
 * 是否有参数覆盖
 */
const hasOverride = computed(() => {
  return props.data.paramsOverride && Object.keys(props.data.paramsOverride).length > 0
})
</script>

<template>
  <div class="model-node">
    <Handle type="target" :position="Position.Left" id="left" />

    <div class="node-header">
      <div class="node-avatar">
        <ThunderboltOutlined />
      </div>
      <div class="node-title">
        <div class="node-name">模型配置</div>
        <div class="node-provider" v-if="data.provider">
          {{ data.provider.name }}
        </div>
      </div>
      <ATag v-if="hasOverride" :bordered="false" color="orange" size="small">已覆盖</ATag>
    </div>

    <template v-if="data.modelConfig">
      <div class="node-model-info">
        <div class="info-item">
          <span class="info-label">模型ID:</span>
          <span class="info-value">{{ data.modelConfig.modelId }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">名称:</span>
          <span class="info-value">{{ data.modelConfig.name }}</span>
        </div>
      </div>

      <div class="node-params" v-if="modelParams">
        <div class="params-row">
          <div class="param-item">
            <span class="param-label">Temperature</span>
            <span class="param-value">{{ modelParams.temperature }}</span>
          </div>
          <div class="param-item">
            <span class="param-label">Top P</span>
            <span class="param-value">{{ modelParams.topP }}</span>
          </div>
        </div>
        <div class="params-row">
          <div class="param-item">
            <span class="param-label">Top K</span>
            <span class="param-value">{{ modelParams.topK }}</span>
          </div>
          <div class="param-item">
            <span class="param-label">Max Tokens</span>
            <span class="param-value">{{ modelParams.maxTokens }}</span>
          </div>
        </div>
      </div>

      <div class="node-flags" v-if="modelParams">
        <div class="flag-item">
          <CheckCircleOutlined v-if="modelParams.streaming" class="flag-icon enabled" />
          <CloseCircleOutlined v-else class="flag-icon disabled" />
          <span>流式输出</span>
        </div>
        <div class="flag-item">
          <CheckCircleOutlined v-if="modelParams.thinking" class="flag-icon enabled" />
          <CloseCircleOutlined v-else class="flag-icon disabled" />
          <span>思维链</span>
        </div>
      </div>
    </template>

    <div class="node-empty" v-else>
      未配置模型
    </div>
  </div>
</template>

<style scoped lang="scss">
.model-node {
  width: 280px;
  padding: 14px;
  background: white;
  border: 1px solid #fff7e6;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(250, 140, 22, 0.08);
  transition: all 0.2s ease;
  &:hover {
    box-shadow: 0 4px 12px rgba(250, 140, 22, 0.3);
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
      background: #fff7e6;
      color: #fa8c16;
      border-radius: 10px;
      font-size: 18px;
    }

    .node-title {
      flex: 1;
      min-width: 0;

      .node-name {
        font-size: 14px;
        font-weight: 600;
        color: #262626;
      }

      .node-provider {
        font-size: 11px;
        color: #8c8c8c;
        margin-top: 2px;
      }
    }

    :deep(.ant-tag) {
      margin: 0;
      font-size: 10px;
    }
  }

  .node-model-info {
    background: #fafafa;
    border-radius: 8px;
    padding: 10px;
    margin-bottom: 10px;

    .info-item {
      display: flex;
      align-items: center;
      font-size: 12px;
      margin-bottom: 4px;

      &:last-child {
        margin-bottom: 0;
      }

      .info-label {
        color: #8c8c8c;
        width: 50px;
        flex-shrink: 0;
      }

      .info-value {
        color: #262626;
        font-family: 'Monaco', 'Menlo', monospace;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }
  }

  .node-params {
    margin-bottom: 10px;

    .params-row {
      display: flex;
      gap: 10px;
      margin-bottom: 6px;

      &:last-child {
        margin-bottom: 0;
      }
    }

    .param-item {
      flex: 1;
      display: flex;
      flex-direction: column;
      background: #f5f5f5;
      border-radius: 6px;
      padding: 6px 8px;

      .param-label {
        font-size: 10px;
        color: #8c8c8c;
      }

      .param-value {
        font-size: 12px;
        font-weight: 500;
        color: #262626;
        font-family: 'Monaco', 'Menlo', monospace;
      }
    }
  }

  .node-flags {
    display: flex;
    gap: 16px;

    .flag-item {
      display: flex;
      align-items: center;
      gap: 4px;
      font-size: 11px;
      color: #595959;

      .flag-icon {
        font-size: 14px;

        &.enabled {
          color: #52c41a;
        }

        &.disabled {
          color: #d9d9d9;
        }
      }
    }
  }

  .node-empty {
    text-align: center;
    color: #bfbfbf;
    font-size: 12px;
    padding: 20px 0;
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
