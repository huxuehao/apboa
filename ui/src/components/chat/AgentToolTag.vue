<script setup lang="ts">
/**
 * Agent 工具标签消息渲染组件
 * 用于在消息流中渲染 <agent-tool>name</agent-tool> 内容
 *
 * @component
 */
import { ToolOutlined } from '@ant-design/icons-vue'

defineProps<{
  /** 工具名称（标签内容） */
  content: string
}>()
</script>

<template>
  <span class="agent-tool-tag" :title="content">
    <ToolOutlined class="agent-tool-tag-icon" />
    <span class="agent-tool-tag-name">{{ content }}</span>
  </span>
</template>

<style scoped lang="scss">
.agent-tool-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  max-width: 100%;
  padding: 2px 8px;
  background: rgba(250, 140, 22, 0.12);
  border-radius: 4px;
  font-size: 13px;
  line-height: 1.4;
  color: #FA8C16;
  vertical-align: middle;
  white-space: nowrap;
  user-select: none;
}

.agent-tool-tag-icon {
  flex-shrink: 0;
  font-size: 14px;
  opacity: 0.9;
}

.agent-tool-tag-name {
  flex-shrink: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 500;
}
</style>
