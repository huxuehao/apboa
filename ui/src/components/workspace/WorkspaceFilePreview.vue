<script setup lang="ts">
import { ref, computed, watch, onUnmounted } from 'vue'
import { Modal, Spin } from 'ant-design-vue'
import {
  FileImageOutlined,
  FileTextOutlined,
  PlayCircleOutlined,
  DownloadOutlined,
  CloseOutlined,
  ZoomInOutlined,
  ZoomOutOutlined,
  RotateLeftOutlined,
  RotateRightOutlined
} from '@ant-design/icons-vue'
import * as workspaceApi from '@/api/workspace'
import type { WorkspaceFileNode } from '@/types'

/**
 * 媒体类型
 */
type MediaType = 'image' | 'audio' | 'video' | 'text' | 'other'

/**
 * 组件属性定义
 */
const props = defineProps<{
  /** 预览是否可见 */
  visible: boolean
  /** 当前预览的文件节点 */
  fileNode: WorkspaceFileNode | null
  /** 当前会话 ID */
  sessionId: string | null
}>()

/**
 * 组件事件定义
 */
const emit = defineEmits<{
  'update:visible': [value: boolean]
}>()

/** 加载状态 */
const loading = ref(false)
/** 媒体文件 URL（图片/音频/视频） */
const mediaUrl = ref('')
/** 文本文件内容 */
const textContent = ref('')

// 图片变换状态
const scale = ref(1)
const rotate = ref(0)
const translateX = ref(0)
const translateY = ref(0)
const isDragging = ref(false)
const dragStartX = ref(0)
const dragStartY = ref(0)
const dragStartTranslateX = ref(0)
const dragStartTranslateY = ref(0)

// 元素引用
const imageRef = ref<HTMLImageElement | null>(null)
const imageWrapperRef = ref<HTMLDivElement | null>(null)

/** 文件后缀（不含点号） */
const extension = computed(() => props.fileNode?.extension?.toLowerCase() ?? '')

/** 文件名称 */
const fileName = computed(() => props.fileNode?.fullName ?? props.fileNode?.name ?? '')

/**
 * 指定扩展名的媒体类型判断
 * @param ext 文件后缀（小写）
 */
function resolveMediaType(ext: string): MediaType {
  const imageExts = ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'svg', 'ico']
  if (imageExts.includes(ext)) return 'image'

  const audioExts = ['mp3', 'wav', 'ogg', 'm4a', 'flac', 'aac', 'wma', 'mpeg']
  if (audioExts.includes(ext)) return 'audio'

  const videoExts = ['mp4', 'webm', 'mov', 'mkv', 'avi', 'flv', 'm3u8', 'mpeg']
  if (videoExts.includes(ext)) return 'video'

  const textExts = [
    'txt', 'csv', 'log', 'md', 'markdown', 'json', 'xml', 'yaml', 'yml', 'toml',
    'ini', 'cfg', 'conf', 'properties', 'env', 'editorconfig',
    'java', 'py', 'js', 'ts', 'jsx', 'tsx', 'go', 'rs', 'c', 'cpp', 'h', 'hpp',
    'cs', 'rb', 'php', 'swift', 'kt', 'scala', 'clj', 'lua', 'r', 'sql', 'sh',
    'bat', 'ps1', 'psm1', 'graphql', 'gql', 'proto', 'dart', 'pl',
    'html', 'htm', 'css', 'scss', 'less', 'sass', 'vue', 'svelte', 'astro',
    'gitignore', 'dockerfile', 'makefile', 'cmake', 'gradle', 'maven',
    'tex', 'rst', 'asciidoc'
  ]
  if (textExts.includes(ext)) return 'text'

  return 'other'
}

const mediaType = computed(() => resolveMediaType(extension.value))
const isImage = computed(() => mediaType.value === 'image')
const isAudio = computed(() => mediaType.value === 'audio')
const isVideo = computed(() => mediaType.value === 'video')
const isText = computed(() => mediaType.value === 'text')
const isOther = computed(() => mediaType.value === 'other')

/**
 * 加载文件内容
 */
async function loadFile() {
  if (!props.fileNode || !props.sessionId) return

  // 不支持预览的类型直接显示下载按钮，无需加载
  if (isOther.value) return

  loading.value = true
  try {
    const fname = props.fileNode.fullName ?? props.fileNode.name
    const res = await workspaceApi.download(props.sessionId, fname)
    const blob = res.data as Blob

    if (isImage.value || isAudio.value || isVideo.value) {
      mediaUrl.value = URL.createObjectURL(blob)
    } else if (isText.value) {
      textContent.value = await blob.text()
    }
  } catch (error) {
    console.error('加载文件失败:', error)
  } finally {
    loading.value = false
  }
}

/** 监听可见性变化，触发加载或清理 */
watch(() => props.visible, (val) => {
  if (val) {
    resetImageTransform()
    loadFile()
  } else {
    cleanup()
  }
})

/**
 * 清理资源
 */
function cleanup() {
  if (mediaUrl.value && mediaUrl.value.startsWith('blob:')) {
    URL.revokeObjectURL(mediaUrl.value)
    mediaUrl.value = ''
  }
  textContent.value = ''
}

/**
 * 重置图片变换参数
 */
function resetImageTransform() {
  scale.value = 1
  rotate.value = 0
  translateX.value = 0
  translateY.value = 0
}

/**
 * 关闭预览
 */
function handleClose() {
  emit('update:visible', false)
}

/**
 * 下载当前文件
 */
async function handleDownload() {
  if (!props.fileNode || !props.sessionId) return
  try {
    const fname = props.fileNode.fullName ?? props.fileNode.name
    const res = await workspaceApi.download(props.sessionId, fname)
    const blob = res.data as Blob
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = fname
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
  } catch (error) {
    console.error('下载失败:', error)
  }
}

// 图片操作

/** 放大 */
function handleZoomIn() {
  scale.value = Math.min(scale.value + 0.25, 3)
}

/** 缩小 */
function handleZoomOut() {
  scale.value = Math.max(scale.value - 0.25, 0.5)
}

/** 滚轮缩放 */
function handleWheel(e: WheelEvent) {
  if (!isImage.value) return
  e.preventDefault()

  const rect = imageWrapperRef.value?.getBoundingClientRect()
  if (!rect) return

  const mouseX = e.clientX - rect.left
  const mouseY = e.clientY - rect.top
  const centerX = rect.width / 2
  const centerY = rect.height / 2
  const oldScale = scale.value
  const oldTranslateX = translateX.value
  const oldTranslateY = translateY.value

  const mouseOnImageX = (mouseX - centerX - oldTranslateX) / oldScale
  const mouseOnImageY = (mouseY - centerY - oldTranslateY) / oldScale

  const delta = -e.deltaY / 500
  let newScale = oldScale + delta
  newScale = Math.min(Math.max(newScale, 0.5), 3)

  if (newScale === oldScale) return

  const newTranslateX = mouseX - centerX - mouseOnImageX * newScale
  const newTranslateY = mouseY - centerY - mouseOnImageY * newScale

  scale.value = newScale
  translateX.value = newTranslateX
  translateY.value = newTranslateY
}

/** 向左旋转 */
function handleRotateLeft() {
  rotate.value -= 90
  translateX.value = 0
  translateY.value = 0
}

/** 向右旋转 */
function handleRotateRight() {
  rotate.value += 90
  translateX.value = 0
  translateY.value = 0
}

/** 图片拖拽 - 按下 */
function onImageMouseDown(e: MouseEvent) {
  e.preventDefault()
  e.stopPropagation()
  isDragging.value = true
  dragStartX.value = e.clientX
  dragStartY.value = e.clientY
  dragStartTranslateX.value = translateX.value
  dragStartTranslateY.value = translateY.value
}

/** 图片拖拽 - 移动 */
function onImageMouseMove(e: MouseEvent) {
  if (!isDragging.value) return
  e.preventDefault()
  const deltaX = e.clientX - dragStartX.value
  const deltaY = e.clientY - dragStartY.value
  translateX.value = dragStartTranslateX.value + deltaX
  translateY.value = dragStartTranslateY.value + deltaY
}

/** 图片拖拽 - 松开 */
function onImageMouseUp() {
  isDragging.value = false
}

// 全局拖拽事件绑定
watch(isDragging, (dragging) => {
  if (dragging) {
    window.addEventListener('mousemove', onImageMouseMove)
    window.addEventListener('mouseup', onImageMouseUp)
  } else {
    window.removeEventListener('mousemove', onImageMouseMove)
    window.removeEventListener('mouseup', onImageMouseUp)
  }
})

/**
 * 根据后缀获取文件图标组件
 */
function getFileIcon(ext: string) {
  const imageExts = ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'svg', 'ico']
  const videoExts = ['mp4', 'webm', 'ogg', 'mov', 'mkv', 'avi', 'flv', 'm3u8']

  if (imageExts.includes(ext.toLowerCase())) return FileImageOutlined
  if (videoExts.includes(ext.toLowerCase())) return PlayCircleOutlined
  return FileTextOutlined
}

// 清理
onUnmounted(() => {
  window.removeEventListener('mousemove', onImageMouseMove)
  window.removeEventListener('mouseup', onImageMouseUp)
  cleanup()
})
</script>

<template>
  <Modal
    :open="visible"
    :footer="null"
    :closable="false"
    :mask-closable="true"
    wrap-class-name="full-modal workspace-preview-modal"
    @cancel="handleClose"
  >
    <div class="workspace-preview-container">
      <!-- 顶部工具栏 -->
      <div class="workspace-preview-header">
        <div class="workspace-preview-title">
          <component
            :is="getFileIcon(extension)"
            class="workspace-preview-icon"
          />
          <span class="workspace-preview-filename" :title="fileName">
            {{ fileName }}
          </span>
        </div>
        <div class="workspace-preview-actions">
          <!-- 图片操作按钮 -->
          <template v-if="isImage">
            <button class="workspace-preview-btn" @click="handleZoomOut" title="缩小">
              <ZoomOutOutlined />
            </button>
            <button class="workspace-preview-btn" @click="handleZoomIn" title="放大">
              <ZoomInOutlined />
            </button>
            <button class="workspace-preview-btn" @click="handleRotateLeft" title="向左旋转">
              <RotateLeftOutlined />
            </button>
            <button class="workspace-preview-btn" @click="handleRotateRight" title="向右旋转">
              <RotateRightOutlined />
            </button>
          </template>
          <button class="workspace-preview-btn" @click="handleDownload" title="下载">
            <DownloadOutlined />
          </button>
          <button
            class="workspace-preview-btn workspace-preview-btn-close"
            @click="handleClose"
            title="关闭"
          >
            <CloseOutlined />
          </button>
        </div>
      </div>

      <!-- 文件内容区域 -->
      <div class="workspace-preview-content">
        <!-- 加载中 -->
        <div v-if="loading" class="workspace-preview-loading">
          <Spin size="large" />
        </div>

        <!-- 图片预览 -->
        <div
          v-else-if="isImage && mediaUrl"
          ref="imageWrapperRef"
          class="workspace-preview-image-wrapper"
          @click.self="handleClose"
          @wheel="handleWheel"
        >
          <img
            ref="imageRef"
            :src="mediaUrl"
            :alt="fileName"
            class="workspace-preview-image"
            :style="{
              transform: `translate(${translateX}px, ${translateY}px) scale(${scale}) rotate(${rotate}deg)`,
              transition: isDragging ? 'none' : 'transform 0.2s cubic-bezier(0.25, 0.46, 0.45, 0.94)',
              cursor: isDragging ? 'grabbing' : 'grab'
            }"
            @mousedown="onImageMouseDown"
            @dragstart.prevent
            @click.stop
          />
        </div>

        <!-- 音频预览 -->
        <div v-else-if="isAudio && mediaUrl" class="workspace-preview-audio-wrapper">
          <audio
            :src="mediaUrl"
            controls
            class="workspace-preview-audio"
            @click.stop
          >
            您的浏览器不支持音频播放
          </audio>
          <div class="workspace-preview-audio-info">
            <FileTextOutlined class="workspace-preview-audio-icon" />
            <span>{{ fileName }}</span>
          </div>
        </div>

        <!-- 视频预览 -->
        <div v-else-if="isVideo && mediaUrl" class="workspace-preview-video-wrapper">
          <video
            :src="mediaUrl"
            controls
            class="workspace-preview-video"
            @click.stop
          >
            您的浏览器不支持视频播放
          </video>
        </div>

        <!-- 文本预览 -->
        <div v-else-if="isText" class="workspace-preview-text-wrapper">
          <pre class="workspace-preview-text-content">{{ textContent }}</pre>
        </div>

        <!-- 不支持预览的文件类型 -->
        <div v-else class="workspace-preview-unsupported">
          <FileTextOutlined class="workspace-preview-unsupported-icon" />
          <p class="workspace-preview-unsupported-text">该文件类型暂不支持预览</p>
          <button class="workspace-preview-download-btn" @click="handleDownload">
            <DownloadOutlined />
            下载文件
          </button>
        </div>
      </div>
    </div>
  </Modal>
</template>

<style scoped lang="scss">
// 覆盖 Modal 内容区域内边距
:deep(.ant-modal .ant-modal-content) {
  padding: 0 !important;
  background-color: transparent !important;
}

:global(.workspace-preview-modal .ant-modal-content) {
  padding: 0 !important;
  background-color: transparent !important;
}

.workspace-preview-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: rgba(0, 0, 0, 0.85);
  border-radius: 0;
  overflow: hidden;
}

// 顶部工具栏
.workspace-preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: rgb(0, 0, 0);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  flex-shrink: 0;
}

.workspace-preview-title {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.workspace-preview-icon {
  font-size: 20px;
  color: #1890ff;
  flex-shrink: 0;
}

.workspace-preview-filename {
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.workspace-preview-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: 16px;
}

.workspace-preview-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    background: rgba(255, 255, 255, 0.2);
  }

  &:active {
    transform: scale(0.95);
  }

  svg {
    font-size: 16px;
  }
}

.workspace-preview-btn-close {
  background: rgba(255, 77, 79, 0.2);

  &:hover {
    background: rgba(255, 77, 79, 0.4);
  }
}

// 内容区域
.workspace-preview-content {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  min-height: 0;
}

.workspace-preview-loading {
  display: flex;
  align-items: center;
  justify-content: center;
}

// 图片预览
.workspace-preview-image-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  cursor: zoom-out;
  overflow: hidden;
}

.workspace-preview-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  user-select: none;
  -webkit-user-drag: none;
  will-change: transform;

  &:active {
    cursor: grabbing;
  }
}

// 音频预览
.workspace-preview-audio-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24px;
}

.workspace-preview-audio {
  width: 400px;
  max-width: 100%;
}

.workspace-preview-audio-info {
  display: flex;
  align-items: center;
  gap: 12px;
  color: rgba(255, 255, 255, 0.8);
  font-size: 14px;
}

.workspace-preview-audio-icon {
  font-size: 48px;
  color: #1890ff;
}

// 视频预览
.workspace-preview-video-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
}

.workspace-preview-video {
  max-width: 100%;
  max-height: 100%;
}

// 文本预览
.workspace-preview-text-wrapper {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  overflow: hidden;
}
.workspace-preview-text-content {
  flex: 1;
  margin: 0;
  padding: 16px;
  overflow: auto;
  color: #FFFFFF;
  font-weight: 350;
  font-size: 16px;
  font-family: 'AlimamaFangYuan',serif;
  line-height: 1.8;
  letter-spacing: 1px;
  border-radius: 0 0 8px 8px;
  white-space: pre;
  tab-size: 4;
  -moz-tab-size: 4;

  &::-webkit-scrollbar {
    width: 8px;
    height: 8px;
  }

  &::-webkit-scrollbar-track {
    background: transparent;
  }

  &::-webkit-scrollbar-thumb {
    background: rgba(255, 255, 255, 0.15);
    border-radius: 4px;

    &:hover {
      background: rgba(255, 255, 255, 0.25);
    }
  }
}

// 不支持预览
.workspace-preview-unsupported {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.workspace-preview-unsupported-icon {
  font-size: 64px;
  color: rgba(255, 255, 255, 0.3);
}

.workspace-preview-unsupported-text {
  color: rgba(255, 255, 255, 0.6);
  font-size: 14px;
  margin: 0;
}

.workspace-preview-download-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 24px;
  border: none;
  border-radius: 6px;
  background: #1890ff;
  color: #fff;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    background: #40a9ff;
  }
}
</style>
