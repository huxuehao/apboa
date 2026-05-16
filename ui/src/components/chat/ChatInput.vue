<script setup lang="ts">
/**
 * 聊天输入框容器组件
 * 组合附件、编辑器、工具栏三大子组件，对外保留原 props/emits 契约
 *
 * @component
 */
import { computed, ref } from 'vue'
import ChatInputAttachments from './ChatInputAttachments.vue'
import ChatInputEditor from './ChatInputEditor.vue'
import ChatInputToolbar from './ChatInputToolbar.vue'
import { useChatAttachments } from '@/composables/chat/useChatAttachments'
import type { FlatFileItem } from '@/composables/chat/useWorkspaceFiles'
import type { UploadedFileItem } from '@/types'

const props = withDefaults(
  defineProps<{
    modelValue: string
    agentId: string
    uploadedFiles?: UploadedFileItem[]
    isRunning?: boolean
    placeholder?: string
    memoryActive?: boolean
    planActive?: boolean
    enableMemory?: boolean
    enablePlanning?: boolean
    toolProcessActive?: boolean
    showToolProcess?: boolean
    allowUploadFileType?: string[]
    sessionId?: string | null
    mentionAllowed?: boolean
  }>(),
  {
    uploadedFiles: () => [],
    memoryActive: false,
    planActive: false,
    enableMemory: false,
    enablePlanning: false,
    toolProcessActive: false,
    showToolProcess: false,
    sessionId: null,
    mentionAllowed: false
  }
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'update:uploadedFiles', value: UploadedFileItem[]): void
  (e: 'send'): void
  (e: 'abort'): void
  (e: 'memory', value: boolean): void
  (e: 'plan', value: boolean): void
  (e: 'toolProcess', value: boolean): void
  (e: 'inputTagPreview', value: FlatFileItem): void
}>()

const fileInputRef = ref<HTMLInputElement | null>()
const editorRef = ref<InstanceType<typeof ChatInputEditor> | null>()

/** 附件操作集合 */
const { fileAcceptAttr, handleFileChange, removeFile } = useChatAttachments({
  getFiles: () => props.uploadedFiles ?? [],
  setFiles: (files) => emit('update:uploadedFiles', files),
  getAllowedTypes: () => props.allowUploadFileType
})

/** 当前 input accept 属性值 */
const fileAccept = computed(() => fileAcceptAttr())

/**
 * 综合判断是否允许触发发送：
 * - 没有上传中的文件
 * - 文本内容或非上传中附件至少存在其一
 */
const canSend = computed(() => {
  const files = props.uploadedFiles ?? []
  const hasUploading = files.some((f) => f.uploading)
  if (hasUploading) return false
  const hasText = props.modelValue.trim().length > 0
  const hasReadyAttach = files.filter((f) => !f.uploading).length > 0
  return hasText || hasReadyAttach
})

/**
 * v-model 文本透传
 */
const handleEditorUpdate = (value: string) => {
  emit('update:modelValue', value)
}
</script>

<template>
  <div class="chat-input-wrap">
    <input
      ref="fileInputRef"
      type="file"
      class="chat-file-input-hidden"
      :accept="fileAccept"
      multiple
      @change="handleFileChange"
    />

    <ChatInputAttachments
      :files="uploadedFiles ?? []"
      @remove="removeFile"
    />

    <ChatInputEditor
      ref="editorRef"
      :agent-id="agentId"
      :model-value="modelValue"
      :placeholder="placeholder || '输入消息...'"
      :session-id="sessionId"
      :mention-allowed="mentionAllowed"
      :is-running="isRunning"
      @update:model-value="handleEditorUpdate"
      @send="emit('send')"
      @input-tag-preview="(item) => emit('inputTagPreview', item)"
    />

    <ChatInputToolbar
      :is-running="isRunning"
      :can-send="canSend"
      :enable-memory="enableMemory"
      :memory-active="memoryActive"
      :show-tool-process="showToolProcess"
      :tool-process-active="toolProcessActive"
      :mention-allowed="mentionAllowed"
      :allow-upload-file-type="allowUploadFileType"
      @memory="(v) => emit('memory', v)"
      @tool-process="(v) => emit('toolProcess', v)"
      @mention-trigger="editorRef?.triggerMention()"
      @pick-file="fileInputRef?.click()"
      @send="emit('send')"
      @abort="emit('abort')"
    />
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

.chat-file-input-hidden {
  position: absolute;
  width: 0;
  height: 0;
  opacity: 0;
  pointer-events: none;
}
</style>
