<script setup lang="ts">
import { ref } from 'vue'
import { LoadingOutlined } from '@ant-design/icons-vue'
import SessionItem from './SessionItem.vue'

defineProps<{
  pinnedSessions: any[]
  otherSessions: any[]
  currentSessionId: string | null
  loading: boolean
  hasMore: boolean
}>()

const emit = defineEmits<{
  (e: 'select', session: any): void
  (e: 'menu', key: string, session: any): void
  (e: 'loadMore'): void
}>()

const historyListRef = ref<HTMLElement | null>(null)

/**
 * 历史记录区域滚动事件，滚动到底部时触发加载更多
 */
const onHistoryScroll = (e: Event) => {
  const el = e.target as HTMLElement
  if (el.scrollTop + el.clientHeight >= el.scrollHeight - 20) {
    emit('loadMore')
  }
}
</script>

<template>
  <div class="chat-session-list-wrapper">
    <!-- 置顶区域：最大占50%高度，超出滚动 -->
    <div v-if="pinnedSessions.length" class="chat-pinned-section">
      <div class="chat-history-section-title">置顶</div>
      <div class="chat-pinned-list">
        <SessionItem
          v-for="s in pinnedSessions"
          :key="s.id"
          :session="s"
          :active="currentSessionId === String(s.id)"
          @click="$emit('select', s)"
          @menu="(key) => $emit('menu', key, s)"
        />
      </div>
    </div>
    <!-- 历史记录区域：占据剩余空间，滚动加载 -->
    <div class="chat-history-section-scrollable">
      <div class="chat-history-section-title">历史记录</div>
      <div
        ref="historyListRef"
        class="chat-history-scroll-area"
        @scroll="onHistoryScroll"
      >
        <SessionItem
          v-for="s in otherSessions"
          :key="s.id"
          :session="s"
          :active="currentSessionId === String(s.id)"
          @click="$emit('select', s)"
          @menu="(key) => $emit('menu', key, s)"
        />
        <!-- 加载状态提示 -->
        <div v-if="loading" class="chat-load-tip">
          <LoadingOutlined style="margin-right: 6px" />
          <span>正在加载...</span>
        </div>
        <div v-else-if="!hasMore && otherSessions.length > 0" class="chat-load-tip chat-load-end">
          已经到底了
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/chat/index.scss' as *;
.chat-session-list-wrapper {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.chat-pinned-section {
  max-height: 50%;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  .chat-pinned-list {
    overflow-y: auto;
    flex: 1;
    min-height: 0;
    margin-left: 4px;
  }
}

.chat-history-section-scrollable {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  .chat-history-scroll-area {
    flex: 1;
    overflow-y: auto;
    min-height: 0;
    margin-left: 4px;
  }
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
