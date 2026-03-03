<script setup lang="ts">
import { computed } from 'vue'
import { renderMarkdown } from '@/utils/chat/markdown'
import type { UploadedFileItem } from '@/types'

const FILE_SEP = '@==##::::##==@'

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
</script>

<template>
  <div class="chat-message" :class="[isUser ? 'chat-message-user' : 'chat-message-assistant']">
    <div class="chat-message-bubble">
      <template v-if="isUser">
        <div v-if="parsedUserContent.files.length > 0" class="chat-message-files">
          <div
            v-for="item in parsedUserContent.files"
            :key="item.id"
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
  padding: 6px 10px;
  background: rgba($chat-primary, 0.06);
  border-radius: var(--border-radius-md);
  font-size: var(--font-size-sm);
  border: 1px solid #cbe1ff;
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
  color: var(--color-text-primary);
}

.chat-message-file-size {
  flex-shrink: 0;
  color: var(--color-text-placeholder);
  font-size: var(--font-size-xs);
}
</style>
