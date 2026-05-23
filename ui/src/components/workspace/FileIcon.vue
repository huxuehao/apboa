<template>
  <svg
    :width="svgWidth"
    :height="svgHeight"
    viewBox="0 0 90 110"
    xmlns="http://www.w3.org/2000/svg"
    class="file-icon-svg"
    :aria-label="`${displayExt || '文件'} 图标`"
  >
    <!-- 主文件体 -->
    <rect
      x="5"
      y="8"
      width="80"
      height="100"
      rx="12"
      ry="12"
      :fill="iconColor"
    />

    <!-- 装饰横条 -->
    <rect
      x="18"
      y="25"
      width="40"
      height="5"
      rx="1.5"
      fill="#ffffff55"
    />
    <rect
      x="18"
      y="35"
      width="25"
      height="5"
      rx="1.5"
      fill="#ffffff44"
    />

    <!-- 文件扩展名文字 -->
    <text
      v-if="displayExt"
      x="45"
      y="90"
      font-family="AlimamaFangYuan, 'Segoe UI', Arial, 'PingFang SC', 'Microsoft YaHei', sans-serif"
      :font-size="fontSize"
      font-weight="bold"
      :fill="textColor"
      text-anchor="middle"
    >
      {{ displayExt }}
    </text>
  </svg>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps({
  /** 文件名（如 "报告.docx"、"script.js"、"README"） */
  fileName: {
    type: String,
    required: true,
  },
  /** SVG 宽度，默认 50（可传数字或带单位的字符串，如 50、"50px"） */
  width: {
    type: [Number, String],
    default: 50,
  },
  /** SVG 高度，默认按 viewBox 比例自动计算；传入后覆盖自动计算 */
  height: {
    type: [Number, String],
    default: undefined,
  },
  /** 文字颜色，默认白色 */
  textColor: {
    type: String,
    default: '#ffffff',
  },
})

const iconConfigs = [
  { ext: 'xls', color: '#289B4F' },
  { ext: 'xlsx', color: '#289B4F' },
  { ext: 'md', color: '#2A8BB5' },
  { ext: 'doc', color: '#3A7FD9' },
  { ext: 'docx', color: '#3A7FD9' },
  { ext: 'ppt', color: '#E5552A' },
  { ext: 'pptx', color: '#E5552A' },
  { ext: 'txt', color: '#6B6B6B' },
  { ext: 'js', color: '#E68A2E' },
  { ext: 'ts', color: '#3178C6' },
  { ext: 'py', color: '#3A91C2' },
  { ext: 'sh', color: '#E54C59' },
  { ext: 'yaml', color: '#F5A623' },
  { ext: 'yml', color: '#F5A623' },
  { ext: 'json', color: '#5B8FF9' },
  { ext: 'java', color: '#B07219' },
  { ext: 'go', color: '#00ADD8' },
  { ext: 'rs', color: '#DEA584' },
  { ext: 'sql', color: '#4479A1' },
  { ext: 'css', color: '#1572B6' },
  { ext: 'html', color: '#E34F26' },
  { ext: 'xml', color: '#0066CC' },
  { ext: 'toml', color: '#9C4221' },
  { ext: 'env', color: '#ECD53F' },
]

/** 匹配不到后缀时的默认颜色 */
const DEFAULT_COLOR = '#9CA3AF'

/**
 * 从文件名提取后缀（小写），无有效后缀返回空字符串
 * 处理规则：
 * - "报告.docx" → "docx"
 * - "script.js" → "js"
 * - "README.md" → "md"
 * - ".gitignore" → "gitignore"（以点开头的隐藏文件，点不算后缀的一部分）
 * - "Makefile" → ""（无后缀）
 * - "archive.tar.gz" → "gz"（取最后一个点之后的部分）
 * - "test." → ""（点后有内容才算后缀）
 */
const rawExt = computed(() => {
  const name = (props.fileName || '').trim()
  if (!name) return ''

  const lastDotIndex = name.lastIndexOf('.')

  // 没有点，或者点在开头（隐藏文件如 .gitignore），或者点在末尾
  if (lastDotIndex <= 0 || name.slice(lastDotIndex + 1).length > 4) {
    return '?'
  }

  // 正常情况：取最后一个点之后的内容
  return name.slice(lastDotIndex + 1).toLowerCase()
})

/** 匹配到的图标配置，匹配不到则为 null */
const matchedConfig = computed(() => {
  if (!rawExt.value) return null
  const found = iconConfigs.find((cfg) => cfg.ext === rawExt.value)
  return found || null
})

/** 最终使用的图标主色 */
const iconColor = computed(() => {
  return matchedConfig.value?.color || DEFAULT_COLOR
})

/** 最终显示的扩展名文本（大写），匹配不到时使用后缀名本身 */
const displayExt = computed(() => {
  if (!rawExt.value) return ''
  // 匹配到时使用配置中的 ext，匹配不到时使用原始后缀名
  const extStr = matchedConfig.value?.ext || rawExt.value
  return extStr.toUpperCase()
})

/** 根据扩展名长度动态调整字体大小 */
const fontSize = computed(() => {
  const len = displayExt.value.length
  if (len === 1) return '40'
  if (len === 2) return '35'
  if (len === 3) return '30'
  if (len === 4) return '25'
  return '17'
})

/** 解析宽度数值（去除单位） */
const numericWidth = computed(() => {
  const parsed = parseFloat(props.width + '')
  return Number.isNaN(parsed) ? 50 : parsed
})

/** 计算 SVG 高度：未传则按 viewBox 比例自动计算 */
const svgHeight = computed(() => {
  if (props.height !== undefined) {
    const parsed = parseFloat(props.height + '')
    if (!Number.isNaN(parsed)) return props.height
  }
  // 自动按比例：viewBox 比例 110:135
  return Math.round(numericWidth.value * (135 / 110))
})

/** SVG 宽度（直接透传，保留单位字符串或数字） */
const svgWidth = computed(() => {
  return props.width
})
</script>

<style scoped>
.file-icon-svg {
  display: inline-block;
  vertical-align: middle; /* 关键：与文字垂直居中 */
  flex-shrink: 0;
  position: relative;
  top: -0.07em; /* 微调，让图标视觉上更居中 */
}
</style>
