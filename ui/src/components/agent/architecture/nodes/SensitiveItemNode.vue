/**
 * 敏感词节点组件
 * 展示敏感词配置的详细信息
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { computed } from 'vue'
import { Handle, Position } from '@vue-flow/core'
import { SafetyCertificateOutlined } from '@ant-design/icons-vue'
import type { SensitiveItemNodeData } from '../types'

/**
 * Props定义
 */
const props = defineProps<{
  data: SensitiveItemNodeData
}>()

/**
 * 描述文本（截断）
 */
const descriptionText = computed(() => {
  const desc = props.data.sensitive.description || '暂无描述'
  return desc.length > 40 ? desc.slice(0, 40) + '...' : desc
})

/**
 * 动作文本
 */
const actionText = computed(() => {
  const actionMap: Record<string, string> = {
    REJECT: '拒绝',
    REPLACE: '替换',
    WARN: '警告'
  }
  return actionMap[props.data.sensitive.action] || props.data.sensitive.action
})
</script>

<template>
  <div class="sensitive-item-node" :class="{ disabled: !data.sensitive.enabled }">
    <Handle type="target" :position="Position.Left" id="left" />

    <div class="node-header">
      <div class="node-avatar">
        <SafetyCertificateOutlined />
      </div>
      <div class="node-name" :title="data.sensitive.name">
        {{ data.sensitive.name }}
      </div>
    </div>

    <div class="node-desc" :title="data.sensitive.description">
      {{ descriptionText }}
    </div>

    <div class="node-footer">
      <ATag size="small" color="red">
        {{ actionText }}
      </ATag>
      <ATag size="small" color="default" v-if="data.sensitive.category">
        {{ data.sensitive.category }}
      </ATag>
    </div>
  </div>
</template>

<style scoped lang="scss">
.sensitive-item-node {
  width: 220px;
  padding: 12px;
  background: white;
  border: 1px solid #fff1f0;
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(245, 34, 45, 0.08);
  transition: all 0.2s ease;

  &:hover {
    transform: translateX(4px);
    border-color: #f5222d;
    box-shadow: 0 4px 12px rgba(245, 34, 45, 0.15);
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
      background: #fff1f0;
      color: #f5222d;
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
    background: #ffa39e;
    border: 2px solid #f5222d;
  }
}
</style>
