/**
 * API Keys 管理组件
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import * as skApi from '@/api/sk'
import type { SecretKeyVO } from '@/types/vo'
import { formatDate } from '@/utils/tools'
import dayjs, { Dayjs } from 'dayjs';

const dataList = ref<SecretKeyVO[]>([])
const loading = ref(false)
const searchKeyword = ref('')

/** 新建/编辑弹窗可见性 */
const modalVisible = ref(false)
/** 是否为编辑模式 */
const isEdit = ref(false)
/** 表单引用 */
const formRef = ref()

/** 表单数据 */
const formData = ref<Partial<SecretKeyVO>>({
  name: '',
  expireTime: null,
  remark: ''
})

/** 表单校验规则 */
const formRules = {
  name: [
    { required: true, message: '请输入名称', trigger: 'blur' },
    { max: 100, message: '名称最多100个字符', trigger: 'blur' }
  ],
  remark: [
    { max: 200, message: '备注最多200个字符', trigger: 'blur' }
  ]
}

/** 创建后显示完整key的弹窗 */
const createdKeyVisible = ref(false)
const createdKeyValue = ref('')

/**
 * 前端搜索过滤后的列表
 */
const filteredList = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  if (!keyword) return dataList.value
  return dataList.value.filter(
    item => item.name.toLowerCase().includes(keyword)
  )
})

/**
 * 加载列表数据
 */
async function load() {
  loading.value = true
  try {
    const res = await skApi.list()
    dataList.value = res.data.data || []
  } finally {
    loading.value = false
  }
}

/**
 * 打开新建弹窗
 */
function handleCreate() {
  isEdit.value = false
  formData.value = { name: '', expireTime: null, remark: '' }
  modalVisible.value = true
}

/**
 * 打开编辑弹窗
 *
 * @param row 当前行数据
 */
function handleEdit(row: SecretKeyVO) {
  isEdit.value = true
  formData.value = { id: row.id, name: row.name }
  modalVisible.value = true
}

/**
 * 提交表单（新建或编辑）
 */
async function handleSubmit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    if (isEdit.value) {
      await skApi.update({ id: formData.value.id!, name: formData.value.name! })
      message.success('更新成功')
      modalVisible.value = false
      load()
    } else {
      const res = await skApi.create(formData.value)
      modalVisible.value = false
      // 展示完整key，仅在创建时显示一次
      createdKeyValue.value = res.data.data?.value || ''
      createdKeyVisible.value = true
      load()
    }
  } finally {
    loading.value = false
  }
}

/**
 * 删除秘钥
 *
 * @param row 当前行数据
 */
function handleDelete(row: SecretKeyVO) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除 "${row.name}" 吗？此操作不可恢复。`,
    icon: null,
    onOk: async () => {
      try {
        loading.value = true
        await skApi.remove([row.id])
        message.success('删除成功')
        load()
      }finally {
        loading.value = false
      }
    }
  })
}

/**
 * 复制key到剪贴板
 *
 * @param value key值
 */
async function handleCopy(value: string) {
  try {
    await navigator.clipboard.writeText(value)
    message.success('已复制到剪贴板')
  } catch {
    message.error('复制失败，请手动复制')
  }
}

onMounted(() => {
  load()
})
</script>

<template>
  <div>
    <div class="api-keys-header">
      <h2 class="settings-page-title m-0">API Keys</h2>
      <div class="api-keys-toolbar">
        <AInput
          v-model:value="searchKeyword"
          placeholder="搜索名称"
          style="width: 220px"
          allow-clear
        />
        <AButton type="primary" @click="handleCreate">创建</AButton>
      </div>
    </div>

    <ASpin :spinning="loading">
      <div class="api-keys-list">
        <div
          v-for="(item, index) in filteredList"
          :key="item.id"
          class="api-keys-item"
        >
          <div class="api-keys-avatar">
            {{ item.name.substring(0, 1) }}
          </div>
          <div class="api-keys-info">
            <div class="api-keys-name-row">
              <span class="api-keys-name">{{ item.name }}</span>
            </div>
            <div class="api-keys-meta">
              <span class="api-keys-value">{{ item.value }}</span>
              <span class="api-keys-divider">·</span>
              <span>{{ item.expireTime ? formatDate(item.expireTime) + ' 过期' : '永久有效' }}</span>
            </div>
            <div v-if="item.remark" class="api-keys-remark">{{ item.remark }}</div>
          </div>
          <div class="api-keys-actions">
            <AButton type="text" size="small" @click="handleEdit(item)">编辑</AButton>
            <AButton type="text" size="small" danger @click="handleDelete(item)">删除</AButton>
          </div>
        </div>
      </div>

      <AEmpty v-if="!loading && filteredList.length === 0" description="暂无API Key" />
    </ASpin>

    <!-- 新建 / 编辑弹窗 -->
    <AModal
      v-model:open="modalVisible"
      :title="isEdit ? '编辑 API Key' : '创建 API Key'"
      :confirm-loading="loading"
      @ok="handleSubmit"
      @cancel="modalVisible = false"
    >
      <AForm
        ref="formRef"
        :model="formData"
        :rules="formRules"
        layout="vertical"
        style="margin-top: 16px"
      >
        <AFormItem label="名称" name="name">
          <AInput v-model:value="formData.name" placeholder="请输入名称" :maxlength="100" show-count />
        </AFormItem>
        <template v-if="!isEdit">
          <AFormItem label="过期时间" name="expireTime">
            <ADatePicker
              v-model:value="formData.expireTime"
              style="width: 100%"
              :show-time="{ defaultValue: dayjs('00:00:00', 'HH:mm:ss') }"
              format="YYYY-MM-DD HH:mm:ss"
              value-format="YYYY-MM-DD HH:mm:ss"
              placeholder="不配置则永久有效"
            />
            <div class="api-keys-hint">不配置过期时间表示永久有效</div>
          </AFormItem>
          <AFormItem label="备注" name="remark">
            <ATextarea
              v-model:value="formData.remark"
              placeholder="请输入备注（选填）"
              :maxlength="200"
              show-count
              :rows="3"
            />
          </AFormItem>
        </template>
      </AForm>
    </AModal>

    <!-- 创建成功后展示完整key -->
    <AModal
      v-model:open="createdKeyVisible"
      title="API Key 已创建"
      :footer="null"
      :maskClosable="false"
      width="800px"
      @cancel="createdKeyVisible = false"
    >
      <div class="api-keys-created-tip">请立即复制并妥善保存，此key仅显示一次。在请求头的Authorization中使用。</div>
      <div class="api-keys-created-value">
        <span class="api-keys-created-text">{{ createdKeyValue }}</span>
        <AButton type="text" size="small" @click="handleCopy(createdKeyValue)">复制</AButton>
      </div>
    </AModal>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/modules/_settings.scss' as *;

.api-keys-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--spacing-xl);
  flex-wrap: wrap;
  gap: var(--spacing-md);
}

.api-keys-toolbar {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.api-keys-list {
  display: flex;
  flex-direction: column;
  max-height: calc(100vh - 200px);
  overflow-y: auto;
}

.api-keys-item {
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

.api-keys-index {
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

.api-keys-avatar {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #ede9fe;
  color: #7c3aed;
  border-radius: var(--border-radius-lg);
  font-size: 18px;
  font-weight: 600;
  flex-shrink: 0;
}

.api-keys-info {
  min-width: 0;
  flex: 1;
}

.api-keys-name-row {
  display: flex;
  align-items: baseline;
  gap: var(--spacing-sm);
}

.api-keys-name {
  font-size: var(--font-size-base);
  font-weight: 500;
  color: var(--color-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.api-keys-meta {
  font-size: var(--font-size-xs);
  color: var(--color-text-placeholder);
  margin-top: 2px;
}

.api-keys-value {
  font-family: monospace;
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
}

.api-keys-remark {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.api-keys-divider {
  margin: 0 6px;
}

.api-keys-actions {
  display: flex;
  align-items: center;
  flex-shrink: 0;
  margin-left: auto;
  gap: var(--spacing-xs);
}

.api-keys-hint {
  font-size: var(--font-size-xs);
  color: var(--color-text-placeholder);
  margin-top: 4px;
}

.api-keys-created-tip {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin-bottom: var(--spacing-md);
}

.api-keys-created-value {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  background: var(--color-bg-secondary);
  border-radius: var(--border-radius-md);
  padding: var(--spacing-sm) var(--spacing-md);
}

.api-keys-created-text {
  flex: 1;
  font-family: monospace;
  font-size: var(--font-size-sm);
  color: var(--color-text-primary);
  word-break: break-all;
}
</style>
