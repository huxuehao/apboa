/**
 * 模型配置管理模态框
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { ref, watch } from 'vue'
import { Modal, message } from 'ant-design-vue'
import type {ModelConfigVO, ModelConfigDTO, ModelConfig} from '@/types'
import * as modelApi from '@/api/model'
import ModelConfigForm from './ModelConfigForm.vue'
import { useAccountStore } from '@/stores'

const accountStore = useAccountStore()

/**
 * Props定义
 */
const props = defineProps<{
  visible: boolean
  providerId: string
  providerName: string
  providerType?: string
}>()

/**
 * Emits定义
 */
const emit = defineEmits<{
  'update:visible': [value: boolean]
}>()

const modelList = ref<ModelConfigVO[]>([])
const loading = ref<boolean>(false)
const searchKeyword = ref<string>('')
const currentPage = ref<number>(1)
const pageSize = ref<number>(10)
const total = ref<number>(0)

const formVisible = ref<boolean>(false)
const currentData = ref<ModelConfigVO | undefined>(undefined)

/**
 * 模型类型显示映射
 */
const modelTypeLabels: Record<string, string> = {
  CHAT: '文本',
  IMAGE: '图像',
  AUDIO: '音频',
  VIDEO: '视频'
}

/**
 * 表格列定义
 */
const columns = [
  {
    title: '名称',
    dataIndex: 'name',
    key: 'name',
  },
  {
    title: '模型ID',
    dataIndex: 'modelId',
    key: 'modelId',
  },
  {
    title: '模型类型',
    dataIndex: 'modelType',
    key: 'modelType',
    width: 220
  },
  {
    title: '是否启用',
    dataIndex: 'enabled',
    key: 'enabled',
    width: 90
  }
]

const hasPermission = accountStore.roles.some(role => ['EDIT','ADMIN'].includes(role));
if (hasPermission) {
  columns.push({
    title: '操作',
    key: 'action',
    width: 110,
    fixed: 'right'
  })
}

watch(
  () => props.visible,
  (newVal) => {
    if (newVal) {
      fetchModelList()
    } else {
      resetSearch()
    }
  }
)

/**
 * 加载模型列表
 */
async function fetchModelList() {
  loading.value = true
  try {
    const query: ModelConfigDTO = {
      page: currentPage.value,
      size: pageSize.value,
      providerId: props.providerId,
      name: searchKeyword.value || undefined
    }

    const response = await modelApi.configPage(query)
    const result = response.data.data

    modelList.value = result.records || []
    total.value = result.total
  } catch (error) {
    console.error('加载模型列表失败:', error)
  } finally {
    loading.value = false
  }
}

/**
 * 处理搜索
 */
function handleSearch() {
  currentPage.value = 1
  fetchModelList()
}

/**
 * 处理分页变化
 */
function handlePageChange(page: number, size: number) {
  currentPage.value = page
  pageSize.value = size
  fetchModelList()
}

/**
 * 重置搜索
 */
function resetSearch() {
  searchKeyword.value = ''
  currentPage.value = 1
  modelList.value = []
}

/**
 * 处理新增
 */
function handleCreate() {
  currentData.value = undefined
  formVisible.value = true
}

/**
 * 处理编辑
 */
async function handleEdit(record: ModelConfigVO) {
  const response = await modelApi.configDetail(record.id)
  currentData.value = response.data.data
  formVisible.value = true
}

/**
 * 处理删除
 */
async function handleDelete(record: ModelConfigVO) {
  const used = await checkUsedWithAgent(record.id)
  if (used.length > 0) {
    Modal.confirm({
      title: '二次确认',
      content: `该模型正在被 [ ${used.join('、')} ] 智能体引用，删除后可能会影响上述智能体的正常使用！`,
      okText: '确认并继续删除',
      onOk: async () => {
        await modelApi.configRemove([record.id])
        await fetchModelList()
      }
    })
    return
  }

  Modal.confirm({
    title: '确认删除',
    content: '删除后无法恢复,是否继续?',
    onOk: async () => {
      await modelApi.configRemove([record.id])
      message.success('删除成功')
      await fetchModelList()
    }
  })
}

/**
 * 检查是否被智能体使用
 */
async function checkUsedWithAgent(id: string): Promise<string[]> {
  const response = await modelApi.configUsedWithAgent([id])
  return response.data.data as string[] || []
}

/**
 * 处理表单提交成功
 */
function handleFormSuccess() {
  fetchModelList()
}

/**
 * 处理模态框关闭
 */
function handleClose() {
  emit('update:visible', false)
}

const enableLoading = ref<boolean>(false)
/**
 * 切换状态
 */
async function handleEnable(id: string) {
  const item = modelList.value.find((x) => x.id === id)
  if (!item) return

  const response = await modelApi.configDetail(id)
  const { enabled } = response.data.data

  const used = await checkUsedWithAgent(id)
  if (used.length > 0 && enabled) {
    Modal.confirm({
      title: '二次确认',
      content: `该模型正在被 [ ${used.join('、')} ] 智能体引用，禁用后可能会影响上述智能体的正常使用！`,
      okText: '确认并继续',
      onOk: async () => {
        await modelApi.configUpdate({ id, enabled: !enabled } as ModelConfig)
        item.enabled = !enabled
      },
      onCancel: () => {
        item.enabled = enabled
      }
    })
    return
  }

  try {
    enableLoading.value = true
    await modelApi.configUpdate({ id, enabled: !enabled } as ModelConfig)
    item.enabled = !enabled
  } finally {
    enableLoading.value = false
  }
}
</script>

<template>
  <ApboaModal
    :open="visible"
    :title="`${providerName} - 配置模型`"
    defaultWidth="1000px"
    :footer="null"
    @cancel="handleClose"
  >
    <div class="model-config-modal">
      <div class="modal-toolbar flex items-center justify-between mb-md">
        <AInput
          v-model:value="searchKeyword"
          placeholder="搜索模型名称"
          style="width: 300px"
          @pressEnter="handleSearch"
        >
          <template #suffix>
            <AButton type="text" size="small" @click="handleSearch">搜索</AButton>
          </template>
        </AInput>

        <AButton type="primary" @click="handleCreate" v-permission="['EDIT','ADMIN']">新增模型</AButton>
      </div>

      <ATable
        :columns="columns"
        :data-source="modelList"
        :loading="loading"
        :pagination="{
          current: currentPage,
          pageSize: pageSize,
          total: total,
          showSizeChanger: true,
          showTotal: (total: number) => `共 ${total} 条`
        }"
        size="small"
        :scroll="{ x: 800 }"
        row-key="id"
        @change="(pagination: any) => handlePageChange(pagination.current, pagination.pageSize)"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <span
              style="color: #0F74FF; cursor: pointer"
              @click="handleEdit(record)"
              v-permission="['EDIT','ADMIN']">{{ record.name }}</span>
            <span v-permission="['READ_ONLY']">{{ record.name }}</span>
          </template>

          <template v-if="column.key === 'modelType'">
            <ASpace v-if="Array.isArray(record.modelType)" :size="4">
              <ATag v-for="t in record.modelType" :key="t" color="default" :bordered="false">
                {{ modelTypeLabels[t] || t }}
              </ATag>
            </ASpace>
            <ATag v-else color="default" :bordered="false">
              {{ modelTypeLabels[record.modelType] || record.modelType }}
            </ATag>
          </template>

          <template v-if="column.key === 'enabled'">
            <ASwitch
              :loading="enableLoading"
              v-model:checked="record.enabled"
              @change="handleEnable(record.id)"
              v-permission="['EDIT','ADMIN']"
            />
            <ATag :color="record.enabled ? 'success' : 'default'" :bordered="false" v-permission="['READ_ONLY']">
              {{ record.enabled ? '启用' : '禁用' }}
            </ATag>
          </template>

          <template v-if="column.key === 'action'" v-permission="['EDIT','ADMIN']">
            <ASpace>
              <AButton type="link" size="small" @click="handleEdit(record)">编辑</AButton>
              <AButton type="link" size="small" danger @click="handleDelete(record)">删除</AButton>
            </ASpace>
          </template>
        </template>

        <template #emptyText>
          <AEmpty description="暂未配置模型,点击'新增模型'添加" />
        </template>
      </ATable>
    </div>

    <ModelConfigForm
      v-model:visible="formVisible"
      :provider-id="providerId"
      :provider-type="providerType"
      :data="currentData"
      @success="handleFormSuccess"
    />
  </ApboaModal>
</template>

<style scoped lang="scss">
.model-config-modal {
  .modal-toolbar {
    padding-bottom: var(--spacing-md);
    //border-bottom: 1px solid var(--color-border-light);
  }
}
</style>
