/**
 * RAG文档分块详情抽屉组件
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import {
  CopyOutlined,
  FileTextOutlined,
  NumberOutlined,
  ColumnWidthOutlined
} from '@ant-design/icons-vue'
import * as ragApi from '@/api/rag'
import type { RagDocumentChunk } from '@/types'

const props = defineProps<{
  open: boolean
  documentId: string
  documentName: string
}>()

const emit = defineEmits<{
  'update:open': [open: boolean]
}>()

const chunks = ref<RagDocumentChunk[]>([])
const loading = ref(false)
const expandedChunks = ref<Set<string>>(new Set())

/**
 * 监听抽屉打开，加载分块数据
 */
watch(() => props.open, (val) => {
  if (val && props.documentId) {
    loadChunks()
  }
})

/**
 * 加载分块列表
 */
async function loadChunks() {
  loading.value = true
  expandedChunks.value = new Set()
  try {
    const response = await ragApi.listChunks(props.documentId)
    chunks.value = response.data.data || []
  } finally {
    loading.value = false
  }
}

/**
 * 切换分块内容展开/折叠
 */
function toggleExpand(chunkId: string) {
  const newSet = new Set(expandedChunks.value)
  if (newSet.has(chunkId)) {
    newSet.delete(chunkId)
  } else {
    newSet.add(chunkId)
  }
  expandedChunks.value = newSet
}

/**
 * 判断内容是否需要展开（超过120字符）
 */
function needsExpand(content: string): boolean {
  return content.length > 120
}

/**
 * 复制分块内容
 */
function copyChunkContent(content: string) {
  navigator.clipboard.writeText(content).then(() => {
    message.success('已复制到剪贴板')
  }).catch(() => {
    message.error('复制失败')
  })
}

/**
 * 关闭抽屉
 */
function handleClose() {
  emit('update:open', false)
}
</script>

<template>
  <ADrawer
    :open="open"
    :title="`分块详情 - ${documentName}`"
    width="50%"
    placement="right"
    @close="handleClose"
  >
    <ASpin :spinning="loading">
      <div v-if="chunks.length === 0 && !loading" class="doc-empty">
        <FileTextOutlined class="doc-empty-icon" />
        <div class="doc-empty-text">暂无分块数据</div>
      </div>

      <div v-else class="chunk-list">
        <div
          v-for="chunk in chunks"
          :key="chunk.id"
          class="chunk-card"
        >
          <div class="chunk-card-header">
            <div class="chunk-card-header-left">
              <ATag color="blue" class="chunk-card-index" :bordered="false"># {{ chunk.chunkIndex }}</ATag>
              <span v-if="chunk.tokenCount" class="chunk-card-meta">
                <NumberOutlined />
                ~{{ chunk.tokenCount }} tokens
              </span>
              <span v-if="chunk.startOffset !== null && chunk.endOffset !== null" class="chunk-card-meta">
                <ColumnWidthOutlined />
                {{ chunk.startOffset }}-{{ chunk.endOffset }}
              </span>
            </div>
            <div class="chunk-card-header-right">
              <AButton type="text" size="small" @click="copyChunkContent(chunk.content)">
                <CopyOutlined />
              </AButton>
            </div>
          </div>

          <div
            class="chunk-card-content"
            :class="{ expanded: expandedChunks.has(chunk.id as string) }"
          >{{ chunk.content }}</div>

          <div
            v-if="needsExpand(chunk.content)"
            class="chunk-card-expand"
            @click="toggleExpand(chunk.id as string)"
          >
            {{ expandedChunks.has(chunk.id as string) ? '收起' : '展开全部' }}
          </div>
        </div>
      </div>
    </ASpin>
  </ADrawer>
</template>

<style scoped lang="scss">
@use '@/styles/rag/_doc-manager.scss' as *;
</style>
