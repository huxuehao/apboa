<script setup lang="ts">
/**
 * Agent 技能标签消息渲染组件
 * 用于在消息流中渲染 <agent-skill>name</agent-skill> 内容
 *
 * @component
 */
import { AppstoreOutlined } from '@ant-design/icons-vue'

defineProps<{
  /** 技能名称（标签内容） */
  content: string
}>()
</script>

<template>
  <span class="agent-skill-tag" :title="content">
    <AppstoreOutlined class="agent-skill-tag-icon" />
    <span class="agent-skill-tag-name">{{ content }}</span>
  </span>
</template>

<style scoped lang="scss">
.agent-skill-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  max-width: 100%;
  padding: 2px 8px;
  background: rgba(114, 46, 209, 0.12);
  border-radius: 4px;
  font-size: 13px;
  line-height: 1.4;
  color: #722ED1;
  vertical-align: middle;
  white-space: nowrap;
  user-select: none;
}

.agent-skill-tag-icon {
  flex-shrink: 0;
  font-size: 14px;
  opacity: 0.9;
}

.agent-skill-tag-name {
  flex-shrink: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 500;
}
</style>
