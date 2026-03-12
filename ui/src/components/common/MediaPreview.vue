<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { Modal, Spin } from 'ant-design-vue'
import {
  FileImageOutlined,
  FileTextOutlined,
  PlayCircleOutlined,
  DownloadOutlined,
  CloseOutlined,
  LeftOutlined,
  RightOutlined,
  ZoomInOutlined,
  ZoomOutOutlined,
  RotateLeftOutlined,
  RotateRightOutlined
} from '@ant-design/icons-vue'
import { download } from '@/api/attach'

/**
 * 媒体类型
 */
type MediaType = 'image' | 'audio' | 'video' | 'other'

/**
 * 媒体预览项
 */
interface MediaItem {
  id: string
  name: string
  extension: string
  size?: string
}

/**
 * 组件属性定义
 */
const props = defineProps<{
  visible: boolean
  items: MediaItem[]
  currentIndex?: number
}>()

/**
 * 组件事件定义
 */
const emit = defineEmits<{
  'update:visible': [value: boolean]
  'close': []
}>()

// 当前索引
const currentIdx = ref(props.currentIndex || 0)
// 加载状态
const loading = ref(false)
// 媒体URL
const mediaUrl = ref('')
// 图片缩放比例
const scale = ref(1)
// 图片旋转角度
const rotate = ref(0)

// 监听索引变化
watch(() => props.currentIndex, (val) => {
  if (val !== undefined) {
    currentIdx.value = val
  }
})

// 监听当前索引变化，加载媒体
watch(currentIdx, async () => {
  await loadMedia()
}, { immediate: true })

// 监听可见性变化
watch(() => props.visible, (val) => {
  if (val) {
    scale.value = 1
    rotate.value = 0
    loadMedia()
  } else {
    // 清理URL对象
    if (mediaUrl.value && mediaUrl.value.startsWith('blob:')) {
      URL.revokeObjectURL(mediaUrl.value)
      mediaUrl.value = ''
    }
  }
})

// 当前媒体项
const currentItem = computed(() => props.items[currentIdx.value])

// 媒体类型判断
const mediaType = computed((): MediaType => {
  if (!currentItem.value) return 'other'
  const ext = currentItem.value.extension.toLowerCase()

  // 图片格式
  const imageExts = ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'svg', 'ico']
  if (imageExts.includes(ext)) return 'image'

  // 音频格式
  const audioExts = ['mp3', 'wav', 'ogg', 'm4a', 'flac', 'aac', 'wma', 'mpeg']
  if (audioExts.includes(ext)) return 'audio'

  // 视频格式
  const videoExts = ['mp4', 'webm', 'mov', 'mkv', 'avi', 'flv', 'm3u8', 'mpeg']
  if (videoExts.includes(ext)) return 'video'

  return 'other'
})

// 是否为图片
const isImage = computed(() => mediaType.value === 'image')
// 是否为音频
const isAudio = computed(() => mediaType.value === 'audio')
// 是否为视频
const isVideo = computed(() => mediaType.value === 'video')

// 是否有上一项
const hasPrev = computed(() => currentIdx.value > 0)
// 是否有下一项
const hasNext = computed(() => currentIdx.value < props.items.length - 1)

/**
 * 加载媒体文件
 */
async function loadMedia() {
  if (!currentItem.value) return

  loading.value = true
  try {
    const res = await download(currentItem.value.id)
    const blob = new Blob([res.data])
    mediaUrl.value = URL.createObjectURL(blob)
  } catch (error) {
    console.error('加载媒体失败:', error)
  } finally {
    loading.value = false
  }
}

/**
 * 关闭预览
 */
function handleClose() {
  emit('update:visible', false)
  emit('close')
}

/**
 * 切换到上一项
 */
function handlePrev() {
  if (hasPrev.value) {
    currentIdx.value--
    scale.value = 1
    rotate.value = 0
  }
}

/**
 * 切换到下一项
 */
function handleNext() {
  if (hasNext.value) {
    currentIdx.value++
    scale.value = 1
    rotate.value = 0
  }
}

/**
 * 下载当前文件
 */
async function handleDownload() {
  if (!currentItem.value) return
  try {
    const res = await download(currentItem.value.id)
    const blob = new Blob([res.data])
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = currentItem.value.name
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
  } catch (error) {
    console.error('下载失败:', error)
  }
}

/**
 * 放大图片
 */
function handleZoomIn() {
  scale.value = Math.min(scale.value + 0.25, 3)
}

/**
 * 缩小图片
 */
function handleZoomOut() {
  scale.value = Math.max(scale.value - 0.25, 0.5)
}

/**
 * 向左旋转
 */
function handleRotateLeft() {
  rotate.value -= 90
}

/**
 * 向右旋转
 */
function handleRotateRight() {
  rotate.value += 90
}

/**
 * 获取文件图标
 */
function getFileIcon(ext: string) {
  const imageExts = ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'svg', 'ico']
  const videoExts = ['mp4', 'webm', 'ogg', 'mov', 'mkv', 'avi', 'flv', 'm3u8']

  if (imageExts.includes(ext.toLowerCase())) return FileImageOutlined
  if (videoExts.includes(ext.toLowerCase())) return PlayCircleOutlined
  return FileTextOutlined
}
</script>

<template>
  <Modal
    :open="visible"
    :footer="null"
    :closable="false"
    :mask-closable="true"
    :mask="false"
    wrap-class-name="full-modal media-preview-modal"
    @cancel="handleClose"
  >
    <div class="media-preview-container">
      <!-- 顶部工具栏 -->
      <div class="media-preview-header">
        <div class="media-preview-title">
          <component :is="getFileIcon(currentItem?.extension || '')" class="media-preview-icon" />
          <span class="media-preview-filename" :title="currentItem?.name">
            {{ currentItem?.name }}
          </span>
          <span v-if="items.length > 1" class="media-preview-counter">
            {{ currentIdx + 1 }} / {{ items.length }}
          </span>
        </div>
        <div class="media-preview-actions">
          <!-- 图片操作按钮 -->
          <template v-if="isImage">
            <button class="media-preview-btn" @click="handleZoomOut" title="缩小">
              <ZoomOutOutlined />
            </button>
            <button class="media-preview-btn" @click="handleZoomIn" title="放大">
              <ZoomInOutlined />
            </button>
            <button class="media-preview-btn" @click="handleRotateLeft" title="向左旋转">
              <RotateLeftOutlined />
            </button>
            <button class="media-preview-btn" @click="handleRotateRight" title="向右旋转">
              <RotateRightOutlined />
            </button>
          </template>
          <button class="media-preview-btn" @click="handleDownload" title="下载">
            <DownloadOutlined />
          </button>
          <button class="media-preview-btn media-preview-btn-close" @click="handleClose" title="关闭">
            <CloseOutlined />
          </button>
        </div>
      </div>

      <!-- 媒体内容区域 -->
      <div class="media-preview-content">
        <!-- 加载中 -->
        <div v-if="loading" class="media-preview-loading">
          <Spin size="large" />
        </div>

        <!-- 图片预览 -->
        <div
          v-else-if="isImage && mediaUrl"
          class="media-preview-image-wrapper"
          @click.self="handleClose"
        >
          <img
            :src="mediaUrl"
            :alt="currentItem?.name"
            class="media-preview-image"
            :style="{
              transform: `scale(${scale}) rotate(${rotate}deg)`,
              transition: 'transform 0.3s ease'
            }"
            @click.stop
          />
        </div>

        <!-- 音频预览 -->
        <div v-else-if="isAudio && mediaUrl" class="media-preview-audio-wrapper">
          <audio
            :src="mediaUrl"
            controls
            class="media-preview-audio"
            @click.stop
          >
            您的浏览器不支持音频播放
          </audio>
          <div class="media-preview-audio-info">
            <FileTextOutlined class="media-preview-audio-icon" />
            <span>{{ currentItem?.name }}</span>
          </div>
        </div>

        <!-- 视频预览 -->
        <div v-else-if="isVideo && mediaUrl" class="media-preview-video-wrapper">
          <video
            :src="mediaUrl"
            controls
            class="media-preview-video"
            @click.stop
          >
            您的浏览器不支持视频播放
          </video>
        </div>

        <!-- 不支持预览的文件类型 -->
        <div v-else class="media-preview-unsupported">
          <FileTextOutlined class="media-preview-unsupported-icon" />
          <p>该文件类型暂不支持预览</p>
          <button class="media-preview-download-btn" @click="handleDownload">
            <DownloadOutlined />
            下载文件
          </button>
        </div>

        <!-- 左右切换按钮 -->
        <button
          v-if="hasPrev"
          class="media-preview-nav media-preview-nav-prev"
          @click="handlePrev"
        >
          <LeftOutlined />
        </button>
        <button
          v-if="hasNext"
          class="media-preview-nav media-preview-nav-next"
          @click="handleNext"
        >
          <RightOutlined />
        </button>
      </div>

      <!-- 底部缩略图列表 -->
      <div v-if="items.length > 1" class="media-preview-thumbnails">
        <div
          v-for="(item, index) in items"
          :key="item.id"
          class="media-preview-thumbnail"
          :class="{ active: index === currentIdx }"
          @click="currentIdx = index"
        >
          <component :is="getFileIcon(item.extension)" class="media-preview-thumbnail-icon" />
          <span class="media-preview-thumbnail-name" :title="item.name">{{ item.name }}</span>
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

// 针对 wrap-class-name 的样式
:global(.media-preview-modal .ant-modal-content) {
  padding: 0 !important;
  background-color: transparent !important;
}

.media-preview-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: rgba(0, 0, 0, 0.85);
  border-radius: 0;
  overflow: hidden;
}

// 顶部工具栏
.media-preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: rgb(0, 0, 0);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.media-preview-title {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.media-preview-icon {
  font-size: 20px;
  color: #1890ff;
  flex-shrink: 0;
}

.media-preview-filename {
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.media-preview-counter {
  color: rgba(255, 255, 255, 0.6);
  font-size: 12px;
  flex-shrink: 0;
}

.media-preview-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: 16px;
}

.media-preview-btn {
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

.media-preview-btn-close {
  background: rgba(255, 77, 79, 0.2);

  &:hover {
    background: rgba(255, 77, 79, 0.4);
  }
}

// 内容区域
.media-preview-content {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  padding: 20px;
}

.media-preview-loading {
  display: flex;
  align-items: center;
  justify-content: center;
}

// 图片预览
.media-preview-image-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  cursor: zoom-out;
}

.media-preview-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  cursor: grab;

  &:active {
    cursor: grabbing;
  }
}

// 音频预览
.media-preview-audio-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24px;
}

.media-preview-audio {
  width: 400px;
  max-width: 100%;
}

.media-preview-audio-info {
  display: flex;
  align-items: center;
  gap: 12px;
  color: rgba(255, 255, 255, 0.8);
  font-size: 14px;
}

.media-preview-audio-icon {
  font-size: 48px;
  color: #1890ff;
}

// 视频预览
.media-preview-video-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
}

.media-preview-video {
  max-width: 100%;
  max-height: 100%;
  border-radius: 8px;
}

// 不支持预览
.media-preview-unsupported {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  color: rgba(255, 255, 255, 0.6);
}

.media-preview-unsupported-icon {
  font-size: 64px;
  color: rgba(255, 255, 255, 0.3);
}

.media-preview-download-btn {
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

// 导航按钮
.media-preview-nav {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border: none;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  font-size: 20px;
  cursor: pointer;
  transition: all 0.2s ease;
  z-index: 10;

  &:hover {
    background: rgba(0, 0, 0, 0.7);
  }

  &:active {
    transform: translateY(-50%) scale(0.95);
  }
}

.media-preview-nav-prev {
  left: 20px;
}

.media-preview-nav-next {
  right: 20px;
}

// 底部缩略图
.media-preview-thumbnails {
  display: flex;
  gap: 8px;
  padding: 12px 16px;
  background: rgba(0, 0, 0, 0.5);
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  overflow-x: auto;
}

.media-preview-thumbnail {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 8px 12px;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.05);
  cursor: pointer;
  transition: all 0.2s ease;
  min-width: 80px;
  max-width: 120px;

  &:hover {
    background: rgba(255, 255, 255, 0.1);
  }

  &.active {
    background: rgba(24, 144, 255, 0.2);
    border: 1px solid #1890ff;
  }
}

.media-preview-thumbnail-icon {
  font-size: 24px;
  color: rgba(255, 255, 255, 0.6);
}

.media-preview-thumbnail-name {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.6);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100%;
}

.media-preview-thumbnail.active .media-preview-thumbnail-icon,
.media-preview-thumbnail.active .media-preview-thumbnail-name {
  color: #1890ff;
}
</style>
