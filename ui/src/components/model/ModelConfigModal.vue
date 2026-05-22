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
import ModelConfigCard from './ModelConfigCard.vue'

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

const formVisible = ref<boolean>(false)
const currentData = ref<ModelConfigVO | undefined>(undefined)

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
 * 加载模型列表（一次性加载全部）
 */
async function fetchModelList() {
  loading.value = true
  try {
    const query: ModelConfigDTO = {
      page: 1,
      size: 500,
      providerId: props.providerId,
      name: searchKeyword.value || undefined
    }

    const response = await modelApi.configPage(query)
    const result = response.data.data

    modelList.value = result.records || []
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
  fetchModelList()
}

/**
 * 重置搜索
 */
function resetSearch() {
  searchKeyword.value = ''
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
async function handleEdit(id: string) {
  const response = await modelApi.configDetail(id)
  currentData.value = response.data.data
  formVisible.value = true
}

/**
 * 处理删除
 */
async function handleDelete(id: string) {
  const record = modelList.value.find((x) => x.id === id)
  if (!record) return

  const used = await checkUsedWithAgent(id)
  if (used.length > 0) {
    Modal.confirm({
      title: '二次确认',
      content: `该模型正在被 [ ${used.join('、')} ] 智能体引用，删除后可能会影响上述智能体的正常使用！`,
      okText: '确认并继续删除',
      onOk: async () => {
        await modelApi.configRemove([id] as string[])
        message.success('删除成功')
        await fetchModelList()
      }
    })
    return
  }

  Modal.confirm({
    title: '确认删除',
    content: '删除后无法恢复,是否继续?',
    onOk: async () => {
      await modelApi.configRemove([id] as string[])
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

const enableLoading = ref<Set<string>>(new Set())
/**
 * 切换启用状态
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
    enableLoading.value.add(id)
    await modelApi.configUpdate({ id, enabled: !enabled } as ModelConfig)
    item.enabled = !enabled
  } finally {
    enableLoading.value.delete(id)
  }
}

const testing = ref<Set<string>>(new Set())
/**
 * 测试模型连接
 */
async function handleTest(id: string) {
  if (testing.value.has(id)) return

  const item = modelList.value.find((x) => x.id === id)
  if (!item) return

  // 立即更新前端状态为检测中
  item.connectivityStatus = 'CHECKING'

  try {
    testing.value.add(id)
    await modelApi.checkModel(id)
    // if (res.data.data.success) {
    //   message.success('测试成功')
    // } else {
    //   message.error(res.data.data.message)
    // }
    // 重新加载列表以获取服务端持久化的最新状态
    await fetchModelList()
  } finally {
    testing.value.delete(id)
  }
}
</script>

<template>
  <ApboaModal
    :open="visible"
    :title="`${providerName} - 配置模型`"
    :default-expanded="true"
    defaultWidth="100%"
    :footer="null"
    :background-color="'#F5F6F8'"
    class="configure-model"
    @cancel="handleClose"
  >
    <div class="model-config-modal">
      <!-- 工具栏 -->
      <div class="modal-toolbar flex items-center justify-between mb-md">
        <AButton type="primary" @click="handleCreate" v-permission="['EDIT','ADMIN']">新增模型</AButton>
        <AInput
          v-model:value="searchKeyword"
          placeholder="搜索模型名称"
          style="width: 300px; border: rgba(14,14,14,0.1) solid 1px !important;"
          @pressEnter="handleSearch"
        >
          <template #suffix>
            <AButton type="text" size="small" @click="handleSearch">搜索</AButton>
          </template>
        </AInput>
      </div>

      <!-- 卡片网格 -->
      <div v-if="modelList.length > 0" class="card-grid">
        <ModelConfigCard
          v-for="item in modelList"
          :key="item.id"
          :data="item"
          @edit="handleEdit"
          @delete="handleDelete"
          @enable="handleEnable"
          @test="handleTest"
        />
      </div>

      <!-- 空状态 -->
      <div v-else class="flex-center" style="padding: 60px 0">
        <AEmpty :description="loading ? '正在加载模型列表...' : `暂未配置模型,点击「新增模型」添加`" />
      </div>
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
  border-radius: var(--border-radius-base);
  padding: var(--spacing-md);
  min-height: 100%;

  .modal-toolbar {
    position: sticky;
    top: 0px;
    z-index: 10;
    background-color: #F5F6F8;
    margin: calc(-1 * var(--spacing-md)) calc(-1 * var(--spacing-md)) 0;
    padding: var(--spacing-md);
  }

  .card-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
    gap: var(--spacing-md);
  }
}
</style>
