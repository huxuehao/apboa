/**
 * 知识库管理主页面
 *
 * @author huxuehao
 */
<script setup lang="ts">
/* eslint-disable vue/multi-word-component-names */
import { onMounted, ref, onUnmounted, h, computed } from 'vue'
import { Modal } from 'ant-design-vue'
import { SearchOutlined } from '@ant-design/icons-vue'
import { useKnowledgeStore } from '@/stores'
import { storeToRefs } from 'pinia'
import * as knowledgeApi from '@/api/knowledge'
import type { KnowledgeBaseConfigVO, KbType } from '@/types'
import KnowledgeCard from '@/components/knowledge/KnowledgeCard.vue'
import CreateCard from '@/components/knowledge/CreateCard.vue'
import KnowledgeForm from '@/components/knowledge/KnowledgeForm.vue'

const store = useKnowledgeStore()
const { list, selectedKbType, keyword, loading, hasMore } = storeToRefs(store)

const formVisible = ref<boolean>(false)
const currentData = ref<KnowledgeBaseConfigVO | undefined>(undefined)
const initialKbType = ref<KbType | undefined>(undefined)
const scrollContainer = ref<HTMLElement>()
const loadMoreObserver = ref<IntersectionObserver>()

/**
 * 当前选中的知识库类型(类型转换)
 */
const currentKbType = computed<KbType | null>(() => selectedKbType.value as KbType | null)

/**
 * 知识库类型选项
 */
const kbTypeOptions = [
  { label: '全部', value: null },
  { label: '百炼', value: 'BAILIAN' },
  { label: 'Dify', value: 'DIFY' },
  { label: 'RagFlow', value: 'RAGFLOW' }
]

/**
 * 知识库类型映射
 */
const kbTypeMap: Record<string, string> = {
  BAILIAN: '百炼',
  DIFY: 'Dify',
  RAGFLOW: 'RAGFlow'
}

/**
 * 处理新增
 */
function handleCreate(kbType: KbType) {
  currentData.value = undefined
  initialKbType.value = kbType
  formVisible.value = true
}

/**
 * 处理查看
 */
async function handleView(id: string) {
  const response = await knowledgeApi.detail(id)
  const data = response.data.data

  const contentItems = [
    h('p', {}, [h('strong', '关联智能体: '), data.used?.length ? data.used.join('、') : '无']),
    h('p', {}, [h('strong', '名称: '), data.name]),
    h('p', {}, [h('strong', '类型: '), kbTypeMap[data.kbType]]),
    h('p', {}, [h('strong', '描述: '), data.description || '无'])
  ]

  if (data.connectionConfig && Object.keys(data.connectionConfig).length > 0) {
    contentItems.push(
      h('p', {}, h('strong', '连接配置:')),
      h('pre', {
        style: {
          maxHeight: '150px',
          overflowY: 'auto',
          background: '#f5f5f5',
          padding: '12px',
          borderRadius: '4px',
          fontSize: '12px'
        }
      }, JSON.stringify(data.connectionConfig, null, 2))
    )
  }

  if (data.endpointConfig && Object.keys(data.endpointConfig).length > 0) {
    contentItems.push(
      h('p', {}, h('strong', '端点配置:')),
      h('pre', {
        style: {
          maxHeight: '150px',
          overflowY: 'auto',
          background: '#f5f5f5',
          padding: '12px',
          borderRadius: '4px',
          overflow: 'auto',
          fontSize: '12px'
        }
      }, JSON.stringify(data.endpointConfig, null, 2))
    )
  }

  if (data.retrievalConfig && Object.keys(data.retrievalConfig).length > 0) {
    contentItems.push(
      h('p', {}, h('strong', '检索配置:')),
      h('pre', {
        style: {
          maxHeight: '150px',
          overflowY: 'auto',
          background: '#f5f5f5',
          padding: '12px',
          borderRadius: '4px',
          fontSize: '12px'
        }
      }, JSON.stringify(data.retrievalConfig, null, 2))
    )
  }

  if (data.rerankingConfig && Object.keys(data.rerankingConfig).length > 0) {
    contentItems.push(
      h('p', {}, h('strong', '重排序配置:')),
      h('pre', {
        style: {
          maxHeight: '150px',
          overflowY: 'auto',
          background: '#f5f5f5',
          padding: '12px',
          borderRadius: '4px',
          fontSize: '12px'
        }
      }, JSON.stringify(data.rerankingConfig, null, 2))
    )
  }

  if (data.queryRewriteConfig && Object.keys(data.queryRewriteConfig).length > 0) {
    contentItems.push(
      h('p', {}, h('strong', '查询重写配置:')),
      h('pre', {
        style: {
          maxHeight: '150px',
          overflowY: 'auto',
          background: '#f5f5f5',
          padding: '12px',
          borderRadius: '4px',
          fontSize: '12px'
        }
      }, JSON.stringify(data.queryRewriteConfig, null, 2))
    )
  }

  if (data.metadataFilters && Object.keys(data.metadataFilters).length > 0) {
    contentItems.push(
      h('p', {}, h('strong', '元数据过滤:')),
      h('pre', {
        style: {
          maxHeight: '150px',
          overflowY: 'auto',
          background: '#f5f5f5',
          padding: '12px',
          borderRadius: '4px',
          fontSize: '12px'
        }
      }, JSON.stringify(data.metadataFilters, null, 2))
    )
  }

  if (data.httpConfig && Object.keys(data.httpConfig).length > 0) {
    contentItems.push(
      h('p', {}, h('strong', 'HTTP配置:')),
      h('pre', {
        style: {
          maxHeight: '150px',
          overflowY: 'auto',
          background: '#f5f5f5',
          padding: '12px',
          borderRadius: '4px',
          fontSize: '12px'
        }
      }, JSON.stringify(data.httpConfig, null, 2))
    )
  }

  contentItems.push(
    h('p', {}, [h('strong', '健康状态: '), data.healthStatus || 'UNKNOWN']),
    h('p', {}, [h('strong', '最后同步时间: '), data.lastSyncTime || '无'])
  )

  Modal.info({
    title: '知识库详情',
    closable: true,
    icon: null,
    footer: null,
    width: 800,
    content: h('div', { style: { maxHeight: '800px', overflowY: 'auto' } }, contentItems)
  })
}

/**
 * 处理编辑
 */
async function handleEdit(id: string) {
  const response = await knowledgeApi.detail(id)
  currentData.value = response.data.data
  initialKbType.value = undefined
  formVisible.value = true
}

/**
 * 处理删除
 */
async function handleDelete(id: string) {
  const used = await store.checkUsedWithAgent(id)
  if (used.length > 0) {
    Modal.confirm({
      title: '二次确认',
      content: `该知识库正在被 [ ${used.join('、')} ] 智能体引用，删除后可能会影响上述智能体的正常使用！`,
      okText: '确认并继续删除',
      onOk: async () => {
        await store.deleteConfig(id)
      }
    })
    return
  }

  Modal.confirm({
    title: '确认删除',
    content: '删除后无法恢复,是否继续?',
    onOk: async () => {
      await store.deleteConfig(id)
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
 * 处理知识库类型切换
 */
function handleKbTypeChange(value: string | null) {
  store.setKbType(value)
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
  const response = await knowledgeApi.detail(id)
  const { enabled } = response.data.data

  const used = await store.checkUsedWithAgent(id)
  if (used.length > 0 && enabled) {
    Modal.confirm({
      title: '二次确认',
      content: `该知识库体正在被 [ ${used.join('、')} ] 智能体引用，禁用后可能会影响上述智能体的正常使用！`,
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
  store.fetchPage(1)
  setTimeout(() => {
    initIntersectionObserver()
  }, 100)
})

onUnmounted(() => {
  loadMoreObserver.value?.disconnect()
})
</script>

<template>
  <div class="knowledge-page">
    <section class="intro-section">
      <h3 class="intro-title">知识库管理</h3>
      <p class="intro-desc text-secondary">
        知识库管理是智能体的“私有记忆与专业智库”，通过先进的RAG（检索增强生成）技术，将企业的私有文档、数据库、业务知识统一构建为可检索、可推理的结构化知识体系，让智能体在通用能力基础上具备企业级专业知识。
      </p>
    </section>

    <section class="filter-section flex justify-between items-center">
      <div class="filter-left">
        <ASegmented
          v-model:value="selectedKbType"
          :options="kbTypeOptions"
          @change="handleKbTypeChange"
        />
      </div>

      <div class="filter-right">
        <AInput
          v-model:value="keyword"
          placeholder="搜索知识库名称"
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
        <CreateCard :kb-type="currentKbType" @create="handleCreate" v-permission="['EDIT','ADMIN']"/>

        <KnowledgeCard
          v-for="item in list"
          :key="item.id"
          :data="item"
          @view="handleView"
          @edit="handleEdit"
          @enable="handleEnable"
          @delete="handleDelete"
        />
      </div>

      <div v-if="loading" class="load-indicator mt-md">
        <ASpin />
        <span class="ml-sm text-secondary">加载中...</span>
      </div>

      <div v-if="!hasMore && list.length > 0" class="no-more-indicator text-secondary mt-md">
        没有更多数据了
      </div>

      <div v-if="!loading && list.length === 0" class="empty-indicator mt-lg">
        <AEmpty description="暂无数据" />
      </div>
    </section>

    <KnowledgeForm
      v-model:visible="formVisible"
      :data="currentData"
      :initial-kb-type="initialKbType"
      @success="handleFormSuccess"
    />
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/knowledge/index.scss' as *;
</style>
