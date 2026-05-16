<script setup lang="ts">
import ChatInput from './ChatInput.vue'
import {MessageOutlined} from "@ant-design/icons-vue";

defineProps<{
  messageSize: number
  headline: string
  inputValue: string
  agentId: string
  description?: string
  uploadedFiles?: import('@/types').UploadedFileItem[]
  isRunning?: boolean
  memoryActive?: boolean
  planActive?: boolean
  enableMemory?: boolean
  enablePlanning?: boolean
  toolProcessActive?: boolean
  showToolProcess?: boolean
  allowUploadFileType?: string[]
  sessionId?: string | null
  mentionAllowed?: boolean
  hasCodeExecutionConfig?: boolean
}>()

defineEmits<{
  (e: 'update:inputValue', value: string): void
  (e: 'update:uploadedFiles', value: import('@/types').UploadedFileItem[]): void
  (e: 'send'): void
  (e: 'memory', value: boolean): void
  (e: 'plan', value: boolean): void
  (e: 'toolProcess', value: boolean): void
  (e: 'newSession'): void
}>()
</script>

<template>
  <div class="chat-welcome">
    <h2 class="chat-welcome-title" :title="headline">{{ headline }}</h2>
    <p v-if="description" class="chat-welcome-desc" :title="description">{{ description }}</p>
    <div v-if="hasCodeExecutionConfig && messageSize === 0" class="chat-welcome-send" @click="$emit('newSession')">
      <MessageOutlined /> <span>开启对话</span>
    </div>
    <div v-else class="chat-input-outer chat-welcome-input">
      <ChatInput
        :model-value="inputValue"
        :agent-id="agentId"
        :uploaded-files="uploadedFiles"
        :isRunning="isRunning"
        :memory-active="memoryActive"
        :plan-active="planActive"
        :enable-memory="enableMemory"
        :enable-planning="enablePlanning"
        :allow-upload-file-type="allowUploadFileType"
        :show-tool-process="showToolProcess"
        :tool-process-active="toolProcessActive"
        :session-id="sessionId"
        :mention-allowed="mentionAllowed"
        @update:model-value="$emit('update:inputValue', $event)"
        @update:uploaded-files="$emit('update:uploadedFiles', $event)"
        @memory="$emit('memory', $event)"
        @plan="$emit('plan', $event)"
        @toolProcess="$emit('toolProcess', $event)"
        @send="$emit('send')"
      />
    </div>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/chat/index.scss' as *;
</style>
