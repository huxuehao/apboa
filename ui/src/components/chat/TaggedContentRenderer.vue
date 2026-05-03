<script setup lang="ts">
import { computed } from 'vue'
import { parseTaggedContent, TagRegistry } from '@/utils/chat/tagSystem'
import WorkspaceFileTag from './WorkspaceFileTag.vue'

/**
 * 注册内置标签渲染器
 */
TagRegistry.register({
  tagName: 'workspace-file',
  component: WorkspaceFileTag
})

const props = defineProps<{
  /** 含标签标记的原始文本 */
  content: string
}>()

/**
 * 解析后的内容段
 */
const segments = computed(() => parseTaggedContent(props.content || ''))
</script>

<template>
  <span class="tagged-content-renderer">
    <template v-for="(seg, index) in segments" :key="index">
      <!-- 文本段 -->
      <template v-if="seg.type === 'text'">
        <template v-for="(line, lineIdx) in (seg.content || '').split('\n')" :key="lineIdx">
          {{ line }}
          <br v-if="lineIdx < (seg.content || '').split('\n').length - 1" />
        </template>
      </template>
      <!-- 标签段 -->
      <component
        v-else-if="seg.type === 'tag' && seg.tagName && TagRegistry.get(seg.tagName)"
        :is="TagRegistry.get(seg.tagName)"
        :path="seg.tagContent"
      />
      <!-- 未知标签或空内容 -->
      <span v-else>{{ seg.content }}</span>
    </template>
  </span>
</template>

<style scoped lang="scss">
.tagged-content-renderer {
  word-break: break-word;
  white-space: pre-wrap;
}
</style>
