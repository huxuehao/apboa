/**
 * 智能体知识库与MCP表单组件
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { RoutePaths } from '@/router/constants.ts'
import * as knowledgeApi from '@/api/knowledge'
import * as mcpApi from '@/api/mcp'
import * as agentApi from '@/api/agent'
import type { KnowledgeBaseConfigVO, McpServerVO, AgentDefinitionVO } from '@/types'
import { countCommonElements } from '@/utils/tools'

/**
 * Props定义
 */
const props = defineProps<{
  modelValue: {
    knowledgeBase: string[]
    mcp: string[]
    subAgent: string[]
  }
  currentAgentId?: string
}>()

/**
 * Emits定义
 */
const emit = defineEmits<{
  'update:modelValue': [value: typeof props.modelValue]
}>()

const formRef = ref()
const loading = ref(false)

const allKnowledgeBases = ref<KnowledgeBaseConfigVO[]>([])
const allMcpServers = ref<McpServerVO[]>([])
const allAgents = ref<AgentDefinitionVO[]>([])

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
const knowledgeBaseTypes = computed(() => {
  return Object.keys(knowledgeBasesByType.value)
})

/**
 * 按协议分组的MCP服务器
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
 * MCP协议列表
 */
const mcpProtocols = computed(() => {
  return Object.keys(mcpServersByProtocol.value)
})

/**
 * 可选的子智能体列表(排除当前智能体)
 */
const availableAgents = computed(() => {
  return allAgents.value.filter(a => a.id !== props.currentAgentId)
})

/**
 * 加载所有知识库
 */
async function loadAllKnowledgeBases() {
  try {
    loading.value = true
    const response = await knowledgeApi.page({ page: 1, size: 1000, enabled: true })
    allKnowledgeBases.value = response.data.data.records || []
  } finally {
    loading.value = false
  }
}

/**
 * 加载所有MCP服务器
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

const handleKBChange = (kbId: string, checked: boolean) => {
  if (checked) {
    formData.value.knowledgeBase.push(kbId);
  } else {
    const index = formData.value.knowledgeBase.indexOf(kbId);
    if (index > -1) {
      formData.value.knowledgeBase.splice(index, 1);
    }
  }
};

const handleMcpChange = (mcpId: string, checked: boolean) => {
  if (checked) {
    formData.value.mcp.push(mcpId);
  } else {
    const index = formData.value.mcp.indexOf(mcpId);
    if (index > -1) {
      formData.value.mcp.splice(index, 1);
    }
  }
};

const handleSubAgentChange = (agentId: string, checked: boolean) => {
  if (checked) {
    formData.value.subAgent.push(agentId);
  } else {
    const index = formData.value.subAgent.indexOf(agentId);
    if (index > -1) {
      formData.value.subAgent.splice(index, 1);
    }
  }
};

onMounted(() => {
  loadAllKnowledgeBases()
  loadAllMcpServers()
  loadAllAgents()
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
                :checked="formData.knowledgeBase.includes(kb.id)"
                @change="(e: any) => handleKBChange(kb.id, e.target.checked)"
                :key="kb.id"
                :value="kb.id"
                class="checkbox-item"
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
          <AButton type="text">未配置知识库？</AButton>
          <AButton type="link" :href="`/#/${RoutePaths.KNOWLEDGE}`" target="_blank">去配置</AButton>
          <AButton type="link" @click="loadAllKnowledgeBases">刷新</AButton>
        </div>
      </AFormItem>

      <AFormItem label="MCP服务器">
        <ACollapse v-if="mcpProtocols?.length > 0">
          <ACollapsePanel
            v-for="protocol in mcpProtocols"
            :key="protocol"
            :header="`${protocol}（${countCommonElements(mcpServersByProtocol[protocol]?.map(i => i.id) || [], formData.mcp)}/${mcpServersByProtocol[protocol]?.length}）`">
            <div class="checkbox-grid">
              <ACheckbox
                v-for="mcp in mcpServersByProtocol[protocol]"
                :checked="formData.mcp.includes(mcp.id)"
                @change="(e: any) => handleMcpChange(mcp.id, e.target.checked)"
                :key="mcp.id"
                :value="mcp.id"
                class="checkbox-item"
              >
                <div class="item-info">
                  <div class="item-name text-ellipsis" :title="mcp.name">{{ mcp.name }}</div>
                  <div class="item-desc text-placeholder text-xs text-ellipsis" :title="mcp.description">{{ mcp.description }}</div>
                </div>
              </ACheckbox>
            </div>
          </ACollapsePanel>
        </ACollapse>
        <div v-else class="text-placeholder mt-xs">
          <AButton type="text">未配置MCP服务？</AButton>
          <AButton type="link" :href="`/#/${RoutePaths.MCP}`" target="_blank">去配置</AButton>
          <AButton type="link" @click="loadAllMcpServers">刷新</AButton>
        </div>
      </AFormItem>

      <AFormItem label="子智能体">
        <template v-if="availableAgents?.length > 0">
          <div class="checkbox-grid">
            <ACheckbox
              v-for="agent in availableAgents"
              :checked="formData.subAgent.includes(agent.id)"
              :key="agent.id"
              :value="agent.id"
              @change="(e: any) => handleSubAgentChange(agent.id, e.target.checked)"
              class="checkbox-item"
            >
              <div class="item-info">
                <div class="item-name text-ellipsis" :title="agent.name">{{ agent.name }}</div>
                <div class="item-desc text-placeholder text-xs text-ellipsis" :title="agent.description">
                  <span style="color: #0F74FF">{{ agent.agentType == 'CUSTOM'? '自定义': 'A2A' }}</span> &nbsp;
                  <span>{{ agent.description }}</span>
                </div>
              </div>
            </ACheckbox>
          </div>
          <div class="text-placeholder text-xs mt-sm">
            选中的智能体，将会被当前智能体作为工具进行使用。
          </div>
        </template>
        <div v-else class="text-placeholder mt-xs">
          <AButton type="text">未添加子智能体？</AButton>
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
  width:300px;
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
    width:250px;
  }

  .item-desc {
    line-height: 1.4;
  }
}

.text-ellipsis {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  width:250px;
}
</style>
