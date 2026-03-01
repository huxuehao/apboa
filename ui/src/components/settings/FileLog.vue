/**
 * 文件日志组件
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import * as attachApi from '@/api/attach'
import type { AttachLog } from '@/types'
import { formatFileSize, formatDate } from '@/utils/tools'

const dataList = ref<AttachLog[]>([])
const loading = ref(false)
const page = reactive({ current: 1, size: 20, total: 0 })
const queryParams = reactive<Partial<AttachLog>>({
  originalName: undefined,
  extension: undefined,
  optType: undefined
})

const optTypeOptions = [
  { label: '上传', value: 'UPLOAD' },
  { label: '下载', value: 'DOWNLOAD' },
  { label: '删除', value: 'DELETE' }
]

function getOptTypeTag(optType: string) {
  switch (optType) {
    case 'UPLOAD':
      return { color: 'success', text: '上传' }
    case 'DOWNLOAD':
      return { color: 'processing', text: '下载' }
    case 'DELETE':
      return { color: 'error', text: '删除' }
    default:
      return { color: 'default', text: optType || '--' }
  }
}

async function load() {
  loading.value = true
  try {
    const res = await attachApi.logPage({
      ...queryParams,
      current: page.current,
      size: page.size
    })
    const result = res.data.data
    dataList.value = result.records || []
    page.total = result.total || 0
  } catch {
    console.error('加载文件日志失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.current = 1
  load()
}

function handleReset() {
  queryParams.originalName = undefined
  queryParams.extension = undefined
  queryParams.optType = undefined
  page.current = 1
  load()
}

onMounted(() => {
  load()
})
</script>

<template>
  <div>
    <div class="file-log-header">
      <h2 class="settings-page-title m-0">文件日志</h2>
      <div class="file-log-toolbar">
        <AInput
          v-model:value="queryParams.originalName"
          placeholder="附件名称"
          style="width: 250px"
          allow-clear
          @press-enter="handleSearch"
        />
        <ASelect
          v-model:value="queryParams.optType"
          placeholder="用户行为"
          style="width: 120px"
          allow-clear
          :options="optTypeOptions"
        />
        <AButton type="primary" @click="handleSearch">
          查询
        </AButton>
      </div>
    </div>

    <ASpin :spinning="loading">
      <div class="file-log-list">
        <div
          v-for="(item, index) in dataList"
          :key="item.id"
          class="file-log-item"
        >
          <div class="file-log-index">
            {{ (page.current - 1) * page.size + index + 1 }}
          </div>
          <div class="file-log-avatar">
            {{ (item.originalName || '未命名').substring(0, 1) }}
          </div>
          <div class="file-log-info">
            <div class="file-log-name-row">
              <span class="file-log-name" :title="item.originalName">
                {{ item.originalName || '未命名' }}
              </span>
              <ATag :color="getOptTypeTag(item.optType).color" :bordered="false" class="file-log-opt-tag">
                {{ getOptTypeTag(item.optType).text }}
              </ATag>
            </div>
            <div class="file-log-meta">
              <span class="file-log-extension">{{ item.extension || '-' }}</span>
              <span class="file-log-divider">·</span>
              <span>{{ formatFileSize(item.attachSize || 0) }}</span>
              <span class="file-log-divider">·</span>
              <span>{{ item.optTime ? formatDate(item.optTime) : '-' }}</span>
              <span class="file-log-divider">·</span>
              <span>{{ item.optUserName || '-' }}</span>
            </div>
          </div>
        </div>
      </div>

      <AEmpty v-if="!loading && dataList.length === 0" description="暂无日志" />

      <div v-if="page.total > 0" class="file-log-pagination">
        <APagination
          v-model:current="page.current"
          v-model:page-size="page.size"
          :total="page.total"
          show-size-changer
          :page-size-options="['20', '30', '40', '50', '60']"
          size="small"
          @change="load"
        />
      </div>
    </ASpin>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/modules/_settings.scss' as *;

.file-log-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--spacing-xl);
  flex-wrap: wrap;
  gap: var(--spacing-md);
}

.file-log-toolbar {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  flex-wrap: wrap;
}

.file-log-list {
  display: flex;
  flex-direction: column;
  max-height: calc(100vh - 250px);
  overflow-y: auto;
}

.file-log-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  padding: var(--spacing-base) 20px;
  border-bottom: 1px solid var(--color-border-extra-light);
  border-radius: 10px;
  transition: 0.1s;

  &:last-child {
    border-bottom: none;
  }
}

.file-log-index {
  flex-shrink: 0;
  min-width: 28px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-size-xs);
  font-weight: 500;
  color: var(--color-text-secondary);
  background: var(--color-bg-secondary);
  border-radius: 6px;
}

.file-log-avatar {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #e3f2fd;
  color: #1565c0;
  border-radius: var(--border-radius-lg);
  font-size: 18px;
  font-weight: 600;
  flex-shrink: 0;
}

.file-log-info {
  min-width: 0;
  flex: 1;
}

.file-log-name-row {
  display: flex;
  align-items: baseline;
  gap: var(--spacing-sm);
  flex-wrap: wrap;
}

.file-log-name {
  font-size: var(--font-size-base);
  font-weight: 500;
  color: var(--color-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 460px;
}

.file-log-extension {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  flex-shrink: 0;
}

.file-log-opt-tag {
  flex-shrink: 0;
}

.file-log-meta {
  font-size: var(--font-size-xs);
  color: var(--color-text-placeholder);
  margin-top: 2px;
}

.file-log-divider {
  margin: 0 6px;
}

.file-log-pagination {
  margin-top: var(--spacing-lg);
  padding: var(--spacing-md) 0;
  display: flex;
  justify-content: flex-end;
  align-items: center;

  :deep(.ant-pagination) {
    .ant-pagination-total-text {
      color: var(--color-text-secondary);
      font-size: var(--font-size-sm);
    }
    .ant-pagination-options {
      margin-left: 12px;
    }
  }
}
</style>
