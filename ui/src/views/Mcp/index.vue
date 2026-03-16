/**
 * MCP服务器管理主页面
 *
 * @author huxuehao
 */
<script setup lang="ts">
/* eslint-disable vue/multi-word-component-names */
import { onMounted, ref, onUnmounted, h, computed } from 'vue'
import { Modal } from 'ant-design-vue'
import { CloudServerOutlined, SearchOutlined } from '@ant-design/icons-vue'
import { useMcpStore } from '@/stores'
import { storeToRefs } from 'pinia'
import * as mcpApi from '@/api/mcp'
import type { McpServerVO, McpProtocol } from '@/types'
import McpCard from '@/components/mcp/McpCard.vue'
import CreateCard from '@/components/mcp/CreateCard.vue'
import McpForm from '@/components/mcp/McpForm.vue'
import {ApboaModalApi} from "@/components/common/ApboaModalApi.ts";

const store = useMcpStore()
const { list, selectedProtocol, keyword, loading, hasMore } = storeToRefs(store)

const formVisible = ref<boolean>(false)
const currentData = ref<McpServerVO | undefined>(undefined)
const initialProtocol = ref<McpProtocol | undefined>(undefined)
const scrollContainer = ref<HTMLElement>()
const loadMoreObserver = ref<IntersectionObserver>()

/**
 * 当前选中的协议(类型转换)
 */
const currentProtocol = computed<McpProtocol | null>(() => selectedProtocol.value as McpProtocol | null)

/**
 * 协议类型选项
 */
const protocolOptions = [
  { label: '全部', value: null },
  { label: 'HTTP', value: 'HTTP' },
  { label: 'SSE', value: 'SSE' },
  { label: 'STDIO', value: 'STDIO' }
]

/**
 * 处理新增
 */
function handleCreate(protocol: McpProtocol) {
  currentData.value = undefined
  initialProtocol.value = protocol
  formVisible.value = true
}

/**
 * 处理查看
 */
async function handleView(id: string) {
  const response = await mcpApi.detail(id)
  const data = response.data.data

  ApboaModalApi.open({
    title: 'MCP服务器详情',
    titleIcon: CloudServerOutlined,
    footer: null,
    content: h('div', {}, [
      h('p', {}, [h('strong', '关联智能体: '), data.used?.length ? data.used.join('、') : '无']),
      h('p', {}, [h('strong', '名称: '), data.name]),
      h('p', {}, [h('strong', '协议: '), data.protocol]),
      h('p', {}, [h('strong', '运行模式: '), data.mode === 'SYNC' ? '同步' : '异步']),
      h('p', {}, [h('strong', '超时时间: '), `${data.timeout}ms`]),
      h('p', {}, [h('strong', '描述: '), data.description]),
      h('p', {}, h('strong', '协议配置:')),
      h('pre', {
        style: {
          background: '#f5f5f5',
          padding: '12px',
          borderRadius: '4px',
        }
      }, JSON.stringify(data.protocolConfig, null, 2)),
      h('p', {}, [h('strong', '健康状态: '), data.healthStatus]),
      ...(data.lastHealthCheck ? [h('p', {}, [h('strong', '最后健康检查: '), data.lastHealthCheck])] : [])
    ])
  })
}

/**
 * 处理编辑
 */
async function handleEdit(id: string) {
  const response = await mcpApi.detail(id)
  currentData.value = response.data.data
  initialProtocol.value = undefined
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
      content: `该MCP服务正在被 [ ${used.join('、')} ] 智能体引用，删除后可能会影响上述智能体的正常使用！`,
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
 * 处理协议类型切换
 */
function handleProtocolChange(value: string | null) {
  store.setProtocol(value)
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
  const response = await mcpApi.detail(id)
  const { enabled } = response.data.data

  const used = await store.checkUsedWithAgent(id)
  if (used.length > 0 && enabled) {
    Modal.confirm({
      title: '二次确认',
      content: `该MCP服务正在被 [ ${used.join('、')} ] 智能体引用，禁用后可能会影响上述智能体的正常使用！`,
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
  <div class="mcp-page">
    <section class="intro-section">
      <h3 class="intro-title">MCP管理</h3>
      <p class="intro-desc text-secondary">
        MCP管理是智能体的“外部能力接入中枢”，通过标准化协议将异构的外部工具、服务、数据源和安全沙箱统一封装为智能体可理解和调用的能力，实现“思考-行动”闭环，让智能体真正具备操作现实世界的能力。
      </p>
    </section>

    <section class="filter-section flex justify-between items-center">
      <div class="filter-left">
        <ASegmented
          v-model:value="selectedProtocol"
          :options="protocolOptions"
          @change="handleProtocolChange"
        />
      </div>

      <div class="filter-right">
        <AInput
          v-model:value="keyword"
          placeholder="搜索MCP服务器名称"
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
        <CreateCard :protocol="currentProtocol" @create="handleCreate" v-permission="['EDIT','ADMIN']" />

        <McpCard
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
        <span class="ml-sm text-secondary">加载中...</span>
      </div>

      <div v-if="!hasMore && list.length > 0" class="no-more-indicator text-secondary mt-md">
        没有更多数据了
      </div>

      <div v-if="!loading && list.length === 0" class="empty-indicator mt-lg">
        <AEmpty description="暂无数据" />
      </div>
    </section>

    <McpForm
      v-model:visible="formVisible"
      :data="currentData"
      :initial-protocol="initialProtocol"
      @success="handleFormSuccess"
    />
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/mcp/index.scss' as *;
</style>
