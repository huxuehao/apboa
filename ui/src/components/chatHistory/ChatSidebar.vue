<script setup lang="ts">
import SessionList from './SessionList.vue'

defineProps<{
  agentName?: string
  sessions: any[]
  currentSessionId: string | null
  loading: boolean
  hasMore: boolean
}>()

defineEmits<{
  (e: 'selectSession', session: any): void
  (e: 'deleteSession', session: any): void
  (e: 'loadMore'): void
}>()
</script>

<template>
  <aside class="chat-sidebar">
    <div class="chat-sidebar-header">
      <div class="chat-sidebar-brand">
        <img src="@/assets/images/logo/logo.png" alt="logo" class="chat-sidebar-logo" />
        <span class="chat-sidebar-name" :title="agentName || '智能体'">{{ agentName || '智能体' }}</span>
      </div>
    </div>
    <div class="chat-sidebar-body">
      <div class="chat-history-section">
        <SessionList
          :sessions="sessions"
          :current-session-id="currentSessionId"
          :loading="loading"
          :has-more="hasMore"
          @select="(session) => $emit('selectSession', session)"
          @delete="(session) => $emit('deleteSession',session)"
          @load-more="$emit('loadMore')"
        />
      </div>
    </div>
  </aside>
</template>

<style scoped lang="scss">
@use '@/styles/chat/index.scss' as *;
</style>
