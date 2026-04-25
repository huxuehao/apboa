<template>
  <svg
    :width="size"
    :height="size"
    viewBox="0 0 24 24"
    :fill="iconColor"
  >
    <!-- 图片类型 -->
    <path
      v-if="isImage"
      d="M21 19V5c0-1.1-.9-2-2-2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2zM8.5 13.5l2.5 3.01L14.5 12l4.5 6H5l3.5-4.5z"
    />

    <!-- 音频类型 -->
    <path
      v-else-if="isAudio"
      d="M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6zm-2 16c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2z"
    />

    <!-- 视频类型 -->
    <path
      v-else-if="isVideo"
      d="M17 10.5V7c0-.55-.45-1-1-1H4c-.55 0-1 .45-1 1v10c0 .55.45 1 1 1h12c.55 0 1-.45 1-1v-3.5l4 4v-11l-4 4z"
    />

    <!-- 其他类型 -->
    <path
      v-else
      d="M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z"
    />
  </svg>
</template>

<script setup>
import { computed } from 'vue'

// 接收类型 + 大小
const props = defineProps({
  type: {
    type: String,
    required: true,
    default: ''
  },
  size: {
    type: [String, Number],
    default: 24 // 默认图标大小
  }
})

// 统一转小写判断
const mediaType = computed(() => props.type.trim().toLowerCase())

// 图片类型
const isImage = computed(() =>
  ['png', 'jpg', 'jpeg', 'gif', 'webp', 'bmp', 'tiff', 'tif', 'svg', 'ico', 'heic', 'heif', 'jfif'].includes(mediaType.value)
)

// 音频类型
const isAudio = computed(() =>
  ['mp3', 'wav', 'ogg', 'flac', 'aac', 'm4a', 'wma', 'amr', 'ape', 'opus'].includes(mediaType.value)
)

// 视频类型
const isVideo = computed(() =>
  ['mp4', 'mpeg', 'mpg', 'mov', 'avi', 'wmv', 'flv', 'webm', 'mkv', 'm4v', '3gp', 'ts'].includes(mediaType.value)
)

// 图标颜色（可自行修改）
const iconColor = computed(() => {
  if (isImage.value) return '#4285F4'
  if (isAudio.value) return '#34A853'
  if (isVideo.value) return '#EA4335'
  return '#999' // 未知类型颜色
})
</script>
