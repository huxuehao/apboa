<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { LoadingOutlined, BulbOutlined, CopyOutlined, CheckOutlined } from '@ant-design/icons-vue'
import MediaPreview from '@/components/common/MediaPreview.vue'
import type { UploadedFileItem } from '@/types'
import MediaIcon from '@/components/common/MediaIcon.vue'
import MarkdownRenderer from "@/components/markdown/MarkdownRenderer.vue";
import TaggedContentRenderer from './TaggedContentRenderer.vue';

const FILE_SEP = '@==##::::##==@'

/**
 * 解析用户内容，分离文件和文本
 */
function parseUserContent(content: string): { files: UploadedFileItem[]; text: string } {
  const idx = content.indexOf(FILE_SEP)
  if (idx === -1) return { files: [], text: content }
  const prefix = content.slice(0, idx)
  const text = content.slice(idx + FILE_SEP.length)
  try {
    const parsed = JSON.parse(prefix) as { files?: UploadedFileItem[] }
    const files = Array.isArray(parsed?.files) ? parsed.files : []
    return { files, text }
  } catch {
    return { files: [], text: content }
  }
}

/** 从文件名解析扩展名（小写） */
const getExtension = (fileName: string): string => {
  const lastDot = fileName.lastIndexOf('.')
  return lastDot > -1 ? fileName.slice(lastDot + 1).toLowerCase() : ''
}

/**
 * 格式化时间显示
 * 输入格式：YYYY-MM-DD HH:mm:ss
 * - 今天：显示 HH:mm
 * - 本年非今天：显示 MM-DD HH:mm
 * - 非本年：显示 YYYY-MM-DD HH:mm
 */
const formatTime = (dateStr?: string): string => {
  if (!dateStr) return ''

  // 直接截取，避免不必要的 split 操作
  const datePart = dateStr.slice(0, 10)
  const timePart = dateStr.slice(11, 16) // HH:mm

  if (datePart.length < 10) return ''

  // 一次性解析日期部分
  const year = datePart.slice(0, 4)
  const month = datePart.slice(5, 7)
  const day = datePart.slice(8, 10)

  const now = new Date()
  const currentYear = String(now.getFullYear())

  // 今日判断：比较时间戳（最高效）
  const todayStr = `${currentYear}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`

  if (datePart === todayStr) {
    return timePart
  }

  if (year === currentYear) {
    return `${month}/${day} ${timePart}`
  }

  return `${year}/${month}/${day} ${timePart}`
}

const props = defineProps<{
  role: 'user' | 'assistant' | 'system' | 'tool' | 'error'
  content: string
  createdAt?: string
  agentHasResult?: boolean
  isStreaming?: boolean
  /** 推理内容 */
  reasoningContent?: string
  /** 推理消息 ID */
  reasoningMessageId?: string
  /** 推理是否还在流式进行中 */
  reasoningStreaming?: boolean
}>()

defineEmits(['inputTagPreview'])

const isUser = computed(() => props.role === 'user')
const isAssistant = computed(() => props.role === 'assistant')
const isTool = computed(() => props.role === 'tool')
const isError = computed(() => props.role === 'error')

const parsedUserContent = computed(() => parseUserContent(props.content))
const formattedTime = computed(() => formatTime(props.createdAt))

// 预览相关状态
const previewVisible = ref(false)
const previewCurrentIndex = ref(0)

// 推理面板展开状态
const reasoningExpanded = ref(false)

// 是否有推理内容
const hasReasoning = computed(() => !!props.reasoningContent)

// 复制成功状态（2秒内）
const copied = ref(false)

/**
 * 待复制的文本内容
 * 用户消息：文件名列表 + 文本；AI消息：仅正文（不包含推理过程）
 */
const copyText = computed(() => {
  if (isUser.value) {
    const { files, text } = parsedUserContent.value
    if (files.length === 0) return text
    const fileNames = files.map(f => f.name).join('\n')
    return text ? `${fileNames}\n${text}` : fileNames
  }
  return props.content
})

/**
 * 复制消息内容到剪贴板
 * 使用 Clipboard API，失败时降级到 execCommand
 */
async function handleCopy() {
  if (copied.value || !copyText.value) return
  const text = copyText.value
  try {
    if (navigator.clipboard && navigator.clipboard.writeText) {
      await navigator.clipboard.writeText(text)
    } else {
      throw new Error('clipboard unavailable')
    }
  } catch {
    const textarea = document.createElement('textarea')
    textarea.value = text
    textarea.style.position = 'fixed'
    textarea.style.opacity = '0'
    document.body.appendChild(textarea)
    textarea.select()
    try {
      document.execCommand('copy')
    } catch {
      // 复制失败静默处理
    }
    document.body.removeChild(textarea)
  }
  copied.value = true
  setTimeout(() => { copied.value = false }, 2000)
}

// 推理进行中时自动展开面板
watch(
  () => props.reasoningStreaming,
  (streaming) => {
    if (streaming) {
      reasoningExpanded.value = true
    }
  }
)

/**
 * 打开文件预览
 */
const openPreview = (index: number) => {
  previewCurrentIndex.value = index
  previewVisible.value = true
}
</script>

<template>
  <div class="chat-message" :class="[isUser ? 'chat-message-user' : 'chat-message-assistant']">
    <template v-if="isUser">
      <div class="chat-message-bubble chat-message-bubble_user" style="position: relative">
        <div class="message-time">{{ formattedTime }}</div>
        <!-- 文件列表 -->
        <div v-if="parsedUserContent.files.length > 0" class="chat-message-files">
          <div
            v-for="(item, index) in parsedUserContent.files"
            :key="item.id"
            @click="openPreview(index)"
            class="chat-message-file-item"
          >
            <MediaIcon :type="(item.extension ?? getExtension(item.name)) || 'FILE'" size="19"/>
            <span class="chat-message-file-name" :title="item.name">{{ item.name }}</span>
          </div>
        </div>
        <!-- 文本内容（支持标签渲染） -->
        <span v-if="parsedUserContent.text" class="chat-message-user-content">
          <TaggedContentRenderer
            @inputTagPreview="$emit('inputTagPreview', $event)"
            :content="parsedUserContent.text" />
        </span>
        <!-- 复制按钮：悬浮显现于气泡左侧 -->
        <span
          class="msg-copy-btn msg-copy-btn--user"
          :class="{ 'is-done': copied }"
          :title="copied ? '已复制' : '复制'"
          @click="handleCopy"
        >
          <CheckOutlined v-if="copied" />
          <CopyOutlined v-else />
        </span>
      </div>
    </template>
    <template v-else-if="isAssistant">
      <div class="chat-message-bubble">
        <div v-if="!agentHasResult && !content && !reasoningContent" class="chat-loading-dots">
          <span></span><span></span><span></span>
        </div>
        <!-- 推理过程面板（独立于正文显示） -->
        <div v-if="hasReasoning" class="chat-reasoning-panel">
          <div class="chat-reasoning-header" @click="reasoningExpanded = !reasoningExpanded">
            <span class="chat-reasoning-icon">
              <LoadingOutlined v-if="reasoningStreaming" spin />
              <BulbOutlined v-else />
            </span>
            <span class="chat-reasoning-title">
              {{ reasoningStreaming ? '思考中...' : '思考过程' }}
            </span>
          </div>
          <div v-show="reasoningExpanded" class="chat-reasoning-content">
            {{reasoningContent}}
          </div>
        </div>
        <!-- 正文内容 -->
        <div v-if="content" class="chat-md-content">
          <MarkdownRenderer :content="content" />
        </div>
        <!-- 复制按钮：悬浮显现于正文下方 -->
        <span
          v-if="content"
          class="msg-copy-btn msg-copy-btn--assistant"
          :class="{ 'is-done': copied }"
          :title="copied ? '已复制' : '复制'"
          @click="handleCopy"
        >
          <CheckOutlined v-if="copied" />
          <CopyOutlined v-else />
        </span>
      </div>
    </template>
    <template v-else-if="isTool">
      <div class="chat-message-bubble">
        <div class="chat-md-content chat-tool-content">
          <MarkdownRenderer :content="content" />
        </div>
      </div>
    </template>
    <template v-else-if="isError">
      <div class="chat-message-bubble">
        <div class="chat-md-content">
          <span class="error-text">{{ content }}</span>
        </div>
      </div>
    </template>
    <!-- 媒体预览组件 -->
    <MediaPreview
      v-if="previewVisible"
      v-model:visible="previewVisible"
      :items="parsedUserContent.files"
      :current-index="previewCurrentIndex"
    />
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/chat/index.scss' as *;

.message-time {
  position: absolute;
  top: -18px;
  right: 3px;
  width: 150px;
  text-align: end;
  font-size: var(--font-size-xs);
  color: #d2d2d2;
}

.error-text {
  color: tomato;
}

.chat-message-files {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 8px;
}

.chat-message-file-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  max-width: 280px;
  padding: 6px 10px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: var(--border-radius-md);
  font-size: var(--font-size-sm);
  cursor: pointer;
  &:hover {
    background: rgba(255, 255, 255, 0.9);
  }
}

.chat-message-file-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/**
 * 复制按钮：悬浮显现 + 图标状态反馈
 * 常态隐藏，鼠标悬停消息气泡时渐显，点击后图标切换为对勾并保持2秒
 */
.msg-copy-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: var(--border-radius-sm);
  font-size: 13px;
  color: #a0a4ab;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.2s ease, color 0.15s ease, background-color 0.15s ease;

  &:hover {
    color: #4a4f57;
  }

  &.is-done {
    color: #52c41a;
    opacity: 1;
  }
}

.msg-copy-btn--user {
  position: absolute;
  right: 0;
  bottom: -25px;
}

.msg-copy-btn--assistant {
  position: absolute;
  left: 10px;
  bottom: -20px;
}

/* 确保气泡作为绝对定位参照 */
.chat-message-assistant .chat-message-bubble {
  position: relative;
}

/* 悬停消息气泡时显示复制按钮 */
.chat-message-bubble:hover .msg-copy-btn {
  opacity: 1;
}
</style>
