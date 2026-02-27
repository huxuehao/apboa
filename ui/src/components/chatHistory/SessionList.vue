<script setup lang="ts">
import SessionItem from './SessionItem.vue'

defineProps<{
  sessions: any[]
  currentSessionId: string | null
}>()

defineEmits<{
  (e: 'select', session: any): void
  (e: 'delete', session: any): void
}>()
</script>

<template>
  <div class="chat-history-section">
    <template v-if="sessions.length">
      <div class="chat-history-section-title">历史记录</div>
      <div class="chat-history-list">
        <SessionItem
          v-for="s in sessions"
          :key="s.id"
          :session="s"
          :active="currentSessionId === String(s.id)"
          @click="$emit('select', s)"
          @delete="$emit('delete', s)"
        />
      </div>
    </template>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/chat/index.scss' as *;
</style>
