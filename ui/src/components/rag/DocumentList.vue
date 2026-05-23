/**
 * RAG文档列表组件
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { computed, onUnmounted, reactive, ref, watch } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  DeleteOutlined,
  DownloadOutlined,
  EyeOutlined,
  ReloadOutlined,
  RedoOutlined,
  SearchOutlined,
  SwapOutlined,
  UploadOutlined,
} from '@ant-design/icons-vue'
import * as knowledgeApi from '@/api/knowledge'
import * as ragApi from '@/api/rag'
import type { KnowledgeBaseConfigVO, RagDocument, RagDocumentProcessOptions } from '@/types'
import ChunkDrawer from './ChunkDrawer.vue'
import FileIcon from '@/components/workspace/FileIcon.vue'

const props = defineProps<{
  knowledgeBaseConfigId: string
}>()

type ChunkStrategy =
  | 'CHARACTER'
  | 'SEPARATOR'
  | 'MARKDOWN'
  | 'SEMANTIC'

interface ChunkingFormState {
  parserType: string
  chunkStrategy: ChunkStrategy
  chunkSize: number
  overlap: number
  separatorsText: string
}

const DEFAULT_PROCESS_OPTIONS: ChunkingFormState = {
  parserType: 'AUTO',
  chunkStrategy: 'CHARACTER',
  chunkSize: 512,
  overlap: 0,
  separatorsText: ''
}

const PARSER_OPTIONS = [
  { label: '自动识别', value: 'AUTO' },
  { label: '文本优先', value: 'TEXT' },
  { label: 'Markdown', value: 'MARKDOWN' },
  { label: '表格/办公文档', value: 'STRUCTURED' }
]

const CHUNK_STRATEGY_OPTIONS: Array<{ label: string; value: ChunkStrategy }> = [
  { label: '按长度分块', value: 'CHARACTER' },
  { label: '按分隔符分块', value: 'SEPARATOR' },
  { label: '按 Markdown 结构', value: 'MARKDOWN' }
]

CHUNK_STRATEGY_OPTIONS.push(
  { label: '语义分块', value: 'SEMANTIC' }
)

const CHUNK_STRATEGY_LABELS: Record<ChunkStrategy, string> = {
  CHARACTER: '固定长度分块',
  SEPARATOR: '分隔符分块',
  MARKDOWN: 'Markdown 结构分块',
  SEMANTIC: '语义分块',
}

CHUNK_STRATEGY_OPTIONS.splice(
  0,
  CHUNK_STRATEGY_OPTIONS.length,
  ...CHUNK_STRATEGY_OPTIONS.map((item) => ({
    ...item,
    label: CHUNK_STRATEGY_LABELS[item.value],
  }))
)

const documents = ref<RagDocument[]>([])
const knowledgeConfig = ref<KnowledgeBaseConfigVO | null>(null)
const loading = ref(false)
const configLoading = ref(false)
const uploading = ref(false)
const reUploadingDocId = ref<string | null>(null)
const reChunkingDocId = ref<string | null>(null)

const chunkDrawerOpen = ref(false)
const chunkDrawerDocId = ref('')
const chunkDrawerDocName = ref('')

const searchKeyword = ref('')
const processOptions = reactive<ChunkingFormState>({ ...DEFAULT_PROCESS_OPTIONS })

let pollTimer: ReturnType<typeof setInterval> | null = null

const hasProcessingDoc = computed(() =>
  documents.value.some((d) => d.status === 'PROCESSING' || d.status === 'PENDING')
)

const filteredDocuments = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  if (!keyword) return documents.value
  return documents.value.filter((d) => d.fileName.toLowerCase().includes(keyword))
})

const docCount = computed(() => filteredDocuments.value.length)

const currentProcessSummary = computed(() => {
  const summary = [
    `策略：${CHUNK_STRATEGY_OPTIONS.find((item) => item.value === processOptions.chunkStrategy)?.label ?? processOptions.chunkStrategy}`,
    `chunkSize：${processOptions.chunkSize}`,
    `overlap：${processOptions.overlap}`,
  ]

  if (processOptions.chunkStrategy === 'SEPARATOR' && processOptions.separatorsText.trim()) {
    summary.push(`分隔符：${processOptions.separatorsText}`)
  }

  return summary.join('  /  ')
})

const statusConfig: Record<string, { label: string; color: string }> = {
  PENDING: { label: '待处理', color: 'orange' },
  PROCESSING: { label: '处理中', color: 'processing' },
  COMPLETED: { label: '已完成', color: 'success' },
  FAILED: { label: '失败', color: 'error' }
}

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

watch(
  () => props.knowledgeBaseConfigId,
  () => {
    void initializePage()
  },
  { immediate: true }
)

watch(
  () => processOptions.chunkStrategy,
  (strategy) => {
    if (strategy === 'MARKDOWN' && processOptions.parserType === 'AUTO') {
      processOptions.parserType = 'MARKDOWN'
    }
  }
)

onUnmounted(() => {
  stopPolling()
})

async function initializePage() {
  stopPolling()
  await Promise.all([loadDocuments(), loadKnowledgeConfig()])
}

async function loadKnowledgeConfig() {
  if (!props.knowledgeBaseConfigId) return

  configLoading.value = true
  try {
    const response = await knowledgeApi.detail(props.knowledgeBaseConfigId)
    knowledgeConfig.value = response.data.data || null
    hydrateProcessOptions(knowledgeConfig.value?.retrievalConfig)
  } catch (error) {
    knowledgeConfig.value = null
    hydrateProcessOptions(undefined)
    console.error(error)
  } finally {
    configLoading.value = false
  }
}

function hydrateProcessOptions(retrievalConfig?: Record<string, unknown> | null) {
  const nextState = { ...DEFAULT_PROCESS_OPTIONS }

  if (retrievalConfig && typeof retrievalConfig === 'object') {
    const rawChunkSize = toNumber(retrievalConfig.chunkSize)
    const rawOverlap = toNumber(retrievalConfig.overlap ?? retrievalConfig.chunkOverlap)
    const rawStrategy = toStrategy(retrievalConfig.chunkStrategy)
    const rawParserType = toStringValue(retrievalConfig.parserType)
    const rawDelimiters = normalizeSeparators(
      retrievalConfig.separators ?? retrievalConfig.chunkDelimiters
    )

    if (rawChunkSize !== null) nextState.chunkSize = rawChunkSize
    if (rawOverlap !== null) nextState.overlap = rawOverlap
    if (rawStrategy) nextState.chunkStrategy = rawStrategy
    if (rawParserType) nextState.parserType = rawParserType
    if (rawDelimiters.length > 0) nextState.separatorsText = rawDelimiters.join(', ')
  }

  Object.assign(processOptions, nextState)
}

function toNumber(value: unknown): number | null {
  if (typeof value === 'number' && Number.isFinite(value)) return value
  if (typeof value === 'string' && value.trim() !== '') {
    const parsed = Number(value)
    return Number.isFinite(parsed) ? parsed : null
  }
  return null
}

function toStringValue(value: unknown): string | null {
  return typeof value === 'string' && value.trim() ? value.trim() : null
}

function toStrategy(value: unknown): ChunkStrategy | null {
  if (
    value === 'CHARACTER' ||
    value === 'SEPARATOR' ||
    value === 'MARKDOWN' ||
    value === 'SEMANTIC'
  ) {
    return value
  }
  return null
}

function normalizeSeparators(value: unknown): string[] {
  if (Array.isArray(value)) {
    return value.filter((item): item is string => typeof item === 'string' && item.trim().length > 0)
  }

  if (typeof value === 'string' && value.trim()) {
    return value
      .split(',')
      .map((item) => item.trim())
      .filter(Boolean)
  }

  return []
}

function buildProcessOptions(): RagDocumentProcessOptions {
  const separators = normalizeSeparators(processOptions.separatorsText)

  return {
    parserType: processOptions.parserType || undefined,
    chunkStrategy: processOptions.chunkStrategy,
    chunkSize: processOptions.chunkSize,
    overlap: processOptions.overlap,
    chunkOverlap: processOptions.overlap,
    separators: separators.length > 0 ? separators : undefined,
    chunkDelimiters: separators.length > 0 ? separators.join(',') : undefined,
  }
}

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

function managePolling() {
  if (hasProcessingDoc.value) {
    startPolling()
  } else {
    stopPolling()
  }
}

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
      // ignore polling errors
    }
  }, 3000)
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

function handleUpload(file: File) {
  if (!props.knowledgeBaseConfigId) {
    message.warning('知识库配置 ID 不存在')
    return false
  }

  uploading.value = true
  ragApi.uploadDocument(file, props.knowledgeBaseConfigId, buildProcessOptions())
    .then(() => {
      message.success('文档上传成功，正在处理中...')
      void loadDocuments()
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

function handleReUpload(doc: RagDocument) {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = '.pdf,.txt,.docx,.doc,.xlsx,.xls,.md,.csv,.pptx,.ppt'
  input.onchange = async (event: Event) => {
    const file = (event.target as HTMLInputElement).files?.[0]
    if (!file) return

    reUploadingDocId.value = doc.id
    try {
      await ragApi.reUploadDocument(doc.id, file, buildProcessOptions())
      message.success('重新上传成功，正在处理中...')
      await loadDocuments()
    } finally {
      reUploadingDocId.value = null
    }
  }
  input.click()
}

function handleReChunk(doc: RagDocument) {
  Modal.confirm({
    title: '确认重新分块',
    content: `将对“${doc.fileName}”按当前页面配置重新解析、分块并向量化，原有分块数据会被清除，是否继续？`,
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      reChunkingDocId.value = doc.id
      try {
        await ragApi.reChunkDocument(doc.id, buildProcessOptions())
        message.success('重新分块已开始，正在处理中...')
        await loadDocuments()
      } finally {
        reChunkingDocId.value = null
      }
    }
  })
}

function handleDelete(doc: RagDocument) {
  Modal.confirm({
    title: '确认删除',
    content: `删除“${doc.fileName}”将同时删除其全部分块和向量数据，是否继续？`,
    okText: '确认删除',
    okButtonProps: { danger: true },
    cancelText: '取消',
    onOk: async () => {
      await ragApi.deleteDocuments([doc.id])
      message.success('删除成功')
      await loadDocuments()
    }
  })
}

function handleViewChunks(doc: RagDocument) {
  chunkDrawerDocId.value = doc.id
  chunkDrawerDocName.value = doc.fileName
  chunkDrawerOpen.value = true
}

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}
</script>

<template>
  <div class="doc-list-container">
    <ACard size="small" class="doc-process-card" :loading="configLoading">
      <template #title>解析与分块配置</template>
      <template #extra>
        <span class="doc-process-summary">{{ currentProcessSummary }}</span>
      </template>

      <AForm layout="vertical">
        <div class="doc-process-grid">
          <AFormItem label="解析策略">
            <ASelect v-model:value="processOptions.parserType" :options="PARSER_OPTIONS" />
          </AFormItem>
          <AFormItem label="分块策略">
            <ASelect v-model:value="processOptions.chunkStrategy" :options="CHUNK_STRATEGY_OPTIONS" />
          </AFormItem>
          <AFormItem label="Chunk Size">
            <AInputNumber v-model:value="processOptions.chunkSize" :min="64" :max="8192" style="width: 100%" />
          </AFormItem>
          <AFormItem label="Overlap">
            <AInputNumber v-model:value="processOptions.overlap" :min="0" :max="2048" style="width: 100%" />
          </AFormItem>
          <AFormItem
            v-if="processOptions.chunkStrategy === 'SEPARATOR'"
            class="doc-process-grid-wide"
            label="分隔符"
          >
            <AInput
              v-model:value="processOptions.separatorsText"
              placeholder="多个分隔符用逗号分隔，例如：\n\n,## ,---"
            />
          </AFormItem>
        </div>
        <div class="doc-process-hint">
          当前配置会用于文档上传、重新上传和重新分块；如果后端暂未消费部分字段，页面会保持兼容，不影响原有流程。
        </div>
      </AForm>
    </ACard>

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

    <div v-else class="doc-table">
      <ASpin :spinning="loading">
        <ATable
          :data-source="filteredDocuments"
          :columns="columns"
          :pagination="false"
          :scroll="{ y: 'calc(100vh - 310px)' }"
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
                  <AButton type="text" size="small" @click="handleDownload(record)">
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
                <ATooltip title="重新分块">
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

    <ChunkDrawer
      v-model:open="chunkDrawerOpen"
      :document-id="chunkDrawerDocId"
      :document-name="chunkDrawerDocName"
    />
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/rag/_doc-manager.scss' as *;

.doc-process-card {
  margin-bottom: var(--spacing-md);

  :deep(.ant-card-head) {
    min-height: auto;
  }
}

.doc-process-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0 var(--spacing-md);
}

.doc-process-grid-wide {
  grid-column: 1 / -1;
}

.doc-process-summary {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
}

.doc-process-hint {
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
  margin-top: var(--spacing-xs);
}

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
    height: 45px;
  }

  .doc-table-actions,
  .doc-table-status {
    display: flex;
    justify-content: center;
    align-items: center;
  }
}

@media (max-width: 1200px) {
  .doc-process-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .doc-process-grid {
    grid-template-columns: 1fr;
  }
}
</style>
