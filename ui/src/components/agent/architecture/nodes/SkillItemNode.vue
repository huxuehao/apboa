/**
 * 技能项节点组件
 * 展示单个技能包的详细信息
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { computed } from 'vue'
import { Handle, Position } from '@vue-flow/core'
import { AppstoreOutlined } from '@ant-design/icons-vue'
import type { SkillItemNodeData } from '../types'

/**
 * Props定义
 */
const props = defineProps<{
  data: SkillItemNodeData
}>()

/**
 * 描述文本（截断）
 */
const descriptionText = computed(() => {
  const desc = props.data.skill.description || '暂无描述'
  return desc.length > 40 ? desc.slice(0, 40) + '...' : desc
})
</script>

<template>
  <div class="skill-item-node" :class="{ disabled: !data.skill.enabled }">
    <Handle type="target" :position="Position.Left" id="left" />

    <div class="node-header">
      <div class="node-avatar">
        <AppstoreOutlined />
      </div>
      <div class="node-name" :title="data.skill.name">
        {{ data.skill.name }}
      </div>
    </div>

    <div class="node-desc" :title="data.skill.description">
      {{ descriptionText }}
    </div>

    <div class="node-footer">
      <ATag :bordered="false" size="small" color="purple" v-if="data.skill.category">
        {{ data.skill.category }}
      </ATag>
    </div>
  </div>
</template>

<style scoped lang="scss">
.skill-item-node {
  width: 220px;
  padding: 12px;
  background: white;
  border: 1px solid #f9f0ff;
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(114, 46, 209, 0.08);
  transition: all 0.2s ease;

  &:hover {
    box-shadow: 0 4px 12px rgba(114, 46, 209, 0.3);
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
      background: #f9f0ff;
      color: #722ed1;
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
