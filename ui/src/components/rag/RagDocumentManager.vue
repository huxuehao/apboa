/**
 * 本地RAG文档管理组件
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { UploadOutlined, DeleteOutlined, SearchOutlined, FileTextOutlined } from '@ant-design/icons-vue'
import * as ragApi from '@/api/rag'
import type { RagDocument, RagDocumentChunk, RagDocumentStatus } from '@/types'

const props = defineProps<{
  knowledgeBaseConfigId: string
}>()

const documents = ref<RagDocument[]>([])
const loading = ref(false)
const uploading = ref(false)
const searchQuery = ref('')
const searchResults = ref<Record<string, unknown>[]>([])
const searching = ref(false)
const showChunks = ref(false)
const chunks = ref<RagDocumentChunk[]>([])
const chunksLoading = ref(false)
const selectedDocId = ref<string | null>(null)

const statusMap: Record<string, { text: string; color: string }> = {
  PENDING: { text: '待处理', color: 'default' },
  PROCESSING: { text: '处理中', color: 'processing' },
  COMPLETED: { text: '已完成', color: 'success' },
  FAILED: { text: '失败', color: 'error' }
}

const fileList = ref<any[]>([])

onMounted(() => {
  loadDocuments()
})

watch(() => props.knowledgeBaseConfigId, () => {
  loadDocuments()
})

async function loadDocuments() {
  if (!props.knowledgeBaseConfigId) return
  loading.value = true
  try {
    const response = await ragApi.listDocuments(props.knowledgeBaseConfigId)
    documents.value = response.data.data || []
  } catch (error) {
    console.error('加载文档列表失败:', error)
  } finally {
    loading.value = false
  }
}

async function handleUpload(file: File) {
  if (!props.knowledgeBaseConfigId) {
    message.warning('请先保存知识库配置')
    return false
  }

  uploading.value = true
  try {
    await ragApi.uploadDocument(file, props.knowledgeBaseConfigId)
    message.success('文档上传成功，正在处理中...')
    await loadDocuments()
  } catch (error) {
    message.error('文档上传失败')
    console.error(error)
  } finally {
    uploading.value = false
  }
  return false
}

function handleDelete(id: string) {
  Modal.confirm({
    title: '确认删除',
    content: '删除文档将同时删除其所有分块和向量数据，是否继续？',
    onOk: async () => {
      await ragApi.deleteDocuments([id])
      message.success('删除成功')
      await loadDocuments()
    }
  })
}

async function handleSearch() {
  if (!searchQuery.value.trim()) {
    message.warning('请输入检索内容')
    return
  }

  searching.value = true
  try {
    const response = await ragApi.search({
      knowledgeBaseConfigId: props.knowledgeBaseConfigId,
      query: searchQuery.value,
      limit: 5,
      scoreThreshold: 0.3
    })
    searchResults.value = response.data.data || []
  } catch (error) {
    message.error('检索失败')
    console.error(error)
  } finally {
    searching.value = false
  }
}

async function handleViewChunks(docId: string) {
  selectedDocId.value = docId
  showChunks.value = true
  chunksLoading.value = true
  try {
    const response = await ragApi.listChunks(docId)
    chunks.value = response.data.data || []
  } catch (error) {
    console.error('加载分块列表失败:', error)
  } finally {
    chunksLoading.value = false
  }
}

function formatFileSize(bytes: number) {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}
</script>

<template>
  <div class="rag-document-manager">
    <div class="flex justify-between items-center mb-md">
      <h4>文档管理</h4>
      <AUpload
        :before-upload="handleUpload"
        :show-upload-list="false"
        accept=".pdf,.txt,.docx,.doc,.xlsx,.xls,.md,.csv,.pptx"
      >
        <AButton type="primary" :loading="uploading" :disabled="!knowledgeBaseConfigId">
          <UploadOutlined /> 上传文档
        </AButton>
      </AUpload>
    </div>

    <div class="flex gap-sm mb-md">
      <AInput
        v-model:value="searchQuery"
        placeholder="输入检索内容测试RAG效果"
        style="flex: 1"
        @pressEnter="handleSearch"
      />
      <AButton type="primary" :loading="searching" @click="handleSearch">
        <SearchOutlined /> 检索
      </AButton>
    </div>

    <div v-if="searchResults.length > 0" class="search-results mb-md">
      <h5 class="mb-sm">检索结果</h5>
      <div v-for="(result, index) in searchResults" :key="index" class="search-result-item">
        <div class="flex items-center gap-sm mb-xs">
          <ATag color="blue">分块 {{ result.chunkIndex }}</ATag>
          <span class="text-placeholder text-xs">文档ID: {{ result.documentId }}</span>
        </div>
        <div class="result-content">{{ result.content }}</div>
      </div>
    </div>

    <ASpin :spinning="loading">
      <AEmpty v-if="documents.length === 0" description="暂无文档，请上传" />
      <div v-else class="document-list">
        <div v-for="doc in documents" :key="doc.id" class="document-item flex items-center gap-sm">
          <FileTextOutlined class="text-lg" style="color: #1890ff" />
          <div class="flex-1">
            <div class="doc-name">{{ doc.fileName }}</div>
            <div class="flex items-center gap-sm text-placeholder text-xs">
              <span>{{ formatFileSize(doc.fileSize) }}</span>
              <span>{{ doc.fileType }}</span>
              <span v-if="doc.chunkCount">分块: {{ doc.chunkCount }}</span>
            </div>
          </div>
          <ATag :color="statusMap[doc.status]?.color || 'default'">
            {{ statusMap[doc.status]?.text || doc.status }}
          </ATag>
          <AButton type="text" size="small" @click="handleViewChunks(doc.id)">分块</AButton>
          <AButton type="text" size="small" danger @click="handleDelete(doc.id)">
            <DeleteOutlined />
          </AButton>
        </div>
      </div>
    </ASpin>

    <ADrawer
      v-model:open="showChunks"
      title="文档分块详情"
      :width="600"
    >
      <ASpin :spinning="chunksLoading">
        <div v-for="chunk in chunks" :key="chunk.id" class="chunk-item">
          <div class="flex items-center gap-sm mb-xs">
            <ATag>分块 #{{ chunk.chunkIndex }}</ATag>
            <span v-if="chunk.tokenCount" class="text-placeholder text-xs">~{{ chunk.tokenCount }} tokens</span>
          </div>
          <div class="chunk-content">{{ chunk.content }}</div>
        </div>
        <AEmpty v-if="chunks.length === 0" description="暂无分块数据" />
      </ASpin>
    </ADrawer>
  </div>
</template>

<style scoped lang="scss">
.rag-document-manager {
  .search-results {
    background-color: var(--color-bg-light);
    border-radius: var(--border-radius-base);
    padding: var(--spacing-md);

    .search-result-item {
      padding: var(--spacing-sm);
      margin-bottom: var(--spacing-sm);
      background-color: var(--color-bg-white);
      border-radius: var(--border-radius-base);
      border: 1px solid var(--color-border-base);

      .result-content {
        font-size: var(--font-size-sm);
        color: var(--color-text-regular);
        line-height: 1.6;
        max-height: 120px;
        overflow-y: auto;
      }
    }
  }

  .document-list {
    .document-item {
      padding: var(--spacing-sm) var(--spacing-md);
      border: 1px solid var(--color-border-base);
      border-radius: var(--border-radius-base);
      margin-bottom: var(--spacing-xs);
      transition: all var(--transition-base);

      &:hover {
        border-color: var(--color-primary);
        background-color: var(--color-bg-light);
      }

      .doc-name {
        font-weight: 500;
        font-size: var(--font-size-sm);
        overflow: hidden;
        white-space: nowrap;
        text-overflow: ellipsis;
        max-width: 300px;
      }
    }
  }

  .chunk-item {
    padding: var(--spacing-sm);
    margin-bottom: var(--spacing-sm);
    border: 1px solid var(--color-border-base);
    border-radius: var(--border-radius-base);

    .chunk-content {
      font-size: var(--font-size-sm);
      color: var(--color-text-regular);
      line-height: 1.6;
      max-height: 150px;
      overflow-y: auto;
      white-space: pre-wrap;
    }
  }
}
</style>
