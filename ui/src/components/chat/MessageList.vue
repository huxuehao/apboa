<script setup lang="ts">
import MessageItem from './MessageItem.vue'
import ToolCallItem from './ToolCallItem.vue'
import type { DisplayMessage } from '@/types'
import type {FlatFileItem} from "@/composables/chat/useWorkspaceFiles.ts";

defineProps<{
  messages: DisplayMessage[]
  agentHasResult?: boolean
  toolCalls: Array<{ id: string; name: string; args: string; result?: string; elapsed?: number, needConfirm?: boolean }>
}>()

const emit = defineEmits<{
  (e: 'toolContent', value: any): void
  (e: 'inputTagPreview', value: FlatFileItem): void
}>()
</script>

<template>
  <div class="chat-main-messages">
    <MessageItem
      v-for="msg in messages"
      @inputTagPreview="$emit('inputTagPreview', $event)"
      :key="msg.id"
      :role="msg.role"
      :content="msg.content"
      :created-at="msg.createdAt"
      :agent-has-result="agentHasResult"
      :is-streaming="msg.isStreaming"
      :reasoning-content="msg.reasoningContent"
      :reasoning-message-id="msg.reasoningMessageId"
      :reasoning-streaming="msg.reasoningStreaming"
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
