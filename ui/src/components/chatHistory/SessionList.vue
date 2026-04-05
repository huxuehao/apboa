<script setup lang="ts">
import { LoadingOutlined } from '@ant-design/icons-vue'
import SessionItem from './SessionItem.vue'

defineProps<{
  sessions: any[]
  currentSessionId: string | null
  loading: boolean
  hasMore: boolean
}>()

const emit = defineEmits<{
  (e: 'select', session: any): void
  (e: 'delete', session: any): void
  (e: 'loadMore'): void
}>()

/**
 * 列表滚动事件，滚动到底部时触发加载更多
 */
const onScroll = (e: Event) => {
  const el = e.target as HTMLElement
  if (el.scrollTop + el.clientHeight >= el.scrollHeight - 20) {
    emit('loadMore')
  }
}
</script>

<template>
  <div class="chat-history-section">
    <div class="chat-history-section-title">历史记录</div>
    <div class="chat-history-scroll-area" @scroll="onScroll">
      <SessionItem
        v-for="s in sessions"
        :key="s.id"
        :session="s"
        :active="currentSessionId === String(s.id)"
        @click="$emit('select', s)"
        @delete="$emit('delete', s)"
      />
      <!-- 加载状态提示 -->
      <div v-if="loading" class="chat-load-tip">
        <LoadingOutlined style="margin-right: 6px" />
        <span>正在加载...</span>
      </div>
      <div v-else-if="!hasMore && sessions.length > 0" class="chat-load-tip chat-load-end">
        已经到底了
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/chat/index.scss' as *;
.chat-history-section {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.chat-history-scroll-area {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
  margin-left: 4px;
}

.chat-load-tip {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 10px 0;
  font-size: 12px;
  color: #999;
  user-select: none;
}

.chat-load-end {
  color: #bbb;
}
</style>
