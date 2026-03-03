/**
 * 系统参数组件
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { SearchOutlined, EditOutlined, CheckOutlined, CloseOutlined } from '@ant-design/icons-vue'
import * as paramsApi from '@/api/params'
import type { Params } from '@/types'

const dataList = ref<Params[]>([])
const loading = ref(false)
const searchKeyword = ref('')
const editingId = ref<string | null>(null)
const editingRow = ref<{ paramName: string; paramValue: string }>({ paramName: '', paramValue: '' })

/** 前端过滤后的列表 */
const filteredList = computed(() => {
  const kw = searchKeyword.value?.trim().toLowerCase()
  if (!kw) return dataList.value
  return dataList.value.filter(
    (item) =>
      (item.paramName ?? '').toLowerCase().includes(kw) ||
      (item.paramKey ?? '').toLowerCase().includes(kw) ||
      (item.paramValue ?? '').toLowerCase().includes(kw)
  )
})

async function load() {
  loading.value = true
  try {
    const res = await paramsApi.page({
      current: 1,
      size: 500
    })
    const result = res.data.data
    dataList.value = result?.records ?? []
  } catch {
    console.error('加载系统参数失败')
  } finally {
    loading.value = false
  }
}

function startEdit(row: Params) {
  editingId.value = row.id
  editingRow.value = {
    paramName: row.paramName ?? '',
    paramValue: row.paramValue ?? ''
  }
}

function cancelEdit() {
  editingId.value = null
  editingRow.value = { paramName: '', paramValue: '' }
}

async function saveEdit(row: Params) {
  const { paramName, paramValue } = editingRow.value
  if (!paramName?.trim()) {
    message.warning('名称不能为空')
    return
  }

  try {
    loading.value = true
    await paramsApi.update({
      id: row.id,
      paramName: paramName.trim(),
      paramKey: row.paramKey,
      paramValue: paramValue ?? ''
    })
    message.success('保存成功')
    Object.assign(row, { paramName: paramName.trim(), paramValue: paramValue ?? '' })
    cancelEdit()
  } catch {
    console.error('保存失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  load()
})
</script>

<template>
  <div>
    <div class="system-params-header">
      <h2 class="settings-page-title m-0">系统参数</h2>
      <div class="system-params-toolbar">
        <AInput
          v-model:value="searchKeyword"
          placeholder="搜索 名称 / key / value"
          style="width: 260px"
          allow-clear
          @press-enter="() => {}"
        >
          <template #prefix>
            <SearchOutlined class="search-icon" />
          </template>
        </AInput>
      </div>
    </div>

    <ASpin :spinning="loading">
      <div class="params-list">
        <div
          v-for="(item, index) in filteredList"
          :key="item.id"
          class="params-list-item"
          :class="{ editing: editingId === item.id }"
        >
          <div class="params-list-index">{{ index + 1 }}</div>
          <div class="params-list-content">
            <div class="params-list-row params-list-name">
              <span class="params-list-label">名称</span>
              <span class="params-list-value">{{ item.paramName || '-' }}</span>
            </div>
            <div class="params-list-row params-list-key">
              <span class="params-list-label">key</span>
              <span class="params-list-value params-list-key-value">{{ item.paramKey }}</span>
            </div>
            <div class="params-list-row params-list-value-row">
              <span class="params-list-label">value</span>
              <template v-if="editingId === item.id">
                <AInput
                  v-model:value="editingRow.paramValue"
                  placeholder="参数值"
                  size="small"
                  class="params-edit-input"
                  @keyup.enter="saveEdit(item)"
                />
              </template>
              <span v-else class="params-list-value params-list-value-text">{{ item.paramValue || '-' }}</span>
            </div>
          </div>
          <div class="params-list-actions">
            <template v-if="editingId === item.id">
              <AButton
                type="text"
                size="small"
                class="params-action-btn params-action-save"
                @click="saveEdit(item)"
              >
                <CheckOutlined />
                保存
              </AButton>
              <AButton
                type="text"
                size="small"
                class="params-action-btn"
                @click="cancelEdit"
              >
                <CloseOutlined />
                取消
              </AButton>
            </template>
            <AButton
              v-else
              type="text"
              size="small"
              class="params-action-btn params-action-edit"
              @click="startEdit(item)"
            >
              <EditOutlined />
              编辑
            </AButton>
          </div>
        </div>
      </div>

      <AEmpty
        v-if="!loading && filteredList.length === 0"
        :description="searchKeyword ? '暂无匹配的系统参数' : '暂无系统参数'"
      />
    </ASpin>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/modules/_settings.scss' as *;

.system-params-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--spacing-xl);
  flex-wrap: wrap;
  gap: var(--spacing-md);
}

.system-params-toolbar {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.search-icon {
  color: var(--color-text-placeholder);
  font-size: 14px;
}

.params-list {
  display: flex;
  flex-direction: column;
  max-height: calc(100vh - 220px);
  overflow-y: auto;
}

.params-list-item {
  display: flex;
  align-items: flex-start;
  gap: var(--spacing-md);
  padding: var(--spacing-base) 20px;
  border-bottom: 1px solid var(--color-border-extra-light);
  border-radius: 10px;
  transition: 0.15s;

  &:last-child {
    border-bottom: none;
  }

  &.editing {
    background: var(--color-bg-base);
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  }
}

.params-list-index {
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

.params-list-content {
  min-width: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

.params-list-row {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  min-height: 24px;
}

.params-list-label {
  flex-shrink: 0;
  min-width: 42px;
  font-size: var(--font-size-xs);
  color: var(--color-text-placeholder);
}

.params-list-value {
  font-size: var(--font-size-base);
  color: var(--color-text-primary);
  word-break: break-all;
}

.params-list-key-value {
  font-size: var(--font-size-sm);
  color: var(--color-primary);
}

.params-list-value-text {
  color: var(--color-primary);
}

.params-edit-input {
  flex: 1;
  min-width: 120px;
  max-width: 400px;
}

.params-list-actions {
  display: flex;
  align-items: center;
  flex-shrink: 0;
  margin-left: auto;
  gap: var(--spacing-xs);
}

.params-action-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.params-action-edit {
  color: var(--color-primary);

  &:hover {
    color: var(--color-primary-hover);
  }
}

.params-action-save {
  color: #52c41a;

  &:hover {
    color: #73d13d;
  }
}
</style>
