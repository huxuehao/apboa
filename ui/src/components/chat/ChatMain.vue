<script setup lang="ts">
import {ref, nextTick, watch, onMounted} from 'vue'
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
  isWelcomeMode: boolean
  welcomeHeadline: string
  welcomeDesc?: string
  messages: DisplayMessage[]
  toolCalls: any[]
  inputValue: string
  uploadedFiles?: UploadedFileItem[]
  isRunning: boolean
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
  (e: 'scroll', event: UIEvent): void
  (e: 'toolContent', value: any): void
  (e: 'abort'): void
  (e: 'memory', value: boolean): void
  (e: 'plan', value: boolean): void
  (e: 'toolProcess', value: boolean): void
  (e: 'toggleSidebar'): void
  (e: 'toggleWorkspace'): void
  /** 触发加载更多历史消息 */
  (e: 'loadMoreHistory'): void
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
  nextTick(() => {
    const el = messagesScrollRef.value
    if (!el) return
    const heightDiff = el.scrollHeight - savedScrollHeight.value
    el.scrollTop = savedScrollTop.value + heightDiff
  })
}

const handleSend = () => {
  emit('send')
  // 发送后强制自动滚动到底部，带丝滑动画
  shouldAutoScroll.value = true
  scrollToBottom(true)
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
          v-if="!isWelcomeMode && hasCodeExecutionConfig"
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

    <div v-if="isWelcomeMode" class="chat-welcome-container">
      <Welcome
        :headline="welcomeHeadline"
        :description="welcomeDesc"
        :input-value="inputValue"
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
        :workspace-panel-open="workspacePanelOpen"
        @update:input-value="$emit('update:inputValue', $event)"
        @update:uploaded-files="$emit('update:uploadedFiles', $event)"
        @memory="$emit('memory', $event)"
        @plan="$emit('plan', $event)"
        @toolProcess="$emit('toolProcess', $event)"
        @send="handleSend"
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
            :workspace-panel-open="workspacePanelOpen"
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
