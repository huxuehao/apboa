/**
 * RAG文档分块详情抽屉组件
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { ref, watch } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  CopyOutlined,
  FileTextOutlined,
  NumberOutlined,
  ColumnWidthOutlined,
  EditOutlined,
  DeleteOutlined
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

const editModalOpen = ref(false)
const editChunkContent = ref('')
const editChunkId = ref<string>('')
const editSaving = ref(false)

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
 * 打开编辑分块对话框
 */
function openEditChunk(chunk: RagDocumentChunk) {
  editChunkId.value = String(chunk.id)
  editChunkContent.value = chunk.content || ''
  editModalOpen.value = true
}

/**
 * 保存分块修改
 */
async function saveEditChunk() {
  if (!editChunkContent.value.trim()) {
    message.warning('分块内容不能为空')
    return
  }
  editSaving.value = true
  try {
    await ragApi.updateChunk(editChunkId.value, editChunkContent.value)
    message.success('更新成功')
    editModalOpen.value = false
    await loadChunks()
  } catch (error) {
    message.error('更新失败')
  } finally {
    editSaving.value = false
  }
}

/**
 * 删除分块
 */
function handleDeleteChunk(chunk: RagDocumentChunk) {
  Modal.confirm({
    title: '确认删除',
    content: `删除分块 #${chunk.chunkIndex} 后无法恢复，是否继续？`,
    okText: '确认删除',
    okButtonProps: { danger: true },
    cancelText: '取消',
    onOk: async () => {
      await ragApi.deleteChunk(String(chunk.id))
      message.success('删除成功')
      await loadChunks()
    }
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
        <AEmpty  description="未找到匹配的检索结果"/>
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
              <AButton type="text" size="small" @click="openEditChunk(chunk)">
                <EditOutlined />
              </AButton>
              <AButton type="text" size="small" danger @click="handleDeleteChunk(chunk)">
                <DeleteOutlined />
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

    <AModal
      v-model:open="editModalOpen"
      title="编辑分块"
      width="600px"
      :confirm-loading="editSaving"
      @ok="saveEditChunk"
    >
      <ATextarea
        v-model:value="editChunkContent"
        placeholder="请输入分块内容"
        :rows="12"
      />
    </AModal>
  </ADrawer>
</template>

<style scoped lang="scss">
@use '@/styles/rag/_doc-manager.scss' as *;
</style>
