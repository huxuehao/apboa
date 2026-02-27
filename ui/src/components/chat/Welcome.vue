<script setup lang="ts">
import ChatInput from './ChatInput.vue'

defineProps<{
  headline: string
  description?: string
  inputValue: string
  isRunning?: boolean
}>()

defineEmits<{
  (e: 'update:inputValue', value: string): void
  (e: 'send'): void
}>()
</script>

<template>
  <div class="chat-welcome">
    <h2 class="chat-welcome-title" :title="headline">{{ headline }}</h2>
    <p v-if="description" class="chat-welcome-desc" :title="description">{{ description }}</p>
    <div class="chat-input-outer chat-welcome-input">
      <ChatInput
        :model-value="inputValue"
        :isRunning="isRunning"
        placeholder="输入消息，Enter 发送，Shift+Enter 换行"
        @update:model-value="$emit('update:inputValue', $event)"
        @send="$emit('send')"
      />
    </div>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/chat/index.scss' as *;
</style>
