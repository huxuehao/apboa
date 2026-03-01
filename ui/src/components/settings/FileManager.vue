/**
 * 文件管理组件
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import * as attachApi from '@/api/attach'
import type { Attach } from '@/types'
import { formatFileSize, formatDate } from '@/utils/tools'


const dataList = ref<Attach[]>([])
const loading = ref(false)
const page = reactive({ current: 1, size: 20, total: 0 })
const queryParams = reactive<Partial<Attach>>({
  originalName: undefined,
})
const selectedRowKeys = ref<string[]>([])

async function load() {
  loading.value = true
  try {
    const res = await attachApi.page({
      ...queryParams,
      current: page.current,
      size: page.size
    })
    const result = res.data.data
    dataList.value = result.records || []
    page.total = result.total || 0
  } catch {
    console.error('加载附件列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.current = 1
  load()
}

async function handleDownload(row: Attach) {
  try {
    const res = await attachApi.download(row.id)
    const blob = res.data as Blob
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = row.originalName || 'download'
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('下载成功')
  } catch {
    message.error('下载失败')
  }
}

function handleDelete(rows: Attach[]) {
  if (!rows || rows.length === 0) {
    message.warning('请至少选择一条数据')
    return
  }

  Modal.confirm({
    title: '确认删除',
    content: `确定要删除选中的 ${rows.length} 个文件吗？此操作不可恢复。`,
    icon: null,
    onOk: async () => {
      try {
        loading.value = true
        await attachApi.remove(rows.map((r) => r.id))
        message.success('删除成功')
        selectedRowKeys.value = []
        load()
      } catch {
        console.error('删除失败')
      } finally {
        loading.value = false
      }
    }
  })
}

onMounted(() => {
  load()
})
</script>

<template>
  <div>
    <div class="file-manager-header">
      <h2 class="settings-page-title m-0">文件管理</h2>
      <div class="file-manager-toolbar">
        <AInput
          v-model:value="queryParams.originalName"
          placeholder="附件名称"
          style="width: 250px"
          allow-clear
          @press-enter="handleSearch"
        />
        <AButton type="primary" @click="handleSearch">
          查询
        </AButton>
      </div>
    </div>

    <ASpin :spinning="loading">
      <div class="file-list">
        <div
          v-for="(item, index) in dataList"
          :key="item.id"
          class="file-list-item"
        >
          <div class="file-list-index">{{ (page.current - 1) * page.size + index + 1 }}</div>
          <div class="file-list-avatar">
            {{ (item.originalName || item.name || '未命名').substring(0,1) }}
          </div>
          <div class="file-list-info">
            <div class="file-list-name-row">
              <span class="file-list-name" :title="item.originalName">
                {{ item.originalName || item.name || '未命名' }}
              </span>
            </div>
            <div class="file-list-meta">
              <span class="file-list-extension">{{ item.extension || '-' }}</span>
              <span class="file-list-divider">·</span>
              <span>{{ formatFileSize(item.attachSize) }}</span>
              <span class="file-list-divider">·</span>
              <span>{{ item.protocol || '-' }}</span>
              <span class="file-list-divider">·</span>
              <span>{{ item.createAt ? formatDate(item.createAt) : '-' }}</span>
            </div>
          </div>
          <div class="file-list-actions">
            <AButton type="text" size="small" @click="handleDownload(item)">
              下载
            </AButton>
            <AButton
              v-permission="['EDIT', 'ADMIN']"
              type="text"
              size="small"
              danger
              @click="handleDelete([item])"
            >
              删除
            </AButton>
          </div>
        </div>
      </div>

      <AEmpty v-if="!loading && dataList.length === 0" description="暂无附件" />

      <div v-if="page.total > 0" class="file-pagination">
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

.file-manager-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--spacing-xl);
  flex-wrap: wrap;
  gap: var(--spacing-md);
}

.file-manager-toolbar {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  flex-wrap: wrap;
}

.file-list {
  display: flex;
  flex-direction: column;
  max-height: calc(100vh - 250px);
  overflow-y: auto;
}

.file-list-item {
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

.file-list-index {
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

.file-list-checkbox {
  flex-shrink: 0;
}

.file-list-avatar {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #e8f5e9;
  color: #2e7d32;
  border-radius: var(--border-radius-lg);
  font-size: 18px;
  font-weight: 600;
  flex-shrink: 0;
}

.file-list-info {
  min-width: 0;
  flex: 1;
}

.file-list-name-row {
  display: flex;
  align-items: baseline;
  gap: var(--spacing-sm);
}

.file-list-name {
  font-size: var(--font-size-base);
  font-weight: 500;
  color: var(--color-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-list-extension {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  flex-shrink: 0;
}

.file-list-meta {
  font-size: var(--font-size-xs);
  color: var(--color-text-placeholder);
  margin-top: 2px;
}

.file-list-divider {
  margin: 0 6px;
}

.file-list-actions {
  display: flex;
  align-items: center;
  flex-shrink: 0;
  margin-left: auto;
  gap: var(--spacing-xs);
}

.file-pagination {
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
