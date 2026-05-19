/**
 * 智能体知识库与 MCP 表单组件
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RoutePaths } from '@/router/constants.ts'
import * as knowledgeApi from '@/api/knowledge'
import * as mcpApi from '@/api/mcp'
import * as agentApi from '@/api/agent'
import type {
  KnowledgeBaseConfigVO,
  McpServerVO,
  AgentDefinitionVO,
  AgentMcpBindingVO,
  McpToolVO
} from '@/types'
import { McpActivationStatus, McpToolExposureMode } from '@/types'
import { countCommonElements } from '@/utils/tools'
import {
  getMcpConnectionStatusColor,
  getMcpConnectionStatusText,
  getMcpUnavailableReason
} from '@/composables/useMcpPresentation'

interface KnowledgeFormModel {
  knowledgeBase: string[]
  mcp: string[]
  mcpBindings: AgentMcpBindingVO[]
  subAgent: string[]
}

/**
 * Props 定义
 */
const props = defineProps<{
  modelValue: KnowledgeFormModel
  currentAgentId?: string | number | undefined
}>()

/**
 * Emits 定义
 */
const emit = defineEmits<{
  'update:modelValue': [value: KnowledgeFormModel]
}>()

const formRef = ref()
const loading = ref(false)
const toolLoadingMap = ref<Record<string, boolean>>({})

const allKnowledgeBases = ref<KnowledgeBaseConfigVO[]>([])
const allMcpServers = ref<McpServerVO[]>([])
const allAgents = ref<AgentDefinitionVO[]>([])
const mcpToolMap = ref<Record<string, McpToolVO[]>>({})

/**
 * 表单数据
 */
const formData = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

/**
 * 按类型分组的知识库
 */
const knowledgeBasesByType = computed(() => {
  const groups: Record<string, KnowledgeBaseConfigVO[]> = {}
  allKnowledgeBases.value.forEach(kb => {
    const type = kb.kbType
    if (!groups[type]) {
      groups[type] = []
    }
    groups[type].push(kb)
  })
  return groups
})

/**
 * 知识库类型列表
 */
const knowledgeBaseTypes = computed(() => Object.keys(knowledgeBasesByType.value))

/**
 * 按协议分组的 MCP 服务
 */
const mcpServersByProtocol = computed(() => {
  const groups: Record<string, McpServerVO[]> = {}
  allMcpServers.value.forEach(mcp => {
    const protocol = mcp.protocol
    if (!groups[protocol]) {
      groups[protocol] = []
    }
    groups[protocol].push(mcp)
  })
  return groups
})

/**
 * MCP 协议列表
 */
const mcpProtocols = computed(() => Object.keys(mcpServersByProtocol.value))

/**
 * 可选的子智能体列表
 */
const availableAgents = computed(() => {
  return allAgents.value.filter(a => a.id !== props.currentAgentId)
})

/**
 * 已选 MCP ID 集合
 */
const selectedMcpIds = computed(() => {
  return new Set((formData.value.mcpBindings || []).map(item => String(item.mcpServerId)))
})

/**
 * 暴露模式选项
 */
const exposureModeOptions = [
  { label: '继承全局', value: McpToolExposureMode.ALL_GLOBAL },
  { label: '局部选择', value: McpToolExposureMode.SELECTED_ONLY }
]

function ensureMcpBindings() {
  if (!Array.isArray(formData.value.mcpBindings)) {
    formData.value.mcpBindings = []
  }
  if (!Array.isArray(formData.value.mcp)) {
    formData.value.mcp = []
  }

  if (formData.value.mcpBindings.length === 0 && formData.value.mcp.length > 0) {
    formData.value.mcpBindings = [...new Set(formData.value.mcp)].map(id => ({
      mcpServerId: id,
      exposureMode: McpToolExposureMode.ALL_GLOBAL,
      mcpToolIds: []
    }))
  }

  syncLegacyMcpIds()
}

function syncLegacyMcpIds() {
  formData.value.mcp = (formData.value.mcpBindings || []).map(item => String(item.mcpServerId))
}

function getBinding(mcpId: string): AgentMcpBindingVO | undefined {
  return (formData.value.mcpBindings || []).find(item => String(item.mcpServerId) === mcpId)
}

function isMcpSelected(mcpId: string) {
  return selectedMcpIds.value.has(mcpId)
}

function isMcpRuntimeAvailable(mcp: McpServerVO) {
  return Boolean(mcp.enabled)
    && mcp.activationStatus === McpActivationStatus.ACTIVE
    && (mcp.availableToolCount || 0) > 0
}

function isMcpSelectable(mcp: McpServerVO) {
  return isMcpRuntimeAvailable(mcp) || isMcpSelected(String(mcp.id))
}

function getMcpTools(mcpId: string) {
  return mcpToolMap.value[mcpId] || []
}

function isToolLoading(mcpId: string) {
  return Boolean(toolLoadingMap.value[mcpId])
}

function isToolAvailable(tool: McpToolVO) {
  return Boolean(tool.enabled) && !tool.missing
}

function isToolSelected(mcpId: string, toolId: string) {
  return getBinding(mcpId)?.mcpToolIds?.includes(toolId) || false
}

function isToolSelectable(mcpId: string, tool: McpToolVO) {
  return isToolAvailable(tool) || isToolSelected(mcpId, String(tool.id))
}

async function loadMcpTools(mcpId: string) {
  if (mcpToolMap.value[mcpId]) {
    return
  }
  toolLoadingMap.value[mcpId] = true
  try {
    const response = await mcpApi.listTools(mcpId)
    mcpToolMap.value[mcpId] = response.data.data || []
  } finally {
    toolLoadingMap.value[mcpId] = false
  }
}

async function preloadSelectedMcpTools() {
  const targets = (formData.value.mcpBindings || [])
    .filter(item => item.exposureMode === McpToolExposureMode.SELECTED_ONLY)
    .map(item => String(item.mcpServerId))
  await Promise.all(targets.map(id => loadMcpTools(id)))
}

/**
 * 加载所有知识库
 */
async function loadAllKnowledgeBases() {
  try {
    loading.value = true
    const response = await knowledgeApi.page({ page: 1, size: 1000, enabled: true })
    allKnowledgeBases.value = response.data.data.records.filter(item => item.enabled) || []
  } finally {
    loading.value = false
  }
}

/**
 * 加载所有 MCP 服务
 */
async function loadAllMcpServers() {
  const response = await mcpApi.page({ page: 1, size: 1000 })
  allMcpServers.value = response.data.data.records || []
}

/**
 * 加载所有智能体
 */
async function loadAllAgents() {
  const response = await agentApi.page({ page: 1, size: 1000, enabled: true })
  allAgents.value = response.data.data.records || []
}

/**
 * 验证表单
 */
async function validate(): Promise<boolean> {
  try {
    await formRef.value?.validate()
    return true
  } catch {
    return false
  }
}

function handleKBChange(kbId: string, checked: boolean) {
  if (checked) {
    formData.value.knowledgeBase.push(kbId)
  } else {
    const index = formData.value.knowledgeBase.indexOf(kbId)
    if (index > -1) {
      formData.value.knowledgeBase.splice(index, 1)
    }
  }
}

function handleMcpChange(mcpId: string, checked: boolean) {
  const bindings = formData.value.mcpBindings || []
  if (checked) {
    if (!bindings.some(item => String(item.mcpServerId) === mcpId)) {
      bindings.push({
        mcpServerId: mcpId,
        exposureMode: McpToolExposureMode.ALL_GLOBAL,
        mcpToolIds: []
      })
    }
  } else {
    formData.value.mcpBindings = bindings.filter(item => String(item.mcpServerId) !== mcpId)
  }
  syncLegacyMcpIds()
}

async function handleExposureModeChange(mcpId: string, exposureMode: McpToolExposureMode) {
  const binding = getBinding(mcpId)
  if (!binding) {
    return
  }
  binding.exposureMode = exposureMode
  if (exposureMode === McpToolExposureMode.SELECTED_ONLY) {
    await loadMcpTools(mcpId)
  }
}

function handleMcpToolChange(mcpId: string, toolId: string, checked: boolean) {
  const binding = getBinding(mcpId)
  if (!binding) {
    return
  }
  if (!Array.isArray(binding.mcpToolIds)) {
    binding.mcpToolIds = []
  }

  if (checked) {
    if (!binding.mcpToolIds.includes(toolId)) {
      binding.mcpToolIds.push(toolId)
    }
  } else {
    binding.mcpToolIds = binding.mcpToolIds.filter(id => id !== toolId)
  }
}

function handleSubAgentChange(agentId: string, checked: boolean) {
  if (checked) {
    formData.value.subAgent.push(agentId)
  } else {
    const index = formData.value.subAgent.indexOf(agentId)
    if (index > -1) {
      formData.value.subAgent.splice(index, 1)
    }
  }
}

onMounted(async () => {
  ensureMcpBindings()
  await Promise.all([
    loadAllKnowledgeBases(),
    loadAllMcpServers(),
    loadAllAgents()
  ])
  await preloadSelectedMcpTools()
})

defineExpose({
  validate
})
</script>

<template>
  <ASpin :spinning="loading">
    <AForm ref="formRef" :model="formData" layout="vertical">
      <AFormItem label="知识库">
        <ACollapse v-if="knowledgeBaseTypes?.length > 0">
          <ACollapsePanel
            v-for="type in knowledgeBaseTypes"
            :key="type"
            :header="`${type}（${countCommonElements(knowledgeBasesByType[type]?.map(i => i.id) || [], formData.knowledgeBase)}/${knowledgeBasesByType[type]?.length}）`">
            <div class="checkbox-grid">
              <ACheckbox
                v-for="kb in knowledgeBasesByType[type]"
                :key="kb.id"
                :checked="formData.knowledgeBase.includes(kb.id as string)"
                class="checkbox-item"
                @change="(e: any) => handleKBChange(kb.id as string, e.target.checked)"
              >
                <div class="item-info">
                  <div class="item-name text-ellipsis" :title="kb.name">{{ kb.name }}</div>
                  <div class="item-desc text-placeholder text-xs text-ellipsis" :title="kb.description">{{ kb.description }}</div>
                </div>
              </ACheckbox>
            </div>
          </ACollapsePanel>
        </ACollapse>
        <div v-else class="text-placeholder mt-xs">
          <AButton type="text">未配置知识库</AButton>
          <AButton type="link" :href="`/#/${RoutePaths.KNOWLEDGE}`" target="_blank">去配置</AButton>
          <AButton type="link" @click="loadAllKnowledgeBases">刷新</AButton>
        </div>
      </AFormItem>

      <AFormItem label="MCP 服务">
        <ACollapse v-if="mcpProtocols?.length > 0">
          <ACollapsePanel
            v-for="protocol in mcpProtocols"
            :key="protocol"
            :header="`${protocol}（${countCommonElements(mcpServersByProtocol[protocol]?.map(i => i.id) || [], formData.mcp)}/${mcpServersByProtocol[protocol]?.length}）`">
            <div class="mcp-grid">
              <div
                v-for="mcp in mcpServersByProtocol[protocol]"
                :key="mcp.id"
                class="mcp-item"
                :class="{
                  selected: isMcpSelected(mcp.id as string),
                  unavailable: !isMcpRuntimeAvailable(mcp)
                }"
              >
                <div class="mcp-item-header">
                  <ACheckbox
                    :checked="isMcpSelected(mcp.id as string)"
                    :disabled="!isMcpSelectable(mcp)"
                    @change="(e: any) => handleMcpChange(mcp.id as string, e.target.checked)"
                  >
                    <div class="item-info">
                      <div class="item-name text-ellipsis" :title="mcp.name">{{ mcp.name }}</div>
                      <div class="item-desc text-placeholder text-xs" :title="mcp.description">{{ mcp.description }}</div>
                    </div>
                  </ACheckbox>
                    <div class="mcp-tag-group">
                      <ATag color="default" :bordered="false">{{ mcp.protocol }}</ATag>
                      <ATag :bordered="false" :color="getMcpConnectionStatusColor(mcp)">
                        {{ getMcpConnectionStatusText(mcp) }}
                      </ATag>
                      <ATag :bordered="false" color="processing">全局可用 {{ mcp.availableToolCount || 0 }}</ATag>
                      <ATag v-if="!isMcpRuntimeAvailable(mcp)" :bordered="false" color="warning">
                        {{ getMcpUnavailableReason(mcp) }}
                    </ATag>
                  </div>
                </div>

                <div
                  v-if="isMcpSelected(mcp.id as string)"
                  class="mcp-binding-panel"
                >
                  <div class="binding-row">
                    <span class="binding-label">工具暴露</span>
                    <ASegmented
                      :value="getBinding(mcp.id as string)?.exposureMode || McpToolExposureMode.ALL_GLOBAL"
                      :options="exposureModeOptions"
                      size="small"
                      @change="(value: McpToolExposureMode) => handleExposureModeChange(mcp.id as string, value)"
                    />
                  </div>

                  <div
                    v-if="!isMcpRuntimeAvailable(mcp)"
                    class="binding-tip warning"
                  >
                    当前绑定会保留用于展示，但运行时不会注册该 MCP 的工具。
                  </div>

                  <template v-if="getBinding(mcp.id as string)?.exposureMode === McpToolExposureMode.SELECTED_ONLY">
                    <div class="binding-tip">
                      仅注册下方勾选、且当前仍全局可用的工具。
                    </div>
                    <ASpin :spinning="isToolLoading(mcp.id as string)">
                      <AButton
                        v-if="!mcpToolMap[mcp.id as string]"
                        type="link"
                        class="tool-load-action"
                        @click="loadMcpTools(mcp.id as string)"
                      >
                        加载工具目录
                      </AButton>
                      <AEmpty
                        v-else-if="!getMcpTools(mcp.id as string).length"
                        description="暂无工具目录"
                      />
                      <div v-else class="tool-grid">
                        <div
                          v-for="tool in getMcpTools(mcp.id as string)"
                          :key="tool.id"
                          class="tool-item"
                          :class="{ unavailable: !isToolAvailable(tool) }"
                        >
                          <ACheckbox
                            :checked="isToolSelected(mcp.id as string, tool.id as string)"
                            :disabled="!isToolSelectable(mcp.id as string, tool)"
                            @change="(e: any) => handleMcpToolChange(mcp.id as string, tool.id as string, e.target.checked)"
                          >
                          <div class="item-info">
                            <div class="item-name text-ellipsis tool-item-name" :title="tool.toolName">
                              {{ tool.toolName }}
                            </div>
                            <div class="item-desc text-placeholder text-xs text-ellipsis tool-item-desc" :title="tool.description">
                              {{ tool.description || '暂无描述' }}
                            </div>
                          </div>
                          </ACheckbox>
                        </div>
                      </div>
                    </ASpin>
                  </template>
                </div>
              </div>
            </div>
          </ACollapsePanel>
        </ACollapse>
        <div v-else class="text-placeholder mt-xs">
          <AButton type="text">未配置 MCP 服务</AButton>
          <AButton type="link" :href="`/#/${RoutePaths.MCP}`" target="_blank">去配置</AButton>
          <AButton type="link" @click="loadAllMcpServers">刷新</AButton>
        </div>
      </AFormItem>

      <AFormItem label="子智能体">
        <template v-if="availableAgents?.length > 0">
          <div class="checkbox-grid">
            <ACheckbox
              v-for="agent in availableAgents"
              :key="agent.id"
              :checked="formData.subAgent.includes(agent.id as string)"
              class="checkbox-item"
              @change="(e: any) => handleSubAgentChange(agent.id as string, e.target.checked)"
            >
              <div class="item-info">
                <div class="item-name text-ellipsis" :title="agent.name">{{ agent.name }}</div>
                <div class="item-desc text-placeholder text-xs text-ellipsis" :title="agent.description">
                  <span style="color: #0F74FF">{{ agent.agentType === 'CUSTOM' ? '自定义' : 'A2A' }}</span>
                  <span>&nbsp;{{ agent.description }}</span>
                </div>
              </div>
            </ACheckbox>
          </div>
          <div class="text-placeholder text-xs mt-sm">
            选中的智能体将作为当前智能体的可调用子智能体。
          </div>
        </template>
        <div v-else class="text-placeholder mt-xs">
          <AButton type="text">未添加子智能体</AButton>
          <AButton type="link" :href="`/#/${RoutePaths.AGENT}`" target="_blank">去添加</AButton>
          <AButton type="link" @click="loadAllAgents">刷新</AButton>
        </div>
      </AFormItem>
    </AForm>
  </ASpin>
</template>

<style scoped lang="scss">
.checkbox-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: var(--spacing-sm);
}

.checkbox-item {
  padding: var(--spacing-sm);
  border: 1px solid var(--color-border-base);
  border-radius: var(--border-radius-md);
  margin: 0 !important;
  width: 300px;
  transition: all var(--transition-base);

  &:hover {
    border-color: var(--color-primary);
    background-color: var(--color-bg-light);
  }
}

.item-info {
  .item-name {
    font-weight: 500;
    margin-bottom: 4px;
    width: 250px;
  }

  .item-desc {
    line-height: 1.4;
  }
}

.mcp-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.mcp-item {
  border: 1px solid var(--color-border-base);
  border-radius: 8px;
  padding: 12px;
  transition: all var(--transition-base);

  &.selected {
    border-color: rgba(15, 116, 255, 0.4);
    background: rgba(15, 116, 255, 0.03);
  }

  &.unavailable {
    background: rgba(15, 23, 42, 0.02);
  }
}

.mcp-item-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.mcp-tag-group {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.mcp-binding-panel {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed rgba(15, 23, 42, 0.08);
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.binding-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.binding-label {
  font-weight: 500;
}

.binding-tip {
  font-size: 12px;
  color: var(--color-text-secondary);

  &.warning {
    color: #d48806;
  }
}

.tool-load-action {
  padding-left: 0;
}

.tool-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 12px;
}

.tool-item {
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 8px;
  padding: 10px;
  width: 270px;

  &.unavailable {
    background: rgba(15, 23, 42, 0.02);
  }
}

.tool-item-name {
  font-weight: 500;
  width: 220px !important;
}

.tool-item-desc {
   width: 220px !important;
}

.text-ellipsis {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  width: 250px;
}
</style>
