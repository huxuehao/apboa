/**
 * MCP 服务管理主页面
 *
 * @author huxuehao
 */
<script setup lang="ts">
/* eslint-disable vue/multi-word-component-names */
import { computed, h, onActivated, onBeforeMount, ref, watch } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { CloudServerOutlined, SearchOutlined } from '@ant-design/icons-vue'
import { storeToRefs } from 'pinia'
import type { McpProtocol, McpServerVO, McpToolVO } from '@/types'
import { McpActivationStatus, McpFailureSource } from '@/types'
import * as mcpApi from '@/api/mcp'
import { useMcpStore } from '@/stores'
import { ApboaModalApi } from '@/components/common/ApboaModalApi.ts'
import ApboaInfiniteLoading from '@/components/common/ApboaInfiniteLoading.vue'
import CreateCard from '@/components/mcp/CreateCard.vue'
import McpCard from '@/components/mcp/McpCard.vue'
import McpForm from '@/components/mcp/McpForm.vue'
import { getMcpConnectionStatusText } from '@/composables/useMcpPresentation'

const store = useMcpStore()
const { list, selectedProtocol, keyword, loading, hasMore } = storeToRefs(store)

const formVisible = ref(false)
const currentData = ref<McpServerVO | undefined>(undefined)
const initialProtocol = ref<McpProtocol | undefined>(undefined)
const infiniteLoadingKey = ref(0)
const isFirstLoad = ref(true)
const hasActivatedOnce = ref(false)
const toolModalVisible = ref(false)
const toolLoading = ref(false)
const currentToolServer = ref<McpServerVO | undefined>(undefined)
const currentTools = ref<McpToolVO[]>([])

const currentProtocol = computed<McpProtocol | null>(() => selectedProtocol.value as McpProtocol | null)

const protocolOptions = [
  { label: '全部', value: null },
  { label: 'HTTP', value: 'HTTP' },
  { label: 'SSE', value: 'SSE' },
  { label: 'STDIO', value: 'STDIO' }
]

const toolGovernanceReadonly = computed(() => {
  return currentToolServer.value?.activationStatus === McpActivationStatus.FAILED
    && currentToolServer.value?.failureSource === McpFailureSource.RUNTIME_AUTO_DEGRADE
})

function connectionText(data: McpServerVO) {
  return getMcpConnectionStatusText(data)
}

function handleCreate(protocol: McpProtocol) {
  currentData.value = undefined
  initialProtocol.value = protocol
  formVisible.value = true
}

async function handleView(id: string) {
  const response = await mcpApi.detail(id)
  const data = response.data.data

  ApboaModalApi.open({
    title: 'MCP 服务详情',
    titleIcon: CloudServerOutlined,
    footer: null,
    content: h('div', {}, [
      h('p', {}, [h('strong', '关联智能体: '), data.used?.length ? data.used.join('、') : '无']),
      h('p', {}, [h('strong', '名称: '), data.name]),
      h('p', {}, [h('strong', '协议: '), data.protocol]),
      h('p', {}, [h('strong', '运行模式: '), data.mode === 'SYNC' ? '同步' : '异步']),
      h('p', {}, [h('strong', '是否启用: '), data.enabled ? '是' : '否']),
      h('p', {}, [h('strong', '超时时间: '), `${data.timeout}ms`]),
      h('p', {}, [h('strong', '自动降级失败次数: '), String(data.runtimeFailThreshold ?? 3)]),
      h('p', {}, [h('strong', '描述: '), data.description]),
      h('p', {}, [h('strong', '连接状态: '), connectionText(data)]),
      h('p', {}, [h('strong', '状态说明: '), data.activationMessage || '无']),
      h('p', {}, [h('strong', '失败来源: '), data.failureSource === McpFailureSource.RUNTIME_AUTO_DEGRADE ? '运行时自动降级' : '无']),
      h('p', {}, [h('strong', '工具总数: '), String(data.toolCount || 0)]),
      h('p', {}, [h('strong', '全局可用工具: '), String(data.availableToolCount || 0)]),
      h('p', {}, [h('strong', '待刷新: '), data.needsSync ? '是' : '否']),
      ...(data.activationStatusChangedAt ? [h('p', {}, [h('strong', '连接状态变更时间: '), data.activationStatusChangedAt])] : []),
      ...(data.lastActivationTime ? [h('p', {}, [h('strong', '上次连接时间: '), data.lastActivationTime])] : []),
      ...(data.lastToolSyncTime ? [h('p', {}, [h('strong', '上次工具刷新时间: '), data.lastToolSyncTime])] : []),
      h('p', {}, [h('strong', '健康状态: '), data.healthStatus]),
      ...(data.lastHealthCheck ? [h('p', {}, [h('strong', '上次健康检查时间: '), data.lastHealthCheck])] : []),
      h('p', {}, h('strong', '协议配置:')),
      h('pre', {
        style: {
          background: '#f5f5f5',
          padding: '12px',
          borderRadius: '4px'
        }
      }, JSON.stringify(data.protocolConfig, null, 2))
    ])
  })
}

async function handleEdit(id: string) {
  const response = await mcpApi.detail(id)
  currentData.value = response.data.data
  initialProtocol.value = undefined
  formVisible.value = true
}

function resetListAndRebuild() {
  list.value = []
  store.resetPagination()
  isFirstLoad.value = true
  infiniteLoadingKey.value++
}

function resetListState() {
  list.value = []
  store.resetPagination()
  isFirstLoad.value = true
}

async function handleDelete(id: string) {
  const used = await store.checkUsedWithAgent(id)
  if (used.length > 0) {
    Modal.confirm({
      title: '二次确认',
      content: `该 MCP 服务正在被 [ ${used.join('、')} ] 智能体引用，删除后可能会影响上述智能体的正常使用。`,
      okText: '确认并继续删除',
      onOk: async () => {
        await store.deleteConfig(id)
        resetListAndRebuild()
      }
    })
    return
  }

  Modal.confirm({
    title: '确认删除',
    content: '删除后无法恢复，是否继续？',
    onOk: async () => {
      await store.deleteConfig(id)
      resetListAndRebuild()
    }
  })
}

function handleFormSuccess() {
  resetListAndRebuild()
}

function handleSearch() {
  store.setKeyword(keyword.value)
}

async function handleEnable(id: string) {
  const response = await mcpApi.detail(id)
  const { enabled } = response.data.data

  const used = await store.checkUsedWithAgent(id)
  if (used.length > 0 && enabled) {
    Modal.confirm({
      title: '二次确认',
      content: `该 MCP 服务正在被 [ ${used.join('、')} ] 智能体引用，停用后可能会影响上述智能体的正常使用。`,
      okText: '确认并继续',
      onOk: async () => {
        await store.toggleEnabled(id, !enabled)
      }
    })
    return
  }

  await store.toggleEnabled(id, !enabled)
}

async function handleActivate(id: string) {
  const item = list.value.find(server => server.id === id)
  const actionLabel = item && item.activationStatus === McpActivationStatus.FAILED ? '重试连接' : '连接'
  await store.activateServer(id, actionLabel)
}

async function handleSync(id: string) {
  await store.syncServerTools(id, '刷新工具')
}

async function loadToolGovernance(id: string) {
  toolLoading.value = true
  try {
    const [detailRes, toolsRes] = await Promise.all([
      mcpApi.detail(id),
      mcpApi.listTools(id)
    ])
    currentToolServer.value = detailRes.data.data
    currentTools.value = toolsRes.data.data || []
    patchMcpItem(detailRes.data.data)
    toolModalVisible.value = true
  } finally {
    toolLoading.value = false
  }
}

async function handleToolGovernance(id: string) {
  await loadToolGovernance(id)
}

async function handleToolEnabledChange(tool: McpToolVO, enabled: boolean) {
  if (!currentToolServer.value) {
    return
  }
  const response = await mcpApi.updateToolsGlobalEnabled(
    currentToolServer.value.id as string,
    [tool.id as string],
    enabled
  )
  patchMcpItem(response.data.data)
  message.success('工具全局可用状态已更新')
  await loadToolGovernance(currentToolServer.value.id as string)
}

function patchMcpItem(next: McpServerVO) {
  const index = list.value.findIndex(item => item.id === next.id)
  if (index >= 0) {
    list.value[index] = next
  }
}

async function handleInfiniteLoading($state: {
  loaded: () => void
  complete: () => void
  error: () => void
}) {
  if (isFirstLoad.value) {
    isFirstLoad.value = false
    if (list.value.length > 0) {
      $state.loaded()
      return
    }
    try {
      await store.fetchPage(1)
      if (hasMore.value) {
        $state.loaded()
      } else {
        $state.complete()
      }
    } catch {
      isFirstLoad.value = true
      $state.error()
    }
    return
  }

  if (!hasMore.value || loading.value) {
    $state.complete()
    return
  }

  try {
    await store.loadMore()
    if (hasMore.value) {
      $state.loaded()
    } else {
      $state.complete()
    }
  } catch {
    $state.error()
  }
}

watch([selectedProtocol, keyword], () => {
  resetListAndRebuild()
})

onBeforeMount(() => {
  resetListState()
})

onActivated(() => {
  if (!hasActivatedOnce.value) {
    hasActivatedOnce.value = true
    return
  }
  resetListAndRebuild()
})
</script>

<template>
  <div class="mcp-page">
    <section class="intro-section">
      <h3 class="intro-title">MCP 管理</h3>
      <p class="intro-desc text-secondary">
        启用决定这个 MCP 是否允许参与系统；连接用于校验连通性并加载工具目录；刷新工具只在当前配置基础上重新获取工具目录。
      </p>
    </section>

    <section class="filter-section flex justify-between items-center">
      <div class="filter-left">
        <ASegmented
          v-model:value="selectedProtocol"
          :options="protocolOptions"
        />
      </div>

      <div class="filter-right">
        <AInput
          v-model:value="keyword"
          placeholder="搜索 MCP 服务名称"
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

    <section class="card-section">
      <div class="card-grid">
        <CreateCard :protocol="currentProtocol" @create="handleCreate" v-permission="['EDIT','ADMIN']" />

        <McpCard
          v-for="item in list"
          :key="item.id"
          :data="item"
          @view="handleView"
          @edit="handleEdit"
          @activate="handleActivate"
          @sync="handleSync"
          @toolGovernance="handleToolGovernance"
          @enable="handleEnable"
          @delete="handleDelete"
        />
      </div>

      <ApboaInfiniteLoading
        :loading-key="infiniteLoadingKey"
        @infinite="handleInfiniteLoading"
      />
    </section>

    <McpForm
      v-model:visible="formVisible"
      :data="currentData"
      :initial-protocol="initialProtocol"
      @success="handleFormSuccess"
    />

    <ApboaModal
      :open="toolModalVisible"
      title="工具治理"
      :footer="null"
      defaultWidth="860px"
      @cancel="toolModalVisible = false"
    >
      <div class="tool-governance">
        <div class="tool-governance-header">
          <div class="tool-governance-title">
            {{ currentToolServer?.name || 'MCP 工具' }}
          </div>
          <div class="text-placeholder text-xs">
            运行时只会注册“全局可用”且未消失的工具。
          </div>
          <AAlert
            v-if="toolGovernanceReadonly"
            type="warning"
            show-icon
            message="当前展示为上次缓存"
            description="该 MCP 因运行时自动降级而进入只读态，重新连接成功前只可查看工具目录，不能修改工具治理。"
          />
        </div>

        <ASpin :spinning="toolLoading">
          <AEmpty v-if="!currentTools.length" description="暂无工具目录" />
          <div v-else class="tool-list">
            <div
              v-for="tool in currentTools"
              :key="tool.id"
              class="tool-item"
            >
              <div class="tool-item-main">
                <div class="tool-item-name-row">
                  <div class="tool-item-name">{{ tool.toolName }}</div>
                  <ATag v-if="tool.missing" color="warning">已消失</ATag>
                  <ATag v-else-if="tool.enabled" color="success">全局可用</ATag>
                  <ATag v-else color="default">全局禁用</ATag>
                </div>
                <div class="tool-item-desc text-placeholder">
                  {{ tool.description || '暂无描述' }}
                </div>
              </div>
              <ASwitch
                :checked="tool.enabled"
                :disabled="toolLoading || toolGovernanceReadonly"
                checked-children="开"
                un-checked-children="关"
                @change="(checked: boolean) => handleToolEnabledChange(tool, checked)"
              />
            </div>
          </div>
        </ASpin>
      </div>
    </ApboaModal>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/mcp/index.scss' as *;

.tool-governance {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.tool-governance-header {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.tool-governance-title {
  font-size: 16px;
  font-weight: 600;
}

.tool-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 560px;
  overflow-y: auto;
}

.tool-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 12px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 8px;
}

.tool-item-main {
  min-width: 0;
  flex: 1;
}

.tool-item-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.tool-item-name {
  font-weight: 600;
}

.tool-item-desc {
  margin-top: 4px;
  line-height: 1.5;
}
</style>
