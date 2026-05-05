/**
 * RAG文档列表组件
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  UploadOutlined,
  DeleteOutlined,
  DownloadOutlined,
  ReloadOutlined,
  RedoOutlined,
  SwapOutlined,
  EyeOutlined,
  SearchOutlined,
} from '@ant-design/icons-vue'
import * as ragApi from '@/api/rag'
import type { RagDocument } from '@/types'
import ChunkDrawer from './ChunkDrawer.vue'
import FileIcon from "@/components/workspace/FileIcon.vue";

const props = defineProps<{
  knowledgeBaseConfigId: string
}>()

const documents = ref<RagDocument[]>([])
const loading = ref(false)
const uploading = ref(false)
const reUploadingDocId = ref<string | null>(null)
const reChunkingDocId = ref<string | null>(null)

/** 分块抽屉状态 */
const chunkDrawerOpen = ref(false)
const chunkDrawerDocId = ref('')
const chunkDrawerDocName = ref('')

/** 轮询定时器 */
let pollTimer: ReturnType<typeof setInterval> | null = null

/** 是否有正在处理的文档 */
const hasProcessingDoc = computed(() =>
  documents.value.some(d => d.status === 'PROCESSING' || d.status === 'PENDING')
)

/** 文档数量 */
const docCount = computed(() => filteredDocuments.value.length)

/** 搜索关键字 */
const searchKeyword = ref('')

/** 过滤后的文档列表 */
const filteredDocuments = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  if (!keyword) return documents.value
  return documents.value.filter(d =>
    d.fileName.toLowerCase().includes(keyword)
  )
})

/**
 * 状态映射
 */
const statusConfig: Record<string, { label: string; color: string }> = {
  PENDING: { label: '待处理', color: 'orange' },
  PROCESSING: { label: '处理中', color: 'processing' },
  COMPLETED: { label: '已完成', color: 'success' },
  FAILED: { label: '失败?', color: 'error' }
}

onMounted(() => {
  loadDocuments()
})

onUnmounted(() => {
  stopPolling()
})

/**
 * 加载文档列表
 */
async function loadDocuments() {
  if (!props.knowledgeBaseConfigId) return
  loading.value = true
  try {
    const response = await ragApi.listDocuments(props.knowledgeBaseConfigId)
    documents.value = response.data.data || []
    managePolling()
  } finally {
    loading.value = false
  }
}

/**
 * 管理轮询：有处理中文档时开启，否则关闭
 */
function managePolling() {
  if (hasProcessingDoc.value) {
    startPolling()
  } else {
    stopPolling()
  }
}

/**
 * 开始轮询
 */
function startPolling() {
  if (pollTimer) return
  pollTimer = setInterval(async () => {
    try {
      const response = await ragApi.listDocuments(props.knowledgeBaseConfigId)
      documents.value = response.data.data || []
      if (!hasProcessingDoc.value) {
        stopPolling()
      }
    } catch {
      // 轮询失败静默处理
    }
  }, 3000)
}

/**
 * 停止轮询
 */
function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

/**
 * 上传文档
 */
function handleUpload(file: File) {
  if (!props.knowledgeBaseConfigId) {
    message.warning('知识库配置ID不存在')
    return false
  }

  uploading.value = true
  ragApi.uploadDocument(file, props.knowledgeBaseConfigId)
    .then(() => {
      message.success('文档上传成功，正在处理中...')
      loadDocuments()
    })
    .catch((error) => {
      message.error('文档上传失败')
      console.error(error)
    })
    .finally(() => {
      uploading.value = false
    })

  return false
}

/**
 * 下载文档
 */
async function handleDownload(doc: RagDocument) {
  const response = await ragApi.downloadDocument(doc.id)
  const url = window.URL.createObjectURL(new Blob([response.data]))
  const link = document.createElement('a')
  link.href = url
  link.download = doc.fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
  message.success('开始下载')
}

/**
 * 重新上传文档
 */
function handleReUpload(doc: RagDocument) {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = '.pdf,.txt,.docx,.doc,.xlsx,.xls,.md,.csv,.pptx,.ppt'
  input.onchange = async (e: Event) => {
    const file = (e.target as HTMLInputElement).files?.[0]
    if (!file) return

    reUploadingDocId.value = doc.id
    try {
      await ragApi.reUploadDocument(doc.id, file)
      message.success('重新上传成功，正在处理中...')
      await loadDocuments()
    } finally {
      reUploadingDocId.value = null
    }
  }
  input.click()
}

/**
 * 重新分片
 */
function handleReChunk(doc: RagDocument) {
  Modal.confirm({
    title: '确认重新分片',
    content: `将对"${doc.fileName}"使用当前知识库配置重新分块和向量化，原有分块数据将被清除，是否继续？`,
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      reChunkingDocId.value = doc.id
      try {
        await ragApi.reChunkDocument(doc.id)
        message.success('重新分片已开始，正在处理中...')
        await loadDocuments()
      } finally {
        reChunkingDocId.value = null
      }
    }
  })
}

/**
 * 删除文档
 */
function handleDelete(doc: RagDocument) {
  Modal.confirm({
    title: '确认删除',
    content: `删除"${doc.fileName}"将同时删除其所有分块和向量数据，是否继续？`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      await ragApi.deleteDocuments([doc.id])
      message.success('删除成功')
      await loadDocuments()
    }
  })
}

/**
 * 查看分块详情
 */
function handleViewChunks(doc: RagDocument) {
  chunkDrawerDocId.value = doc.id
  chunkDrawerDocName.value = doc.fileName
  chunkDrawerOpen.value = true
}

/**
 * 格式化文件大小
 */
function formatFileSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

/**
 * 文档表格列定义
 */
const columns = computed(() => [
  {
    title: '文件名',
    key: 'fileName',
  },
  {
    title: '大小',
    key: 'fileSize',
    width: 100,
    align: 'center' as const,
  },
  {
    title: '分块数',
    key: 'chunkCount',
    width: 100,
    align: 'center' as const,
  },
  {
    title: '状态',
    key: 'status',
    width: 120,
    align: 'center' as const,
  },
  {
    title: '操作',
    key: 'actions',
    width: 200,
    align: 'center' as const,
  },
])
</script>

<template>
  <div class="doc-list-container">
    <!-- 工具栏 -->
    <div class="doc-list-toolbar">
      <div class="doc-list-toolbar-left">
        <AUpload
          :before-upload="handleUpload"
          :show-upload-list="false"
          accept=".pdf,.txt,.docx,.doc,.xlsx,.xls,.md,.csv,.pptx,.ppt"
        >
          <AButton type="primary" :loading="uploading" v-permission="['EDIT','ADMIN']">
            <UploadOutlined /> 上传文档
          </AButton>
        </AUpload>
        <AButton @click="loadDocuments">
          <ReloadOutlined />
        </AButton>
      </div>
      <div class="doc-list-toolbar-right">
        <AInput
          v-model:value="searchKeyword"
          placeholder="搜索文件名..."
          style="width: 200px"
          allow-clear
        >
          <template #prefix>
            <SearchOutlined />
          </template>
        </AInput>
        <span class="doc-list-stats">共 {{ docCount }} 个文档</span>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-if="filteredDocuments.length === 0 && !loading" class="doc-empty">
      <AEmpty :description="searchKeyword ? '没有匹配的文档' : '暂无文档，请上传文件到知识库'" />
      <AUpload
        v-if="!searchKeyword"
        :before-upload="handleUpload"
        :show-upload-list="false"
        accept=".pdf,.txt,.docx,.doc,.xlsx,.xls,.md,.csv,.pptx,.ppt"
      >
        <AButton type="primary" v-permission="['EDIT','ADMIN']">上传第一个文档</AButton>
      </AUpload>
    </div>

    <!-- 文档表格 -->
    <div v-else class="doc-table">
      <ASpin :spinning="loading">
        <ATable
          :data-source="filteredDocuments"
          :columns="columns"
          :pagination="false"
          :scroll="{ y: 'calc(100vh - 190px)' }"
          row-key="id"
          size="small"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'fileName'">
              <div class="doc-table-file">
                <FileIcon :file-name="record.fileName as string" width="20" />
                <div class="doc-table-file-info">
                  <div class="doc-table-file-name" :title="record.fileName">{{ record.fileName }}</div>
                </div>
              </div>
            </template>
            <template v-else-if="column.key === 'fileSize'">
              {{ formatFileSize(record.fileSize) }}
            </template>
            <template v-else-if="column.key === 'chunkCount'">
              <span v-if="record.chunkCount > 0">{{ record.chunkCount }}</span>
              <span v-else class="text-placeholder">-</span>
            </template>
            <template v-else-if="column.key === 'status'">
              <div class="doc-table-status">
                <ATooltip v-if="record.status === 'FAILED' && record.errorMessage" :title="record.errorMessage">
                  <ATag
                    style="cursor: help"
                    :color="statusConfig[record.status]?.color || 'default'"
                    :bordered="false"
                    size="small"
                  >
                    {{ statusConfig[record.status]?.label || record.status }}
                  </ATag>
                </ATooltip>
                <ATag
                  v-else
                  :color="statusConfig[record.status]?.color || 'default'"
                  :bordered="false"
                  size="small"
                >
                  {{ statusConfig[record.status]?.label || record.status }}
                </ATag>
              </div>
            </template>
            <template v-else-if="column.key === 'actions'">
              <div class="doc-table-actions">
                <ATooltip title="查看分块">
                  <AButton
                    type="text"
                    size="small"
                    :disabled="record.chunkCount === 0"
                    @click="handleViewChunks(record)"
                  >
                    <EyeOutlined />
                  </AButton>
                </ATooltip>
                <ATooltip title="下载文件">
                  <AButton
                    type="text"
                    size="small"
                    @click="handleDownload(record)"
                  >
                    <DownloadOutlined />
                  </AButton>
                </ATooltip>
                <ATooltip title="重新上传">
                  <AButton
                    type="text"
                    size="small"
                    :loading="reUploadingDocId === record.id"
                    v-permission="['EDIT','ADMIN']"
                    @click="handleReUpload(record)"
                  >
                    <SwapOutlined />
                  </AButton>
                </ATooltip>
                <ATooltip title="重新分片">
                  <AButton
                    type="text"
                    size="small"
                    :loading="reChunkingDocId === record.id"
                    v-permission="['EDIT','ADMIN']"
                    @click="handleReChunk(record)"
                  >
                    <RedoOutlined />
                  </AButton>
                </ATooltip>
                <ATooltip title="删除">
                  <AButton
                    type="text"
                    size="small"
                    danger
                    v-permission="['EDIT','ADMIN']"
                    @click="handleDelete(record)"
                  >
                    <DeleteOutlined />
                  </AButton>
                </ATooltip>
              </div>
            </template>
          </template>
        </ATable>
      </ASpin>
    </div>

    <!-- 分块详情抽屉 -->
    <ChunkDrawer
      v-model:open="chunkDrawerOpen"
      :document-id="chunkDrawerDocId"
      :document-name="chunkDrawerDocName"
    />
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/rag/_doc-manager.scss' as *;

@use '@/styles/rag/_doc-manager.scss' as *;



.doc-table {

  :deep(table) {
    border-collapse: collapse;
  }

  :deep(td),
  :deep(th) {
    vertical-align: middle !important;
  }

  :deep(.ant-table-tbody > tr > td) {
    vertical-align: middle !important;
    height: 45px; /* 固定行高，便于垂直居中 */
  }

  .doc-table-actions,
  .doc-table-status {
    display: flex;
    justify-content: center;
    align-items: center;
  }
}
</style>
