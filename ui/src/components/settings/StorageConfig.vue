/**
 * 存储配置组件
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { ref, reactive, onMounted, h, shallowReactive, computed, watch } from 'vue'
import type { Component } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { SearchOutlined, DatabaseOutlined } from '@ant-design/icons-vue'
import * as storageApi from '@/api/storageProtocol'
import type { StorageProtocol } from '@/types'
import S3Form from './storage/S3Form.vue'
import FtpForm from './storage/FtpForm.vue'
import LocalForm from './storage/LocalForm.vue'

const protocolFormComponents = shallowReactive<Record<string, Component>>({
  S3: S3Form,
  FTP: FtpForm,
  LOCAL: LocalForm
})

const protocolOptions = [
  { label: 'S3', value: 'S3' },
  { label: 'FTP', value: 'FTP' },
  { label: 'LOCAL', value: 'LOCAL' }
]

const allDataList = ref<StorageProtocol[]>([])
const loading = ref(false)
const page = reactive({ current: 1, size: 20 })
const searchKeyword = ref('')

/** 前端过滤：按名称、备注、协议匹配 */
const filteredList = computed(() => {
  const kw = searchKeyword.value?.trim().toLowerCase() || ''
  if (!kw) return allDataList.value
  return allDataList.value.filter(
    (item) =>
      item.name?.toLowerCase().includes(kw) ||
      item.remark?.toLowerCase().includes(kw) ||
      item.protocol?.toLowerCase().includes(kw)
  )
})

/** 当前页展示的数据 */
const displayList = computed(() => {
  const start = (page.current - 1) * page.size
  return filteredList.value.slice(start, start + page.size)
})

watch(searchKeyword, () => {
  page.current = 1
})

const formVisible = ref(false)
const formMode = ref<'add' | 'edit' | 'view'>('add')
const formData = ref<Partial<StorageProtocol>>({ valid: 0 })
const formRef = ref()
const formLoading = ref(false)

const protocolModalVisible = ref(false)
const protocolModalData = reactive({
  id: '',
  protocol: '',
  config: {} as Record<string, unknown>
})
const protocolFormRef = ref()
const protocolFormLoading = ref(false)

const rules = {
  name: [{ required: true, message: '请输入存储名称', trigger: 'blur' }],
  protocol: [{ required: true, message: '请选择存储协议', trigger: 'blur' }],
  valid: [{ required: true, message: '请选择是否启用', trigger: 'blur' }]
}

async function load() {
  loading.value = true
  try {
    const res = await storageApi.page({
      current: 1,
      size: 999
    })
    const result = res.data.data
    allDataList.value = result.records || []
  } catch {
    console.error('加载存储配置失败')
  } finally {
    loading.value = false
  }
}

function handleAdd() {
  formMode.value = 'add'
  formData.value = { valid: 0 }
  formVisible.value = true
}

async function handleEdit(id: string) {
  formMode.value = 'edit'
  const res = await storageApi.selectOne(id)
  formData.value = { ...res.data.data }
  formVisible.value = true
}

async function handleView(item: StorageProtocol) {
  let configStr = '暂无'
  try {
    const parsed = JSON.parse(item.protocolConfig || '{}')
    configStr = JSON.stringify(parsed, null, 2)
  } catch {
    configStr = item.protocolConfig || '暂无'
  }

  Modal.info({
    title: '存储配置详情',
    closable: true,
    icon: null,
    footer: null,
    width: 560,
    content: h('div', { style: { maxHeight: '400px', overflowY: 'auto' } }, [
      h('p', {}, [h('strong', '名称: '), item.name]),
      h('p', {}, [h('strong', '协议: '), item.protocol]),
      h('p', {}, [h('strong', '状态: '), item.valid === 1 ? '启用' : '禁用']),
      h('p', {}, [h('strong', '备注: '), item.remark || '暂无']),
      h('p', {}, h('strong', '协议配置:')),
      h('pre', {
        style: {
          background: '#f5f5f5',
          padding: '12px',
          borderRadius: '4px',
          maxHeight: '200px',
          overflowY: 'auto',
          fontSize: '12px'
        }
      }, configStr)
    ])
  })
}

function getDefaultProtocolConfig(protocol: string): string {
  const defaults: Record<string, object> = {
    S3: { bucketName: 'apboa', pathStyleAccess: true },
    FTP: { port: 21, encoding: 'UTF-8' },
    LOCAL: { localDir: '/home' }
  }
  return JSON.stringify(defaults[protocol] || {})
}

async function handleFormSubmit() {
  try {
    await formRef.value.validate()
    formLoading.value = true
    const data = formData.value as StorageProtocol
    if (formMode.value === 'add') {
      const payload = {
        ...data,
        protocolConfig: data.protocolConfig || getDefaultProtocolConfig(data.protocol || '')
      }
      await storageApi.add(payload)
      message.success('保存成功')
    } else if (formMode.value === 'edit') {
      await storageApi.update(data)
      message.success('保存成功')
    }
    formVisible.value = false
    load()
  } catch (error: unknown) {
    if (error && typeof error === 'object' && 'errorFields' in error) return
  } finally {
    formLoading.value = false
  }
}

/** 启用/禁用切换（使用 Switch 一键切换，无需二次确认） */
async function handleToggleValid(item: StorageProtocol) {
  if (item.valid === 1) {
    message.warning('当前已是启用状态，系统仅允许一个配置启用')
    return
  }
  await storageApi.validSuccess(item.id)
  message.success('已启用')
  item.valid = 1
  load()
}

/**
 * 打开协议配置弹窗
 */
async function handleOpenProtocolConfig(id: string) {
  const res = await storageApi.selectOne(id)
  const data = res.data.data
  protocolModalData.id = String(data.id)
  protocolModalData.protocol = data.protocol || ''
  try {
    protocolModalData.config = JSON.parse(data.protocolConfig || '{}')
  } catch {
    protocolModalData.config = {}
  }
  protocolModalVisible.value = true
}

async function handleSaveProtocolConfig() {
  try {
    await protocolFormRef.value.form.validate()
    protocolFormLoading.value = true
    const config = protocolFormRef.value.getConfig()
    await storageApi.updateProtocol({
      id: protocolModalData.id,
      protocolConfig: JSON.stringify(config)
    })
    message.success('保存成功')
    protocolModalVisible.value = false
    load()
  } catch (error: unknown) {
    if (error && typeof error === 'object' && 'errorFields' in error) return
  } finally {
    protocolFormLoading.value = false
  }
}

function handleDelete(item: StorageProtocol) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除存储配置 "${item.name}" 吗？此操作不可恢复。`,
    icon: null,
    onOk: async () => {
      try {
        await storageApi.remove([item.id])
        message.success('删除成功')
        load()
      } catch {
        console.error('删除失败')
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
    <div class="storage-config-header">
      <h2 class="settings-page-title m-0">存储配置</h2>
      <div class="storage-config-toolbar">
        <AInput
          v-model:value="searchKeyword"
          placeholder="搜索名称、备注、协议"
          style="width: 250px"
          allow-clear
        >
          <template #suffix>
            <SearchOutlined />
          </template>
        </AInput>
        <AButton type="primary" @click="handleAdd" v-permission="['EDIT','ADMIN']">
          新增配置
        </AButton>
      </div>
    </div>

    <ASpin :spinning="loading">
      <div class="storage-list">
        <div
          v-for="item in displayList"
          :key="item.id"
          class="storage-list-item"
        >
          <div class="storage-list-avatar">
            <DatabaseOutlined />
          </div>
          <div class="storage-list-info">
            <div class="storage-list-name-row">
              <span
                class="storage-list-name cursor-pointer"
                @click="handleView(item)"
              >
                {{ item.name }}
              </span>
              <span class="storage-list-protocol">{{ item.protocol }}</span>
            </div>
            <div class="storage-list-remark">{{ item.remark || '暂无备注' }}</div>
          </div>
          <ASwitch
              :checked="item.valid === 1"
              :disabled="item.valid === 1"
              @change="(checked: boolean) => { if (checked) handleToggleValid(item) }"
            />
          <div v-permission="['EDIT','ADMIN']" class="storage-list-actions">
            <a-divider type="vertical" />

            <AButton
              type="text"
              size="small"
              @click="handleOpenProtocolConfig(item.id)"
            >
              协议配置
            </AButton>
            <AButton type="text" size="small" @click="handleEdit(item.id)">
              编辑
            </AButton>
            <AButton
              type="text"
              size="small"
              danger
              @click="handleDelete(item)"
            >
              删除
            </AButton>
          </div>
        </div>
      </div>

      <AEmpty v-if="!loading && filteredList.length === 0" description="暂无存储配置" />

      <div v-if="filteredList.length > page.size" class="storage-pagination">
        <APagination
          v-model:current="page.current"
          v-model:page-size="page.size"
          :total="filteredList.length"
          show-size-changer
          :page-size-options="['10', '20', '30', '50']"
          @showSizeChange="() => { page.current = 1 }"
        />
      </div>
    </ASpin>

    <!-- 新增/编辑表单弹窗 -->
    <AModal
      v-model:open="formVisible"
      :title="formMode === 'add' ? '新增存储配置' : formMode === 'edit' ? '编辑存储配置' : '查看配置'"
      :confirm-loading="formLoading"
      @ok="handleFormSubmit"
    >
      <template #footer>
        <AButton @click="formVisible = false">关闭</AButton>
        <AButton
          v-if="formMode !== 'view'"
          type="primary"
          :loading="formLoading"
          @click="handleFormSubmit"
        >
          保存
        </AButton>
      </template>
      <AForm
        v-if="formVisible"
        ref="formRef"
        :model="formData"
        :rules="rules"
        layout="vertical"
        :disabled="formMode === 'view'"
      >
        <AFormItem label="存储名称" name="name">
          <AInput v-model:value="formData.name" placeholder="请输入存储名称" maxlength="50" show-count />
        </AFormItem>
        <AFormItem label="存储协议" name="protocol">
          <ASelect
            v-model:value="formData.protocol"
            placeholder="请选择协议"
            :options="protocolOptions"
          />
        </AFormItem>
        <AFormItem label="备注" name="remark">
          <AInput v-model:value="formData.remark" placeholder="请输入备注" type="textarea" :rows="3" />
        </AFormItem>
        <AFormItem label="是否启用" name="valid">
          <ASelect v-model:value="formData.valid" placeholder="请选择">
            <ASelectOption :value="1">启用</ASelectOption>
            <ASelectOption :value="0">禁用</ASelectOption>
          </ASelect>
        </AFormItem>
      </AForm>
    </AModal>

    <!-- 协议配置弹窗 -->
    <AModal
      v-model:open="protocolModalVisible"
      title="协议配置"
      :confirm-loading="protocolFormLoading"
      @ok="handleSaveProtocolConfig"
    >
      <template #footer>
        <AButton @click="protocolModalVisible = false">关闭</AButton>
        <AButton type="primary" :loading="protocolFormLoading" @click="handleSaveProtocolConfig">
          保存
        </AButton>
      </template>
      <component
        v-if="protocolModalVisible && protocolModalData.protocol"
        ref="protocolFormRef"
        :is="protocolFormComponents[protocolModalData.protocol]"
        :config="protocolModalData.config"
      />
    </AModal>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/modules/_settings.scss' as *;

.storage-config-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--spacing-xl);
  flex-wrap: wrap;
  gap: var(--spacing-md);
}

.storage-config-toolbar {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  flex-wrap: wrap;
}

.storage-list {
  display: flex;
  flex-direction: column;
  max-height: calc(100vh - 220px);
  overflow-y: auto;
}

.storage-list-item {
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

.storage-list-avatar {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #e3f2fd;
  color: #2196f3;
  border-radius: var(--border-radius-lg);
  font-size: 18px;
  flex-shrink: 0;
}

.storage-list-info {
  min-width: 0;
  flex: 1;
}

.storage-list-name-row {
  display: flex;
  align-items: baseline;
  gap: var(--spacing-sm);
}

.storage-list-name {
  font-size: var(--font-size-base);
  font-weight: 500;
  color: var(--color-text-primary);

  &:hover {
    color: var(--color-primary);
  }
}

.storage-list-protocol {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
}

.storage-list-remark {
  font-size: var(--font-size-xs);
  color: var(--color-text-placeholder);
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.storage-list-meta {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  flex-shrink: 0;
}

.storage-list-actions {
  display: flex;
  align-items: center;
  flex-shrink: 0;
  margin-left: auto;
  gap: var(--spacing-xs);
}

.storage-pagination {
  margin-top: var(--spacing-lg);
  display: flex;
  justify-content: center;
}

.cursor-pointer {
  cursor: pointer;
}
</style>
