<script setup lang="ts">
/**
 * 聊天输入框工具栏组件
 * 负责记忆开关、工具调用历史开关、@按钮、上传按钮、发送/中断按钮
 *
 * @component
 */
import {
  ArrowUpOutlined,
  ClockCircleOutlined,
  PaperClipOutlined,
  ThunderboltOutlined
} from '@ant-design/icons-vue'

const props = withDefaults(
  defineProps<{
    isRunning?: boolean
    /** 是否允许触发发送（综合上传中、内容、附件等条件） */
    canSend: boolean
    enableMemory?: boolean
    memoryActive?: boolean
    showToolProcess?: boolean
    toolProcessActive?: boolean
    mentionAllowed?: boolean
    allowUploadFileType?: string[]
  }>(),
  {
    isRunning: false,
    enableMemory: false,
    memoryActive: false,
    showToolProcess: false,
    toolProcessActive: false,
    mentionAllowed: false
  }
)

const emit = defineEmits<{
  (e: 'memory', value: boolean): void
  (e: 'toolProcess', value: boolean): void
  (e: 'mentionTrigger'): void
  (e: 'pickFile'): void
  (e: 'send'): void
  (e: 'abort'): void
}>()

/**
 * 切换记忆按钮，未启用记忆能力时不响应
 */
const toggleMemory = () => {
  if (!props.enableMemory) return
  emit('memory', !props.memoryActive)
}

/**
 * 切换工具调用历史按钮，未启用时不响应
 */
const toggleToolProcess = () => {
  if (!props.showToolProcess) return
  emit('toolProcess', !props.toolProcessActive)
}
</script>

<template>
  <div class="chat-input-toolbar">
    <div class="chat-input-toolbar-left">
      <ATooltip placement="bottom">
        <template #title>
          <span v-if="enableMemory">{{ (memoryActive && enableMemory) ? '点击关闭记忆' : '点击开启记忆' }}</span>
          <span v-else>不支持记忆持久化</span>
        </template>
        <button
          :disabled="!enableMemory"
          type="button"
          class="chat-toolbar-btn chat-toolbar-btn-icon chat-toolbar-btn-circle"
          :class="{ 'is-active': memoryActive && enableMemory }"
          @click="toggleMemory"
        >
          <ClockCircleOutlined />
        </button>
      </ATooltip>

      <ATooltip placement="bottom">
        <template #title>
          <span v-if="showToolProcess">{{ (toolProcessActive && showToolProcess) ? '点击关闭工具调用历史' : '点击显示工具调用历史' }}</span>
          <span v-else>不支持控制工具调用显示</span>
        </template>
        <button
          :disabled="!showToolProcess"
          type="button"
          class="chat-toolbar-btn chat-toolbar-btn-icon chat-toolbar-btn-circle"
          :class="{ 'is-active': toolProcessActive && showToolProcess }"
          @click="toggleToolProcess"
        >
          <ThunderboltOutlined />
        </button>
      </ATooltip>
    </div>
    <div class="chat-input-toolbar-right">
      <!-- @ 添加上下文按钮 -->
      <ATooltip placement="bottom" title="添加上下文">
        <button
          :disabled="!mentionAllowed"
          type="button"
          class="chat-toolbar-btn chat-toolbar-btn-icon chat-toolbar-btn-circle"
          @mousedown.prevent
          @click="emit('mentionTrigger')"
        >
          @
        </button>
      </ATooltip>
      <ATooltip placement="bottom">
        <template #title>
          <span v-if="allowUploadFileType && allowUploadFileType.length > 0">
            点击上传文件（{{ allowUploadFileType.join('、') }}）
          </span>
          <span v-else>不支持上传文件</span>
        </template>
        <button
          :disabled="!allowUploadFileType?.length"
          type="button"
          class="chat-toolbar-btn chat-toolbar-btn-icon chat-toolbar-btn-circle"
          style="margin-right: 15px"
          @click="emit('pickFile')"
        >
          <PaperClipOutlined />
        </button>
      </ATooltip>
      <button
        type="button"
        class="chat-send-btn-inner"
        :disabled="!isRunning && !canSend"
        @click="isRunning ? emit('abort') : emit('send')"
      >
        <template v-if="isRunning"><div class="send"></div></template>
        <ArrowUpOutlined v-else />
      </button>
    </div>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/chat/index.scss' as *;

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
  background-color: #f5f5f5;
  cursor: pointer;
  color: var(--color-text-secondary);
  transition: color 0.2s ease, background-color 0.2s ease;
  border-radius: var(--border-radius-md);
  margin-right: 10px;

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

.chat-send-btn-inner {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background-color: $chat-primary;
  border: none;
  color: white;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover:not(:disabled) {
    transform: scale(1.05);
  }

  &:disabled {
    background-color: #e0e0e0;
    cursor: not-allowed;
    opacity: 0.6;
  }

  .send {
    width: 13px;
    height: 13px;
    background-color: #fff;
    border-radius: 2px;
  }
}
</style>
