<script setup lang="ts">
import MessageItem from './MessageItem.vue'
import ToolCallItem from './ToolCallItem.vue'
import type { DisplayMessage } from '../types'

defineProps<{
  messages: DisplayMessage[]
  toolCalls: Array<{ id: string; name: string; args: string; result?: string; elapsed?: number, needConfirm?: boolean }>
}>()

const emit = defineEmits<{
  (e: 'toolContent', value: any): void
}>()
</script>

<template>
  <div class="chat-main-messages">
    <MessageItem
      v-for="msg in messages"
      :key="msg.id"
      :role="msg.role"
      :content="msg.content"
      :is-streaming="msg.isStreaming"
    />
    <ToolCallItem
      v-for="t in toolCalls"
      :key="t.id"
      :id="t.id"
      :name="t.name"
      :args="t.args"
      :result="t.result"
      :elapsed="t.elapsed"
      :loading="t.result == null"
      :need-confirm="t.needConfirm"
      @toolContent="(content: any) => $emit('toolContent', content)"
    />
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/chat/index.scss' as *;
</style>
