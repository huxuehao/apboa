/**
 * 模型供应商管理主页面
 *
 * @author huxuehao
 */
<script setup lang="ts">
/* eslint-disable vue/multi-word-component-names */
import { onMounted, ref, onUnmounted, computed, h } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { SearchOutlined } from '@ant-design/icons-vue'
import { useModelStore } from '@/stores'
import { storeToRefs } from 'pinia'
import * as modelApi from '@/api/model'
import {ModelProviderType, type ModelProviderVO} from '@/types'
import ModelProviderCard from '@/components/model/ModelProviderCard.vue'
import CreateProviderCard from '@/components/model/CreateProviderCard.vue'
import ModelProviderForm from '@/components/model/ModelProviderForm.vue'
import ModelConfigModal from '@/components/model/ModelConfigModal.vue'

const store = useModelStore()
const { providerList, selectedProviderType, keyword, loading, hasMore } = storeToRefs(store)

const formVisible = ref<boolean>(false)
const currentData = ref<ModelProviderVO | undefined>(undefined)
const scrollContainer = ref<HTMLElement>()
const loadMoreObserver = ref<IntersectionObserver>()

const configModalVisible = ref<boolean>(false)
const currentProviderId = ref<string>('')
const currentProviderName = ref<string>('')

/**
 * 供应商类型选项
 */
const providerTypeOptions = [
  { label: '全部', value: null },
  { label: 'DashScope', value: 'DASH_SCOPE' },
  { label: 'OpenAI', value: 'OPEN_AI' },
  { label: 'Anthropic', value: 'ANTHROPIC' },
//   { label: 'Gemini', value: 'GEMINI' },
  { label: 'Ollama', value: 'OLLAMA' }
]

/**
 * 是否显示新增卡片
 */
const showCreateCard = computed(() => {
  return selectedProviderType.value !== null
})

/**
 * 处理新增
 */
function handleCreate() {
  currentData.value = undefined
  formVisible.value = true
}

/**
 * 处理查看
 */
async function handleView(id: string) {
  const response = await modelApi.providerDetail(id)
  const data = response.data.data

  Modal.info({
    title: '供应商详情',
    closable: true,
    icon: null,
    footer: null,
    width: 600,
    content: h('div', { style: { maxHeight: '800px', overflowY: 'auto' } }, [
      h('p', {}, [h('strong', '供应商类型: '), getProviderTypeLabel(data.type)]),
      h('p', {}, [h('strong', '名称: '), data.name]),
      h('p', {}, [h('strong', '描述: '), data.description]),
      h('p', {}, [h('strong', 'Base URL: '), data.baseUrl]),
      h('p', {}, [h('strong', '认证类型: '), data.authType === 'CONFIG' ? '直接配置' : '环境变量']),
      ...(data.authType === 'CONFIG' ? [
        h('p', {}, [h('strong', 'API密钥: '), '********'])
      ] : [
        h('p', {}, [h('strong', '环境变量名: '), data.envVarName])
      ]),
      h('p', {}, [h('strong', '是否启用: '), data.enabled ? '是' : '否']),
      h('p', {}, [h('strong', '创建时间: '), data.createdAt]),
      h('p', {}, [h('strong', '更新时间: '), data.updatedAt])
    ])
  })
}

/**
 * 获取供应商类型标签
 */
function getProviderTypeLabel(type: string): string {
  const labels: Record<string, string> = {
    DASH_SCOPE: 'DashScope',
    OPEN_AI: 'OpenAI',
    ANTHROPIC: 'Anthropic',
    GEMINI: 'Gemini',
    OLLAMA: 'Ollama'
  }
  return labels[type] || type
}

/**
 * 处理编辑
 */
async function handleEdit(id: string) {
  const response = await modelApi.providerDetail(id)
  currentData.value = response.data.data
  formVisible.value = true
}

/**
 * 处理模型配置
 */
function handleConfigModels(id: string, name: string) {
  currentProviderId.value = id
  currentProviderName.value = name
  configModalVisible.value = true
}

/**
 * 处理删除
 */
async function handleDelete(id: string) {
  const isUsed = await store.checkUsedWithModel(id)
  if (isUsed) {
    message.warning('请先删除模型配置')
    return
  }

  Modal.confirm({
    title: '确认删除',
    content: '删除后无法恢复,是否继续?',
    onOk: async () => {
      await store.deleteProvider(id)
    }
  })
}

/**
 * 处理表单提交成功
 */
function handleFormSuccess() {
  store.resetAndFetch()
}

/**
 * 处理供应商类型切换
 */
function handleProviderTypeChange(value: string | null) {
  store.setProviderType(value)
}

/**
 * 处理搜索
 */
function handleSearch() {
  store.setKeyword(keyword.value)
}

/**
 * 处理状态
 */
async function handleEnable(id: string) {
  const response = await modelApi.providerDetail(id)
  const { enabled } = response.data.data

  const used = await store.checkUsedWithModel(id)
  if (used && enabled) {
    Modal.confirm({
      title: '二次确认',
      content: `该模型提供商存在关联模型，禁用后会影响关联模型的正常使用！`,
      okText: '确认并继续',
      onOk: async () => {
        await store.toggleEnabled(id, !enabled)
      }
    })
    return
  }

  await store.toggleEnabled(id, !enabled)
}

/**
 * 初始化无限滚动观察器
 */
function initIntersectionObserver() {
  if (!scrollContainer.value) return

  const sentinel = document.createElement('div')
  sentinel.className = 'scroll-sentinel'
  scrollContainer.value.appendChild(sentinel)

  loadMoreObserver.value = new IntersectionObserver(
    (entries) => {
      const entry = entries[0]
      if (entry && entry.isIntersecting && hasMore.value && !loading.value) {
        store.loadMore()
      }
    },
    { threshold: 0.1 }
  )

  loadMoreObserver.value.observe(sentinel)
}

onMounted(() => {
  store.fetchProviderPage(1)
  setTimeout(() => {
    initIntersectionObserver()
  }, 100)
})

onUnmounted(() => {
  loadMoreObserver.value?.disconnect()
})
</script>

<template>
  <div class="model-provider-page">
    <section class="intro-section">
      <h3 class="intro-title">模型供应商管理</h3>
      <p class="intro-desc text-secondary">
        模型供应商管理是企业级智能体平台的核心基础设施模块，负责统一接入、配置和调度多家AI模型供应商的服务。通过标准化的供应商与模型管理体系，为上层智能体提供稳定、可比较、可切换的推理能力支持。
      </p>
    </section>

    <section class="filter-section flex justify-between items-center">
      <div class="filter-left">
        <ASegmented
          v-model:value="selectedProviderType"
          :options="providerTypeOptions"
          @change="handleProviderTypeChange"
        />
      </div>

      <div class="filter-right">
        <AInput
          v-model:value="keyword"
          placeholder="搜索供应商名称"
          style="width: 300px; border: rgba(14,14,14,0.1) solid 1px !important;"
          @pressEnter="handleSearch"
        >
          <template #suffix>
            <AButton type="text" size="small" @click="handleSearch">
              <SearchOutlined />
            </AButton>
          </template>
        </AInput>
      </div>
    </section>

    <section ref="scrollContainer" class="card-section">
      <div class="card-grid">
        <CreateProviderCard @click="handleCreate" v-permission="['EDIT','ADMIN']" />

        <ModelProviderCard
          v-for="item in providerList"
          :key="item.id"
          :data="item"
          @view="handleView"
          @edit="handleEdit"
          @enable="handleEnable"
          @config-models="handleConfigModels"
          @delete="handleDelete"
        />
      </div>

      <div v-if="loading" class="load-indicator mt-md">
        <ASpin />
        <span class="ml-sm text-secondary">加载中...</span>
      </div>

      <div v-if="!hasMore && providerList.length > 0" class="no-more-indicator text-secondary mt-md">
        没有更多数据了
      </div>

      <div v-if="!loading && providerList.length === 0" class="empty-indicator mt-lg">
        <AEmpty description="暂无数据" />
      </div>
    </section>

    <ModelProviderForm
      v-model:visible="formVisible"
      :data="currentData"
      :model-provider-type="selectedProviderType as ModelProviderType"
      @success="handleFormSuccess"
    />

    <ModelConfigModal
      v-model:visible="configModalVisible"
      :provider-id="currentProviderId"
      :provider-name="currentProviderName"
    />
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/model/index.scss' as *;
</style>
