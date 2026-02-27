<script setup lang="ts">
import MessageItem from './MessageItem.vue'
import type { DisplayMessage } from '@/types'

defineProps<{
  messages: DisplayMessage[]
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
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/chat/index.scss' as *;
</style>
