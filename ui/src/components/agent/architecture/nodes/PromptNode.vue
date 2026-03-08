/**
 * 提示词节点组件
 * 展示系统提示词的详细信息
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { computed, ref } from 'vue'
import { Handle, Position } from '@vue-flow/core'
import { FileTextOutlined, ExpandOutlined, CompressOutlined } from '@ant-design/icons-vue'
import type { PromptNodeData } from '../types'

/**
 * Props定义
 */
const props = defineProps<{
  data: PromptNodeData
}>()

/**
 * 是否展开
 */
const expanded = ref(false)

/**
 * 提示词名称
 */
const promptName = computed(() => {
  if (props.data.followTemplate && props.data.promptTemplate) {
    return props.data.promptTemplate.name
  }
  return '自定义提示词'
})

/**
 * 提示词内容
 */
const promptContent = computed(() => {
  if (props.data.followTemplate && props.data.promptTemplate) {
    return props.data.promptTemplate.content || '暂无内容'
  }
  return props.data.systemPrompt || '暂无内容'
})

/**
 * 截断的提示词内容
 */
const truncatedContent = computed(() => {
  const content = promptContent.value
  if (content.length > 100) {
    return content.slice(0, 100) + '...'
  }
  return content
})

/**
 * 切换展开状态
 */
function toggleExpand() {
  expanded.value = !expanded.value
}
</script>

<template>
  <div class="prompt-node" :class="{ expanded }">
    <Handle type="target" :position="Position.Left" id="left" />

    <div class="node-header">
      <div class="node-avatar">
        <FileTextOutlined />
      </div>
      <div class="node-title">
        <div class="node-name">提示词</div>
        <div class="node-template-name" :title="promptName">
          {{ promptName }}
        </div>
      </div>
      <div class="node-actions">
        <AButton type="text" size="small" @click="toggleExpand">
          <CompressOutlined v-if="expanded" />
          <ExpandOutlined v-else />
        </AButton>
      </div>
    </div>

    <div class="node-content">
      <div class="content-text" :class="{ expanded }">
        {{ expanded ? promptContent : truncatedContent }}
      </div>
    </div>

    <div class="node-footer" v-if="data.promptTemplate">
      <ATag v-if="data.followTemplate" :bordered="false" color="blue" size="small">跟随模板</ATag>
      <ATag size="small" :bordered="false" color="default" v-if="data.promptTemplate.category">
        {{ data.promptTemplate.category }}
      </ATag>
    </div>
  </div>
</template>

<style scoped lang="scss">
.prompt-node {
  width: 280px;
  padding: 14px;
  background: white;
  border: 1px solid #f0f5ff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(47, 84, 235, 0.08);
  transition: all 0.3s ease;
  &:hover {
    box-shadow: 0 4px 12px rgba(47, 84, 235, 0.3);
  }

  &.expanded {
    width: 360px;
  }

  .node-header {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 10px;

    .node-avatar {
      width: 36px;
      height: 36px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: #f0f5ff;
      color: #2f54eb;
      border-radius: 10px;
      font-size: 18px;
      flex-shrink: 0;
    }

    .node-title {
      flex: 1;
      min-width: 0;

      .node-name {
        font-size: 14px;
        font-weight: 600;
        color: #262626;
      }

      .node-template-name {
        font-size: 11px;
        color: #8c8c8c;
        margin-top: 2px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }

    .node-actions {
      display: flex;
      align-items: center;
      gap: 4px;

      :deep(.ant-tag) {
        margin: 0;
        font-size: 10px;
      }

      :deep(.ant-btn) {
        padding: 2px 6px;
        height: auto;
      }
    }
  }

  .node-content {
    background: #fafafa;
    border-radius: 8px;
    padding: 10px;

    .content-text {
      font-size: 11px;
      color: #595959;
      line-height: 1.6;
      max-height: 80px;
      overflow: hidden;
      word-break: break-all;

      &.expanded {
        max-height: 300px;
        overflow-y: auto;
      }
    }
  }

  .node-footer {
    margin-top: 10px;
    display: flex;
    gap: 4px;

    :deep(.ant-tag) {
      margin: 0;
      font-size: 10px;
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
