<script setup lang="ts">
import { computed } from 'vue'
import { renderMarkdown } from '@/utils/chat/markdown'

const props = defineProps<{
  role: 'user' | 'assistant' | 'system' | 'tool'
  content: string
  isStreaming?: boolean
}>()

const isUser = computed(() => props.role === 'user')
const isAssistant = computed(() => props.role === 'assistant')
const isTool = computed(() => props.role === 'tool')
</script>

<template>
  <div class="chat-message" :class="[isUser ? 'chat-message-user' : 'chat-message-assistant']">
    <div class="chat-message-bubble">
      <template v-if="isUser">
        <span class="chat-message-user-content">
          {{ content }}
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
</style>
