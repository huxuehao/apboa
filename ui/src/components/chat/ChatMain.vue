<script setup lang="ts">
import {ref, watch, onMounted} from 'vue'
import {
  MenuOutlined,
  FolderOutlined,
  FolderOpenOutlined,
  LoadingOutlined
} from '@ant-design/icons-vue'
import MessageList from './MessageList.vue'
import ChatInput from './ChatInput.vue'
import Welcome from './Welcome.vue'
import type { DisplayMessage, UploadedFileItem } from '@/types'
import type {FlatFileItem} from "@/composables/chat/useWorkspaceFiles.ts";
import WorkspaceFilePreview from "@/components/workspace/WorkspaceFilePreview.vue";

const props = defineProps<{
  title: string
  messageSize: number
  welcomeHeadline: string
  welcomeDesc?: string
  messages: DisplayMessage[]
  toolCalls: any[]
  inputValue: string
  uploadedFiles?: UploadedFileItem[]
  isRunning: boolean
  agentId: string
  memoryActive?: boolean
  planActive?: boolean
  enableMemory?: boolean
  enablePlanning?: boolean
  toolProcessActive?: boolean
  showToolProcess?: boolean
  allowUploadFileType?: string[]
  agentHasResult?: boolean
  workspacePanelOpen?: boolean
  hasCodeExecutionConfig?: boolean
  sessionId?: string | null
  /** 是否还有更早的历史消息 */
  hasMoreHistory?: boolean
  /** 历史消息加载中 */
  historyLoading?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:inputValue', value: string): void
  (e: 'update:uploadedFiles', value: UploadedFileItem[]): void
  (e: 'send'): void
  (e: 'scroll', event: Event): void
  (e: 'toolContent', value: any): void
  (e: 'abort'): void
  (e: 'memory', value: boolean): void
  (e: 'plan', value: boolean): void
  (e: 'toolProcess', value: boolean): void
  (e: 'toggleSidebar'): void
  (e: 'toggleWorkspace'): void
  /** 触发加载更多历史消息 */
  (e: 'loadMoreHistory'): void
  (e: 'newSession'): void
}>()

// 滚动容器 ref
const messagesScrollRef = ref<HTMLElement | null>()
const shouldAutoScroll = ref(true)
const SCROLL_BOTTOM_THRESHOLD = 80
// 触顶加载阈值
const SCROLL_TOP_THRESHOLD = 50
// 加载前记录滚动位置
const savedScrollHeight = ref(0)
const savedScrollTop = ref(0)

const workspaceFilePreviewVisible = ref(false)
const workspaceFilePreviewNode = ref<FlatFileItem | null>(null)

// 标志位：区分程序化滚动与用户手动滚动，防止 scrollToBottom 触发的 scroll 事件错误更新 shouldAutoScroll
let programmaticScrolling = false
// 待执行的自动滚动 rAF ID，用于在用户主动上滑时立即取消，打破"抗衡"
let scrollRafId: number | null = null

/**
 * 滚动到底部
 * 使用 requestAnimationFrame 确保在浏览器绘制前拿到最新的 scrollHeight。
 * 每次调度前取消上一次的待执行回调，执行时二次校验 shouldAutoScroll，
 * 防止用户上滑后被已调度的回调拉回底部。
 */
const scrollToBottom = () => {
  const el = messagesScrollRef.value
  if (!el) return

  // 取消上一次待执行的自动滚动，避免 rAF 堆积
  if (scrollRafId !== null) {
    cancelAnimationFrame(scrollRafId)
    scrollRafId = null
  }

  scrollRafId = requestAnimationFrame(() => {
    scrollRafId = null
    if (!el) return

    // 二次校验：rAF 回调执行时用户可能已经上滑，此时应放弃自动滚底
    if (!shouldAutoScroll.value) return

    programmaticScrolling = true
    el.scrollTop = el.scrollHeight
    // 延迟重置标志位，确保本次 scroll 事件已触发并被跳过
    setTimeout(() => { programmaticScrolling = false }, 100)
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
const handleScroll = (event: Event) => {
  // 跳过程序化滚动触发的事件，避免展开面板/DOM变化时错误更新 shouldAutoScroll
  if (programmaticScrolling) {
    emit('scroll', event)
    return
  }

  checkAndUpdateAutoScroll()

  // 用户主动上滑离开底部时，立即取消待执行的自动滚动，消除"抗衡感"
  if (!shouldAutoScroll.value && scrollRafId !== null) {
    cancelAnimationFrame(scrollRafId)
    scrollRafId = null
  }

  // 触顶检测：向上滚动接近顶部时加载更多历史消息
  const el = messagesScrollRef.value
  if (
    el && el.scrollTop <= SCROLL_TOP_THRESHOLD
    && props.hasMoreHistory
    && !props.historyLoading
  ) {
    // 记录加载前的滚动位置
    savedScrollHeight.value = el.scrollHeight
    savedScrollTop.value = el.scrollTop
    emit('loadMoreHistory')
  }

  emit('scroll', event)
}

// 处理发送事件
/**
 * 加载历史消息后保持滚动位置不变（视觉上不跳动）
 */
const maintainScrollPosition = () => {
  requestAnimationFrame(() => {
    const el = messagesScrollRef.value
    if (!el) return
    const heightDiff = el.scrollHeight - savedScrollHeight.value
    programmaticScrolling = true
    el.scrollTop = savedScrollTop.value + heightDiff
    setTimeout(() => { programmaticScrolling = false }, 100)
  })
}

const handleSend = () => {
  emit('send')
  // 发送后强制自动滚动到底部
  shouldAutoScroll.value = true
  scrollToBottom()
}

// 预览输入tag
const inputTagPreviewHandle = (file: FlatFileItem) => {
  workspaceFilePreviewVisible.value = true
  workspaceFilePreviewNode.value = file
}

// 监听消息变化，自动滚动
watch(
  () => props.messages,
  (newVal: DisplayMessage[], oldVal: DisplayMessage[]) => {
    // 历史消息加载：新消息被插入头部
    if (oldVal && newVal.length > oldVal.length && newVal[0]?.id !== oldVal[0]?.id) {
      maintainScrollPosition()
      return
    }
    // 正常新消息：自动滚动到底部
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
      <!-- 移动端菜单按钮 -->
      <button
        type="button"
        class="chat-mobile-menu-btn"
        title="打开会话列表"
        @click="$emit('toggleSidebar')"
      >
        <MenuOutlined />
      </button>
      <h1 class="chat-main-title" :title="title">{{ title }}</h1>
      <!-- 工作空间入口按钮（与左侧菜单按钮对称） -->
      <ATooltip placement="left" title="工作空间">
        <button
          v-if="sessionId && hasCodeExecutionConfig"
          type="button"
          class="chat-workspace-btn"
          :class="{ 'is-active': workspacePanelOpen }"
          @click="$emit('toggleWorkspace')"
        >
          <FolderOpenOutlined v-if="workspacePanelOpen" />
          <FolderOutlined v-else />
        </button>
      </ATooltip>

    </header>

    <div v-if="messageSize <= 1" class="chat-welcome-container">
      <Welcome
        :message-size="messageSize"
        :headline="welcomeHeadline"
        :input-value="inputValue"
        :agent-id="agentId"
        :description="welcomeDesc"
        :uploaded-files="uploadedFiles"
        :isRunning="isRunning"
        :memory-active="memoryActive"
        :plan-active="planActive"
        :enable-memory="enableMemory"
        :enable-planning="enablePlanning"
        :allow-upload-file-type="allowUploadFileType"
        :show-tool-process="showToolProcess"
        :tool-process-active="toolProcessActive"
        :session-id="sessionId"
        :has-code-execution-config="hasCodeExecutionConfig"
        :mention-allowed="true"
        @update:input-value="$emit('update:inputValue', $event)"
        @update:uploaded-files="$emit('update:uploadedFiles', $event)"
        @memory="$emit('memory', $event)"
        @plan="$emit('plan', $event)"
        @toolProcess="$emit('toolProcess', $event)"
        @send="handleSend"
        @new-session="$emit('newSession')"
      />
    </div>

    <template v-else>
      <div
        ref="messagesScrollRef"
        class="chat-main-messages-scroll"
        @scroll="handleScroll"
      >
        <!-- 历史消息加载提示 -->
        <div v-if="hasMoreHistory || historyLoading" class="chat-history-loading">
          <template v-if="historyLoading">
            <LoadingOutlined style="margin-right: 6px; font-size: 14px" />
            <span>正在加载</span>
          </template>
          <template v-else-if="hasMoreHistory">
            <span>下拉加载更多历史消息</span>
          </template>
        </div>
        <MessageList
          :agent-has-result="agentHasResult"
          :messages="messages"
          :tool-calls="toolCalls"
          @inputTagPreview="inputTagPreviewHandle"
          @toolContent="(content: any) => $emit('toolContent', content)"
        />
      </div>
      <div class="chat-main-input-wrap">
        <div class="chat-input-outer">
          <ChatInput
            :model-value="inputValue"
            :agent-id="agentId"
            :uploaded-files="uploadedFiles"
            :isRunning="isRunning"
            :memory-active="memoryActive"
            :plan-active="planActive"
            :enable-memory="enableMemory"
            :enable-planning="enablePlanning"
            :allow-upload-file-type="allowUploadFileType"
            :show-tool-process="showToolProcess"
            :tool-process-active="toolProcessActive"
            :session-id="sessionId"
            :mention-allowed="true"
            @inputTagPreview="inputTagPreviewHandle"
            @update:model-value="$emit('update:inputValue', $event)"
            @update:uploaded-files="$emit('update:uploadedFiles', $event)"
            @memory="$emit('memory', $event)"
            @plan="$emit('plan', $event)"
            @toolProcess="$emit('toolProcess', $event)"
            @send="handleSend"
            @abort="$emit('abort')"
          />
          <div class="text-placeholder text-xs mt-sm" style="text-align: center; margin: 5px 0;">内容由AI生成，仅供参考</div>
        </div>
      </div>
    </template>
    <!-- 输入Tag文件预览弹窗 -->
    <WorkspaceFilePreview
      v-model:visible="workspaceFilePreviewVisible"
      :file-node="workspaceFilePreviewNode"
      :session-id="sessionId as string"
    />
  </main>
</template>

<style scoped lang="scss">
@use '@/styles/chat/index.scss' as *;

/* 历史消息加载提示 */
.chat-history-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12px 0;
  color: #999;
  font-size: 13px;
}
</style>
