<script setup lang="ts">
import { ref, nextTick, watch } from 'vue'
import { message } from 'ant-design-vue'
import {
  ArrowUpOutlined,
  PaperClipOutlined,
  CloseOutlined,
  UnorderedListOutlined,
  ClockCircleOutlined
} from '@ant-design/icons-vue'

const props = withDefaults(
  defineProps<{
    modelValue: string
    isRunning?: boolean
    placeholder?: string
    memoryActive?: boolean
    planActive?: boolean
  }>(),
  { memoryActive: false, planActive: false }
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'send'): void
  (e: 'abort'): void
  (e: 'memory', value: boolean): void
  (e: 'plan', value: boolean): void
  (e: 'file', file: File): void
}>()

const textareaRef = ref<HTMLTextAreaElement | null>()
const fileInputRef = ref<HTMLInputElement | null>()
const selectedFiles = ref<File[]>([])

/** 格式化文件大小显示 */
const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(2))} ${sizes[i]}`
}

const toggleMemory = () => {
  emit('memory', !props.memoryActive)
}
const togglePlan = () => {
  emit('plan', !props.planActive)
}
const handleFileClick = () => {
  message.info('暂未开放，敬请期待')
  // fileInputRef.value?.click()
}
const handleFileChange = (e: Event) => {
  const input = e.target as HTMLInputElement
  const files = input.files
  if (files?.length) {
    const newFiles = Array.from(files)
    selectedFiles.value = [...selectedFiles.value, ...newFiles]
    newFiles.forEach((file) => emit('file', file))
  }
  input.value = ''
}
const removeFile = (index: number) => {
  selectedFiles.value = selectedFiles.value.filter((_, i) => i !== index)
}

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
  <div class="chat-input-wrap">
    <input
      ref="fileInputRef"
      type="file"
      class="chat-file-input-hidden"
      accept="*/*"
      multiple
      @change="handleFileChange"
    />
    <!-- 已选文件列表：显示于 textarea 上方，支持横向滚动与单个移除 -->
    <div v-if="selectedFiles.length > 0" class="chat-input-files-row">
      <div class="chat-input-files-scroll">
        <div
          v-for="(file, index) in selectedFiles"
          :key="`${file.name}-${file.size}-${index}`"
          class="chat-input-file-item"
        >
          <span class="chat-input-file-name" :title="file.name">{{ file.name }}</span>
          <span class="chat-input-file-size">{{ formatFileSize(file.size) }}</span>
          <button
            type="button"
            class="chat-input-file-remove"
            title="移除"
            @click="removeFile(index)"
          >
            <CloseOutlined />
          </button>
        </div>
      </div>
    </div>
    <div class="chat-input-textarea-row">
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
    </div>
    <div class="chat-input-toolbar">
      <div class="chat-input-toolbar-left">
        <button
          type="button"
          class="chat-toolbar-btn chat-toolbar-btn-icon  chat-toolbar-btn-circle"
          title="记忆"
          :class="{ 'is-active': memoryActive }"
          @click="toggleMemory"
        >
          <ClockCircleOutlined />
        </button>
        <button
          type="button"
          class="chat-toolbar-btn chat-toolbar-btn-icon  chat-toolbar-btn-circle"
          title="规划"
          :class="{ 'is-active': planActive }"
          @click="togglePlan"
        >
          <UnorderedListOutlined />
        </button>
        <button
          type="button"
          class="chat-toolbar-btn chat-toolbar-btn-icon chat-toolbar-btn-circle"
          title="附件"
          @click="handleFileClick"
        >
          <PaperClipOutlined />
        </button>
      </div>
      <div class="chat-input-toolbar-right">
        <button
          type="button"
          class="chat-send-btn-inner"
          :disabled="!isRunning && !modelValue.trim()"
          @click="isRunning ? $emit('abort') : $emit('send')"
        >
          <template v-if="isRunning"><div class="send"></div></template>
          <ArrowUpOutlined v-else />
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/chat/index.scss' as *;

.chat-input-wrap {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
  border-radius: 24px;
  border: 1px solid var(--color-border-light);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  padding: 10px 12px;
  background-color: $chat-bg-main;
  transition: border-color 0.25s ease, box-shadow 0.25s ease;
  max-height: 400px;

  &:focus-within {
    border-color: $chat-primary;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06), 0 0 0 2px rgba($chat-primary, 0.1);
  }
}

.chat-input-files-row {
  flex-shrink: 0;
  min-height: 0;
  overflow: hidden;
}

.chat-input-files-scroll {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  overflow-y: hidden;
  padding: 4px 0;
  scrollbar-width: thin;
  scrollbar-color: var(--color-border-light) transparent;

  &::-webkit-scrollbar {
    height: 6px;
  }
  &::-webkit-scrollbar-track {
    background: transparent;
  }
  &::-webkit-scrollbar-thumb {
    background: var(--color-border-light);
    border-radius: 3px;
  }
  &::-webkit-scrollbar-thumb:hover {
    background: var(--color-text-placeholder);
  }
}

.chat-input-file-item {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  max-width: 220px;
  padding: 6px 10px;
  background: rgba($chat-primary, 0.06);
  // border: 1px solid rgba($chat-primary, 0.15);
  border-radius: var(--border-radius-md);
  font-size: var(--font-size-sm);
  transition: background-color 0.2s ease, border-color 0.2s ease;

  &:hover {
    background: rgba($chat-primary, 0.1);
    border-color: rgba($chat-primary, 0.25);
  }
}

.chat-input-file-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--color-text-primary);
}

.chat-input-file-size {
  flex-shrink: 0;
  color: var(--color-text-placeholder);
  font-size: var(--font-size-xs);
}

.chat-input-file-remove {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  padding: 0;
  border: none;
  background: transparent;
  color: var(--color-text-placeholder);
  cursor: pointer;
  border-radius: 50%;
  font-size: 12px;
  transition: color 0.2s ease, background-color 0.2s ease;

  &:hover {
    color: var(--color-text-primary);
    background: rgba(0, 0, 0, 0.08);
  }
}

.chat-input-textarea-row {
  flex: 1;
  min-height: 0;
  display: flex;
}

.chat-input-textarea {
  flex: 1;
  min-height: 0;
  max-height: 300px;
  overflow-y: auto;
  border: none;
  outline: none;
  resize: none;
  font-size: var(--font-size-base);
  line-height: 1.5;
  color: var(--color-text-primary);
  background: transparent;
  padding: 5px 0;

  &::placeholder {
    color: var(--color-text-placeholder);
  }
}

.chat-input-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
  min-height: 36px;
}

.chat-input-toolbar-left {
  display: flex;
  align-items: center;
  gap: 4px;
}

.chat-input-toolbar-right {
  display: flex;
  align-items: center;
}

.chat-toolbar-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  cursor: pointer;
  color: var(--color-text-secondary);
  transition: color 0.2s ease, background-color 0.2s ease;
  border-radius: var(--border-radius-md);

  &:hover {
    color: $chat-primary;
    background-color: rgba($chat-primary, 0.06);
  }

  &.is-active {
    color: $chat-primary;
    background-color: rgba($chat-primary, 0.1);
    font-weight: 500;
  }
}

.chat-toolbar-btn-text {
  padding: 6px 10px;
  font-size: var(--font-size-sm);
}

.chat-toolbar-btn-icon {
  width: 32px;
  height: 32px;
  font-size: 16px;
}

.chat-toolbar-btn-circle {
  border-radius: 50%;
}

.chat-file-input-hidden {
  position: absolute;
  width: 0;
  height: 0;
  opacity: 0;
  pointer-events: none;
}

.send {
  width: 13px;
  height: 13px;
  background-color: #fff;
  border-radius: 2px;
}
</style>
