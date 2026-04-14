<!-- src/components/chat/MarkdownRenderer.vue -->
<template>
  <div ref="container" class="markdown-renderer">
    <template v-for="(part, index) in parts" :key="index">
      <div v-if="part.type === 'html'" v-html="part.content"></div>
      <MermaidRenderer v-else-if="part.type === 'mermaid'" :code="part.code as string" />
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, nextTick } from 'vue'
import { renderMarkdown } from '@/utils/chat/markdown.ts'
import MermaidRenderer from '@/components/markdown/MermaidRenderer.vue'

const props = defineProps<{
  content: string
}>()

const container = ref<HTMLElement>()
const parts = ref<Array<{ type: 'html' | 'mermaid'; content?: string; code?: string }>>([])

// 提取 mermaid 代码块
const extractMermaidBlocks = (html: string) => {
  const blocks: Array<{ placeholder: string; code: string; fullMatch: string }> = []

  // 匹配 mermaid 块的正则表达式
  const mermaidRegex = /<(pre|div)\s+class="mermaid"[^>]*>([\s\S]*?)<\/\1>/gi

  let match: RegExpExecArray | null
  let blockIndex = 0

  while ((match = mermaidRegex.exec(html)) !== null) {
    // 现在 TypeScript 知道 match 不是 null
    const fullMatch = match[0]
    const code = match[2]?.trim() || ''
    const placeholder = `__MERMAID_PLACEHOLDER_${blockIndex}__`

    blocks.push({
      placeholder,
      code,
      fullMatch
    })

    blockIndex++
  }

  return blocks
}

const parseContent = () => {
  if (!props.content) {
    parts.value = []
    return
  }

  // 先渲染为 HTML
  const html = renderMarkdown(props.content)

  // 提取 mermaid 块
  const mermaidBlocks = extractMermaidBlocks(html)

  console.log('mermaidBlocks===>', mermaidBlocks)

  if (mermaidBlocks.length === 0) {
    // 没有 mermaid 块，直接使用 HTML
    parts.value = [{ type: 'html', content: html }]
    return
  }

  // 替换 mermaid 占位符，分割 HTML 和 mermaid 组件
  let currentHtml = html
  const newParts: Array<{ type: 'html' | 'mermaid'; content?: string; code?: string }> = []

  for (const block of mermaidBlocks) {
    const index = currentHtml.indexOf(block.fullMatch)
    if (index === -1) continue

    if (index > 0) {
      // 添加占位符前的 HTML
      newParts.push({
        type: 'html',
        content: currentHtml.substring(0, index)
      })
    }

    // 添加 mermaid 组件
    newParts.push({
      type: 'mermaid',
      code: block.code
    })

    // 继续处理剩余部分
    currentHtml = currentHtml.substring(index + block.fullMatch.length)
  }

  // 添加剩余的 HTML
  if (currentHtml && currentHtml.trim()) {
    newParts.push({ type: 'html', content: currentHtml })
  }

  parts.value = newParts
}

onMounted(() => {
  parseContent()
})

watch(() => props.content, () => {
  nextTick(() => parseContent())
})
</script>

<style scoped>
.markdown-renderer {
  width: 100%;
}
</style>
