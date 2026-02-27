<script setup lang="ts">
import { ref, nextTick, watch } from 'vue'
import { ArrowUpOutlined } from '@ant-design/icons-vue'

const props = defineProps<{
  modelValue: string
  isRunning?: boolean
  placeholder?: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'send'): void
  (e: 'abort'): void
}>()

const textareaRef = ref<HTMLTextAreaElement | null>()

const autoResize = () => {
  const el = textareaRef.value
  if (!el) return
  el.style.height = 'auto'
  const maxHeight = 300
  el.style.height = `${Math.min(el.scrollHeight, maxHeight)}px`
}

const handleInput = (e: Event) => {
  const target = e.target as HTMLTextAreaElement
  emit('update:modelValue', target.value)
  nextTick(autoResize)
}

const handleKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    emit('send')
  }
}

watch(() => props.modelValue, () => {
  nextTick(autoResize)
})
</script>

<template>
  <div class="chat-input-textarea-wrap">
    <textarea
      ref="textareaRef"
      :value="modelValue"
      :placeholder="placeholder || '输入消息，Enter 发送，Shift+Enter 换行'"
      :disabled="isRunning"
      rows="1"
      class="chat-input-textarea"
      @input="handleInput"
      @keydown="handleKeydown"
    />
    <button
      type="button"
      class="chat-send-btn-inner"
      :disabled="!isRunning && !modelValue.trim()"
      @click="isRunning ? $emit('abort') : $emit('send')"
    >
      <div v-if="isRunning" style="width: 13px; height: 13px; background-color: #fff;border-radius: 2px;"></div>
      <ArrowUpOutlined v-else />
    </button>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/chat/index.scss' as *;
</style>
