<script setup lang="ts">
import SessionItem from './SessionItem.vue'

defineProps<{
  pinnedSessions: any[]
  otherSessions: any[]
  currentSessionId: string | null
}>()

defineEmits<{
  (e: 'select', session: any): void
  (e: 'menu', key: string, session: any): void
}>()
</script>

<template>
  <div class="chat-history-section">
    <template v-if="pinnedSessions.length">
      <div class="chat-history-section-title">置顶</div>
      <div class="chat-history-list">
        <SessionItem
          v-for="s in pinnedSessions"
          :key="s.id"
          :session="s"
          :active="currentSessionId === String(s.id)"
          @click="$emit('select', s)"
          @menu="(key) => $emit('menu', key, s)"
        />
      </div>
    </template>
    <template v-if="otherSessions.length">
      <div class="chat-history-section-title">历史记录</div>
      <div class="chat-history-list">
        <SessionItem
          v-for="s in otherSessions"
          :key="s.id"
          :session="s"
          :active="currentSessionId === String(s.id)"
          @click="$emit('select', s)"
          @menu="(key) => $emit('menu', key, s)"
        />
      </div>
    </template>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/chat/index.scss' as *;
</style>
