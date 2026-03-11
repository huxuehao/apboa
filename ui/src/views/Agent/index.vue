/**
 * 智能体管理主页面
 *
 * @author huxuehao
 */
<script setup lang="ts">
/* eslint-disable vue/multi-word-component-names */
import { onMounted, ref, onUnmounted, computed, h } from 'vue'
import { Modal } from 'ant-design-vue'
import { SearchOutlined } from '@ant-design/icons-vue'
import { useAgentStore } from '@/stores'
import { storeToRefs } from 'pinia'
import * as agentApi from '@/api/agent'
import * as agentA2aApi from '@/api/agentA2a'
import type { AgentDefinitionVO, WellKnownAgentConfig, NacosAgentConfig } from '@/types'
import AgentCard from '@/components/agent/AgentCard.vue'
import CreateCard from '@/components/agent/CreateCard.vue'
import AgentForm from '@/components/agent/AgentForm.vue'
import AgentA2aForm from '@/components/agent/AgentA2aForm.vue'
import AgentArchitectureDiagram from '@/components/agent/architecture/AgentArchitectureDiagram.vue'
import AgentJobForm from '@/components/agent/AgentJobForm.vue'
import * as jobApi from '@/api/job'
import type { JobInfo } from '@/types'

const store = useAgentStore()
const { list, tags, selectedAgentType, selectedTag, keyword, loading, hasMore } = storeToRefs(store)

const formVisible = ref<boolean>(false)
const currentData = ref<AgentDefinitionVO | undefined>(undefined)
const scrollContainer = ref<HTMLElement>()
const loadMoreObserver = ref<IntersectionObserver>()
/** A2A 智能体表单 */
const a2aFormVisible = ref<boolean>(false)
/** A2A 类型（新建时传入） */
const currentA2aType = ref<'WELLKNOWN' | 'NACOS' | undefined>(undefined)
/** 架构图弹窗可见性 */
const architectureVisible = ref<boolean>(false)
/** 当前查看架构图的智能体ID */
const currentArchitectureId = ref<string>('')
/** 定时任务表单弹窗可见性 */
const jobFormVisible = ref<boolean>(false)
/** 当前编辑定时任务的智能体ID */
const currentJobAgentId = ref<string>('')
/** 当前智能体的定时任务信息 */
const currentJobInfo = ref<JobInfo | null>(null)

/**
 * 智能体类型选项
 */
const agentTypeOptions = [
  { label: '全部', value: null },
  { label: '自定义', value: 'CUSTOM' },
  { label: 'A2A', value: 'A2A' }
]

/**
 * 标签选项列表
 */
const tagOptions = computed(() => {
  const options = tags.value.map((tag: string) => ({
    label: tag,
    value: tag
  }))
  return [
    { label: '全部', value: null },
    ...options
  ]
})

/**
 * 新建自定义智能体
 */
function handleCreateCustom() {
  currentData.value = undefined
  formVisible.value = true
}

/**
 * 新建 A2A 智能体
 *
 * @param type A2A 协议类型
 */
function handleCreateA2a(type: 'WELLKNOWN' | 'NACOS') {
  currentData.value = undefined
  currentA2aType.value = type
  a2aFormVisible.value = true
}

/**
 * 处理查看 — 根据类型分居展示内容
 *
 * @param id 智能体 ID
 */
async function handleView(id: string) {
  const response = await agentApi.detail(id)
  const data = response.data.data

  if (data.agentType === 'A2A') {
    try {
      const a2aRes = await agentA2aApi.getA2aConfig(id)
      const a2aRecord = a2aRes.data.data
      const type = a2aRecord?.a2aType as string
      const config = a2aRecord?.a2aConfig

      let content
      if (type === 'WELLKNOWN' && config) {
        const c = config as WellKnownAgentConfig
        const headerRows = (c.authHeaders || []).map((item) =>
          h('p', { style: { paddingLeft: '16px' } }, [
            `${item.key}: `,
            item.evn
              ? h('span', { style: { color: '#888' } }, `环境变量 ${item.key}`)
              : item.value
          ])
        )
        content = h('div', { style: { maxHeight: '800px', overflowY: 'auto' } }, [
          h('p', {}, [h('strong', 'A2A 类型: '), 'WellKnown']),
          ...(data.tag ? [h('p', {}, [h('strong', '标签: '), data.tag])] : []),
          h('p', {}, [h('strong', '名称: '), data.name]),
          h('p', {}, [h('strong', '智能体编号: '), data.agentCode]),
          h('p', {}, [h('strong', '描述: '), data.description]),
          h('p', {}, [h('strong', '钩子数量: '), data.hook.length]),
          h('p', {}, [h('strong', '启用记忆: '), data.enableMemory ? '是' : '否']),
          h('p', {}, [h('strong', 'Base URL: '), c.baseUrl]),
          h('p', {}, [h('strong', 'Agent Card 路径: '), c.relativeCardPath]),
          h('p', {}, [h('strong', '认证头数量: '), (c.authHeaders || []).length]),
          ...headerRows
        ])
      } else if (type === 'NACOS' && config) {
        const c = config as NacosAgentConfig
        const propRows = (c.nacosProperties || []).map((p) =>
          h('p', { style: { paddingLeft: '16px' } }, [
            `${p.key}: `,
            p.evn
              ? h('span', { style: { color: '#888' } }, `环境变量 ${p.key}`)
              : p.value
          ])
        )
        content = h('div', { style: { maxHeight: '800px', overflowY: 'auto' } }, [
          h('p', {}, [h('strong', 'A2A 类型: '), 'Nacos']),
          ...(data.tag ? [h('p', {}, [h('strong', '标签: '), data.tag])] : []),
          h('p', {}, [h('strong', '名称: '), data.name]),
          h('p', {}, [h('strong', '智能体编号: '), data.agentCode]),
          h('p', {}, [h('strong', '描述: '), data.description]),
          h('p', {}, [h('strong', '钩子数量: '), data.hook.length]),
          h('p', {}, [h('strong', '启用记忆: '), data.enableMemory ? '是' : '否']),
          h('p', {}, [h('strong', 'Nacos 属性数量: '), (c.nacosProperties || []).length]),
          ...propRows
        ])
      } else {
        content = h('p', {}, 'A2A 配置暂无数据')
      }

      Modal.info({
        title: 'A2A 智能体详情',
        closable: true,
        icon: null,
        footer: null,
        style: { top: '50px' },
        width: 700,
        content
      })
    } catch (e) {
      console.error('加载 A2A 配置失败:', e)
    }
    return
  }

  Modal.info({
    title: '智能体详情',
    closable: true,
    icon: null,
    footer: null,
    style: {top: '50px'},
    width: 800,
    content: h('div', { style: { maxHeight: '800px', overflowY: 'auto' } }, [
      h('p', {}, [h('strong', '主智能体: '), data.used?.length ? data.used.join('、') : '无']),
      h('p', {}, [h('strong', '名称: '), data.name]),
      h('p', {}, [h('strong', '智能体编号: '), data.agentCode]),
      h('p', {}, [h('strong', '描述: '), data.description]),
      ...(data.tag ? [h('p', {}, [h('strong', '标签: '), data.tag])] : []),
      h('p', {}, [h('strong', '模型配置ID: '), data.modelConfigId]),
      h('p', {}, [h('strong', '系统提示词模板ID: '), data.systemPromptTemplateId]),
      h('p', {}, [h('strong', '随模板变化: '), data.followTemplate ? '是' : '否']),
      h('p', {}, [h('strong', '钩子数量: '), data.hook.length]),
      h('p', {}, [h('strong', '工具选择策略: '), data.toolChoiceStrategy]),
      h('p', {}, [h('strong', '工具数量: '), data.tool.length]),
      h('p', {}, [h('strong', '技能包数量: '), data.skill.length]),
      h('p', {}, [h('strong', '知识库数量: '), data.knowledgeBase.length]),
      h('p', {}, [h('strong', 'MCP数量: '), data.mcp.length]),
      h('p', {}, [h('strong', '子智能体数量: '), data.subAgent.length]),
      h('p', {}, [h('strong', '启用敏感词: '), data.sensitiveFilterEnabled ? '是' : '否']),
      h('p', {}, [h('strong', '启用计划: '), data.enablePlanning ? '是' : '否']),
      h('p', {}, [h('strong', '启用记忆: '), data.enableMemory ? '是' : '否']),
      h('p', {}, [h('strong', '结构化输出: '), data.structuredOutputEnabled ? '是' : '否'])
    ])
  })
}

/**
 * 处理编辑 — 根据类型打开对应表单
 *
 * @param id 智能体 ID
 */
async function handleEdit(id: string) {
  const response = await agentApi.detail(id)
  const data = response.data.data

  if (data.agentType === 'A2A') {
    currentData.value = data
    currentA2aType.value = undefined
    a2aFormVisible.value = true
    return
  }

  currentData.value = data
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
      content: `该智能体正在被 [ ${used.join('、')} ] 智能体引用，删除后可能会影响上述智能体的正常使用！`,
      okText: '确认并继续删除',
      onOk: async () => {
        await store.deleteConfig(id)
        store.resetAndFetch()
      }
    })
    return
  }

  Modal.confirm({
    title: '确认删除',
    content: '删除后无法恢复,是否继续?',
    onOk: async () => {
      await store.deleteConfig(id)
      store.resetAndFetch()
    }
  })
}

/**
 * 处理表单提交成功
 */
function handleFormSuccess() {
  store.resetAndFetch()
  store.fetchTags()
}

/**
 * 处理智能体类型切换
 */
function handleAgentTypeChange(value: string | null) {
  store.setAgentType(value)
}

/**
 * 处理标签切换
 */
function handleTagChange(value: string | null) {
  store.setTag(value)
}

/**
 * 处理搜索
 */
function handleSearch() {
  store.setKeyword(keyword.value)
}

/**
 * 访问智能体对话（新开页）
 */
function handleGoVisit(id: string) {
  const hash = `#/chat/${encodeURIComponent(id)}`
  const url = `${window.location.origin}${window.location.pathname}${hash}`
  window.open(url, '_blank')
}

/**
 * 智能体对话历史（新开页）
 */
function handleAccessLog(id: string) {
  const hash = `#/chat/history/${encodeURIComponent(id)}`
  const url = `${window.location.origin}${window.location.pathname}${hash}`
  window.open(url, '_blank')
}

/**
 * 处理查看架构图
 *
 * @param id 智能体 ID
 */
function handleArchitecture(id: string) {
  currentArchitectureId.value = id
  architectureVisible.value = true
}

/**
 * 处理定时任务配置
 *
 * @param id 智能体 ID
 */
async function handleTiming(id: string) {
  currentJobAgentId.value = id
  try {
    const response = await jobApi.getByBizId(id)
    currentJobInfo.value = response.data.data || null
  } catch {
    currentJobInfo.value = null
  }
  jobFormVisible.value = true
}

/**
 * 处理定时任务配置成功
 */
function handleJobFormSuccess() {
  jobFormVisible.value = false
  store.resetAndFetch()
}

/**
 * 处理状态
 */
async function handleEnable(id: string) {
  const response = await agentApi.detail(id)
  const { enabled } = response.data.data

  const used = await store.checkUsedWithAgent(id)
  if (used.length > 0 && enabled) {
    Modal.confirm({
      title: '二次确认',
      content: `该智能体正在被 [ ${used.join('、')} ] 智能体引用，禁用后可能会影响上述智能体的正常使用！`,
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
  store.fetchTags()
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
  <div class="agent-page">
    <section class="intro-section">
      <h3 class="intro-title">智能体管理</h3>
      <p class="intro-desc text-secondary">
        智能体是AI系统的核心组件,通过配置模型、工具、提示词、敏感词、技能包、MCP和知识库等多个维度,
        赋予智能体强大的感知、推理和执行能力。精心设计的智能体能够理解复杂需求、制定执行计划、
        调用外部工具、访问知识库,并以结构化的方式输出结果,真正实现从"对话助手"到"智能伙伴"的跨越。
      </p>
    </section>

    <section class="filter-section flex justify-between items-center">
      <div class="filter-left">
        <ASegmented
          v-model:value="selectedAgentType"
          :options="agentTypeOptions"
          @change="handleAgentTypeChange"
        />
      </div>

      <div class="filter-right flex items-center gap-md">
        <ASelect
          v-model:value="selectedTag"
          placeholder="选择标签"
          style="width: 200px; border: rgba(14,14,14,0.1) solid 1px !important; border-radius: 6px;"
          @change="handleTagChange"
        >
          <ASelectOption v-for="opt in tagOptions" :key="opt.value" :value="opt.value">
            {{ opt.label }}
          </ASelectOption>
        </ASelect>

        <AInput
          v-model:value="keyword"
          placeholder="搜索智能体名称"
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
        <CreateCard
          :agent-type="selectedAgentType"
          v-permission="['EDIT','ADMIN']"
          @create-custom="handleCreateCustom"
          @create-a2a="handleCreateA2a"
        />

        <AgentCard
          v-for="item in list"
          :key="item.id"
          :data="item"
          @view="handleView"
          @edit="handleEdit"
          @enable="handleEnable"
          @delete="handleDelete"
          @go-visit="handleGoVisit"
          @access-log="handleAccessLog"
          @architecture="handleArchitecture"
          @timing="handleTiming"
        />
      </div>

      <div v-if="loading" class="load-indicator mt-md">
        <span class="ml-sm text-secondary">加载中...</span>
      </div>

      <div v-if="!hasMore && list.length > 0" class="no-more-indicator text-secondary mt-md">
        没有更多数据了
      </div>

      <div v-if="!loading && list.length === 0" class="empty-indicator mt-lg">
        <AEmpty description="暂无数据" />
      </div>
    </section>

    <!-- 自定义智能体表单 -->
    <AgentForm
      v-model:visible="formVisible"
      :data="currentData"
      :tags="tags"
      @success="handleFormSuccess"
    />

    <!-- A2A 智能体表单 -->
    <AgentA2aForm
      v-model:visible="a2aFormVisible"
      :data="currentData"
      :a2a-type="currentA2aType"
      :tags="tags"
      @success="handleFormSuccess"
    />

    <!-- 架构图弹窗 -->
    <AModal
      v-model:open="architectureVisible"
      title="智能体架构图"
      :footer="null"
      wrap-class-name="full-modal"
      destroyOnClose
    >
      <AgentArchitectureDiagram
        v-if="currentArchitectureId"
        :agent-id="currentArchitectureId"
      />
    </AModal>

    <!-- 定时任务表单 -->
    <AgentJobForm
      v-model:visible="jobFormVisible"
      :agent-id="currentJobAgentId"
      :job-info="currentJobInfo"
      @success="handleJobFormSuccess"
    />
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/agent/index.scss' as *;
</style>
