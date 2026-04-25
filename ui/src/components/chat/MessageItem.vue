<script setup lang="ts">
import { computed, ref } from 'vue'
import MediaPreview from '@/components/common/MediaPreview.vue'
import type { UploadedFileItem } from '@/types'
import MediaIcon from '@/components/common/MediaIcon.vue'
import MarkdownRenderer from "@/components/markdown/MarkdownRenderer.vue";

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
  role: 'user' | 'assistant' | 'system' | 'tool' | 'error'
  content: string
  agentHasResult?: boolean
  isStreaming?: boolean
}>()

const isUser = computed(() => props.role === 'user')
const isAssistant = computed(() => props.role === 'assistant')
const isTool = computed(() => props.role === 'tool')
const isError = computed(() => props.role === 'error')

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
    <template v-if="isUser">
      <div class="chat-message-bubble chat-message-bubble_user">
        <!-- 文件列表 -->
        <div v-if="parsedUserContent.files.length > 0" class="chat-message-files">
          <div
            v-for="(item, index) in parsedUserContent.files"
            :key="item.id"
            @click="openPreview(index)"
            class="chat-message-file-item"
          >
            <MediaIcon :type="(item.extension ?? getExtension(item.name)) || 'FILE'" size="19"/>
<!--            <MediaIcon type="FILE" size="19"/>-->
            <span class="chat-message-file-name" :title="item.name">{{ item.name }}</span>
<!--            <span class="chat-message-file-size">{{ item.size }}</span>-->
          </div>
        </div>
        <!-- 文本内容 -->
        <span v-if="parsedUserContent.text" class="chat-message-user-content">
          {{ parsedUserContent.text }}
        </span>
      </div>
    </template>
    <template v-else-if="isAssistant">
      <div class="chat-message-bubble">
        <div v-if="!agentHasResult && !content" class="chat-loading-dots">
          <span></span><span></span><span></span>
        </div>
        <div v-else class="chat-md-content">
          <MarkdownRenderer :content="content" />
        </div>
      </div>
    </template>
    <template v-else-if="isTool">
      <div class="chat-message-bubble">
        <div class="chat-md-content">
          <MarkdownRenderer :content="content" />
        </div>
      </div>
    </template>
    <template v-else-if="isError">
      <div class="chat-message-bubble">
        <div class="chat-md-content">
          <span style="color: tomato">{{content}}</span>
        </div>
      </div>
    </template>
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
  gap: 4px;
  max-width: 280px;
  padding: 6px 10px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: var(--border-radius-md);
  font-size: var(--font-size-sm);
  cursor: pointer;
  &:hover {
    background: rgba(255, 255, 255, 0.9);
  }
}

.chat-message-file-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-message-file-size {
  flex-shrink: 0;
  color: var(--color-text-placeholder);
  font-size: var(--font-size-xs);
}
</style>
