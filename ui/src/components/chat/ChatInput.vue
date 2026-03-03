<script setup lang="ts">
import { ref, nextTick, watch } from 'vue'
import { message } from 'ant-design-vue'
import {
  ArrowUpOutlined,
  PaperClipOutlined,
  CloseOutlined,
  UnorderedListOutlined,
  ClockCircleOutlined,
  LoadingOutlined
} from '@ant-design/icons-vue'
import * as attachApi from '@/api/attach'
import type { UploadedFileItem } from '@/types'

const props = withDefaults(
  defineProps<{
    modelValue: string
    uploadedFiles?: UploadedFileItem[]
    isRunning?: boolean
    placeholder?: string
    memoryActive?: boolean
    planActive?: boolean
    enableMemory?: boolean
    enablePlanning?: boolean
    allowUploadFileType?: string[]
  }>(),
  { uploadedFiles: () => [], memoryActive: false, planActive: false, enableMemory: false, enablePlanning: false }
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'update:uploadedFiles', value: UploadedFileItem[]): void
  (e: 'send'): void
  (e: 'abort'): void
  (e: 'memory', value: boolean): void
  (e: 'plan', value: boolean): void
}>()

const textareaRef = ref<HTMLTextAreaElement | null>()
const fileInputRef = ref<HTMLInputElement | null>()

/** 格式化文件大小显示 */
const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(2))} ${sizes[i]}`
}

/** 从文件名解析扩展名（小写） */
const getExtension = (fileName: string): string => {
  const lastDot = fileName.lastIndexOf('.')
  return lastDot > -1 ? fileName.slice(lastDot + 1).toLowerCase() : ''
}

/** 检查文件类型是否在允许列表中 */
const isFileTypeAllowed = (extension: string): boolean => {
  const allowed = props.allowUploadFileType
  if (!allowed?.length) return true
  return allowed.some((t) => t.toLowerCase() === extension)
}

/** 根据 allowUploadFileType 生成 input accept 属性值 */
const fileAcceptAttr = (): string => {
  const allowed = props.allowUploadFileType
  if (!allowed?.length) return '*/*'
  return allowed.map((t) => `.${t}`).join(',')
}

const toggleMemory = () => {
  if (!props.enableMemory) return
  emit('memory', !props.memoryActive)
}
const togglePlan = () => {
  if (!props.enablePlanning) return
  emit('plan', !props.planActive)
}
const handleFileClick = () => {
  fileInputRef.value?.click()
}
const handleFileChange = async (e: Event) => {
  const input = e.target as HTMLInputElement
  const files = input.files
  if (!files?.length) {
    input.value = ''
    return
  }
  const fileArray = Array.from(files)
  const allowedFiles: File[] = []
  const rejectedNames: string[] = []
  for (const file of fileArray) {
    const ext = getExtension(file.name)
    if (isFileTypeAllowed(ext)) {
      allowedFiles.push(file)
    } else {
      rejectedNames.push(file.name)
    }
  }
  if (rejectedNames.length > 0) {
    message.warning(`以下文件类型不允许上传: ${rejectedNames.join(', ')}`)
  }
  if (allowedFiles.length === 0) {
    input.value = ''
    return
  }

  const current = props.uploadedFiles ?? []
  const newList = [...current]
  const tempIds: string[] = []

  // 立即将文件加入列表并显示（上传中状态）
  for (let i = 0; i < allowedFiles.length; i++) {
    const file = allowedFiles[i]
    if (!file) continue
    const tempId = `temp-${Date.now()}-${i}-${Math.random().toString(36).slice(2, 9)}`
    tempIds.push(tempId)
    newList.push({
      id: tempId,
      name: file.name,
      extension: getExtension(file.name),
      size: formatFileSize(file.size),
      uploading: true
    })
  }
  emit('update:uploadedFiles', newList)
  input.value = ''

  // 后台逐个上传，完成后更新对应项
  for (let i = 0; i < allowedFiles.length; i++) {
    const file = allowedFiles[i]
    const tempId = tempIds[i]
    if (!file || tempId === undefined) continue
    try {
      const res = await attachApi.upload(file)
      const data = res?.data?.data
      if (data) {
        const updated = (props.uploadedFiles ?? []).map((item) =>
          item.id === tempId ? { ...item, id: data, uploading: false } : item
        )
        emit('update:uploadedFiles', updated)
      } else {
        message.error(`上传失败: ${file.name}`)
        const filtered = (props.uploadedFiles ?? []).filter((f) => f.id !== tempId)
        emit('update:uploadedFiles', filtered)
      }
    } catch {
      message.error(`上传失败: ${file.name}`)
      const filtered = (props.uploadedFiles ?? []).filter((f) => f.id !== tempId)
      emit('update:uploadedFiles', filtered)
    }
  }
}
const removeFile = async (item: UploadedFileItem) => {
  // 上传中的文件无需调用删除接口
  if (!item.uploading && !item.id.startsWith('temp-')) {
    await attachApi.remove([item.id])
  }
  const newList = (props.uploadedFiles ?? []).filter((f) => f.id !== item.id)
  emit('update:uploadedFiles', newList)
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
      :accept="fileAcceptAttr()"
      multiple
      @change="handleFileChange"
    />
    <!-- 已上传附件列表：显示于 textarea 上方，支持横向滚动与单个移除 -->
    <div v-if="(uploadedFiles ?? []).length > 0" class="chat-input-files-row">
      <div class="chat-input-files-scroll">
        <div
          v-for="item in (uploadedFiles ?? [])"
          :key="item.id"
          class="chat-input-file-item"
        >
          <span v-if="item.uploading" class="chat-input-file-loading">
            <LoadingOutlined spin />
          </span>
          <span class="chat-input-file-tag">
            {{ (item.extension ?? getExtension(item.name)).toUpperCase() || 'FILE' }}
          </span>
          <span class="chat-input-file-name" :title="item.name">{{ item.name }}</span>
          <span class="chat-input-file-size">{{ item.size }}</span>
          <button
            type="button"
            class="chat-input-file-remove"
            title="移除"
            @click="removeFile(item)"
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
          :disabled="!enableMemory"
          type="button"
          class="chat-toolbar-btn chat-toolbar-btn-icon  chat-toolbar-btn-circle"
          title="记忆"
          :class="{ 'is-active': memoryActive && enableMemory }"
          @click="toggleMemory"
        >
          <ClockCircleOutlined />
        </button>
        <button
          :disabled="!enablePlanning"
          type="button"
          class="chat-toolbar-btn chat-toolbar-btn-icon  chat-toolbar-btn-circle"
          title="规划"
          :class="{ 'is-active': planActive && enablePlanning }"
          @click="togglePlan"
        >
          <UnorderedListOutlined />
        </button>
        <button
          :disabled="!allowUploadFileType?.length"
          type="button"
          class="chat-toolbar-btn chat-toolbar-btn-icon chat-toolbar-btn-circle"
          title="文件"
          @click="handleFileClick"
        >
          <PaperClipOutlined />
        </button>
      </div>
      <div class="chat-input-toolbar-right">
        <button
          type="button"
          class="chat-send-btn-inner"
          :disabled="!isRunning && ((uploadedFiles ?? []).some((f) => f.uploading) || (!modelValue.trim() && (uploadedFiles ?? []).filter((f) => !f.uploading).length === 0))"
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
  max-width: 280px;
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

.chat-input-file-loading {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  color: $chat-primary;
  font-size: 14px;
}

.chat-input-file-tag {
  flex-shrink: 0;
  padding: 2px 6px;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.04em;
  border-radius: 4px;
  color: var(--color-text-regular);
  background: rgba(0, 0, 0, 0.06);
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
  margin-right: 5px;

  &:hover {
    color: $chat-primary;
    background-color: rgba($chat-primary, 0.06);
  }

  &.is-active {
    color: $chat-primary;
    background-color: rgba($chat-primary, 0.1);
    font-weight: 500;
  }

  &:disabled,
  &[disabled] {
    &:hover {
      cursor: not-allowed;
      color: var(--color-text-secondary);
      background-color: transparent;
    }
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
