<script setup lang="ts">
import { computed, ref } from 'vue'
import { renderMarkdown } from '@/utils/chat/markdown'
import MediaPreview from '@/components/common/MediaPreview.vue'
import type { UploadedFileItem } from '@/types'

const FILE_SEP = '@==##::::##==@'

/**
 * 解析用户内容，分离文件和文本
 */
function parseUserContent(content: string): { files: UploadedFileItem[]; text: string } {
  const idx = content.indexOf(FILE_SEP)
  if (idx === -1) return { files: [], text: content }
  const prefix = content.slice(0, idx)
  const text = content.slice(idx + FILE_SEP.length)
  try {
    const parsed = JSON.parse(prefix) as { files?: UploadedFileItem[] }
    const files = Array.isArray(parsed?.files) ? parsed.files : []
    return { files, text }
  } catch {
    return { files: [], text: content }
  }
}

/** 从文件名解析扩展名（小写） */
const getExtension = (fileName: string): string => {
  const lastDot = fileName.lastIndexOf('.')
  return lastDot > -1 ? fileName.slice(lastDot + 1).toLowerCase() : ''
}

const props = defineProps<{
  role: 'user' | 'assistant' | 'system' | 'tool'
  content: string
  isStreaming?: boolean
}>()

const isUser = computed(() => props.role === 'user')
const isAssistant = computed(() => props.role === 'assistant')
const isTool = computed(() => props.role === 'tool')

const parsedUserContent = computed(() => parseUserContent(props.content))

// 预览相关状态
const previewVisible = ref(false)
const previewCurrentIndex = ref(0)

/**
 * 打开文件预览
 */
const openPreview = (index: number) => {
  previewCurrentIndex.value = index
  previewVisible.value = true
}
</script>

<template>
  <div class="chat-message" :class="[isUser ? 'chat-message-user' : 'chat-message-assistant']">
    <div class="chat-message-bubble">
      <template v-if="isUser">
        <div v-if="parsedUserContent.files.length > 0" class="chat-message-files">
          <div
            v-for="(item, index) in parsedUserContent.files"
            :key="item.id"
            @click="openPreview(index)"
            class="chat-message-file-item"
          >
            <span class="chat-input-file-tag">
              {{ (item.extension ?? getExtension(item.name)).toUpperCase() || 'FILE' }}
            </span>
            <span class="chat-message-file-name" :title="item.name">{{ item.name }}</span>
            <span class="chat-message-file-size">{{ item.size }}</span>
          </div>
        </div>
        <span v-if="parsedUserContent.text" class="chat-message-user-content">
          {{ parsedUserContent.text }}
        </span>
      </template>
      <template v-else-if="isAssistant">
        <div v-if="!content && isStreaming" class="chat-loading-dots">
          <span></span><span></span><span></span>
        </div>
        <div v-else class="chat-md-content" v-html="renderMarkdown(content)"></div>
      </template>
      <template v-else-if="isTool">
        <div class="chat-md-content" v-html="renderMarkdown(content)"></div>
      </template>
    </div>

    <!-- 媒体预览组件 -->
    <MediaPreview
      v-model:visible="previewVisible"
      :items="parsedUserContent.files"
      :current-index="previewCurrentIndex"
    />
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/chat/index.scss' as *;

.chat-message-files {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 8px;
}

.chat-message-file-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  max-width: 280px;
  border-radius: var(--border-radius-md);
  font-size: var(--font-size-sm);
  cursor: pointer;
  &:hover {
    color: var(--color-primary);
  }
}

.chat-input-file-tag {
  flex-shrink: 0;
  padding: 2px 6px;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.04em;
  border-radius: 4px;
  color: var(--color-text-regular);
  background: rgba(0, 0, 0, 0.06);
}

.chat-message-file-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  //color: var(--color-text-primary);
}

.chat-message-file-size {
  flex-shrink: 0;
  color: var(--color-text-placeholder);
  font-size: var(--font-size-xs);
}
</style>
