<script setup lang="ts">
import { ref, nextTick, watch, onMounted } from 'vue'
import MessageList from './MessageList.vue'
import ChatInput from './ChatInput.vue'
import Welcome from './Welcome.vue'
import type { DisplayMessage } from '../types'

const props = defineProps<{
  title: string
  isWelcomeMode: boolean
  welcomeHeadline: string
  welcomeDesc?: string
  messages: DisplayMessage[]
  toolCalls: any[]
  inputValue: string
  isRunning: boolean
}>()

const emit = defineEmits<{
  (e: 'update:inputValue', value: string): void
  (e: 'send'): void
  (e: 'scroll', event: UIEvent): void
  (e: 'toolContent', value: any): void
  (e: 'abort'): void
}>()

// 滚动容器 ref
const messagesScrollRef = ref<HTMLElement | null>()
const shouldAutoScroll = ref(true)
const SCROLL_BOTTOM_THRESHOLD = 80

// 滚动到底部
const scrollToBottom = (smooth = false) => {
  const el = messagesScrollRef.value
  if (!el) return

  nextTick(() => {
    if (smooth) {
      el.scrollTo({
        top: el.scrollHeight,
        behavior: 'smooth'
      })
    } else {
      el.scrollTop = el.scrollHeight
    }
  })
}

// 检查并更新自动滚动状态
const checkAndUpdateAutoScroll = () => {
  const el = messagesScrollRef.value
  if (!el) return

  const { scrollTop, scrollHeight, clientHeight } = el
  const distanceFromBottom = scrollHeight - scrollTop - clientHeight
  shouldAutoScroll.value = distanceFromBottom <= SCROLL_BOTTOM_THRESHOLD
}

// 处理滚动事件
const handleScroll = (event: UIEvent | any) => {
  checkAndUpdateAutoScroll()
  emit('scroll', event)
}

// 监听消息变化，自动滚动
watch(
  () => props.messages,
  () => {
    if (shouldAutoScroll.value) {
      scrollToBottom()
    }
  },
  { deep: true, flush: 'post' }
)

// 监听流式内容
watch(
  () => props.toolCalls,
  () => {
    if (shouldAutoScroll.value) {
      scrollToBottom()
    }
  },
  { deep: true, flush: 'post' }
)

// 组件挂载后滚动到底部
onMounted(() => {
  if (props.messages.length > 0) {
    scrollToBottom()
  }
})

// 暴露方法给父组件（如果需要）
defineExpose({
  scrollToBottom
})
</script>

<template>
  <main class="chat-main">
    <header class="chat-main-header">
      <h1 class="chat-main-title" :title="title">{{ title }}</h1>
    </header>

    <div v-if="isWelcomeMode" class="chat-welcome-container">
      <Welcome
        :headline="welcomeHeadline"
        :description="welcomeDesc"
        :input-value="inputValue"
        :isRunning="isRunning"
        @update:input-value="$emit('update:inputValue', $event)"
        @send="$emit('send')"
      />
    </div>

    <template v-else>
      <div
        ref="messagesScrollRef"
        class="chat-main-messages-scroll"
        @scroll="handleScroll"
      >
        <MessageList
          :messages="messages"
          :tool-calls="toolCalls"
          @toolContent="(content: any) => $emit('toolContent', content)"
        />
      </div>
      <div class="chat-main-input-wrap">
        <div class="chat-input-outer">
          <ChatInput
            :model-value="inputValue"
            :isRunning="isRunning"
            @update:model-value="$emit('update:inputValue', $event)"
            @send="$emit('send')"
            @abort="$emit('abort')"
          />
          <div class="text-placeholder text-xs mt-sm" style="text-align: center; margin: 5px 0;">内容由AI生成，仅供参考</div>
        </div>
      </div>
    </template>
  </main>
</template>

<style scoped lang="scss">
@use '@/styles/chat/index.scss' as *;
</style>
